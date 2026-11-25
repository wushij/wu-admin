<template>
  <span
    class="file-type-icon"
    :class="[`file-type-icon--${size}`, `file-type-icon--${kind}`]"
    aria-hidden="true"
  >
    <svg
      viewBox="0 0 40 48"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      class="file-type-icon__svg"
    >
      <!-- 文档主体路径（右上角切出折角区域 + 主题自适应描边） -->
      <path
        d="M4.5 0.75H27.6893L39.25 12.3107V43.5C39.25 45.5711 37.5711 47.25 35.5 47.25H4.5C2.42893 47.25 0.75 45.5711 0.75 43.5V4.5C0.75 2.42893 2.42893 0.75 4.5 0.75Z"
        fill="var(--fti-bg)"
        stroke="var(--fti-border)"
        stroke-width="1.35"
      />
      <!-- 右上角折纸翻折角 -->
      <path
        d="M27.5 0.75L39.25 12.5H30.5C28.8431 12.5 27.5 11.1569 27.5 9.5V0.75Z"
        fill="var(--fti-fold)"
      />

      <!-- ZIP / RAR 压缩包拉链图案 -->
      <g v-if="kind === 'archive'">
        <rect x="16" y="1" width="4" height="3.5" fill="var(--fti-ink)" opacity="0.88" />
        <rect x="20" y="4.5" width="4" height="3.5" fill="var(--fti-ink)" opacity="0.88" />
        <rect x="16" y="8" width="4" height="3.5" fill="var(--fti-ink)" opacity="0.88" />
        <rect x="20" y="11.5" width="4" height="3.5" fill="var(--fti-ink)" opacity="0.88" />
        <rect x="16" y="15" width="4" height="3.5" fill="var(--fti-ink)" opacity="0.88" />
        <rect x="20" y="18.5" width="4" height="3.5" fill="var(--fti-ink)" opacity="0.88" />
        <rect
          x="15.5"
          y="24"
          width="9"
          height="11"
          rx="2.2"
          fill="var(--fti-ink)"
        />
        <rect
          x="18.2"
          y="27"
          width="3.6"
          height="4.6"
          rx="1"
          fill="var(--fti-bg)"
        />
      </g>

      <!-- 代码文件符号 </> -->
      <text
        v-else-if="kind === 'code'"
        x="20"
        y="33.5"
        text-anchor="middle"
        fill="var(--fti-ink)"
        font-size="13"
        font-weight="700"
        font-family="ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace"
      >
        &lt;/&gt;
      </text>

      <!-- 视频播放图形 -->
      <g v-else-if="kind === 'video'">
        <circle cx="20" cy="29.5" r="9.2" stroke="var(--fti-ink)" stroke-width="2" />
        <path d="M17.4 25L25 29.5L17.4 34V25Z" fill="var(--fti-ink)" />
      </g>

      <!-- 音频波形图形 -->
      <g v-else-if="kind === 'audio'">
        <rect x="10.5" y="27" width="3" height="8" rx="1.5" fill="var(--fti-ink)" />
        <rect x="15.5" y="22" width="3" height="15" rx="1.5" fill="var(--fti-ink)" />
        <rect x="20.5" y="24.5" width="3" height="11.5" rx="1.5" fill="var(--fti-ink)" />
        <rect x="25.5" y="20" width="3" height="17.5" rx="1.5" fill="var(--fti-ink)" />
      </g>

      <!-- 图片山峰图形 -->
      <g v-else-if="kind === 'image'">
        <rect x="9.5" y="20.5" width="21" height="17" rx="3" stroke="var(--fti-ink)" stroke-width="1.9" />
        <circle cx="15.2" cy="25.8" r="2" fill="var(--fti-ink)" />
        <path
          d="M11 35L17.2 28.8L21.4 32.8L25 29.5L29 35H11Z"
          fill="var(--fti-ink)"
          opacity="0.88"
        />
      </g>

      <!-- 通用未知文档横线 -->
      <g v-else-if="kind === 'default'">
        <rect x="9" y="22" width="22" height="2.8" rx="1.4" fill="var(--fti-ink)" opacity="0.86" />
        <rect x="9" y="28.5" width="22" height="2.8" rx="1.4" fill="var(--fti-ink)" opacity="0.72" />
        <rect x="9" y="35" width="14" height="2.8" rx="1.4" fill="var(--fti-ink)" opacity="0.58" />
      </g>

      <!-- PDF / W / X / P / MD / TXT 徽标文字 -->
      <text
        v-else
        x="20"
        :y="badgeText.length >= 3 ? 33.5 : 34.5"
        text-anchor="middle"
        fill="var(--fti-ink)"
        :font-size="badgeText.length >= 3 ? 11.2 : badgeText.length === 2 ? 13.5 : 17.5"
        font-weight="700"
        letter-spacing="-0.2"
        font-family="Inter, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif"
      >
        {{ badgeText }}
      </text>
    </svg>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { resolveFileBadgeText, resolveFileDocKind } from '@/utils/fileTypeIcon'

