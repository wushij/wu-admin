<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="pageTitle"
      :subtitle="pageSubtitle"
      icon="cluster-o"
      theme="dept"
    />

    <ListLoading v-if="loading" />

    <template v-else>
      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="cluster-o" theme="dept" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">{{ entity === 'dept' ? '部门信息' : '岗位信息' }}</text>
            <text class="form-section__desc">
              {{ entity === 'dept' ? '上级与名称为必填项' : '编码与名称为必填项' }}
            </text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            :label="entity === 'dept' ? '上级部门' : '上级岗位'"
            clickable
            boxed
            arrow
            @click="pickParent"
          >
            <text class="picker-value">{{ parentLabel }}</text>
          </FormCell>

          <template v-if="entity === 'dept'">
            <FormCell
              v-model="deptForm.name"
              label="部门名称"
              editable
              boxed
              placeholder="请输入部门名称"
            />
            <FormCell label="负责人" clickable boxed arrow last @click="pickLeader">
              <text
                class="picker-value"
                :class="{ 'picker-value--muted': leaderLabel === '不设置' }"
              >
                {{ leaderLabel }}
              </text>
            </FormCell>
          </template>

          <template v-else>
            <FormCell
              v-model="postForm.postCode"
              label="岗位编码"
              editable
              boxed
              placeholder="请输入岗位编码"
              :disabled="!isCreate"
            />
            <FormCell
              v-model="postForm.postName"
              label="岗位名称"
              editable
              boxed
              placeholder="请输入岗位名称"
              last
            />
          </template>
        </view>
      </view>

      <view v-if="entity === 'dept'" class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="contact-o" theme="dept" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">联系方式</text>
            <text class="form-section__desc">电话与邮箱可按需填写</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-model="deptForm.phone"
            label="联系电话"
            editable
            boxed
            input-type="digit"
            placeholder="选填"
          />
          <FormCell
            v-model="deptForm.email"
            label="邮箱"
            editable
            boxed
            placeholder="选填"
            last
          />
        </view>
      </view>

      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="setting-o" theme="dept" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">排序与状态</text>
            <text class="form-section__desc">数值越小排序越靠前</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-if="entity === 'dept'"
            v-model="deptSortText"
            label="排序"
            editable
            boxed
            input-type="number"
            placeholder="0"
          />
          <template v-else>
            <FormCell
              v-model="postSortText"
              label="排序"
              editable
              boxed
              input-type="number"
              placeholder="0"
            />
            <FormCell
              v-model="postForm.remark"
              label="备注"
              editable
              boxed
              placeholder="选填"
            />
          </template>
          <FormCell label="状态" clickable boxed arrow last @click="pickStatus">
            <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
          </FormCell>
        </view>
      </view>

      <PageFooter>
        <button class="page-footer__btn" :loading="saving" @click="onSave">
          {{ saveButtonText }}
        </button>
      </PageFooter>
    </template>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useOrgForm, type OrgEntity } from '@/composables/useOrgForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import { showActionSheet } from '@/utils/app-dialog'
import { consumePagePickerResult, clearPagePickerResult } from '@/utils/page-picker-result'
import { appendNavFromParam } from '@/utils/nav-from'
import { beginLeaderPick, consumeLeaderPick, clearLeaderPick } from '@/utils/leader-pick'
import { registerPageShallowFallback } from '@/utils/navigate-back'

const ORG_INDEX_URL = '/pages-sub/system/org/index'

const recordId = ref(0)
const pendingLeaderPick = ref('')
const {
  loading,
  saving,
  isCreate,
  entity,
  deptForm,
  postForm,
  parentLabel,
  excludeParentId,
  leaderLabel,
  statusOptions,
  statusIndex,
  initCreate,
  loadEdit,
  setParentSelection,
  setLeaderSelection,
  onStatusChange,
  save,
} = useOrgForm()

const { resetBaseline, leaveAfterSave } = useEditPageGuard(
  () => ({
    entity: entity.value,
    deptForm: { ...deptForm },
    postForm: { ...postForm },
    parentLabel: parentLabel.value,
    leaderLabel: leaderLabel.value,
    statusIndex: statusIndex.value,
  }),
  { loading },
)

const pageTitle = computed(() => {
  if (entity.value === 'dept') return isCreate.value ? '新增部门' : '编辑部门'
  return isCreate.value ? '新增岗位' : '编辑岗位'
})

