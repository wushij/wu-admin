<template>
  <div class="storage-config-pane">
    <!-- 存储引擎选择网格 (4大引擎快速切换与状态监控) -->
    <div class="storage-engine-grid">
      <div
        v-for="eng in engineList"
        :key="eng.type"
        class="engine-card"
        :class="{
          selected: selectedEngine === eng.type,
          active: activeType === eng.type
        }"
        @click="canEdit && !savingStorage && !testingStorage && (selectedEngine = eng.type)"
      >
        <div class="engine-top">
          <div class="engine-icon-wrap" :class="eng.type">
            <el-icon><component :is="eng.icon" /></el-icon>
          </div>
          <div class="engine-status-tags">
            <el-tag v-if="activeType === eng.type" type="success" size="small" effect="dark">
              当前生效
            </el-tag>
            <el-tag v-else type="info" size="small" effect="plain">
              待激活
            </el-tag>
          </div>
        </div>
        <div class="engine-title">{{ eng.name }}</div>
        <div class="engine-desc">{{ eng.desc }}</div>
      </div>
    </div>

    <!-- 选中存储引擎的参数配置表单 Card -->
    <el-card shadow="never" class="engine-param-card">
      <template #header>
        <div class="card-header-row">
          <div class="header-left">
            <el-icon class="header-icon"><Setting /></el-icon>
            <span class="card-title">{{ getEngineTitle(selectedEngine) }} 参数配置</span>
            <el-tag
              v-if="selectedEngine === activeType"
              :type="healthy ? 'success' : 'danger'"
              size="small"
              class="health-tag"
            >
              {{ healthy ? '连通正常' : '连通异常' }}
            </el-tag>
          </div>
          <div class="header-actions">
            <el-button
              v-if="selectedEngine !== activeType"
              type="warning"
              plain
              size="small"
              :disabled="!canEdit || savingStorage || testingStorage"
              @click="handleApplyCurrentEngine"
            >
              设为当前生效引擎
            </el-button>
            <el-button
              type="primary"
              plain
              size="small"
              :icon="Connection"
              :loading="testingStorage"
              :disabled="!canEdit || savingStorage"
              @click="handleTestConnection"
            >
              测试当前引擎连通性
            </el-button>
            <el-button
              type="primary"
              size="small"
              :icon="Check"
              :loading="savingStorage"
              :disabled="!canEdit || testingStorage"
              @click="handleSaveAndReload"
            >
              保存并热重载存储引擎
            </el-button>
          </div>
        </div>
      </template>

      <!-- 1. 本地磁盘存储配置 -->
      <el-form
        v-if="selectedEngine === 'local'"
        label-width="140px"
        class="config-form"
        :disabled="!canEdit || savingStorage || testingStorage"
        autocomplete="off"
        @submit.prevent
      >
        <el-form-item label="存储根目录">
          <el-input
            v-model="storageForm.localPath"
            placeholder="例如：./data/uploads 或绝对路径（留空使用默认目录）"
          />
          <div class="form-tip">服务端本地存储文件的存放根目录，支持相对或绝对路径</div>
        </el-form-item>
        <el-form-item label="外网访问域名">
          <el-input
            v-model="storageForm.localDomain"
            placeholder="例如：https://static.example.com（选填，留空走默认系统接口）"
          />
          <div class="form-tip">留空时系统默认通过当前后端网关 /files/ 路由提供静态文件读取</div>
        </el-form-item>
      </el-form>

      <!-- 2. MinIO 对象存储配置 -->
      <el-form
        v-if="selectedEngine === 'minio'"
        label-width="140px"
        class="config-form"
        :disabled="!canEdit || savingStorage || testingStorage"
        autocomplete="off"
        @submit.prevent
      >
        <el-form-item label="服务端点 (Endpoint)">
          <el-input
            v-model="storageForm.minioEndpoint"
            placeholder="例如：http://127.0.0.1:9000 或 https://minio.example.com"
          />
          <div class="form-tip">MinIO 服务的 API 地址（包含 http:// 或 https:// 协议头）</div>
        </el-form-item>
        <el-form-item label="存储桶 (Bucket)">
          <el-input
            v-model="storageForm.minioBucket"
            placeholder="例如：wu-admin（不存在时将自动创建）"
          />
        </el-form-item>
        <el-form-item label="AccessKey">
          <el-input
            v-model="storageForm.minioAccessKey"
            placeholder="MinIO 访问密钥 AccessKey"
            autocomplete="off"
            name="storage-minio-ak"
          />
        </el-form-item>
        <el-form-item label="SecretKey">
          <el-input
            v-model="storageForm.minioSecretKey"
            type="password"
            show-password
            autocomplete="new-password"
            name="storage-minio-sk"
            :placeholder="storageForm.minioSecretKeyConfigured ? '已配置有效密钥（填入新值覆盖，保留不变请留空）' : '请输入 MinIO SecretKey'"
          />
        </el-form-item>
        <el-form-item label="外网访问域名">
          <el-input
            v-model="storageForm.minioDomain"
            placeholder="例如：https://oss.example.com（选填，用于客户端直接访问与 CDN 加速）"
          />
          <div class="form-tip">选填，配置后文件 URL 将直接拼接该域名；未配置时使用服务端点</div>
        </el-form-item>
      </el-form>

      <!-- 3. 腾讯云 COS 配置 -->
      <el-form
        v-if="selectedEngine === 'cos'"
        label-width="140px"
        class="config-form"
        :disabled="!canEdit || savingStorage || testingStorage"
        autocomplete="off"
        @submit.prevent
      >
        <el-form-item label="地域 (Region)">
          <el-input
            v-model="storageForm.cosRegion"
            placeholder="例如：ap-guangzhou、ap-beijing 或 ap-shanghai"
          />
          <div class="form-tip">腾讯云 COS 存储桶所在的区域简称</div>
        </el-form-item>
        <el-form-item label="存储桶 (Bucket)">
          <el-input
            v-model="storageForm.cosBucket"
            placeholder="例如：wuadmin-1250000000（包含 APPID）"
          />
        </el-form-item>
        <el-form-item label="SecretId">
          <el-input
            v-model="storageForm.cosSecretId"
            placeholder="腾讯云 API 访问凭证 SecretId"
            autocomplete="off"
            name="storage-cos-sid"
          />
        </el-form-item>
        <el-form-item label="SecretKey">
          <el-input
            v-model="storageForm.cosSecretKey"
            type="password"
            show-password
            autocomplete="new-password"
            name="storage-cos-skey"
            :placeholder="storageForm.cosSecretKeyConfigured ? '已配置有效密钥（填入新值覆盖，保留不变请留空）' : '请输入 SecretKey'"
          />
        </el-form-item>
        <el-form-item label="CDN加速域名">
          <el-input
            v-model="storageForm.cosDomain"
            placeholder="例如：https://cos.example.com（选填）"
          />
          <div class="form-tip">选填，腾讯云自定义绑定域名或 CDN 加速域名</div>
        </el-form-item>
      </el-form>

      <!-- 4. 阿里云 OSS 配置 -->
      <el-form
        v-if="selectedEngine === 'oss'"
        label-width="140px"
        class="config-form"
        :disabled="!canEdit || savingStorage || testingStorage"
        autocomplete="off"
        @submit.prevent
      >
        <el-form-item label="地域节点 (Endpoint)">
          <el-input
            v-model="storageForm.ossEndpoint"
            placeholder="例如：oss-cn-hangzhou.aliyuncs.com"
          />
          <div class="form-tip">阿里云 OSS 外网访问域名节点</div>
        </el-form-item>
        <el-form-item label="存储空间 (Bucket)">
          <el-input
            v-model="storageForm.ossBucket"
            placeholder="例如：wuadmin-bucket"
          />
        </el-form-item>
        <el-form-item label="AccessKey ID">
          <el-input
            v-model="storageForm.ossAccessKeyId"
            placeholder="LTAIxxxxxxxxxxxxxxxx"
            autocomplete="off"
            name="storage-oss-ak"
          />
        </el-form-item>
        <el-form-item label="AccessKey Secret">
          <el-input
            v-model="storageForm.ossAccessKeySecret"
            type="password"
            show-password
            autocomplete="new-password"
            name="storage-oss-sk"
            :placeholder="storageForm.ossAccessKeySecretConfigured ? '已配置有效密钥（填入新值覆盖，保留不变请留空）' : '请输入 AccessKey Secret'"
          />
        </el-form-item>
        <el-form-item label="自定义域名">
          <el-input
            v-model="storageForm.ossDomain"
            placeholder="例如：https://oss.example.com（选填）"
          />
          <div class="form-tip">选填，用户绑定在 OSS 上的独立加速域名</div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 全局文件上传规格与格式限制 Card -->
    <el-card shadow="never" class="policy-card">
      <template #header>
        <div class="card-header-row">
          <div class="header-left">
            <el-icon class="header-icon"><Setting /></el-icon>
            <span class="card-title">文件上传规格与格式限制</span>
          </div>
        </div>
      </template>
      <el-form
        :disabled="!canEdit || savingStorage || testingStorage"
        label-width="140px"
        class="config-form"
        autocomplete="off"
        @submit.prevent
      >
        <el-form-item label="单文件上限">
          <el-input-number
            v-model="storageForm.maxSizeMb"
            :min="1"
            :max="platformMaxFileMb"
            controls-position="right"
            style="width: 160px"
          />
          <span class="unit">MB（平台统一单文件上传大小限制，最高 {{ platformMaxFileMb }} MB）</span>
        </el-form-item>
        <el-form-item label="允许扩展名">
          <el-input
            v-model="storageForm.allowedExtensions"
            type="textarea"
            :rows="3"
            placeholder="逗号分隔，例如：jpg,jpeg,png,gif,webp,pdf,doc,docx,zip"
          />
          <div class="form-tip">全系统文件上传白名单，留空表示允许常见常规格式，危险可执行扩展名将被底层拦截</div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  FolderOpened,
  Coin,
  Cloudy,
  Platform,
  Setting,
  Connection,
  Check
} from '@element-plus/icons-vue'
import {
  getStorageConfig,
  updateStorageConfig,
  testStorageConfig
} from '@/api/system/config'
import { getErrorMessage, isErrorToastShown } from '@/utils/axiosError'
import type { ConfigGroupMap, StorageEngineType, StorageConfigDTO, StorageConfigVO } from '@/types/config'

