<template>
  <div class="app-container module-page">
    <PageHeader
      title="工单管理"
      description="创建与跟踪工单，支持状态流转、评论与附件管理"
      :icon="MODULE_PAGE_ICON.ticket"
      :stats="[{ label: '工单总数', value: total }]"
    />

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="标题">
          <el-input v-model="queryParams.title" placeholder="请输入工单标题" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="queryParams.status"
            :dict-type="DICT_TYPE.TICKET_STATUS"
            value-type="string"
            placeholder="请选择状态"
            width="160px"
          />
        </el-form-item>
        <el-form-item label="优先级">
          <DictSelect
            v-model="queryParams.priority"
            :dict-type="DICT_TYPE.TICKET_PRIORITY"
            value-type="string"
            placeholder="请选择优先级"
            width="160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>工单列表</span>
          <div class="header-actions">
            <ListExportButton module="ticket" :query-params="queryParams" permission="system:ticket:list" />
            <RecycleCenterLink tab="ticket" />
            <el-button type="primary" v-permission="'system:ticket:create'" @click="handleCreate">新建工单</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-if="ticketList.length || loading"
        ref="tableRef"
        :data="ticketList"
        border
        stripe
        v-loading="loading"
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column prop="ticketNo" label="编号" width="190" align="center" header-align="center" />
        <el-table-column prop="title" label="标题" min-width="180" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <DictTag :value="row.priority" :dict-type="DICT_TYPE.TICKET_PRIORITY" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center" header-align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" :dict-type="DICT_TYPE.TICKET_STATUS" />
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="120" align="center" header-align="center" />
        <el-table-column prop="assigneeName" label="处理人" width="120" align="center" header-align="center" />
        <el-table-column prop="deadline" label="截止时间" width="180" align="center" header-align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" header-align="center" />
        <el-table-column label="操作" width="250" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="handleDetail(row)">详情</el-button>
              <el-dropdown
                v-if="canTransitionRow(row)"
                trigger="click"
                teleported
                @command="(command: string) => handleStatusCommand(command, row)"
              >
                <el-button size="small" type="primary">流转状态</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="opt in transitionOptions(row)"
                      :key="String(opt.value)"
                      :command="opt.value"
                    >{{ opt.label }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button
                v-permission="'system:ticket:delete'"
                size="small"
                type="danger"
                plain
                @click="handleDelete(row)"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="暂无工单" description="当前筛选条件下没有工单记录，点击右上角可新建工单" />

      <el-pagination
        v-if="total > 0"
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="getList"
        @current-change="getList"
      />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑工单' : '新建工单'" width="680px" :lock-scroll="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入工单标题" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入工单描述" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <DictSelect
            v-model="form.priority"
            :dict-type="DICT_TYPE.TICKET_PRIORITY"
            value-type="string"
            apply-default
            :clearable="false"
          />
        </el-form-item>
        <el-form-item label="处理人" prop="assigneeUserId">
          <el-select v-model="form.assigneeUserId" clearable filterable placeholder="请选择处理人" style="width: 100%">
            <el-option label="全部人员（全员通知）" :value="0" />
            <el-option v-for="u in userOptions" :key="u.id" :label="`${u.username}(${u.nickname})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="请选择截止时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="工单详情" size="45%" :lock-scroll="false">
      <el-descriptions :column="1" border v-if="currentTicket.id">
        <el-descriptions-item label="编号">{{ currentTicket.ticketNo }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentTicket.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <DictTag :value="currentTicket.status" :dict-type="DICT_TYPE.TICKET_STATUS" />
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          <DictTag :value="currentTicket.priority" :dict-type="DICT_TYPE.TICKET_PRIORITY" />
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentTicket.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="comment-header">评论记录</div>
      <el-timeline>
        <el-timeline-item v-for="comment in comments" :key="comment.id" :timestamp="comment.createTime">
          <strong>{{ comment.username || '-' }}</strong>: {{ comment.content }}
        </el-timeline-item>
      </el-timeline>
      <div class="comment-header">附件</div>
      <el-upload :show-file-list="false" :auto-upload="false" :on-change="handleUploadAttachment" accept="*">
        <el-button type="primary" plain>上传附件</el-button>
      </el-upload>
      <div class="attachment-list" v-if="attachments.length">
        <div class="attachment-item" v-for="attachment in attachments" :key="attachment.id">
          <FileTypeIcon :file-name="attachment.fileName" size="xs" />
          <a :href="downloadAttachmentUrl(attachment.id)" target="_blank">{{ attachment.fileName }}</a>
          <span class="attachment-meta">({{ attachment.uploaderName || '-' }}，{{ attachment.createTime }})</span>
        </div>
      </div>
      <el-input v-model="commentText" type="textarea" :rows="3" placeholder="请输入评论内容" class="comment-input" />
      <div class="comment-actions">
        <el-button type="primary" @click="submitComment">发表评论</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, TableInstance } from 'element-plus'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import FileTypeIcon from '@/components/ui/FileTypeIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import ListExportButton from '@/components/ListExportButton.vue'
import RecycleCenterLink from '@/components/RecycleCenterLink.vue'
import { DICT_TYPE } from '@/constants/dict'
import { useTicketPage } from './composables/useTicketPage'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const tableRef = ref<TableInstance | null>(null)
const formRef = ref<FormInstance | null>(null)

const {
  loading,
  total,
  ticketList,
  tableRef: composableTableRef,
  userOptions,
  formVisible,
  detailVisible,
  formRef: composableFormRef,
  comments,
  attachments,
  commentText,
  currentTicket,
  queryParams,
  form,
  rules,
  canTransitionRow,
  transitionOptions,
  getList,
  handleQuery,
  resetQuery,
  handleCreate,
  handleDetail,
  handleUploadAttachment,
  downloadAttachmentUrl,
  handleStatusCommand,
  handleDelete,
  submitForm,
  submitComment,
} = useTicketPage()

watchEffect(() => {
  composableTableRef.value = tableRef.value
  composableFormRef.value = formRef.value
})
</script>

<style scoped>
.comment-header {
  margin: var(--space-4) 0 var(--space-3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.attachment-list {
  margin-top: var(--space-3);
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.attachment-meta {
  margin-left: 6px;
  color: var(--color-text-muted);
  font-size: var(--text-caption);
}

.comment-input {
  margin-top: var(--space-3);
}

.comment-actions {
  margin-top: var(--space-3);
  text-align: right;
}
</style>
