import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus, { ElDropdown, ElPopover, ElTooltip } from 'element-plus'
import { createApp, defineComponent } from 'vue'
import { setupGlobalTooltip } from '@/utils/tooltip-defaults'

describe('setupGlobalTooltip 全局悬浮提示隔离与控制', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('不修改原生 ElTooltip/ElPopover/ElDropdown 的默认 showAfter，内置组件保持 0ms', () => {
    const app = createApp({ template: '<div />' })
    app.use(ElementPlus)
    setupGlobalTooltip(app)

    const nativeTooltipProps = (ElTooltip as unknown as { props: { showAfter: { default: number } } }).props
    const popoverProps = (ElPopover as unknown as { props: { showAfter: { default: number } } }).props
    expect(nativeTooltipProps.showAfter.default).toBe(0)
    expect(popoverProps.showAfter.default).toBe(0)
  })

  it('点击 ElPopover 与 ElDropdown 时立即弹出，完全不受 1000ms 延迟影响', async () => {
    const Wrapper = defineComponent({
      template: `
        <div>
          <el-popover trigger="click" content="通知面板">
            <template #reference>
              <button type="button" class="popover-btn">铃铛</button>
            </template>
          </el-popover>
          <el-dropdown trigger="click">
            <button type="button" class="dropdown-btn">用户菜单</button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人中心</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      `,
    })

    const wrapper = mount(Wrapper, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus, { install: setupGlobalTooltip }],
        stubs: { teleport: true },
      },
    })

    await wrapper.find('.popover-btn').trigger('click')
    vi.advanceTimersByTime(10)
    await wrapper.vm.$nextTick()
    const popoverEl = document.querySelector('.el-popover') as HTMLElement | null
    expect(popoverEl).toBeTruthy()
    expect(popoverEl?.style.display).not.toBe('none')

    await wrapper.find('.dropdown-btn').trigger('click')
    vi.advanceTimersByTime(10)
    await wrapper.vm.$nextTick()
    const dropdownEl = document.querySelector('.el-dropdown__popper') as HTMLElement | null
    expect(dropdownEl).toBeTruthy()
    expect(dropdownEl?.style.display).not.toBe('none')

    // 确认组件名与原生对象未受污染
    expect(ElDropdown.name).toBe('ElDropdown')
    wrapper.unmount()
  })

  it('业务模板 <el-tooltip> 默认悬浮 1000ms 才展示，点击或窗口失焦时立即收起且不再延迟弹出', async () => {
    const Wrapper = defineComponent({
      template: `
        <el-tooltip content="测试提示">
          <button type="button" class="test-btn">上传文件</button>
        </el-tooltip>
      `,
    })

    const wrapper = mount(Wrapper, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus, { install: setupGlobalTooltip }],
        stubs: { teleport: true },
      },
    })
    const isTooltipVisible = () => {
      const popper = document.querySelector('.el-popper') as HTMLElement | null
      return Boolean(popper && popper.style.display !== 'none')
    }
    const triggerBtn = wrapper.find('.test-btn')

    // 1. 悬浮 900ms 时不应弹出
    await triggerBtn.trigger('mouseenter')
    vi.advanceTimersByTime(900)
    await wrapper.vm.$nextTick()
    expect(isTooltipVisible()).toBe(false)

    // 2. 满 1000ms 后弹出
    vi.advanceTimersByTime(150)
    await wrapper.vm.$nextTick()
    expect(isTooltipVisible()).toBe(true)

    // 3. 点击（pointerdown）后立即收起
    document.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    await wrapper.vm.$nextTick()
    expect(isTooltipVisible()).toBe(false)

    // 4. 悬浮未满 1000ms 时点击按钮或窗口失焦，之后即使过完 1000ms 也不应再弹出
    await triggerBtn.trigger('mouseenter')
    vi.advanceTimersByTime(300)
    window.dispatchEvent(new Event('blur'))
    vi.advanceTimersByTime(1200)
    await wrapper.vm.$nextTick()
    expect(isTooltipVisible()).toBe(false)

    wrapper.unmount()
  })
})