const props = defineProps<{
  draft: ConfigGroupMap['file']
  canEdit: boolean
  platformMaxFileMb: number
}>()

const emit = defineEmits<{
  (e: 'storageLoaded'): void
}>()

const selectedEngine = ref<StorageEngineType>('local')
const activeType = ref<StorageEngineType>('local')
const healthy = ref<boolean>(true)
const savingStorage = ref(false)
const testingStorage = ref(false)

const engineList = [
  {
    type: 'local' as StorageEngineType,
    name: '本地磁盘存储',
    desc: '服务端本地目录高速读写，开发与单机部署首选',
    icon: FolderOpened
  },
  {
    type: 'minio' as StorageEngineType,
    name: 'MinIO 对象存储',
    desc: '私有化部署开源对象存储，兼容 S3 协议规范',
    icon: Coin
  },
  {
    type: 'cos' as StorageEngineType,
    name: '腾讯云 COS',
    desc: '腾讯云高可用对象存储，自带全球加速与 CDN',
    icon: Cloudy
  },
  {
    type: 'oss' as StorageEngineType,
    name: '阿里云 OSS',
    desc: '阿里云高可靠对象存储，企业级海量资源推荐',
    icon: Platform
  }
]

const storageForm = reactive<StorageConfigVO & { minioSecretKey?: string; cosSecretKey?: string; ossAccessKeySecret?: string }>({
  type: 'local',
  localPath: './data/uploads',
  localDomain: '',
  minioDomain: '',
  minioEndpoint: 'http://127.0.0.1:9000',
  minioAccessKey: '',
  minioSecretKey: '',
  minioBucket: 'wu-admin',
  minioSecretKeyConfigured: false,
  cosRegion: 'ap-guangzhou',
  cosBucket: '',
  cosSecretId: '',
  cosSecretKey: '',
  cosDomain: '',
  cosSecretKeyConfigured: false,
  ossEndpoint: 'oss-cn-hangzhou.aliyuncs.com',
  ossBucket: '',
  ossAccessKeyId: '',
  ossAccessKeySecret: '',
  ossDomain: '',
  ossAccessKeySecretConfigured: false,
  maxSizeMb: props.draft.maxSizeMb || 50,
  allowedExtensions: props.draft.allowedExtensions || 'jpg,jpeg,png,gif,webp',
  activeType: 'local',
  activeBucket: '',
  healthy: true
})

