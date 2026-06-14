<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="isCreate ? '新增字典项' : '编辑字典项'"
      :subtitle="dictType ? `字典类型：${dictType}` : '填写标签与键值'"
      icon="records-o"
      theme="dict"
    />

    <view class="form-section card--elevated">
      <view class="form-section__head">
        <ModuleIcon icon="notes-o" theme="dict" size="sm" />
        <view class="form-section__intro">
          <text class="form-section__title">字典项内容</text>
          <text class="form-section__desc">标签用于展示，键值用于存储</text>
        </view>
      </view>

      <view class="form-fields">
        <FormCell
          v-model="form.dictLabel"
          label="字典标签"
          editable
          boxed
          placeholder="如：待处理"
        />
        <FormCell
          v-model="form.dictValue"
          label="字典键值"
          editable
          boxed
          placeholder="如：0"
        />
        <FormCell
          v-model="sortText"
          label="显示排序"
          editable
          boxed
          input-type="number"
          placeholder="0"
          last
        />
      </view>
    </view>

    <view class="form-section card--elevated">
      <view class="form-section__head">
        <ModuleIcon icon="setting-o" theme="dict" size="sm" />
        <view class="form-section__intro">
          <text class="form-section__title">样式与状态</text>
          <text class="form-section__desc">回显样式与默认项配置</text>
        </view>
      </view>

      <view class="form-fields">
        <FormCell label="回显样式" clickable boxed arrow @click="pickListClass">
          <view class="picker-value picker-value--tag">
            <DictTag
              :label="LIST_CLASS_OPTIONS[listClassIndex]?.label"
              :effect="listClassToTagType(form.listClass)"
            />
          </view>
        </FormCell>
        <FormCell label="是否默认" clickable boxed arrow @click="pickIsDefault">
          <text class="picker-value">{{ isDefaultIndex === 0 ? '是' : '否' }}</text>
        </FormCell>
        <FormCell label="状态" clickable boxed arrow @click="pickStatus">
          <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
        </FormCell>
        <FormCell v-model="form.remark" label="备注" editable boxed placeholder="选填" last />
      </view>
    </view>

    <PageFooter>
      <button class="page-footer__btn" :loading="saving" @click="save(dataId)">
        {{ isCreate ? '创建字典项' : '保存' }}
      </button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import { listDictDataForManage } from '@/api/system/dict'
import { listClassToTagType } from '@/composables/useDict'
import { useDictDataForm, LIST_CLASS_OPTIONS } from '@/composables/useDictDataForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'

const dataId = ref(0)
const dictType = ref('')
const {
  saving,
  isCreate,
  form,
  statusOptions,
  statusIndex,
  listClassIndex,
  isDefaultIndex,
  initCreate,
  loadFromItem,
  onStatusChange,
  onListClassChange,
  onIsDefaultChange,
  save,
} = useDictDataForm()

const { resetBaseline } = useEditPageGuard(() => ({
  ...form,
  statusIndex: statusIndex.value,
  listClassIndex: listClassIndex.value,
  isDefaultIndex: isDefaultIndex.value,
}))

const sortText = computed({
  get: () => String(form.sort),
  set: (v) => {
    form.sort = Number(v) || 0
  },
})

function pickStatus() {
  uni.showActionSheet({
    itemList: statusOptions.map((s) => s.label),
    success: (res) => onStatusChange({ detail: { value: res.tapIndex } }),
  })
}

function pickListClass() {
  uni.showActionSheet({
    itemList: LIST_CLASS_OPTIONS.map((s) => s.label),
    success: (res) => onListClassChange({ detail: { value: res.tapIndex } }),
  })
}

function pickIsDefault() {
  uni.showActionSheet({
    itemList: ['是', '否'],
    success: (res) => onIsDefaultChange({ detail: { value: res.tapIndex } }),
  })
}

onLoad(async (options) => {
  dictType.value = decodeURIComponent(String(options?.dictType || ''))
  if (options?.mode === 'create') {
    initCreate(dictType.value)
    resetBaseline()
    return
  }
  dataId.value = Number(options?.dataId)
  if (dataId.value && dictType.value) {
    const res = await listDictDataForManage(dictType.value)
    const item = (res.data || []).find((d) => d.id === dataId.value)
    if (item) loadFromItem(dictType.value, item)
    resetBaseline()
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';
@import '@/styles/module-form-page.scss';
</style>
