<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="680px"
    :lock-scroll="false"
    destroy-on-close
    class="menu-form-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="上级菜单" prop="parentId">
        <el-tree-select
          v-model="form.parentId"
          :data="parentOptions"
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          placeholder="请选择上级菜单"
          check-strictly
          default-expand-all
          clearable
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="菜单类型" prop="type">
        <el-radio-group v-model="form.type" @change="$emit('type-change')">
          <el-radio :label="1">目录</el-radio>
          <el-radio :label="2">菜单</el-radio>
          <el-radio :label="3">按钮</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="菜单名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入菜单名称" maxlength="50" show-word-limit />
      </el-form-item>

      <el-form-item v-if="form.type !== 3" label="是否外链">
        <el-switch v-model="form.isFrame" :active-value="1" :inactive-value="0" />
        <span class="form-tip">开启后组件路径填写完整 URL，在新窗口打开</span>
      </el-form-item>

      <el-form-item v-if="form.type !== 3 && !form.isFrame" label="路由地址" prop="path">
        <el-input v-model="form.path" placeholder="如 /system/user" />
      </el-form-item>

      <el-form-item v-if="form.type !== 3 && form.isFrame" label="外链地址" prop="component">
        <el-input v-model="form.component" placeholder="https://example.com" />
      </el-form-item>

      <el-form-item v-if="form.type === 2 && !form.isFrame" label="组件路径" prop="component">
        <el-input v-model="form.component" placeholder="如 system/user/index">
          <template #append>
            <el-dropdown trigger="click" @command="(c) => (form.component = c)">
              <el-button>常用</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="item in componentPresets" :key="item" :command="item">
                    {{ item }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-input>
        <div class="form-tip">对应 views 下路径，不含 .vue</div>
      </el-form-item>

      <el-form-item v-if="form.type === 3" label="权限标识" prop="permission">
        <el-input v-model="form.permission" placeholder="如 system:user:create">
          <template #append>
            <el-button @click="$emit('fill-permission-prefix')">按上级生成</el-button>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item v-if="form.type === 2" label="列表权限" prop="permission">
        <el-input v-model="form.permission" placeholder="侧栏与路由权限，如 system:user:list" />
      </el-form-item>

      <el-form-item v-if="form.type !== 3" label="图标" prop="icon">
        <IconSelect v-model="form.icon" />
      </el-form-item>

      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="form.sort" :min="0" controls-position="right" style="width: 100%" />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <DictSelect
          v-model="form.status"
          dict-type="sys_normal_disable"
          value-type="number"
          :clearable="false"
          width="160px"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="$emit('submit')">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { MenuVO, MenuSaveDTO } from '@/api/system/menu'
import IconSelect from '@/components/IconSelect.vue'
import { componentPresets } from '../constants/menuMeta'

defineProps<{
  visible: boolean
  title: string
  form: MenuSaveDTO & { isFrame: number }
  rules: FormRules
  parentOptions: MenuVO[]
  submitLoading: boolean
}>()

defineEmits<{
  'update:visible': [value: boolean]
  'type-change': []
  'fill-permission-prefix': []
  submit: []
}>()

const formRef = ref<FormInstance | null>(null)
defineExpose({ formRef })
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
