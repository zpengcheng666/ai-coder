<template>
  <div class="document-manager">
    <div class="document-header">
      <h3>知识库管理</h3>
      <div class="document-actions">
        <button @click="showAddDialog = true" class="add-doc-btn">
          <span class="icon">📁</span>
          添加文档
        </button>
        <button @click="reloadDocuments" class="reload-btn" :disabled="loading">
          <span class="icon">🔄</span>
          重新加载
        </button>
      </div>
    </div>

    <div class="document-info">
      <p class="info-text">
        <span class="icon">💡</span>
        管理AI助手的知识库文档，支持添加新文档和重新加载现有文档。
      </p>
    </div>

    <div class="document-stats">
      <div class="stat-item">
        <span class="stat-label">文档路径:</span>
        <span class="stat-value">src/main/resources/docs</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">支持格式:</span>
        <span class="stat-value">.txt, .md, .doc, .pdf</span>
      </div>
    </div>

    <div class="operation-log">
      <h4>操作日志</h4>
      <div class="log-items">
        <div
          v-for="(log, index) in logs"
          :key="index"
          :class="['log-item', log.type]"
        >
          <span class="log-time">{{ formatTime(log.timestamp) }}</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <span class="loading-spinner">⏳</span>
      处理中...
    </div>

    <!-- 添加文档对话框 -->
    <div v-if="showAddDialog" class="dialog-overlay" @click="showAddDialog = false">
      <div class="dialog" @click.stop>
        <h3>添加文档到知识库</h3>
        <div class="dialog-content">
          <p class="dialog-description">
            请输入要添加到知识库的文档完整路径。文档将被自动解析并添加到AI助手的知识库中。
          </p>
          <input
            v-model="newDocumentPath"
            placeholder="例如: /path/to/your/document.txt"
            class="dialog-input"
            @keydown.enter="confirmAddDocument"
          />
          <div class="path-examples">
            <p class="examples-title">路径示例:</p>
            <ul class="examples-list">
              <li>src/main/resources/docs/新文档.md</li>
              <li>D:/documents/项目说明.txt</li>
              <li>/home/user/知识文档.pdf</li>
            </ul>
          </div>
        </div>
        <div class="dialog-actions">
          <button @click="showAddDialog = false" class="cancel-btn">取消</button>
          <button 
            @click="confirmAddDocument" 
            class="confirm-btn"
            :disabled="!newDocumentPath.trim() || loading"
          >
            添加
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { addDocumentToRAG, reloadRAGDocuments } from '../api/chatApi.js'
import { formatTime } from '../utils/index.js'

export default {
  name: 'DocumentManager',
  data() {
    return {
      loading: false,
      showAddDialog: false,
      newDocumentPath: '',
      logs: []
    }
  },
  methods: {
    async addDocument(filePath) {
      this.loading = true
      try {
        const response = await addDocumentToRAG(filePath)
        this.addLog('success', `成功添加文档: ${filePath}`)
        this.showAddDialog = false
        this.newDocumentPath = ''
        this.$emit('document-added', response)
      } catch (error) {
        const message = error.response?.data?.error || `添加文档失败: ${error.message}`
        this.addLog('error', message)
        console.error('添加文档失败:', error)
      } finally {
        this.loading = false
      }
    },

    async reloadDocuments() {
      this.loading = true
      try {
        const response = await reloadRAGDocuments()
        this.addLog('success', '知识库文档重新加载成功')
        this.$emit('documents-reloaded', response)
      } catch (error) {
        const message = error.response?.data?.error || `重新加载失败: ${error.message}`
        this.addLog('error', message)
        console.error('重新加载文档失败:', error)
      } finally {
        this.loading = false
      }
    },

    confirmAddDocument() {
      if (!this.newDocumentPath.trim()) {
        this.addLog('warning', '请输入有效的文档路径')
        return
      }
      this.addDocument(this.newDocumentPath.trim())
    },

    addLog(type, message) {
      this.logs.unshift({
        type,
        message,
        timestamp: new Date()
      })
      
      // 只保留最近20条日志
      if (this.logs.length > 20) {
        this.logs = this.logs.slice(0, 20)
      }
    },

    formatTime(date) {
      return formatTime(date)
    }
  },

  mounted() {
    // 添加欢迎日志
    this.addLog('info', '知识库管理器已启动')
  }
}
</script>

