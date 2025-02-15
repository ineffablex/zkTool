<template>
  <div class="cluster-tab" :class="{ 'is-hidden': isHidden }">
    <div class="tab-header">
      <button class="toggle-btn" @click="toggleTab">
        <i :class="isHidden ? 'fas fa-chevron-right' : 'fas fa-chevron-left'"></i>
      </button>
      <h2 class="title">集群列表</h2>
    </div>
    
    <div class="action-buttons">
      <button class="btn add-btn" @click="showAddDialog">
        <i class="fas fa-plus"></i> 新增
      </button>
      <button class="btn edit-btn" @click="showEditDialog" :disabled="!selectedCluster">
        <i class="fas fa-edit"></i> 修改
      </button>
      <button class="btn delete-btn" @click="confirmDelete" :disabled="!selectedCluster">
        <i class="fas fa-trash"></i> 删除
      </button>
    </div>
    
    <div class="clusters-container">
      <div class="card" v-for="cluster in clusters" :key="cluster.id" 
           @mouseover="hoverCard(cluster)" 
           @click="connectCluster(cluster)"
           :class="{ 'active': selectedCluster?.id === cluster.id }">
        <div class="flex justify-between items-center">
          <h3>{{ cluster.name }}</h3>
          <span :class="[
            'px-2 py-1 text-xs rounded-full',
            (selectedCluster?.id === cluster.id && store.isConnected) ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
          ]">
            {{ selectedCluster?.id === cluster.id && store.isConnected ? '已连接' : '未连接' }}
          </span>
        </div>
        <p class="host-info">{{ cluster.host }}:{{ cluster.port }}</p>
        <p class="description">{{ cluster.description }}</p>
      </div>
    </div>

    <!-- 集群配置弹窗 -->
    <div v-if="showDialog" class="dialog-overlay">
      <div class="dialog">
        <h2>{{ isEdit ? '修改集群配置' : '新增集群配置' }}</h2>
        <div class="form-group">
          <label>集群名称 <span class="required">*</span></label>
          <input v-model="editingCluster.name" type="text" placeholder="请输入集群名称" required>
          <span v-if="validationErrors.name" class="error-message">{{ validationErrors.name }}</span>
        </div>
        <div class="form-group">
          <label>主机地址 <span class="required">*</span></label>
          <input v-model="editingCluster.host" type="text" placeholder="请输入主机地址" required>
          <span v-if="validationErrors.host" class="error-message">{{ validationErrors.host }}</span>
        </div>
        <div class="form-group">
          <label>端口 <span class="required">*</span></label>
          <input v-model="editingCluster.port" type="number" placeholder="请输入端口号" required>
          <span v-if="validationErrors.port" class="error-message">{{ validationErrors.port }}</span>
        </div>
        <div class="form-group">
          <label>重试次数 <span class="required">*</span></label>
          <input v-model="editingCluster.retryCount" type="number" placeholder="请输入重试次数" required>
          <span v-if="validationErrors.retryCount" class="error-message">{{ validationErrors.retryCount }}</span>
        </div>
        <div class="form-group">
          <label>超时时间(ms) <span class="required">*</span></label>
          <input v-model="editingCluster.timeout" type="number" placeholder="请输入超时时间" required>
          <span v-if="validationErrors.timeout" class="error-message">{{ validationErrors.timeout }}</span>
        </div>
        <div class="form-group">
          <label>描述</label>
          <input v-model="editingCluster.description" type="text" placeholder="请输入描述信息">
        </div>
        <div class="dialog-buttons">
          <button class="btn cancel-btn" @click="closeDialog">取消</button>
          <button class="btn save-btn" @click="saveCluster">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { useZkStore } from '../store';
import { ElMessage } from 'element-plus';

