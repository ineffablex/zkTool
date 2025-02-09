import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import TreeView from '../TreeView.vue'
import { useZkStore } from '../../store'
import type { NodeData } from '../../types/node'

const mockData: NodeData[] = [
  {
    path: '/root',
    name: 'root',
    expanded: false,
    selected: false,
    children: [
      {
        path: '/root/child1',
        name: 'child1',
        expanded: false,
        selected: false,
        isLeaf: true,
        data: 'child1 data'
      },
      {
        path: '/root/child2',
        name: 'child2',
        expanded: false,
        selected: false,
        children: [
          {
            path: '/root/child2/grandchild',
            name: 'grandchild',
            expanded: false,
            selected: false,
            isLeaf: true,
            data: 'grandchild data'
          }
        ]
      }
    ]
  }
]

const createWrapper = () => {
  const pinia = createTestingPinia({
    createSpy: vi.fn
  })
  const wrapper = mount(TreeView, {
    props: {
      data: JSON.parse(JSON.stringify(mockData))
    },
    global: {
      plugins: [pinia],
      stubs: {
        ContextMenu: true
      }
    }
  })
  const store = useZkStore()
  return { wrapper, store }
}

const createStatusWrapper = () => {
  const statusData = {
    path: '/test',
    name: 'test',
    status: 'connected',
    expanded: false,
    selected: false
  }
  
  return mount(TreeView, {
    props: {
      data: [statusData]
    },
    global: {
      plugins: [createTestingPinia()],
      stubs: {
        ContextMenu: true
      }
    }
  })
}

describe('TreeView 组件', () => {
  describe('基础功能', () => {
    beforeEach(() => {
      document.body.innerHTML = ''
    })

    it('初始状态只显示根节点，展开后显示子节点', async () => {
      const { wrapper } = createWrapper()
      
      // 检查初始状态
      let items = wrapper.findAll('li')
      expect(items.length).toBe(1)
      expect(wrapper.text()).toContain('root')
      
      // 展开根节点
      const toggleButton = wrapper.find('[data-testid="toggle-button"]')
      await toggleButton.trigger('click')
      
      // 验证子节点显示
      items = wrapper.findAll('li')
      expect(items.length).toBe(3)
      expect(wrapper.text()).toContain('child1')
      expect(wrapper.text()).toContain('child2')
    })

    it('点击展开/折叠按钮可以控制子节点的显示和隐藏', async () => {
      const { wrapper } = createWrapper()
      const rootNode = wrapper.find('li')
      const toggleButton = rootNode.find('[data-testid="toggle-button"]')
      
      // 初始状态是折叠的
      expect(wrapper.find('.pl-6').exists()).toBe(false)
      
      // 点击展开
      await toggleButton.trigger('click')
      expect(wrapper.find('.pl-6').exists()).toBe(true)
      
      // 点击折叠
      await toggleButton.trigger('click')
      expect(wrapper.find('.pl-6').exists()).toBe(false)
    })

    it('点击节点可以正确设置选中状态', async () => {
      const { wrapper, store } = createWrapper()
      const nodeDiv = wrapper.find('div[class*="flex items-center"]')
      
      await nodeDiv.trigger('click')
      
      expect(nodeDiv.classes()).toContain('bg-blue-50')
      expect(store.setSelectedNode).toHaveBeenCalledWith({
        path: '/root',
        name: 'root',
        expanded: false,
        selected: true,
        children: expect.any(Array)
      })
    })
  })

  describe('交互功能', () => {
    it('右键点击节点可以显示上下文菜单', async () => {
      const { wrapper } = createWrapper()
      const nodeDiv = wrapper.find('div[class*="flex items-center"]')
      
      await nodeDiv.trigger('contextmenu', {
        clientX: 100,
        clientY: 100,
        preventDefault: () => {}
      })
      
      expect(wrapper.emitted('node-context-menu')).toBeTruthy()
      const emittedEvent = wrapper.emitted('node-context-menu')?.[0][0]
      expect(emittedEvent).toEqual({
        node: {
          path: '/root',
          name: 'root',
          expanded: false,
          selected: false,
          children: expect.any(Array)
        },
        x: 100,
        y: 100
      })
    })

    it('根据节点类型显示不同的图标样式', async () => {
      const { wrapper } = createWrapper()
      
      // 展开节点以显示所有图标
      await wrapper.find('[data-testid="toggle-button"]').trigger('click')
      
      // 检查文件夹图标
      const folderIcon = wrapper.find('[data-testid="folder-icon"]')
      expect(folderIcon.exists()).toBe(true)
      
      // 检查叶子节点图标
      const leafIcons = wrapper.findAll('[data-testid="leaf-icon"]')
      expect(leafIcons.length).toBeGreaterThan(0)
    })

    it('正确显示节点的连接状态', () => {
      const wrapper = createStatusWrapper()
      const statusBadge = wrapper.find('span[class*="bg-green-100"]')
      
      expect(statusBadge.exists()).toBe(true)
      expect(statusBadge.text()).toBe('connected')
    })

    it('触发正确的事件并传递正确的参数', async () => {
      const { wrapper } = createWrapper()
      const nodeDiv = wrapper.find('div[class*="flex items-center"]')
      
      // 点击事件
      await nodeDiv.trigger('click')
      expect(wrapper.emitted('node-click')).toBeTruthy()
      expect(wrapper.emitted('node-click')?.[0][0]).toEqual({
        path: '/root',
        name: 'root',
        expanded: false,
        selected: true,
        children: expect.any(Array)
      })
      
      // 右键菜单事件
      await nodeDiv.trigger('contextmenu', {
        clientX: 100,
        clientY: 100,
        preventDefault: () => {}
      })
      expect(wrapper.emitted('node-context-menu')).toBeTruthy()
    })
  })
})