<style scoped>
.document-manager {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.document-header {
  padding: 20px;
  border-bottom: 1px solid #e1e5e9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f8f9fa;
}

.document-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.document-actions {
  display: flex;
  gap: 10px;
}

.add-doc-btn, .reload-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background-color 0.2s;
}

.add-doc-btn {
  background-color: #28a745;
  color: white;
}

.add-doc-btn:hover {
  background-color: #218838;
}

.reload-btn {
  background-color: #007bff;
  color: white;
}

.reload-btn:hover:not(:disabled) {
  background-color: #0056b3;
}

.reload-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.document-info {
  padding: 15px 20px;
  background-color: #e3f2fd;
  border-bottom: 1px solid #e1e5e9;
}

.info-text {
  margin: 0;
  font-size: 14px;
  color: #1976d2;
  display: flex;
  align-items: center;
  gap: 8px;
}

.document-stats {
  padding: 20px;
  border-bottom: 1px solid #e1e5e9;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
}

.stat-item:last-child {
  margin-bottom: 0;
}

.stat-label {
  color: #666;
  font-weight: 500;
}

.stat-value {
  color: #333;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.operation-log {
  padding: 20px;
}

.operation-log h4 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #333;
}

.log-items {
  max-height: 300px;
  overflow-y: auto;
}

.log-item {
  padding: 8px 12px;
  margin-bottom: 8px;
  border-radius: 4px;
  font-size: 13px;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.log-item.success {
  background-color: #d4edda;
  color: #155724;
  border-left: 3px solid #28a745;
}

.log-item.error {
  background-color: #f8d7da;
  color: #721c24;
  border-left: 3px solid #dc3545;
}

.log-item.warning {
  background-color: #fff3cd;
  color: #856404;
  border-left: 3px solid #ffc107;
}

.log-item.info {
  background-color: #d1ecf1;
  color: #0c5460;
  border-left: 3px solid #17a2b8;
}

.log-time {
  white-space: nowrap;
  opacity: 0.8;
  font-size: 12px;
}

.log-message {
  flex: 1;
  word-break: break-word;
}

.loading {
  padding: 20px;
  text-align: center;
  color: #666;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.loading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 对话框样式 */
.dialog-overlay {
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

.dialog {
  background-color: white;
  border-radius: 8px;
  width: 500px;
  max-width: 90vw;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dialog h3 {
  margin: 0;
  padding: 20px 24px;
  font-size: 18px;
  color: #333;
  border-bottom: 1px solid #e1e5e9;
}

.dialog-content {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

.dialog-description {
  margin: 0 0 16px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.dialog-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  margin-bottom: 16px;
  outline: none;
  transition: border-color 0.2s;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.dialog-input:focus {
  border-color: #007bff;
}

.path-examples {
  background-color: #f8f9fa;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

.examples-title {
  margin: 0 0 8px 0;
  font-size: 13px;
  font-weight: 500;
  color: #495057;
}

.examples-list {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  color: #666;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.examples-list li {
  margin-bottom: 4px;
}

.dialog-actions {
  padding: 16px 24px;
  border-top: 1px solid #e1e5e9;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn, .confirm-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background-color: #e0e0e0;
}

.confirm-btn {
  background-color: #28a745;
  color: white;
}

.confirm-btn:hover:not(:disabled) {
  background-color: #218838;
}

.confirm-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.icon {
  font-style: normal;
}

/* 滚动条样式 */
.log-items::-webkit-scrollbar,
.dialog-content::-webkit-scrollbar {
  width: 6px;
}

.log-items::-webkit-scrollbar-track,
.dialog-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.log-items::-webkit-scrollbar-thumb,
.dialog-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.log-items::-webkit-scrollbar-thumb:hover,
.dialog-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

@media (max-width: 768px) {
  .document-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .document-actions {
    justify-content: stretch;
  }

  .add-doc-btn, .reload-btn {
    flex: 1;
    justify-content: center;
  }

  .stat-item {
    flex-direction: column;
    gap: 4px;
  }

  .dialog {
    width: 95vw;
    height: 90vh;
  }
}
</style>