const pageSubtitle = computed(() => {
  if (entity.value === 'dept') {
    return isCreate.value ? '创建部门并指定上级与负责人' : '修改部门资料与联系方式'
  }
  return isCreate.value ? '创建岗位并配置编码' : '修改岗位名称与状态'
})

const saveButtonText = computed(() => {
  if (entity.value === 'dept') return isCreate.value ? '创建部门' : '保存'
  return isCreate.value ? '创建岗位' : '保存'
})

const deptSortText = computed({
  get: () => String(deptForm.sort ?? 0),
  set: (v) => {
    deptForm.sort = Number(v) || 0
  },
})

const postSortText = computed({
  get: () => String(postForm.sort ?? 0),
  set: (v) => {
    postForm.sort = Number(v) || 0
  },
})

function applyLeaderPick(result: { id?: number | null; label?: string }) {
  setLeaderSelection(result.id ?? null, result.label)
  resetBaseline()
}

function consumeLeaderPickResult() {
  const picked = consumeLeaderPick(pendingLeaderPick.value || undefined)
  if (picked) {
    pendingLeaderPick.value = ''
    applyLeaderPick(picked)
    return true
  }
  return false
}

function pickLeader() {
  clearPagePickerResult()
  clearLeaderPick()
  const initialId = deptForm.leaderUserId && deptForm.leaderUserId > 0 ? deptForm.leaderUserId : ''
  pendingLeaderPick.value = beginLeaderPick()
  const pick = pendingLeaderPick.value
  uni.navigateTo({
    url: appendNavFromParam(
      `/pages-sub/system/user-select?pick=${pick}&initialId=${initialId}&allowEmpty=1&emptyLabel=${encodeURIComponent('不设置')}&title=${encodeURIComponent('选择负责人')}`,
    ),
    events: {
      pickUserLeader(result: { id?: number | null; label?: string }) {
        pendingLeaderPick.value = ''
        applyLeaderPick(result)
      },
    },
  })
}

function pickParent() {
  const isDept = entity.value === 'dept'
  const parentId = isDept ? deptForm.parentId : postForm.parentId
  const selectedId = parentId && parentId > 0 ? parentId : ''
  const excludeId = excludeParentId.value ? `&excludeId=${excludeParentId.value}` : ''
  const type = isDept ? 'dept-parent' : 'post-parent'
  const emptyLabel = encodeURIComponent(isDept ? '顶级部门' : '顶级岗位')
  uni.navigateTo({
    url: appendNavFromParam(
      `/pages-sub/system/tree-select?type=${type}&allowEmpty=1&selectedId=${selectedId}${excludeId}&emptyLabel=${emptyLabel}&title=${encodeURIComponent(isDept ? '选择上级部门' : '选择上级岗位')}`,
    ),
  })
}

function applyPickerResults() {
  const result = consumePagePickerResult()
  if (!result) return
  if (result.kind === 'dept-parent' && entity.value === 'dept') {
    setParentSelection(result.id ?? null, result.label)
  } else if (result.kind === 'post-parent' && entity.value === 'post') {
    setParentSelection(result.id ?? null, result.label)
  } else if (result.kind === 'user-leader' && entity.value === 'dept') {
    applyLeaderPick(result)
  }
}

async function pickStatus() {
  try {
    const index = await showActionSheet({
      title: '状态',
      items: statusOptions.map((s) => ({ label: s.label })),
    })
    onStatusChange(index)
  } catch {
    /* cancelled */
  }
}

async function onSave() {
  const ok = await save(recordId.value)
  if (!ok) return
  leaveAfterSave()
}

onShow(() => {
  registerPageShallowFallback(ORG_INDEX_URL)
  if (consumeLeaderPickResult()) return
  applyPickerResults()
})

onLoad(async (options) => {
  const ent = (options?.entity === 'post' ? 'post' : 'dept') as OrgEntity
  if (options?.mode === 'create') {
    const parentId = Number(options?.parentId) || undefined
    await initCreate(ent, parentId)
    resetBaseline()
    return
  }
  recordId.value = Number(options?.id)
  if (recordId.value) await loadEdit(ent, recordId.value)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';
@import '@/styles/module-form-page.scss';
</style>
