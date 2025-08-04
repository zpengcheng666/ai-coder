<template>
  <div class="settings-panel">
    <div class="settings-header">
      <h3>系统设置</h3>
      <button @click="$emit('close')" class="close-btn">
        <span class="icon">✕</span>
      </button>
    </div>

    <div class="settings-content">
      <!-- 用户设置 -->
      <div class="settings-section">
        <h4>用户设置</h4>
        <div class="setting-item">
          <label class="setting-label">用户ID</label>
          <input
            v-model="localSettings.userId"
            class="setting-input"
            placeholder="输入用户ID"
            @input="saveSettings"
          />
          <p class="setting-description">用于标识您的身份，会话记录将与此ID关联</p>
        </div>
        <div class="setting-item">
          <label class="setting-label">用户名称</label>
          <input
            v-model="localSettings.userName"
            class="setting-input"
            placeholder="输入用户名称"
            @input="saveSettings"
          />
          <p class="setting-description">显示在界面上的用户名称</p>
        </div>
      </div>

      <!-- 聊天设置 -->
      <div class="settings-section">
        <h4>聊天设置</h4>
        <div class="setting-item">
          <label class="setting-label">
            <input
              type="checkbox"
              v-model="localSettings.streamMode"
              @change="saveSettings"
            />
            <span class="checkbox-label">启用流式输出</span>
          </label>
          <p class="setting-description">AI回复时实时显示内容（推荐）</p>
        </div>
        <div class="setting-item">
          <label class="setting-label">
            <input
              type="checkbox"
              v-model="localSettings.autoScroll"
              @change="saveSettings"
            />
            <span class="checkbox-label">自动滚动到底部</span>
          </label>
          <p class="setting-description">新消息到达时自动滚动到聊天底部</p>
        </div>
        <div class="setting-item">
          <label class="setting-label">
            <input
              type="checkbox"
              v-model="localSettings.showTimestamp"
              @change="saveSettings"
            />
            <span class="checkbox-label">显示消息时间</span>
          </label>
          <p class="setting-description">在消息中显示发送时间</p>
        </div>
      </div>

      <!-- 连接设置 -->
      <div class="settings-section">
        <h4>连接设置</h4>
        <div class="setting-item">
          <label class="setting-label">后端服务地址</label>
          <input
            v-model="localSettings.apiBaseUrl"
            class="setting-input"
            placeholder="http://localhost:8081/api"
            @input="saveSettings"
          />
          <p class="setting-description">AI助手后端服务的地址</p>
        </div>
        <div class="setting-item">
          <label class="setting-label">连接超时时间 (秒)</label>
          <input
            type="number"
            v-model.number="localSettings.timeout"
            class="setting-input"
            min="5"
            max="60"
            @input="saveSettings"
          />
          <p class="setting-description">网络请求的超时时间</p>
        </div>
      </div>

      <!-- 系统信息 -->
      <div class="settings-section">
        <h4>系统信息</h4>
        <div class="system-info">
          <div class="info-item">
            <span class="info-label">连接状态:</span>
            <span :class="['info-value', connectionStatus.class]">
              {{ connectionStatus.text }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">版本:</span>
            <span class="info-value">v1.0.0</span>
          </div>
          <div class="info-item">
            <span class="info-label">最后检查:</span>
            <span class="info-value">{{ lastHealthCheck || '未检查' }}</span>
          </div>
        </div>
        <button @click="checkHealth" class="health-check-btn" :disabled="checking">
          <span class="icon">🔍</span>
          {{ checking ? '检查中...' : '检查连接' }}
        </button>
      </div>

      <!-- 数据管理 -->
      <div class="settings-section">
        <h4>数据管理</h4>
        <div class="data-actions">
          <button @click="exportData" class="data-btn export-btn">
            <span class="icon">📤</span>
            导出设置
          </button>
          <button @click="importData" class="data-btn import-btn">
            <span class="icon">📥</span>
            导入设置
          </button>
          <button @click="resetSettings" class="data-btn reset-btn">
            <span class="icon">🔄</span>
            重置设置
          </button>
        </div>
        <input
          ref="fileInput"
          type="file"
          accept=".json"
          style="display: none"
          @change="handleFileImport"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { checkServiceHealth } from '../api/chatApi.js'
import { formatTime } from '../utils/index.js'

export default {
  name: 'SettingsPanel',
  props: {
    settings: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      localSettings: {
        userId: 'default_user',
        userName: '用户',
        streamMode: true,
        autoScroll: true,
        showTimestamp: true,
        apiBaseUrl: 'http://localhost:8081/api',
        timeout: 10,
        ...this.settings
      },
      checking: false,
      connectionStatus: {
        text: '未知',
        class: 'unknown'
      },
      lastHealthCheck: null
    }
  },
  methods: {
    saveSettings() {
      // 保存到localStorage
      localStorage.setItem('ai-helper-settings', JSON.stringify(this.localSettings))
      // 通知父组件
      this.$emit('settings-changed', this.localSettings)
    },

    async checkHealth() {
      this.checking = true
      try {
        const isHealthy = await checkServiceHealth()
        if (isHealthy) {
          this.connectionStatus = {
            text: '连接正常',
            class: 'success'
          }
        } else {
          this.connectionStatus = {
            text: '连接失败',
            class: 'error'
          }
        }
        this.lastHealthCheck = formatTime(new Date())
      } catch (error) {
        this.connectionStatus = {
          text: '检查失败',
          class: 'error'
        }
        console.error('健康检查失败:', error)
      } finally {
        this.checking = false
      }
    },

    exportData() {
      const data = {
        settings: this.localSettings,
        exportTime: new Date().toISOString(),
        version: '1.0.0'
      }
      
      const blob = new Blob([JSON.stringify(data, null, 2)], {
        type: 'application/json'
      })
      
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `ai-helper-settings-${new Date().toISOString().split('T')[0]}.json`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    },

    importData() {
      this.$refs.fileInput.click()
    },

    handleFileImport(event) {
      const file = event.target.files[0]
      if (!file) return

      const reader = new FileReader()
      reader.onload = (e) => {
        try {
          const data = JSON.parse(e.target.result)
          if (data.settings) {
            this.localSettings = { ...this.localSettings, ...data.settings }
            this.saveSettings()
            alert('设置导入成功')
          } else {
            alert('无效的设置文件')
          }
        } catch (error) {
          alert('设置文件格式错误')
          console.error('导入设置失败:', error)
        }
      }
      reader.readAsText(file)
      
      // 重置文件输入
      event.target.value = ''
    },

    resetSettings() {
      if (confirm('确定要重置所有设置吗？这将清除您的个人配置。')) {
        this.localSettings = {
          userId: 'default_user',
          userName: '用户',
          streamMode: true,
          autoScroll: true,
          showTimestamp: true,
          apiBaseUrl: 'http://localhost:8081/api',
          timeout: 10
        }
        this.saveSettings()
        alert('设置已重置')
      }
    },

    loadSettings() {
      try {
        const saved = localStorage.getItem('ai-helper-settings')
        if (saved) {
          this.localSettings = { ...this.localSettings, ...JSON.parse(saved) }
        }
      } catch (error) {
        console.error('加载设置失败:', error)
      }
    }
  },

  mounted() {
    this.loadSettings()
    this.checkHealth()
  },

  watch: {
    settings: {
      handler(newSettings) {
        this.localSettings = { ...this.localSettings, ...newSettings }
      },
      deep: true
    }
  }
}
</script>

