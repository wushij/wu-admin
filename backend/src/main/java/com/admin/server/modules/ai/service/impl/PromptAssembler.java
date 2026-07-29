package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.modules.ai.util.AiPromptTemplates;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.dept.DeptService;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.admin.server.modules.system.service.permission.event.UserPermissionChangedEvent;
import com.admin.server.modules.system.service.role.RoleService;
import com.admin.server.modules.system.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * L1 系统提示词组装器（设计方案 §3）
 * <p>
 * 三段式拼接：人设排版规范 → 全局项目知识(<project_knowledge>) → 用户可见菜单(<menu_structure>)
 * → 用户上下文(<user_context>) → 模型自定义 systemPrompt → 注入防护声明。
 * 用户上下文与菜单按 userId 走 Redis 缓存（10 分钟 + 权限变更事件主动失效）；
 * 全程只读且任何异常都不阻断对话主流程（降级为缺省片段）。
 * </p>
 */
@Component
public class PromptAssembler {

    private static final Logger log = LoggerFactory.getLogger(PromptAssembler.class);

    /** 用户上下文缓存键前缀（值为 JSON: ctx/menu/menuTop） */
    private static final String CACHE_KEY_PREFIX = "ai:user_context:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    /** ②知识+③菜单+④上下文 合计字符预算（约 2000 token，设计 §3.1 容量红线） */
    private static final int CONTEXT_CHAR_BUDGET = 3000;
    /** 用户上下文单段上限（优先级最高，但本身必须精简） */
    private static final int USER_CTX_MAX_CHARS = 400;

    @Resource
    private SystemConfigHelper systemConfigHelper;
    @Resource
    private SystemMenuKnowledgeProvider menuKnowledgeProvider;
    @Resource
    private UserService userService;
    @Resource
    private DeptService deptService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private KnowledgeRetriever knowledgeRetriever;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 组装完整 system 提示词；userId 无效时仅返回人设+全局知识（游客兜底，当前入口要求登录）
     *
     * @param modelSystemPrompt 模型级自定义提示词（可空，作为补充拼接在上下文之后）
     * @param source            请求来源 pc/mobile
     * @param latestUserQuery   本轮用户最新提问（用于 L2 知识库检索，可空）
     */
    public String assemble(Long userId, String modelSystemPrompt, String source, String latestUserQuery) {
        StringBuilder sb = new StringBuilder(AiPromptTemplates.PERSONA);
        if ("focus".equals(systemConfigHelper.getAiAnswerScope())) {
            sb.append(AiPromptTemplates.FOCUS_SCOPE_RULE);
        }

        // ---- ②③④ 段按容量红线组装：用户上下文 > 全局知识 > 菜单结构（设计 §3.1） ----
        JSONObject cached = loadUserBlocks(userId, source);
        String userCtx = cached.getStr("ctx", "");
        String menuFull = cached.getStr("menu", "");
        String menuTop = cached.getStr("menuTop", "");
        String knowledge = safeGlobalKnowledge();

        int budget = CONTEXT_CHAR_BUDGET;
        userCtx = StrUtil.brief(userCtx, USER_CTX_MAX_CHARS);
        budget -= userCtx.length();

        if (knowledge.length() > budget) {
            log.warn("AI 全局知识块超出容量预算被截断: {} -> {} chars，建议精简 ai.globalKnowledge 或下沉知识库", knowledge.length(), budget);
            knowledge = StrUtil.brief(knowledge, Math.max(budget, 0));
        }
        budget -= knowledge.length();

        String menu = menuFull;
        if (menu.length() > budget) {
            // 降级一：仅顶级菜单；降级二：整块舍弃
            menu = menuTop.length() <= budget ? menuTop : "";
            log.warn("AI 菜单知识超出剩余预算，已降级为{}（full={} chars, budget={}）",
                    menu.isEmpty() ? "舍弃" : "仅顶级", menuFull.length(), budget);
        }

        if (StrUtil.isNotBlank(knowledge)) {
            sb.append("\n<project_knowledge>\n").append(knowledge).append("\n</project_knowledge>\n");
        }
        if (StrUtil.isNotBlank(menu)) {
            sb.append("\n<menu_structure>\n当前用户在系统中可见的功能菜单（回答功能类问题以此为准，不要编造其它模块）：\n")
                    .append(menu).append("\n</menu_structure>\n");
        }
        if (StrUtil.isNotBlank(userCtx)) {
            sb.append("\n<user_context>\n").append(userCtx).append("\n</user_context>\n");
        }
        // ---- L2 知识库检索注入（设计 §4）：依本轮最新提问检索 reference_data，失败不阻断 ----
        String referenceBlock = safeRetrieve(latestUserQuery);
        if (StrUtil.isNotBlank(referenceBlock)) {
            sb.append('\n').append(referenceBlock).append('\n');
        }
        // 模型级自定义提示词作为补充（语气/领域微调），位于全局知识与上下文之后
        if (StrUtil.isNotBlank(modelSystemPrompt)) {
            sb.append('\n').append(modelSystemPrompt.trim()).append('\n');
        }
        sb.append(AiPromptTemplates.INJECTION_GUARD);
        return sb.toString();
    }

