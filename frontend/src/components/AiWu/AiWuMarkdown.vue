<template>
  <div class="ai-wu-markdown" :class="{ 'is-streaming': streaming }" v-html="html" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useStreamingMarkdown } from '@/composables/useStreamingMarkdown'

const props = defineProps<{
  content: string
  streaming?: boolean
}>()

const { html } = useStreamingMarkdown(
  computed(() => props.content),
  computed(() => props.streaming),
)
</script>

<!-- v-html 生成的节点无法命中 scoped 样式，此处使用全局样式并以 .ai-wu-markdown 收敛作用域 -->
<style lang="scss">
.ai-wu-markdown {
  width: 100%;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.7;
  color: inherit;
  word-break: break-word;

  p {
    margin: 0 0 8px;
  }

  p:first-child,
  h1:first-child,
  h2:first-child,
  h3:first-child,
  h4:first-child {
    margin-top: 0;
  }

  p:last-child,
  ul:last-child,
  ol:last-child,
  pre:last-child,
  blockquote:last-child,
  table:last-child {
    margin-bottom: 0;
  }

  strong {
    font-weight: 700;
    color: #0f172a;
  }

  h1,
  h2,
  h3,
  h4 {
    margin: 10px 0 6px;
    font-weight: 700;
    line-height: 1.35;
    color: #0f172a;
  }

  h1,
  h2,
  h3 {
    font-size: 13.5px;
  }

  h4 {
    font-size: 13px;
  }

  ul,
  ol {
    margin: 6px 0 8px;
    padding-left: 18px;
  }

  li {
    margin: 3px 0;
  }

  li::marker {
    color: var(--theme-primary, #6366f1);
  }

  blockquote {
    margin: 8px 0;
    padding: 6px 10px;
    border-left: 3px solid var(--theme-primary, #6366f1);
    background: rgba(99, 102, 241, 0.06);
    border-radius: 0 8px 8px 0;
    color: #475569;
  }

  a {
    color: var(--theme-primary, #4f46e5);
    text-decoration: underline;
    text-underline-offset: 2px;
    cursor: pointer;

    &:hover {
      opacity: 0.8;
    }
  }

  table {
    width: 100%;
    table-layout: fixed;
    border-collapse: collapse;
    margin: 8px 0;
    font-size: 12px;
  }

  th,
  td {
    border: 1px solid #e2e8f0;
    padding: 5px 6px;
    text-align: left;
    vertical-align: top;
    white-space: normal;
    word-break: break-all;
    overflow-wrap: anywhere;
  }

  th {
    background: #f8fafc;
    font-weight: 600;
    color: #0f172a;
  }

  pre {
    margin: 8px 0;
    padding: 12px;
    border-radius: 10px;
    background: #0f172a;
    overflow-x: auto;
    font-size: 12px;
    line-height: 1.55;

    &::-webkit-scrollbar {
      height: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(148, 163, 184, 0.4);
      border-radius: 4px;
    }
  }

  code {
    font-family: Consolas, 'Courier New', ui-monospace, monospace;
    font-size: 12px;
  }

  :not(pre) > code {
    padding: 2px 6px;
    border-radius: 4px;
    background: rgba(99, 102, 241, 0.1);
    color: #4338ca;
  }

  pre code {
    background: transparent;
    color: #e2e8f0;
    padding: 0;
  }

  img {
    max-width: 100%;
    border-radius: 10px;
    margin: 6px 0;
  }

  hr {
    border: none;
    border-top: 1px solid #e2e8f0;
    margin: 10px 0;
  }

  /* 流式输出时在最后一个块元素尾部追加闪烁光标 */
  &.is-streaming > *:last-child::after {
    content: '';
    display: inline-block;
    width: 2px;
    height: 13px;
    margin-left: 2px;
    vertical-align: -2px;
    background: var(--theme-primary, #6366f1);
    animation: ai-wu-md-cursor 0.9s steps(2) infinite;
  }
}

@keyframes ai-wu-md-cursor {
  50% {
    opacity: 0;
  }
}
</style>