<style scoped>
.settings-panel {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  max-width: 600px;
  width: 100%;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.settings-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e1e5e9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f8f9fa;
}

.settings-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.close-btn {
  padding: 4px 8px;
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  border-radius: 4px;
  color: #666;
  transition: background-color 0.2s, color 0.2s;
}

.close-btn:hover {
  background-color: #e9ecef;
  color: #333;
}

.settings-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.settings-section {
  margin-bottom: 32px;
}

.settings-section:last-child {
  margin-bottom: 0;
}

.settings-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #007bff;
  padding-bottom: 8px;
}

.setting-item {
  margin-bottom: 20px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #555;
}

.setting-label input[type="checkbox"] {
  margin-right: 8px;
}

.checkbox-label {
  font-weight: normal;
}

.setting-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.setting-input:focus {
  border-color: #007bff;
}

.setting-description {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

.system-info {
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  color: #666;
  font-weight: 500;
}

.info-value {
  color: #333;
}

.info-value.success {
  color: #28a745;
  font-weight: 500;
}

.info-value.error {
  color: #dc3545;
  font-weight: 500;
}

.info-value.unknown {
  color: #6c757d;
}

.health-check-btn {
  width: 100%;
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: background-color 0.2s;
}

.health-check-btn:hover:not(:disabled) {
  background-color: #0056b3;
}

.health-check-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.data-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.data-btn {
  flex: 1;
  min-width: 120px;
  padding: 8px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: background-color 0.2s;
}

.export-btn {
  background-color: #28a745;
  color: white;
}

.export-btn:hover {
  background-color: #218838;
}

.import-btn {
  background-color: #17a2b8;
  color: white;
}

.import-btn:hover {
  background-color: #138496;
}

.reset-btn {
  background-color: #ffc107;
  color: #212529;
}

.reset-btn:hover {
  background-color: #e0a800;
}

.icon {
  font-style: normal;
}

/* 滚动条样式 */
.settings-content::-webkit-scrollbar {
  width: 6px;
}

.settings-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.settings-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.settings-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

@media (max-width: 768px) {
  .settings-panel {
    max-width: 95vw;
    max-height: 90vh;
  }

  .settings-content {
    padding: 16px 20px;
  }

  .data-actions {
    flex-direction: column;
  }

  .data-btn {
    min-width: auto;
  }

  .info-item {
    flex-direction: column;
    gap: 4px;
  }
}
</style>