    /** L2 检索兜底：任何异常都不阻断对话 */
    private String safeRetrieve(String latestUserQuery) {
        if (StrUtil.isBlank(latestUserQuery)) {
            return "";
        }
        try {
            return knowledgeRetriever.retrieveReferenceBlock(latestUserQuery);
        } catch (Exception e) {
            log.warn("AI 知识库检索异常，本轮跳过知识注入: {}", e.getMessage());
            return "";
        }
    }

    // ------------------------------------------------------------------
    // 用户上下文/菜单 缓存
    // ------------------------------------------------------------------

    /** 读取（或构建并缓存）用户上下文与菜单块；任何异常降级为空 JSON，不阻断对话 */
    private JSONObject loadUserBlocks(Long userId, String source) {
        if (userId == null || userId <= 0) {
            return new JSONObject();
        }
        String cacheKey = CACHE_KEY_PREFIX + userId;
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(cachedJson)) {
                JSONObject json = JSONUtil.parseObj(cachedJson);
                json.set("ctx", appendSource(json.getStr("ctx", ""), source));
                return json;
            }
        } catch (Exception e) {
            log.warn("AI 用户上下文缓存读取失败，降级实时构建: {}", e.getMessage());
        }

        JSONObject blocks = new JSONObject()
                .set("ctx", buildUserContext(userId))
                .set("menu", safeBuild(() -> menuKnowledgeProvider.buildMenuKnowledge(userId)))
                .set("menuTop", safeBuild(() -> menuKnowledgeProvider.buildTopLevelOnly(userId)));
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, blocks.toString(), CACHE_TTL);
        } catch (Exception e) {
            log.warn("AI 用户上下文缓存写入失败（不影响本轮对话）: {}", e.getMessage());
        }
        JSONObject result = JSONUtil.parseObj(blocks.toString());
        result.set("ctx", appendSource(result.getStr("ctx", ""), source));
        return result;
    }

    /** 构建用户上下文（昵称/账号/角色/部门），逐项降级 */
    private String buildUserContext(Long userId) {
        List<String> lines = new ArrayList<>();
        try {
            UserDO user = userService.getDetail(userId);
            if (user != null) {
                lines.add("当前用户：" + StrUtil.blankToDefault(user.getNickname(), user.getUsername())
                        + " (" + user.getUsername() + ")");
                String deptName = resolveDeptName(user.getDeptId());
                if (StrUtil.isNotBlank(deptName)) {
                    lines.add("所属部门：" + deptName);
                }
            }
        } catch (Exception e) {
            log.warn("AI 用户上下文-用户信息构建失败: {}", e.getMessage());
        }
        try {
            String roleNames = resolveRoleNames(userId);
            if (StrUtil.isNotBlank(roleNames)) {
                lines.add("角色：" + roleNames);
            }
        } catch (Exception e) {
            log.warn("AI 用户上下文-角色信息构建失败: {}", e.getMessage());
        }
        return String.join("\n", lines);
    }

    private String resolveDeptName(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return "";
        }
        try {
            DeptDO dept = deptService.getById(deptId);
            return dept == null ? "" : StrUtil.nullToEmpty(dept.getName());
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveRoleNames(Long userId) {
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return "";
        }
        List<String> names = new ArrayList<>(roleIds.size());
        for (Long roleId : roleIds) {
            try {
                RoleDO role = roleService.getById(roleId);
                if (role != null && StrUtil.isNotBlank(role.getName())) {
                    names.add(role.getName());
                }
            } catch (Exception ignored) {
                // 单个角色查询失败跳过
            }
        }
        return String.join("、", names);
    }

    /** 端来源不入缓存（同一用户可能双端切换），读取后动态追加 */
    private String appendSource(String ctx, String source) {
        String sourceLine = "mobile".equalsIgnoreCase(source) ? "当前使用移动端" : "当前使用 PC 端";
        return StrUtil.isBlank(ctx) ? sourceLine : ctx + "\n" + sourceLine;
    }

    private String safeGlobalKnowledge() {
        try {
            return StrUtil.trim(systemConfigHelper.getAiGlobalKnowledge());
        } catch (Exception e) {
            log.warn("AI 全局知识读取失败，本轮跳过注入: {}", e.getMessage());
            return "";
        }
    }

    private String safeBuild(java.util.function.Supplier<String> supplier) {
        try {
            return StrUtil.nullToEmpty(supplier.get());
        } catch (Exception e) {
            log.warn("AI 菜单知识构建失败，本轮跳过注入: {}", e.getMessage());
            return "";
        }
    }

    // ------------------------------------------------------------------
    // 权限变更事件 → 缓存主动失效（system 发布，ai 监听，单向依赖）
    // ------------------------------------------------------------------

    @EventListener
    public void onUserPermissionChanged(UserPermissionChangedEvent event) {
        try {
            if (event.affectsAllUsers()) {
                // 键空间小（活跃对话用户 × 10min TTL），keys 扫描可接受
                Set<String> keys = stringRedisTemplate.keys(CACHE_KEY_PREFIX + "*");
                if (CollUtil.isNotEmpty(keys)) {
                    stringRedisTemplate.delete(keys);
                }
            } else {
                stringRedisTemplate.delete(CACHE_KEY_PREFIX + event.getUserId());
            }
        } catch (Exception e) {
            log.warn("AI 用户上下文缓存失效处理异常（等待 TTL 自然过期）: {}", e.getMessage());
        }
    }
}
