import { vi } from 'vitest'
import { config } from '@vue/test-utils'

// 全局组件模拟
config.global.components = {}

// 全局属性模拟
config.global.mocks = {}

// ResizeObserver模拟
const ResizeObserverMock = vi.fn(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn()
}))

window.ResizeObserver = ResizeObserverMock

// 剪贴板API模拟
Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn()
  }
})

// 事件监听器模拟
window.addEventListener = vi.fn()
window.removeEventListener = vi.fn()

// 控制台警告抑制
console.warn = vi.fn()