/**
 * 通用折角文档类型图标组件（对齐 EduMind 折角封面语言，适配亮/暗双主题与皮肤主色）
 */
const props = withDefaults(
  defineProps<{
    fileName?: string
    fileType?: string
    size?: 'xs' | 'sm' | 'md' | 'lg'
  }>(),
  {
    fileName: '',
    fileType: '',
    size: 'md',
  },
)

const kind = computed(() => resolveFileDocKind(props.fileName, props.fileType))
const badgeText = computed(() => resolveFileBadgeText(kind.value))
</script>

<style lang="scss">
.file-type-icon {
  --fti-bg: var(--el-color-primary-light-9);
  --fti-fold: var(--el-color-primary-light-7);
  --fti-border: var(--el-color-primary-light-5);
  --fti-ink: var(--el-color-primary);

  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 0;
  user-select: none;

  &__svg {
    width: 100%;
    height: 100%;
    display: block;
  }

  &--xs {
    width: 18px;
    height: 22px;
  }

  &--sm {
    width: 30px;
    height: 36px;
  }

  &--md {
    width: 38px;
    height: 46px;
  }

  &--lg {
    width: 52px;
    height: 62px;
  }

  /* ── 亮色模式：柔光珠光纸感底 + 细腻描边 + 高对比语义标识 ── */
  &--pdf {
    --fti-bg: #fef2f2;
    --fti-fold: #fecaca;
    --fti-border: rgba(239, 68, 68, 0.34);
    --fti-ink: #dc2626;
  }

  &--word {
    --fti-bg: #eff6ff;
    --fti-fold: #bfdbfe;
    --fti-border: rgba(37, 99, 235, 0.32);
    --fti-ink: #1d4ed8;
  }

  &--excel {
    --fti-bg: #ecfdf5;
    --fti-fold: #a7f3d0;
    --fti-border: rgba(16, 185, 129, 0.34);
    --fti-ink: #047857;
  }

  &--ppt {
    --fti-bg: #fff7ed;
    --fti-fold: #fed7aa;
    --fti-border: rgba(234, 88, 12, 0.34);
    --fti-ink: #c2410c;
  }

  &--archive {
    --fti-bg: #fffbeb;
    --fti-fold: #fde68a;
    --fti-border: rgba(217, 119, 6, 0.34);
    --fti-ink: #b45309;
  }

  /* Markdown 与通用文档直接融入当前皮肤主色 */
  &--markdown,
  &--default {
    --fti-bg: var(--el-color-primary-light-9);
    --fti-fold: var(--el-color-primary-light-7);
    --fti-border: var(--el-color-primary-light-5);
    --fti-ink: var(--el-color-primary);
  }

  &--code {
    --fti-bg: #f5f3ff;
    --fti-fold: #ddd6fe;
    --fti-border: rgba(124, 58, 237, 0.32);
    --fti-ink: #6d28d9;
  }

  &--video {
    --fti-bg: #f0fdfa;
    --fti-fold: #99f6e4;
    --fti-border: rgba(13, 148, 136, 0.34);
    --fti-ink: #0f766e;
  }

  &--audio {
    --fti-bg: #fdf2f8;
    --fti-fold: #fbcfe8;
    --fti-border: rgba(219, 39, 119, 0.32);
    --fti-ink: #be185d;
  }

  &--image {
    --fti-bg: #f0f9ff;
    --fti-fold: #bae6fd;
    --fti-border: rgba(2, 132, 199, 0.32);
    --fti-ink: #0369a1;
  }

  &--text {
    --fti-bg: var(--color-bg-subtle);
    --fti-fold: var(--color-border);
    --fti-border: var(--color-border-strong);
    --fti-ink: var(--color-text-secondary);
  }
}

