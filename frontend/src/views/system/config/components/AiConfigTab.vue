<template>
  <div class="ai-config-tab">
    <el-form :model="draft" label-width="120px" class="config-form">
      <el-form-item label="AI 助手">
        <div class="switch-with-tip">
          <el-switch v-model="draft.assistantEnabled" :disabled="!canEdit" />
          <span class="form-item-tip">关闭后，PC 端与移动端的悬浮 AI 助手小窗将不再显示</span>
        </div>
      </el-form-item>

      <el-form-item label="回答边界">
        <el-radio-group v-model="draft.answerScope" :disabled="!canEdit">
          <el-radio label="focus">聚焦本系统</el-radio>
          <el-radio label="open">开放问答</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <div class="token-quota-card">
      <div class="card-header">
        <div class="title-group">
          <el-icon class="title-icon"><Coin /></el-icon>
          <span class="title-text">Token 每日配额设置</span>
        </div>
        <span class="subtitle-text">限制每位用户或不同角色每天可消耗的最大 AI Token 数量</span>
      </div>

      <div class="card-body">
        <div class="quota-item default-quota-item">
          <div class="item-info">
            <div class="item-title">每用户默认每日配额</div>
            <div class="item-desc">未匹配单独角色配额规则的用户，将统一按此配额限制</div>
          </div>
          <div class="item-control">
            <el-input-number
              v-model="draft.tokensPerUserDaily"
              :min="0"
              :max="10000000"
              :step="10000"
              controls-position="right"
              :disabled="!canEdit"
              class="quota-input-number"
            />
            <span class="unit-text">Tokens / 天</span>
          </div>
        </div>

        <el-divider class="quota-divider" />

        <div class="quota-item role-quota-item">
          <div class="role-header-row">
            <div class="item-info">
              <div class="item-title">角色差异化配额</div>
              <div class="item-desc">可为特定角色单独指定 Token 配额（如管理员、高级用户等）</div>
            </div>
            <el-button
              type="primary"
              :icon="Plus"
              :disabled="!canEdit || draft.roleTokenQuotas.length >= 50"
              @click="addRow"
            >
              添加角色配额
            </el-button>
          </div>

          <div class="role-table-container">
            <el-table
              v-if="draft.roleTokenQuotas.length"
              :data="draft.roleTokenQuotas"
              size="default"
              stripe
              border
              class="role-quota-table"
            >
              <el-table-column label="目标角色" min-width="220">
                <template #default="{ row }">
                  <el-select
                    v-model="row.roleId"
                    :disabled="!canEdit"
                    placeholder="请选择角色"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="role in roleOptions"
                      :key="role.id"
                      :label="`${role.name}（${role.code}）`"
                      :value="role.id"
                      :disabled="isRoleUsed(role.id, row.roleId)"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="每日 Token 配额" min-width="200">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.tokensDaily"
                    :min="0"
                    :max="10000000"
                    :step="10000"
                    controls-position="right"
                    :disabled="!canEdit"
                    style="width: 100%"
                  />
                </template>
              </el-table-column>
              <el-table-column label="生效规则说明" width="130" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.tokensDaily === 0" type="success" size="small" effect="light">无限制</el-tag>
                  <el-tag v-else type="info" size="small" effect="plain" class="motion-tabular">
                    {{ row.tokensDaily?.toLocaleString() }} Tokens
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ $index }">
                  <el-button
                    type="danger"
                    link
                    size="small"
                    :disabled="!canEdit"
                    @click="removeRow($index)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <EmptyState
              v-else
              title="暂未配置角色差异化配额"
              description="当前全员将统一按默认每日配额执行"
            >
              <template v-if="canEdit" #actions>
                <el-button type="primary" plain :icon="Plus" @click="addRow">添加第一条角色配额</el-button>
              </template>
            </EmptyState>
          </div>

          <InlineAlert
            type="info"
            title="多角色用户自动按最高配额生效；配额设为 0 表示该角色不限制使用 Token。"
            compact
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Coin, Plus } from '@element-plus/icons-vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import InlineAlert from '@/components/ui/InlineAlert.vue'
import type { ConfigGroupMap } from '@/types/config'
import type { RoleOption } from '../composables/useConfigDraft'
import { showSnackbar } from '@/composables/ui/useSnackbar'

const draft = defineModel<ConfigGroupMap['ai']>('draft', { required: true })

defineProps<{
  canEdit: boolean
  roleOptions: RoleOption[]
}>()

/** 已被其它行选用的角色置灰（当前行已选值除外），避免重复配置同一角色 */
function isRoleUsed(roleId: number, currentRoleId: number): boolean {
  if (roleId === currentRoleId) return false
  return draft.value.roleTokenQuotas.some((q) => q.roleId === roleId)
}

function addRow() {
  draft.value.roleTokenQuotas.push({ roleId: 0, tokensDaily: 0 })
}

function removeRow(index: number) {
  const removed = draft.value.roleTokenQuotas.splice(index, 1)[0]
  if (!removed) return
  showSnackbar({
    message: '已移除一条角色 Token 配额规则',
    variant: 'info',
    actionLabel: '撤销',
    onAction: () => {
      draft.value.roleTokenQuotas.splice(index, 0, removed)
    },
  })
}
</script>

<style scoped lang="scss">
.ai-config-tab {
  max-width: 760px;
  padding-top: var(--space-2);
}

.config-form {
  margin-bottom: var(--space-5);
}

.switch-with-tip {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.form-item-tip {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.token-quota-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background-color: var(--color-bg-surface);
  overflow: hidden;
}

.card-header {
  padding: var(--space-4) var(--space-5);
  background-color: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border-subtle);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-group {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.title-icon {
  font-size: 18px;
  color: var(--el-color-primary);
}

.title-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.subtitle-text {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.card-body {
  padding: var(--space-5);
}

.quota-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.default-quota-item {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.item-info {
  flex: 1;
  min-width: 200px;
}

.item-title {
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.item-desc {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.item-control {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.quota-input-number {
  width: 220px;
}

.unit-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.quota-divider {
  margin: var(--space-5) 0;
}

.role-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.role-table-container {
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.role-quota-table {
  width: 100%;
}
</style>
