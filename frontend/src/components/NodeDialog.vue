<template>
  <div class="node-dialog-overlay" v-if="show" @click="closeDialog">
    <div class="node-dialog" @click.stop>
      <div class="dialog-header">
        <h3>{{ isEdit ? '编辑节点' : '新建节点' }}</h3>
        <button class="close-btn" @click="closeDialog">&times;</button>
      </div>
      <div class="dialog-content">
        <div class="form-group">
          <label>节点路径</label>
          <div class="path-display">{{ fullPath }}</div>
        </div>
        <div class="form-group">
          <label>节点名称 <span class="required">*</span></label>
          <input 
            v-model="nodeName" 
            type="text" 
            :placeholder="isEdit ? '节点名称' : '请输入节点名称'"
            :disabled="isEdit"
            class="form-input"
            required
          >
          <span v-if="validationErrors.name" class="error-message">{{ validationErrors.name }}</span>
        </div>
        <div class="form-group">
          <label>节点内容</label>
          <textarea 
            v-model="nodeData" 
            :placeholder="'请输入节点内容'"
            class="form-textarea"
            rows="6"
          ></textarea>
        </div>
      </div>
      <div class="dialog-footer">
        <button class="btn cancel-btn" @click="closeDialog">取消</button>
        <button class="btn save-btn" @click="saveNode">保存</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.node-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.node-dialog {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.dialog-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 4px;
}

.close-btn:hover {
  color: #666;
}

.dialog-content {
  padding: 24px;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.required {
  color: #ff4d4f;
  margin-left: 4px;
}

.path-display {
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 4px;
  color: #666;
  font-family: monospace;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
}

.form-input:focus, .form-textarea:focus {
  border-color: #40a9ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
  outline: none;
}

.form-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.dialog-footer {
  padding: 16px 24px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background: white;
  border: 1px solid #d9d9d9;
  color: #666;
}

.cancel-btn:hover {
  color: #40a9ff;
  border-color: #40a9ff;
}

.save-btn {
  background: #1890ff;
  border: 1px solid #1890ff;
  color: white;
}

.save-btn:hover {
  background: #40a9ff;
  border-color: #40a9ff;
}

.error-message {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
  display: block;
}
</style>

<script lang="ts">
import { defineComponent, ref, computed, watch } from 'vue'
import { useZkStore } from '../store'
import { ElMessage } from 'element-plus'

export default defineComponent({
  name: 'NodeDialog',
  props: {
    show: {
      type: Boolean,
      required: true
    },
    isEdit: {
      type: Boolean,
      default: false
    },
    parentPath: {
      type: String,
      default: ''
    }
  },
  emits: ['update:show', 'save'],
  setup(props, { emit }) {
    const store = useZkStore()
    const nodeName = ref('')
    const nodeData = ref('')
    const validationErrors = ref({ name: '' })

    const fullPath = computed(() => {
      if (props.isEdit) {
        return store.selectedNode?.path || ''
      }
      return props.parentPath ? `${props.parentPath}/${nodeName.value}` : `/${nodeName.value}`
    })

    watch(() => props.show, (newVal) => {
      if (newVal && props.isEdit && store.selectedNode) {
        nodeName.value = store.selectedNode.path.split('/').pop() || ''
        nodeData.value = store.selectedNode.data || ''
      } else if (newVal) {
        nodeName.value = ''
        nodeData.value = ''
      }
    })

    const validateForm = () => {
      validationErrors.value.name = ''
      
      if (!nodeName.value && !props.isEdit) {
        validationErrors.value.name = '请输入节点名称'
        ElMessage.warning('请输入节点名称')
        return false
      }

      if (!props.isEdit && /[\\/:*?"<>|]/.test(nodeName.value)) {
        validationErrors.value.name = '节点名称包含非法字符'
        ElMessage.warning('节点名称包含非法字符')
        return false
      }

      return true
    }

    const saveNode = async () => {
      if (!validateForm()) return

      try {
        const nodeInfo = {
          path: fullPath.value,
          data: nodeData.value
        }
        
        emit('save', nodeInfo)
        closeDialog()
      } catch (error) {
        console.error('保存节点失败:', error)
        ElMessage.error('保存节点失败')
      }
    }

    const closeDialog = () => {
      emit('update:show', false)
      nodeName.value = ''
      nodeData.value = ''
      validationErrors.value.name = ''
    }

    return {
      nodeName,
      nodeData,
      validationErrors,
      fullPath,
      saveNode,
      closeDialog
    }
  }
})
</script> 