/* ── 暗黑模式：深空彩釉玻璃通透基底 + 柔亮描边与高对比标识 ── */
html.dark .file-type-icon--pdf {
  --fti-bg: rgba(239, 68, 68, 0.16);
  --fti-fold: rgba(248, 113, 113, 0.32);
  --fti-border: rgba(248, 113, 113, 0.42);
  --fti-ink: #fca5a5;
}

html.dark .file-type-icon--word {
  --fti-bg: rgba(59, 130, 246, 0.16);
  --fti-fold: rgba(96, 165, 250, 0.32);
  --fti-border: rgba(96, 165, 250, 0.42);
  --fti-ink: #93c5fd;
}

html.dark .file-type-icon--excel {
  --fti-bg: rgba(16, 185, 129, 0.16);
  --fti-fold: rgba(52, 211, 153, 0.32);
  --fti-border: rgba(52, 211, 153, 0.42);
  --fti-ink: #6ee7b7;
}

html.dark .file-type-icon--ppt {
  --fti-bg: rgba(249, 115, 22, 0.16);
  --fti-fold: rgba(251, 146, 60, 0.32);
  --fti-border: rgba(251, 146, 60, 0.42);
  --fti-ink: #fdba74;
}

html.dark .file-type-icon--archive {
  --fti-bg: rgba(245, 158, 11, 0.16);
  --fti-fold: rgba(251, 191, 36, 0.32);
  --fti-border: rgba(251, 191, 36, 0.42);
  --fti-ink: #fde68a;
}

html.dark .file-type-icon--markdown,
html.dark .file-type-icon--default {
  --fti-bg: color-mix(in srgb, var(--el-color-primary) 16%, var(--color-bg-surface));
  --fti-fold: color-mix(in srgb, var(--el-color-primary) 34%, var(--color-bg-surface));
  --fti-border: color-mix(in srgb, var(--el-color-primary) 44%, transparent);
  --fti-ink: var(--el-color-primary);
}

html.dark .file-type-icon--code {
  --fti-bg: rgba(139, 92, 246, 0.16);
  --fti-fold: rgba(167, 139, 250, 0.32);
  --fti-border: rgba(167, 139, 250, 0.42);
  --fti-ink: #c4b5fd;
}

html.dark .file-type-icon--video {
  --fti-bg: rgba(20, 184, 166, 0.16);
  --fti-fold: rgba(45, 212, 191, 0.32);
  --fti-border: rgba(45, 212, 191, 0.42);
  --fti-ink: #5eead4;
}

html.dark .file-type-icon--audio {
  --fti-bg: rgba(236, 72, 153, 0.16);
  --fti-fold: rgba(244, 114, 182, 0.32);
  --fti-border: rgba(244, 114, 182, 0.42);
  --fti-ink: #f9a8d4;
}

html.dark .file-type-icon--image {
  --fti-bg: rgba(14, 165, 233, 0.16);
  --fti-fold: rgba(56, 189, 248, 0.32);
  --fti-border: rgba(56, 189, 248, 0.42);
  --fti-ink: #7dd3fc;
}

html.dark .file-type-icon--text {
  --fti-bg: rgba(148, 163, 184, 0.12);
  --fti-fold: rgba(148, 163, 184, 0.26);
  --fti-border: rgba(148, 163, 184, 0.32);
  --fti-ink: var(--color-text-secondary);
}
</style>
