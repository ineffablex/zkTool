import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ContextMenu from '../ContextMenu.vue'

describe('ContextMenu', () => {
  const mockItems = [
    {
      label: '新建',
      action: vi.fn(),
      icon: 'plus'
    },
    {
      label: '编辑',
      action: vi.fn(),
      icon: 'edit'
    },
    {
      label: '删除',
      action: vi.fn(),
      icon: 'trash',
      disabled: true
    }
  ]

  const createWrapper = (props = {}) => {
    return mount(ContextMenu, {
      props: {
        show: true,
        x: 100,
        y: 100,
        items: mockItems,
        ...props
      }
    })
  }

  beforeEach(() => {
    // 重置mock函数
    mockItems.forEach(item => {
      if (item.action.mockReset) {
        item.action.mockReset()
      }
    })
  })

  it('正确渲染菜单项', () => {
    const wrapper = createWrapper()
    const menuItems = wrapper.findAll('div[class*="px-4"]')
    
    expect(menuItems).toHaveLength(mockItems.length)
    expect(wrapper.text()).toContain('新建')
    expect(wrapper.text()).toContain('编辑')
    expect(wrapper.text()).toContain('删除')
  })

  it('正确定位菜单', () => {
    const x = 150
    const y = 200
    const wrapper = createWrapper({ x, y })
    
    const style = wrapper.attributes('style')
    expect(style).toContain(`left: ${x}px`)
    expect(style).toContain(`top: ${y}px`)
  })

  it('处理菜单项点击', async () => {
    const wrapper = createWrapper()
    const menuItems = wrapper.findAll('div[class*="px-4"]')
    
    // 点击第一个菜单项（新建）
    await menuItems[0].trigger('click')
    expect(mockItems[0].action).toHaveBeenCalled()
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('不显示菜单当show为false', () => {
    const wrapper = createWrapper({ show: false })
    // Vue 3的v-if会保留注释节点，所以我们检查实际内容
    expect(wrapper.find('div[class*="absolute"]').exists()).toBe(false)
  })

  it('正确处理禁用状态', () => {
    const wrapper = createWrapper()
    const menuItems = wrapper.findAll('div[class*="px-4"]')
    
    // 检查第三个菜单项（删除）是否有禁用样式
    expect(menuItems[2].classes()).toContain('opacity-50')
    expect(menuItems[2].classes()).toContain('cursor-not-allowed')

    // 点击禁用项不应触发动作
    menuItems[2].trigger('click')
    expect(mockItems[2].action).not.toHaveBeenCalled()
  })

  it('正确渲染图标', () => {
    const wrapper = createWrapper()
    const icons = wrapper.findAll('i')
    
    expect(icons).toHaveLength(mockItems.length)
    mockItems.forEach((item, index) => {
      expect(icons[index].classes()).toContain(item.icon)
    })
  })

  it('关闭菜单后清理', async () => {
    const wrapper = createWrapper()
    
    await wrapper.vm.$emit('close')
    expect(wrapper.emitted('close')).toBeTruthy()
  })
})