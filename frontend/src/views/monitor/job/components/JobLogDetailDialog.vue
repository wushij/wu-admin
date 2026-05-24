<template>
  <el-dialog
    :model-value="visible"
    title="执行详情"
    width="560px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-descriptions :column="1" border size="small">
      <el-descriptions-item label="任务">{{ detail?.jobName }}</el-descriptions-item>
      <el-descriptions-item label="调用目标">{{ detail?.invokeTarget }}</el-descriptions-item>
      <el-descriptions-item label="信息">{{ detail?.jobMessage }}</el-descriptions-item>
      <el-descriptions-item label="开始">{{ detail?.startTime }}</el-descriptions-item>
      <el-descriptions-item label="结束">{{ detail?.stopTime }}</el-descriptions-item>
      <el-descriptions-item v-if="detail?.exceptionInfo" label="异常">
        <pre class="exception-pre">{{ detail.exceptionInfo }}</pre>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import type { SysJobLog } from '@/api/monitor/job'

defineProps<{
  visible: boolean
  detail: SysJobLog | null
}>()

defineEmits<{
  'update:visible': [value: boolean]
}>()
</script>

<style scoped>
.exception-pre {
  margin: 0;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