export default {
  data() {
    return {
      isHidden: false,
      clusters: [],
      selectedCluster: null,
      showDialog: false,
      isEdit: false,
      editingCluster: {
        name: '',
        host: '',
        port: 2181,
        retryCount: 3,
        timeout: 5000,
        description: ''
      },
      validationErrors: {
        name: '',
        host: '',
        port: '',
        retryCount: '',
        timeout: ''
      }
    };
  },
  setup() {
    const store = useZkStore();
    return { store };
  },
  created() {
    this.fetchClusters();
  },
  methods: {
    async fetchClusters() {
      try {
        const response = await axios.get('/api/clusters');
        this.clusters = response.data;
      } catch (error) {
        console.error('获取集群列表失败:', error);
        ElMessage.error('获取集群列表失败');
      }
    },
    toggleTab() {
      this.isHidden = !this.isHidden;
    },
    hoverCard(cluster) {
      // 鼠标悬浮效果已通过 CSS 实现
    },
    async connectCluster(cluster) {
      this.selectedCluster = cluster;
      try {
        // 发送连接请求到后端
        const response = await axios.get('/api/zk/connect', {
          params: {
            address: `${cluster.host}:${cluster.port}`
          }
        });
        
        if (response.data.success) {
          // 连接成功后获取根节点数据
          const rootData = await axios.get('/api/zk/nodes', {
            params: { path: '/' }
          });
          
          if (rootData.data.success) {
            // 通知父组件连接成功并传递节点数据
            this.$emit('cluster-connected', {
              cluster: cluster,
              nodes: rootData.data.data
            });
            ElMessage.success('集群连接成功');
          } else {
            throw new Error(rootData.data.message || '获取节点数据失败');
          }
        } else {
          throw new Error(response.data.message || '连接失败');
        }
      } catch (error) {
        console.error('连接集群失败:', error);
        // 显示错误提示
        const errorMessage = error.response?.data?.message || error.message;
        this.$emit('connection-error', errorMessage);
        ElMessage.error(`连接失败: ${errorMessage}`);
        // 更新连接状态
        this.store.$patch({ 
          isConnected: false,
          currentCluster: null
        });
        // 清空节点树
        this.store.$patch({ nodeTree: [] });
      }
    },
    showAddDialog() {
      this.isEdit = false;
      this.editingCluster = {
        name: '',
        host: '',
        port: 2181,
        retryCount: 3,
        timeout: 5000,
        description: ''
      };
      this.showDialog = true;
    },
    showEditDialog() {
      if (!this.selectedCluster) return;
      this.isEdit = true;
      this.editingCluster = { ...this.selectedCluster };
      this.showDialog = true;
    },
    closeDialog() {
      this.showDialog = false;
      this.editingCluster = {
        name: '',
        host: '',
        port: 2181,
        retryCount: 3,
        timeout: 5000,
        description: ''
      };
    },
    validateForm() {
      let isValid = true;
      this.validationErrors = {
        name: '',
        host: '',
        port: '',
        retryCount: '',
        timeout: ''
      };

      if (!this.editingCluster.name) {
        this.validationErrors.name = '请输入集群名称';
        ElMessage.warning('请输入集群名称');
        isValid = false;
      }

      if (!this.editingCluster.host) {
        this.validationErrors.host = '请输入主机地址';
        ElMessage.warning('请输入主机地址');
        isValid = false;
      }

      if (!this.editingCluster.port) {
        this.validationErrors.port = '请输入端口号';
        ElMessage.warning('请输入端口号');
        isValid = false;
      } else if (this.editingCluster.port < 1 || this.editingCluster.port > 65535) {
        this.validationErrors.port = '端口号必须在1-65535之间';
        ElMessage.warning('端口号必须在1-65535之间');
        isValid = false;
      }

      if (!this.editingCluster.retryCount && this.editingCluster.retryCount !== 0) {
        this.validationErrors.retryCount = '请输入重试次数';
        ElMessage.warning('请输入重试次数');
        isValid = false;
      } else if (this.editingCluster.retryCount < 0) {
        this.validationErrors.retryCount = '重试次数不能小于0';
        ElMessage.warning('重试次数不能小于0');
        isValid = false;
      }

      if (!this.editingCluster.timeout) {
        this.validationErrors.timeout = '请输入超时时间';
        ElMessage.warning('请输入超时时间');
        isValid = false;
      } else if (this.editingCluster.timeout < 100) {
        this.validationErrors.timeout = '超时时间不能小于100ms';
        ElMessage.warning('超时时间不能小于100ms');
        isValid = false;
      }

      return isValid;
    },
    async saveCluster() {
      if (!this.validateForm()) {
        return;
      }

      try {
        let response;
        if (this.isEdit) {
          response = await axios.put(`/api/clusters/${this.editingCluster.id}`, this.editingCluster);
          const index = this.clusters.findIndex(c => c.id === this.editingCluster.id);
          this.clusters.splice(index, 1, response.data);
          ElMessage.success('集群配置修改成功');
        } else {
          response = await axios.post('/api/clusters', this.editingCluster);
          this.clusters.push(response.data);
          ElMessage.success('集群配置添加成功');
        }
        this.closeDialog();
      } catch (error) {
        console.error(this.isEdit ? '修改集群失败:' : '添加集群失败:', error);
        ElMessage.error(this.isEdit ? '修改集群失败' : '添加集群失败');
      }
    },
    async confirmDelete() {
      if (!this.selectedCluster) return;
      if (confirm('确定要删除该集群吗？')) {
        try {
          await axios.delete(`/api/clusters/${this.selectedCluster.id}`);
          const index = this.clusters.findIndex(c => c.id === this.selectedCluster.id);
          this.clusters.splice(index, 1);
          this.selectedCluster = null;
          ElMessage.success('集群删除成功');
        } catch (error) {
          console.error('删除集群失败:', error);
          ElMessage.error('删除集群失败');
        }
      }
    }
  }
};
</script>