function getEngineTitle(type: StorageEngineType) {
  switch (type) {
    case 'local':
      return '本地磁盘存储 (Local)'
    case 'minio':
      return 'MinIO 私有化对象存储'
    case 'cos':
      return '腾讯云 COS 对象存储'
    case 'oss':
      return '阿里云 OSS 对象存储'
    default:
      return type
  }
}

async function loadConfig() {
  try {
    const res = await getStorageConfig()
    if (res?.data) {
      Object.assign(storageForm, res.data)
      storageForm.minioSecretKey = ''
      storageForm.cosSecretKey = ''
      storageForm.ossAccessKeySecret = ''
      activeType.value = res.data.activeType || 'local'
      selectedEngine.value = res.data.type || res.data.activeType || 'local'
      healthy.value = !!res.data.healthy

      // 同步回 props.draft
      syncToDraft()
      emit('storageLoaded')
    }
  } catch (e: any) {
    if (!isErrorToastShown(e)) {
      ElMessage.error(getErrorMessage(e) || '获取存储配置失败')
    }
  }
}

function syncToDraft() {
  props.draft.type = selectedEngine.value
  props.draft.localPath = storageForm.localPath
  props.draft.localDomain = storageForm.localDomain
  props.draft.minioDomain = storageForm.minioDomain
  props.draft.minioEndpoint = storageForm.minioEndpoint
  props.draft.minioAccessKey = storageForm.minioAccessKey
  if (storageForm.minioSecretKey) {
    props.draft.minioSecretKey = storageForm.minioSecretKey
  }
  props.draft.minioBucket = storageForm.minioBucket
  props.draft.cosRegion = storageForm.cosRegion
  props.draft.cosBucket = storageForm.cosBucket
  props.draft.cosSecretId = storageForm.cosSecretId
  if (storageForm.cosSecretKey) {
    props.draft.cosSecretKey = storageForm.cosSecretKey
  }
  props.draft.cosDomain = storageForm.cosDomain
  props.draft.ossEndpoint = storageForm.ossEndpoint
  props.draft.ossBucket = storageForm.ossBucket
  props.draft.ossAccessKeyId = storageForm.ossAccessKeyId
  if (storageForm.ossAccessKeySecret) {
    props.draft.ossAccessKeySecret = storageForm.ossAccessKeySecret
  }
  props.draft.ossDomain = storageForm.ossDomain
  props.draft.maxSizeMb = storageForm.maxSizeMb
  props.draft.allowedExtensions = storageForm.allowedExtensions
}

