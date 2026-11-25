<template>
  <div class="app-container module-page">
    <PageHeader
      title="审批单中心"
      description="提交、审批与归档各类业务审批单，支持多类型审批流程"
      :icon="MODULE_PAGE_ICON.approval"
      :stats="[{ label: '审批单数', value: total }]"
    />

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="标题">
          <el-input v-model="queryParams.title" placeholder="请输入审批标题" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <DictSelect
            v-model="queryParams.formType"
            :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE"
            value-type="string"
            placeholder="请选择类型"
            width="160px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="queryParams.status"
            :dict-type="DICT_TYPE.APPROVAL_STATUS"
            value-type="string"
            placeholder="请选择状态"
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
          <span>审批单列表</span>
          <div class="header-actions">
            <ListExportButton module="approval" :query-params="queryParams" permission="system:approval:list" />
            <RecycleCenterLink tab="approval" />
            <el-button type="primary" v-permission="'system:approval:create'" @click="handleCreate">提交审批单</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-if="list.length || loading"
        ref="tableRef"
        :data="list"
        border
        stripe
        v-loading="loading"
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column prop="formNo" label="单号" width="190" align="center" header-align="center" />
        <el-table-column prop="title" label="标题" min-width="170" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="formType" label="类型" width="110" align="center" header-align="center">
          <template #default="{ row }">
            <DictTag :value="row.formType" :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center" header-align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" :dict-type="DICT_TYPE.APPROVAL_STATUS" />
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="110" align="center" header-align="center" />
        <el-table-column prop="approverName" label="审批人" width="110" align="center" header-align="center" />
        <el-table-column prop="createTime" label="提交时间" width="170" align="center" header-align="center" />
        <el-table-column label="操作" width="240" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="openDetail(row.id)">详情</el-button>
              <el-dropdown v-if="canApproveRow(row) || canArchiveRow(row)" trigger="click">
                <el-button size="small" type="primary">处理</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-if="canApproveRow(row)"
                      v-permission="'system:approval:approve'"
                      @click="openApproveDialog(row, 'APPROVE')"
                    >通过</el-dropdown-item>
                    <el-dropdown-item
                      v-if="canApproveRow(row)"
                      v-permission="'system:approval:approve'"
                      @click="openApproveDialog(row, 'REJECT')"
                    >驳回</el-dropdown-item>
                    <el-dropdown-item
                      v-if="canArchiveRow(row)"
                      v-permission="'system:approval:archive'"
                      @click="handleArchive(row)"
                    >归档</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button
                v-if="canDeleteRow(row)"
                size="small"
                type="danger"
                plain
                v-permission="'system:approval:delete'"
                @click="handleDelete(row)"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="暂无审批单" description="当前筛选条件下没有审批记录，点击右上角可提交新审批单" />

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

    <el-dialog v-model="formVisible" title="提交审批单" width="680px" :lock-scroll="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="审批类型" prop="formType">
          <DictSelect
            v-model="form.formType"
            :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE"
            value-type="string"
            apply-default
            :exclude-values="['REGISTER']"
            :clearable="false"
          />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入审批标题" />
        </el-form-item>
        <el-form-item label="审批人" prop="approverUserId">
          <el-select v-model="form.approverUserId" filterable placeholder="请选择审批人" style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="`${u.username}(${u.nickname})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入审批说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="approveVisible" :title="approveAction === 'APPROVE' ? '审批通过' : '审批驳回'" width="520px" :lock-scroll="false">
      <el-input v-model="approveRemark" type="textarea" :rows="4" placeholder="请输入审批意见（可选）" />
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button :type="approveAction === 'APPROVE' ? 'primary' : 'danger'" @click="submitApprove">
          {{ approveAction === 'APPROVE' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="审批单详情" size="45%" :lock-scroll="false">
      <el-descriptions :column="1" border v-if="current.id">
        <el-descriptions-item label="单号">{{ current.formNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <DictTag :value="current.formType" :dict-type="DICT_TYPE.APPROVAL_FORM_TYPE" />
        </el-descriptions-item>
        <el-descriptions-item label="标题">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <DictTag :value="current.status" :dict-type="DICT_TYPE.APPROVAL_STATUS" />
        </el-descriptions-item>
        <el-descriptions-item label="申请人">{{ current.applicantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ current.approverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见">{{ String(current.resultRemark || '').trim() || '无' }}</el-descriptions-item>
        <el-descriptions-item v-if="current.formType === 'REGISTER'" label="注册账号">
          {{ registerDetail.username || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="current.formType === 'REGISTER'" label="昵称">
          {{ registerDetail.nickname || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="current.formType === 'REGISTER'" label="手机号">
          {{ registerDetail.mobile || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-else label="内容">{{ current.content || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="record-title">审批记录</div>
      <el-timeline>
        <el-timeline-item v-for="item in records" :key="item.id" :timestamp="item.createTime">
          <strong>{{ item.operatorName || '-' }}</strong> {{ actionText(item.action) }}
          <span v-if="item.remark">：{{ item.remark }}</span>
        </el-timeline-item>
      </el-timeline>
      <template #footer>
        <div v-if="current.id && (canApproveRow(current as ApprovalVO) || canArchiveRow(current as ApprovalVO))" class="detail-actions">
          <el-button
            v-if="canApproveRow(current as ApprovalVO)"
            v-permission="'system:approval:approve'"
            type="primary"
            @click="openApproveDialog(current as ApprovalVO, 'APPROVE')"
          >通过</el-button>
          <el-button
            v-if="canApproveRow(current as ApprovalVO)"
            v-permission="'system:approval:approve'"
            plain
            type="danger"
            @click="openApproveDialog(current as ApprovalVO, 'REJECT')"
          >驳回</el-button>
          <el-button
            v-if="canArchiveRow(current as ApprovalVO)"
            v-permission="'system:approval:archive'"
            plain
            @click="handleArchive(current as ApprovalVO)"
          >归档</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, TableInstance } from 'element-plus'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import type { ApprovalVO } from '@/api/system/approval'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import ListExportButton from '@/components/ListExportButton.vue'
import RecycleCenterLink from '@/components/RecycleCenterLink.vue'
import { DICT_TYPE } from '@/constants/dict'
import { useApprovalPage } from './composables/useApprovalPage'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const tableRef = ref<TableInstance | null>(null)
const formRef = ref<FormInstance | null>(null)

const {
  loading,
  tableRef: composableTableRef,
  total,
  list,
  userOptions,
  formVisible,
  approveVisible,
  detailVisible,
  formRef: composableFormRef,
  current,
  records,
  approveAction,
  approveRemark,
  registerDetail,
  queryParams,
  form,
  rules,
  actionText,
  canDeleteRow,
  getList,
  handleQuery,
  resetQuery,
  handleCreate,
  submitForm,
  canApproveRow,
  canArchiveRow,
  openApproveDialog,
  submitApprove,
  handleArchive,
  handleDelete,
  openDetail,
} = useApprovalPage()

watchEffect(() => {
  composableTableRef.value = tableRef.value
  composableFormRef.value = formRef.value
})
</script>

<style scoped>
.record-title {
  margin: var(--space-4) 0 var(--space-3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
}
</style>