<style scoped>
.cluster-tab {
  width: 300px;
  height: 98vh;
  background-color: #f8f9fa;
  border-right: 1px solid #dee2e6;
  transition: transform 0.3s ease;
  display: flex;
  flex-direction: column;
}

.is-hidden {
  transform: translateX(-100%);
}

.tab-header {
  padding: 1rem;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #dee2e6;
}

.toggle-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
}

.title {
  margin: 0 0 0 1rem;
  font-size: 1.2rem;
}

.action-buttons {
  padding: 0.5rem;
  display: flex;
  gap: 0.5rem;
  border-bottom: 1px solid #dee2e6;
  background-color: #fff;
}

.clusters-container {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.card {
  padding: 1rem;
  margin-bottom: 1rem;
  background-color: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.card.active {
  border-color: #007bff;
  background-color: #f8f9ff;
}

.host-info {
  color: #6c757d;
  font-size: 0.9rem;
  margin: 0.5rem 0;
}

.description {
  color: #495057;
  font-size: 0.9rem;
  margin: 0;
}

.btn {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.add-btn {
  background-color: #28a745;
  color: white;
}

.edit-btn {
  background-color: #ffc107;
  color: #212529;
}

.delete-btn {
  background-color: #dc3545;
  color: white;
}

/* 添加弹窗样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog {
  background-color: white;
  padding: 2rem;
  border-radius: 8px;
  width: 400px;
  max-width: 90%;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #333;
}

.form-group input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
}

.save-btn {
  background-color: #28a745;
  color: white;
}

.cancel-btn {
  background-color: #6c757d;
  color: white;
}

.required {
  color: #dc3545;
  margin-left: 4px;
}

.error-message {
  color: #dc3545;
  font-size: 0.875rem;
  margin-top: 0.25rem;
  display: block;
}

.form-group input:invalid {
  border-color: #dc3545;
}

.form-group input:focus {
  outline: none;
  border-color: #80bdff;
  box-shadow: 0 0 0 0.2rem rgba(0,123,255,.25);
}

.form-group input:invalid:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 0.2rem rgba(220,53,69,.25);
}
</style> 