// 监听表单变更，实时同步回 draft，确保父组件全局保存时包含全部最新改动
watch(
  () => ({ ...storageForm, selectedEngine: selectedEngine.value }),
  () => {
    syncToDraft()
  },
  { deep: true }
)

function handleApplyCurrentEngine() {
  void handleSaveAndReload()
}

function buildDto(): StorageConfigDTO {
  return {
    type: selectedEngine.value,
    localPath: storageForm.localPath,
    localDomain: storageForm.localDomain,
    minioDomain: storageForm.minioDomain,
    minioEndpoint: storageForm.minioEndpoint,
    minioAccessKey: storageForm.minioAccessKey,
    minioSecretKey: storageForm.minioSecretKey,
    minioBucket: storageForm.minioBucket,
    cosRegion: storageForm.cosRegion,
    cosBucket: storageForm.cosBucket,
    cosSecretId: storageForm.cosSecretId,
    cosSecretKey: storageForm.cosSecretKey,
    cosDomain: storageForm.cosDomain,
    ossEndpoint: storageForm.ossEndpoint,
    ossBucket: storageForm.ossBucket,
    ossAccessKeyId: storageForm.ossAccessKeyId,
    ossAccessKeySecret: storageForm.ossAccessKeySecret,
    ossDomain: storageForm.ossDomain,
    maxSizeMb: storageForm.maxSizeMb,
    allowedExtensions: storageForm.allowedExtensions
  }
}

