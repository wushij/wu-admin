package com.admin.server.modules.ai.service.rag;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 标题感知切片器（纯函数，便于单测）。
 *
 * <p>策略与 EduMind 对齐并做了收敛：</p>
 * <ol>
 *   <li>优先按二级标题切分章节，大章节之间不允许拼进同一片；</li>
 *   <li>章节超长时在段落、列表、句末标点处断开，避免半句话成片；</li>
 *   <li>围栏代码块内不允许断开，必要时延长到代码块闭合；</li>
 *   <li>按 overlap 回退时对齐到段落边界；</li>
 *   <li>过滤纯标题、纯分割线与空白片。</li>
 * </ol>
 */
public class MarkdownChunkSplitter {

    /** 切片目标字符数下限，防止误配置成过小值导致碎片化 */
    public static final int MIN_CHUNK_SIZE = 256;
    /** 最大目标字符数，防止单片超出常见向量模型输入上限 */
    public static final int MAX_CHUNK_SIZE = 4000;

    /** 代码块断点延长上限：最多延长到目标长度的 1.6 倍 */
    private static final double CODE_BLOCK_EXTEND_RATIO = 1.6;
    /** overlap 最小回退距离 */
    private static final int MIN_OVERLAP = 0;

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("^(第[一二三四五六七八九十百千0-9]+[章节部分篇]|[一二三四五六七八九十]+、)");
    private static final Pattern DIVIDER_PATTERN = Pattern.compile("^[-*_\\s]{3,}$");

    private final int chunkSize;
    private final int overlap;

    public MarkdownChunkSplitter(int chunkSize, int overlap) {
        int size = chunkSize <= 0 ? 800 : chunkSize;
        this.chunkSize = Math.max(MIN_CHUNK_SIZE, Math.min(size, MAX_CHUNK_SIZE));
        int requestedOverlap = Math.max(overlap, MIN_OVERLAP);
        // overlap 不得超过目标长度的一半，避免切片原地踏步
        this.overlap = Math.min(requestedOverlap, this.chunkSize / 2);
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getOverlap() {
        return overlap;
    }

    /** 切片结果：标题、正文、字符数与中文感知的 Token 估算 */
    public record Chunk(String heading, String content, int charCount, int tokenEstimate) {
    }

    /** 内部章节结构 */
    private record Section(String heading, String body, boolean major) {
    }

    /**
     * 切分正文；空文本返回空列表。
     */
    public List<Chunk> split(String raw) {
        String text = normalize(raw);
        if (StrUtil.isBlank(text)) {
            return List.of();
        }
        List<Section> sections = splitSections(text);
        List<Chunk> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String bufferHeading = "";

        for (Section section : sections) {
            String body = section.body().trim();
            if (body.isEmpty()) {
                continue;
            }
            if (buffer.length() > 0
                    && (section.major() || buffer.length() + body.length() + 2 > chunkSize)) {
                flush(bufferHeading, buffer, chunks);
                buffer.setLength(0);
            }
            if (buffer.length() == 0) {
                bufferHeading = section.heading();
            } else {
                buffer.append("\n\n");
            }
            buffer.append(body);
        }
        flush(bufferHeading, buffer, chunks);

        List<Chunk> merged = coalesceHeadingOnly(chunks);
        // 重新编号由调用方负责，这里只保证内容质量
        return merged;
    }

    /**
     * Token 估算：中文字符约 1.2 token/字，其他字符约 0.35 token/字。
     * 与 EduMind 的口径一致，比 chars/4 更贴近中文实际消耗。
     */
    public static int estimateTokens(String text) {
        if (StrUtil.isBlank(text)) {
            return 0;
        }
        int cn = 0;
        for (int i = 0; i < text.length(); i++) {
            if (isChinese(text.charAt(i))) {
                cn++;
            }
        }
        int other = text.length() - cn;
        return Math.max(1, (int) Math.round(cn * 1.2 + other * 0.35));
    }

    private static boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FFF;
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        // 去掉 UTF-8 BOM
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        return text.trim();
    }

