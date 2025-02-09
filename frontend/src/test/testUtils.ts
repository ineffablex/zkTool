import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import type { Component } from 'vue'

export const createTestingOptions = (config: any = {}) => ({
  global: {
    plugins: [createTestingPinia({
      createSpy: vi.fn
    })],
    stubs: {
      ContextMenu: true,
      ...config.global?.stubs
    },
    ...config.global
  },
  props: config.props || {}
})

export const createWrapper = (component: Component, config = {}) => {
  return mount(component, createTestingOptions(config))
}

export const waitForResponse = async () => {
  await new Promise((resolve) => setTimeout(resolve, 0))
}

export const mockFetch = (mockData: any) => {
  return vi.spyOn(global, 'fetch').mockImplementation(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve(mockData)
    } as Response)
  )
}

export const mockFetchError = (errorMessage: string) => {
  return vi.spyOn(global, 'fetch').mockImplementation(() =>
    Promise.reject(new Error(errorMessage))
  )
}

export const findByTestId = (wrapper: any, testId: string) => {
  return wrapper.find(`[data-testid="${testId}"]`)
}

export const triggerContextMenu = async (element: any, coords = { x: 100, y: 100 }) => {
  await element.trigger('contextmenu', {
    clientX: coords.x,
    clientY: coords.y,
    preventDefault: () => {}
  })
}

export const mockResizeObserver = () => {
  return vi.fn(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn()
  }))
}

// 清理所有的mock
export const cleanupMocks = () => {
  vi.clearAllMocks()
  vi.restoreAllMocks()
}

// 创建测试数据
export const createTestNode = (path: string, name: string, opts = {}) => {
  return {
    path,
    name,
    expanded: false,
    selected: false,
    isLeaf: true,
    ...opts
  }
}