async function handleTestConnection() {
  testingStorage.value = true
  try {
    const dto = buildDto()
    await testStorageConfig(dto)
    ElMessage.success(`「${getEngineTitle(selectedEngine.value)}」连通性测试通过，存储服务可正常读写！`)
    if (selectedEngine.value === activeType.value) {
      healthy.value = true
    }
  } catch (e: any) {
    if (!isErrorToastShown(e)) {
      ElMessage.error(getErrorMessage(e) || '存储引擎连通性测试失败，请检查配置参数')
    }
    if (selectedEngine.value === activeType.value) {
      healthy.value = false
    }
  } finally {
    testingStorage.value = false
  }
}

async function handleSaveAndReload() {
  savingStorage.value = true
  try {
    const dto = buildDto()
    await updateStorageConfig(dto)
    ElMessage.success(`文件存储引擎已保存并成功热切换为「${getEngineTitle(selectedEngine.value)}」！`)
    await loadConfig()
  } catch (e: any) {
    if (!isErrorToastShown(e)) {
      ElMessage.error(getErrorMessage(e) || '保存并热重载存储配置失败')
    }
  } finally {
    savingStorage.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.storage-config-pane {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 4大引擎卡片选择网格 */
.storage-engine-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .storage-engine-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 680px) {
  .storage-engine-grid {
    grid-template-columns: 1fr;
  }
}

.engine-card {
  position: relative;
  background: var(--el-bg-color-overlay, #ffffff);
  border: 1.5px solid var(--el-border-color-lighter, #e4e7ed);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, background-color 0.2s ease;
  display: flex;
  flex-direction: column;
  user-select: none;
  box-sizing: border-box;
  overflow: hidden;
}

.engine-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.engine-card.selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9, rgba(64, 158, 255, 0.06));
  box-shadow: 0 0 0 1px var(--el-color-primary);
}

.engine-card.selected:hover {
  box-shadow: 0 0 0 1px var(--el-color-primary), 0 4px 12px rgba(0, 0, 0, 0.05);
}

.engine-card.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--el-color-success);
  border-top-left-radius: 10px;
  border-bottom-left-radius: 10px;
  pointer-events: none;
}

.engine-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.engine-icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.engine-icon-wrap.local {
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
}

.engine-icon-wrap.minio {
  background: rgba(230, 76, 76, 0.12);
  color: #e64c4c;
}

.engine-icon-wrap.cos {
  background: rgba(0, 164, 255, 0.12);
  color: #00a4ff;
}

.engine-icon-wrap.oss {
  background: rgba(255, 106, 0, 0.12);
  color: #ff6a00;
}

.engine-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 6px;
}

.engine-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

/* 参数面板与卡片 */
.engine-param-card,
.policy-card {
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter, #e4e7ed);
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 16px;
  color: var(--el-color-primary);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.health-tag {
  margin-left: 6px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.config-form {
  padding-top: 10px;
}

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  line-height: 1.4;
}

.unit {
  margin-left: 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