    /**
     * 切章节：优先 `## `，没有二级标题时退回三级及以下标题；仍无标题则整体作为一章。
     * <p>按行扫描并维护围栏状态，代码块内的 `##` 不会被误判为标题。</p>
     */
    private List<Section> splitSections(String text) {
        boolean hasH2 = containsHeadingOutsideFence(text, 2);
        boolean hasInner = containsHeadingOutsideFence(text, 3);
        if (!hasH2 && !hasInner) {
            return List.of(new Section(extractHeading(text), text, false));
        }
        // hasH2 时只按二级标题切；否则按三级及以下标题切
        int requiredLevel = hasH2 ? 2 : -1;
        List<Section> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String heading = "";
        boolean major = false;
        boolean inFence = false;
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            boolean fenceLine = trimmed.startsWith("```");
            if (!inFence && !fenceLine) {
                Matcher matcher = HEADING_PATTERN.matcher(trimmed);
                if (matcher.matches()) {
                    int level = matcher.group(1).length();
                    boolean splitPoint = requiredLevel == -1
                            ? level >= 2 && level <= 6
                            : level == requiredLevel;
                    if (splitPoint && current.length() > 0) {
                        sections.add(new Section(heading, current.toString(), major));
                        current.setLength(0);
                        heading = "";
                        major = false;
                    }
                    if (splitPoint) {
                        heading = StrUtil.brief(matcher.group(2).trim(), 255);
                        major = isMajorHeading(trimmed, heading);
                    }
                }
            }
            current.append(line).append('\n');
            if (fenceLine) {
                inFence = !inFence;
            }
        }
        if (current.length() > 0) {
            if (StrUtil.isBlank(heading)) {
                heading = extractHeading(current.toString());
            }
            sections.add(new Section(heading, current.toString(), major));
        }
        return sections;
    }

    /** 判断非代码块区域内是否存在至少 minLevel 级的标题 */
    private boolean containsHeadingOutsideFence(String text, int minLevel) {
        boolean inFence = false;
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                inFence = !inFence;
                continue;
            }
            if (inFence) {
                continue;
            }
            Matcher matcher = HEADING_PATTERN.matcher(trimmed);
            if (matcher.matches() && matcher.group(1).length() >= minLevel) {
                return true;
            }
        }
        return false;
    }

    /** 提取章节首行标题，去掉 # 前缀 */
    private String extractHeading(String body) {
        int lineEnd = body.indexOf('\n');
        String firstLine = lineEnd < 0 ? body : body.substring(0, lineEnd);
        Matcher matcher = HEADING_PATTERN.matcher(firstLine.trim());
        if (matcher.matches()) {
            return StrUtil.brief(matcher.group(2).trim(), 255);
        }
        return "";
    }

    /** 二级标题或「第X章 / 一、」视为大章节边界，禁止与相邻章节拼片 */
    private boolean isMajorHeading(String body, String heading) {
        int lineEnd = body.indexOf('\n');
        String firstLine = (lineEnd < 0 ? body : body.substring(0, lineEnd)).trim();
        if (firstLine.startsWith("## ") && !firstLine.startsWith("### ")) {
            return true;
        }
        return CHAPTER_PATTERN.matcher(heading).find();
    }

    private void flush(String heading, StringBuilder buffer, List<Chunk> chunks) {
        String segment = buffer.toString().trim();
        if (segment.isEmpty()) {
            return;
        }
        if (segment.length() <= chunkSize) {
            emit(heading, segment, chunks);
            return;
        }
        emitLong(heading, segment, chunks);
    }

    /** 超长章节按断点切成多片，并保留 overlap */
    private void emitLong(String heading, String segment, List<Chunk> out) {
        int cursor = 0;
        int length = segment.length();
        while (cursor < length) {
            int end = Math.min(cursor + chunkSize, length);
            if (end < length) {
                end = findBreakPoint(segment, cursor, end);
            }
            String piece = segment.substring(cursor, end).trim();
            emit(heading, piece, out);
            if (end >= length) {
                break;
            }
            int next = Math.max(cursor + 1, end - overlap);
            next = alignOverlapStart(segment, next);
            if (next <= cursor) {
                next = end;
            }
            cursor = next;
        }
    }

    /**
     * 在 [cursor, end] 内反向寻找最佳断点：
     * 段落 &gt; 标题/列表行首 &gt; 换行 &gt; 句末标点；代码围栏内的断点跳过。
     */
    private int findBreakPoint(String text, int cursor, int end) {
        int lowest = cursor + Math.max(1, chunkSize / 3);
        String[] candidates = {"\n\n", "\n### ", "\n## ", "\n- ", "\n", "。", "！", "？", "；", ".", "!", "?", ";"};
        for (String candidate : candidates) {
            int found = lastIndexOfOutsideCode(text, candidate, lowest, end);
            if (found > cursor) {
                return candidate.startsWith("\n") ? found : found + candidate.length();
            }
        }
        // 整段都在代码块内时，尽量延长到代码块闭合，避免把代码切碎
        int extendLimit = Math.min(text.length(), cursor + (int) (chunkSize * CODE_BLOCK_EXTEND_RATIO));
        int closingFence = text.indexOf("\n```", end);
        if (closingFence > 0 && closingFence <= extendLimit) {
            return closingFence;
        }
        return end;
    }

    /** 从 end 向前查找候选断点，跳过落在未闭合代码围栏内的位置 */
    private int lastIndexOfOutsideCode(String text, String candidate, int from, int end) {
        int index = text.lastIndexOf(candidate, end);
        while (index >= from) {
            if (!isInsideCodeFence(text, index)) {
                return index;
            }
            index = text.lastIndexOf(candidate, index - 1);
        }
        return -1;
    }

    /** 判断位置 index 是否处于未闭合的围栏代码块内 */
    private boolean isInsideCodeFence(String text, int index) {
        int fenceCount = 0;
        int cursor = 0;
        while (true) {
            int found = text.indexOf("```", cursor);
            if (found < 0 || found >= index) {
                break;
            }
            fenceCount++;
            cursor = found + 3;
        }
        return fenceCount % 2 == 1;
    }

    /** overlap 回退点对齐到段落、行首或句末之后，避免从半个词开始 */
    private int alignOverlapStart(String text, int index) {
        int limit = Math.min(text.length() - 1, index + Math.max(16, overlap / 2));
        for (int i = index; i < limit; i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '！' || c == '？' || c == '；' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        return index;
    }

    /** 写入一片并做质量过滤：纯分割线、空白、过短标题不单独成片 */
    private void emit(String heading, String content, List<Chunk> out) {
        String cleaned = ensureFenceBalanced(stripDividers(content));
        if (cleaned.isEmpty()) {
            return;
        }
        int tokens = estimateTokens(cleaned);
        if (tokens < 4) {
            return;
        }
        out.add(new Chunk(StrUtil.nullToEmpty(heading), cleaned, cleaned.length(), tokens));
    }

    /** 兜底：片段内代码围栏数量为奇数时补齐闭合，保证切片 Markdown 结构完整 */
    private String ensureFenceBalanced(String content) {
        long fences = content.lines().filter(line -> line.trim().startsWith("```")).count();
        if (fences % 2 == 1) {
            return content + "\n```";
        }
        return content;
    }

    private String stripDividers(String text) {
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || DIVIDER_PATTERN.matcher(trimmed).matches()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString().trim();
    }

    /** 把「只有标题没有正文」的片并入相邻片，避免低信息量切片占用召回额度 */
    private List<Chunk> coalesceHeadingOnly(List<Chunk> chunks) {
        List<Chunk> result = new ArrayList<>(chunks.size());
        Chunk pending = null;
        for (Chunk chunk : chunks) {
            if (isHeadingOnly(chunk.content())) {
                pending = pending == null ? chunk : merge(pending, chunk);
                continue;
            }
            String content = pending == null ? chunk.content() : pending.content() + "\n\n" + chunk.content();
            String heading = pending != null && StrUtil.isNotBlank(pending.heading())
                    ? pending.heading() : chunk.heading();
            result.add(new Chunk(heading, content, content.length(), estimateTokens(content)));
            pending = null;
        }
        // 末尾仍是纯标题：并入最后一片，避免信息丢失
        if (pending != null && !result.isEmpty()) {
            Chunk last = result.remove(result.size() - 1);
            result.add(merge(last, pending));
        }
        return result;
    }

    private Chunk merge(Chunk first, Chunk second) {
        String content = first.content() + "\n\n" + second.content();
        String heading = StrUtil.isNotBlank(first.heading()) ? first.heading() : second.heading();
        return new Chunk(heading, content, content.length(), estimateTokens(content));
    }

    private boolean isHeadingOnly(String content) {
        if (StrUtil.isBlank(content)) {
            return true;
        }
        boolean hasBody = false;
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || HEADING_PATTERN.matcher(trimmed).matches()) {
                continue;
            }
            hasBody = true;
            break;
        }
        return !hasBody;
    }
}
