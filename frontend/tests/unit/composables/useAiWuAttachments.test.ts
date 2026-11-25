import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { useAiWuAttachments } from '@/composables/aiWu/useAiWuAttachments'

const mockUploadChatAttachment = vi.fn()

vi.mock('@/api/ai', () => ({
  uploadChatAttachment: (...args: unknown[]) => mockUploadChatAttachment(...args),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      warning: vi.fn(),
      error: vi.fn(),
    },
  }
})

describe('useAiWuAttachments 响应式状态同步', () => {
  beforeEach(() => {
    mockUploadChatAttachment.mockReset()
  })

  it('文档解析完成后 items 中的 uploading 必须响应式变为 false', async () => {
    mockUploadChatAttachment.mockResolvedValue({
      data: {
        attachmentId: 'att-1',
        fileName: '高等数学.pdf',
        fileSize: 181862,
        fileType: 'pdf',
        isImage: false,
        parseStatus: 'SUCCESS',
        previewText: '极限与连续...',
      },
    })

    let api!: ReturnType<typeof useAiWuAttachments>
    const Comp = defineComponent({
      setup() {
        api = useAiWuAttachments()
        return { items: api.items }
      },
      template: `
        <div>
          <span v-for="item in items" :key="item.key" class="state">
            {{ item.uploading ? 'loading' : 'ready' }}
          </span>
        </div>
      `,
    })

    const wrapper = mount(Comp)
    const file = new File(['%PDF-1.4'], '高等数学.pdf', { type: 'application/pdf' })
    await api.addFiles([file])
    await flushPromises()

    expect(api.items.value).toHaveLength(1)
    expect(api.items.value[0].uploading).toBe(false)
    expect(api.readyCount.value).toBe(1)
    expect(wrapper.find('.state').text()).toBe('ready')

    wrapper.unmount()
  })
})
