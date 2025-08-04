<template>
  <div class="app">
    <!-- 头部工具栏 -->
    <div class="app-header">
      <div class="header-left">
        <button @click="toggleSidebar" class="sidebar-toggle">
          <span class="icon">☰</span>
        </button>
        <h1 class="app-title">AI 编程小助手</h1>
      </div>
      <div class="header-center">
        <div class="app-subtitle">{{ currentConversationTitle || '帮助您解答编程学习和求职面试相关问题' }}</div>
      </div>
      <div class="header-right">
        <button @click="showDocumentManager = true" class="toolbar-btn" title="文档管理">
          <span class="icon">📚</span>
        </button>
        <button @click="showSettings = true" class="toolbar-btn" title="设置">
          <span class="icon">⚙️</span>
        </button>
        <div class="connection-indicator">
          <span :class="['indicator', connectionStatus]" :title="connectionStatusText"></span>
        </div>
      </div>
    </div>

    <div class="app-body">
      <!-- 侧边栏 -->
      <div :class="['sidebar', { collapsed: sidebarCollapsed }]">
        <ConversationList
          :userId="settings.userId"
          :currentConversationId="currentConversationId"
          @conversation-selected="selectConversation"
          @conversation-created="selectConversation"
        />
      </div>

      <!-- 主聊天区域 -->
      <div class="main-content">
        <!-- 消息列表 -->
        <div class="messages-container" ref="messagesContainer">
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-content">
              <div class="welcome-icon">🤖</div>
              <h2>欢迎使用 AI 编程小助手</h2>
              <p>我可以帮助您：</p>
              <ul>
                <li>解答编程技术问题</li>
                <li>提供代码示例和解释</li>
                <li>协助求职面试准备</li>
                <li>分享编程学习建议</li>
              </ul>
              <p>请随时向我提问吧！</p>
            </div>
          </div>

          <!-- 历史消息 -->
          <ChatMessage
            v-for="message in messages"
            :key="message.id"
            :message="message.content"
            :is-user="message.isUser"
            :timestamp="settings.showTimestamp ? message.timestamp : null"
          />

          <!-- AI 正在回复的消息 -->
          <div v-if="isAiTyping" class="chat-message ai-message">
            <div class="message-avatar">
              <div class="avatar ai-avatar">AI</div>
            </div>
            <div class="message-content">
              <div class="message-bubble">
                <div class="ai-typing-content">
                  <div class="ai-response-text message-markdown" v-html="currentAiResponseRendered"></div>
                  <LoadingDots v-if="isStreaming" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入框 -->
        <ChatInput
          :disabled="isAiTyping"
          @send-message="sendMessage"
          placeholder="请输入您的编程问题..."
        />
      </div>
    </div>

    <!-- 连接状态提示 -->
    <div v-if="connectionError" class="connection-error">
      <div class="error-content">
        <span class="error-icon">⚠️</span>
        <span>连接服务器失败，请检查后端服务是否启动</span>
      </div>
    </div>

    <!-- 文档管理对话框 -->
    <div v-if="showDocumentManager" class="dialog-overlay" @click="showDocumentManager = false">
      <div class="dialog-container" @click.stop>
        <DocumentManager
          @document-added="onDocumentAdded"
          @documents-reloaded="onDocumentsReloaded"
        />
        <button @click="showDocumentManager = false" class="dialog-close">
          <span class="icon">✕</span>
        </button>
      </div>
    </div>

    <!-- 设置对话框 -->
    <div v-if="showSettings" class="dialog-overlay" @click="showSettings = false">
      <div class="dialog-container" @click.stop>
        <SettingsPanel
          :settings="settings"
          @settings-changed="updateSettings"
          @close="showSettings = false"
        />
      </div>
    </div>
  </div>
</template>

<script>
import ChatMessage from './components/ChatMessage.vue'
import ChatInput from './components/ChatInput.vue'
import LoadingDots from './components/LoadingDots.vue'
import ConversationList from './components/ConversationList.vue'
import DocumentManager from './components/DocumentManager.vue'
import SettingsPanel from './components/SettingsPanel.vue'
import { chatWithSSE, checkServiceHealth } from './api/chatApi.js'
import { generateMemoryId } from './utils/index.js'
import { marked } from 'marked'

export default {
  name: 'App',
  components: {
    ChatMessage,
    ChatInput,
    LoadingDots,
    ConversationList,
    DocumentManager,
    SettingsPanel
  },
  data() {
    return {
      messages: [],
      currentConversationId: null,
      currentConversationTitle: '',
      isAiTyping: false,
      isStreaming: false,
      currentAiResponse: '',
      currentEventSource: null,
      connectionError: false,
      sidebarCollapsed: false,
      showDocumentManager: false,
      showSettings: false,
      connectionStatus: 'unknown', // 'connected', 'disconnected', 'unknown'
      connectionStatusText: '连接状态未知',
      settings: {
        userId: 'default_user',
        userName: '用户',
        streamMode: true,
        autoScroll: true,
        showTimestamp: true,
        apiBaseUrl: 'http://localhost:8081/api',
        timeout: 10
      }
    }
  },
  computed: {
    currentAiResponseRendered() {
      if (!this.currentAiResponse) return ''
      // 配置marked选项
      marked.setOptions({
        breaks: true, // 支持换行
        gfm: true, // 支持GitHub风格的Markdown
        sanitize: false, // 不过滤HTML（根据需要可以开启）
        highlight: function(code, lang) {
          // 可以在这里添加代码高亮功能
          return code
        }
      })
      return marked(this.currentAiResponse)
    }
  },
  methods: {
    sendMessage(message) {
      // 添加用户消息
      this.addMessage(message, true)
      
      // 开始AI回复
      this.startAiResponse(message)
    },
    
    addMessage(content, isUser = false) {
      const message = {
        id: Date.now() + Math.random(),
        content,
        isUser,
        timestamp: new Date()
      }
      this.messages.push(message)
      
      if (this.settings.autoScroll) {
        this.scrollToBottom()
      }
    },
    
    startAiResponse(userMessage) {
      this.isAiTyping = true
      this.isStreaming = this.settings.streamMode
      this.currentAiResponse = ''
      this.connectionError = false
      
      // 关闭之前的连接
      if (this.currentEventSource) {
        this.currentEventSource.close()
      }
      
      // 确保有当前会话ID
      if (!this.currentConversationId) {
        this.currentConversationId = 'conversation_' + generateMemoryId()
      }
      
      // 开始SSE连接
      if (this.settings.streamMode) {
        this.currentEventSource = chatWithSSE(
          this.currentConversationId,
          userMessage,
          this.settings.userId,
          this.handleAiMessage,
          this.handleAiError,
          this.handleAiClose
        )
      }
    },
    
    handleAiMessage(data) {
      this.currentAiResponse += data
      if (this.settings.autoScroll) {
        this.scrollToBottom()
      }
    },
    
    handleAiError(error) {
      console.error('AI 回复出错:', error)
      this.connectionError = true
      this.updateConnectionStatus('disconnected')
      this.finishAiResponse()
      
      // 5秒后自动隐藏错误提示
      setTimeout(() => {
        this.connectionError = false
      }, 5000)
    },
    
    handleAiClose() {
      this.finishAiResponse()
    },
    
    finishAiResponse() {
      this.isStreaming = false
      
      // 如果有内容，添加到消息列表
      if (this.currentAiResponse.trim()) {
        this.addMessage(this.currentAiResponse.trim(), false)
      }
      
      // 重置状态
      this.isAiTyping = false
      this.currentAiResponse = ''
      
      // 重置连接错误状态（确保正常结束时清除错误提示）
      this.connectionError = false
      
      // 关闭连接
      if (this.currentEventSource) {
        this.currentEventSource.close()
        this.currentEventSource = null
      }
    },
    
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    
    selectConversation(conversation) {
      this.currentConversationId = conversation.conversationId
      this.currentConversationTitle = conversation.title
      this.messages = [] // 清空当前消息，实际应用中应该加载历史消息
      console.log('选择会话:', conversation)
    },
    
    updateSettings(newSettings) {
      this.settings = { ...this.settings, ...newSettings }
      // 保存到localStorage
      localStorage.setItem('ai-helper-settings', JSON.stringify(this.settings))
    },
    
    loadSettings() {
      try {
        const saved = localStorage.getItem('ai-helper-settings')
        if (saved) {
          this.settings = { ...this.settings, ...JSON.parse(saved) }
        }
      } catch (error) {
        console.error('加载设置失败:', error)
      }
    },
    
    async updateConnectionStatus(status = null) {
      if (status) {
        this.connectionStatus = status
      } else {
        try {
          const isHealthy = await checkServiceHealth()
          this.connectionStatus = isHealthy ? 'connected' : 'disconnected'
        } catch (error) {
          this.connectionStatus = 'disconnected'
        }
      }
      
      // 更新状态文本
      switch (this.connectionStatus) {
        case 'connected':
          this.connectionStatusText = '已连接'
          break
        case 'disconnected':
          this.connectionStatusText = '连接断开'
          break
        default:
          this.connectionStatusText = '连接状态未知'
      }
    },
    
    onDocumentAdded(response) {
      console.log('文档添加成功:', response)
      // 可以在这里显示成功消息或刷新相关状态
    },
    
    onDocumentsReloaded(response) {
      console.log('文档重新加载成功:', response)
      // 可以在这里显示成功消息或刷新相关状态
    },
    
    initializeApp() {
      // 如果没有当前会话，创建一个默认会话
      if (!this.currentConversationId) {
        this.currentConversationId = 'conversation_' + generateMemoryId()
      }
      console.log('会话ID:', this.currentConversationId)
    }
  },
  
  mounted() {
    this.loadSettings()
    this.initializeApp()
    this.updateConnectionStatus()
    
    // 定期检查连接状态
    setInterval(() => {
      this.updateConnectionStatus()
    }, 30000) // 每30秒检查一次
  },
  
  beforeUnmount() {
    // 组件销毁前关闭连接
    if (this.currentEventSource) {
      this.currentEventSource.close()
    }
  }
}
</script>

<style scoped>
.app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f0f0;
}

.app-header {
  background-color: #fff;
  padding: 12px 20px;
  border-bottom: 1px solid #e1e5e9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 60px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 0 0 auto;
}

.sidebar-toggle {
  padding: 8px;
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  color: #666;
  font-size: 18px;
  transition: background-color 0.2s, color 0.2s;
}

.sidebar-toggle:hover {
  background-color: #f5f5f5;
  color: #333;
}

.app-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.header-center {
  flex: 1;
  text-align: center;
  padding: 0 20px;
}

.app-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.toolbar-btn {
  padding: 8px 10px;
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  color: #666;
  font-size: 16px;
  transition: background-color 0.2s, color 0.2s;
}

.toolbar-btn:hover {
  background-color: #f5f5f5;
  color: #333;
}

.connection-indicator {
  display: flex;
  align-items: center;
}

.indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ccc;
}

.indicator.connected {
  background-color: #28a745;
  box-shadow: 0 0 0 2px rgba(40, 167, 69, 0.3);
}

.indicator.disconnected {
  background-color: #dc3545;
  box-shadow: 0 0 0 2px rgba(220, 53, 69, 0.3);
}

.indicator.unknown {
  background-color: #6c757d;
}

.app-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  flex: 0 0 300px;
  background-color: #f8f9fa;
  border-right: 1px solid #e1e5e9;
  transition: margin-left 0.3s ease;
}

.sidebar.collapsed {
  margin-left: -300px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.welcome-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.welcome-content {
  text-align: center;
  max-width: 400px;
  color: #666;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.welcome-content h2 {
  font-size: 20px;
  margin-bottom: 15px;
  color: #333;
}

.welcome-content p {
  margin-bottom: 10px;
  line-height: 1.5;
}

.welcome-content ul {
  text-align: left;
  margin: 15px 0;
}

.welcome-content li {
  margin-bottom: 5px;
}

/* AI 正在回复时的消息样式 */
.chat-message {
  display: flex;
  margin-bottom: 20px;
  padding: 0 20px;
}

.ai-message {
  justify-content: flex-start;
  flex-direction: row;
}

.message-avatar {
  display: flex;
  align-items: flex-start;
  margin: 0 10px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: white;
}

.ai-avatar {
  background-color: #6c757d;
}

.message-content {
  max-width: 70%;
  min-width: 100px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
  word-wrap: break-word;
  word-break: break-word;
  background-color: #f1f3f4;
  color: #333;
  border-bottom-left-radius: 4px;
}

.ai-typing-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-response-text {
  font-size: 14px;
  line-height: 1.5;
}

/* AI实时回复的Markdown样式 */
.ai-response-text.message-markdown h1,
.ai-response-text.message-markdown h2,
.ai-response-text.message-markdown h3,
.ai-response-text.message-markdown h4,
.ai-response-text.message-markdown h5,
.ai-response-text.message-markdown h6 {
  margin: 0.5em 0;
  font-weight: bold;
}

.ai-response-text.message-markdown h1 { font-size: 1.5em; }
.ai-response-text.message-markdown h2 { font-size: 1.3em; }
.ai-response-text.message-markdown h3 { font-size: 1.2em; }
.ai-response-text.message-markdown h4 { font-size: 1.1em; }
.ai-response-text.message-markdown h5 { font-size: 1em; }
.ai-response-text.message-markdown h6 { font-size: 0.9em; }

.ai-response-text.message-markdown p {
  margin: 0.5em 0;
}

.ai-response-text.message-markdown ul,
.ai-response-text.message-markdown ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.ai-response-text.message-markdown li {
  margin: 0.2em 0;
}

.ai-response-text.message-markdown code {
  background-color: rgba(0, 0, 0, 0.1);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
}

.ai-response-text.message-markdown pre {
  background-color: rgba(0, 0, 0, 0.1);
  padding: 1em;
  border-radius: 5px;
  overflow-x: auto;
  margin: 0.5em 0;
}

.ai-response-text.message-markdown pre code {
  background-color: transparent;
  padding: 0;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
}

.ai-response-text.message-markdown blockquote {
  border-left: 4px solid #ccc;
  padding-left: 1em;
  margin: 0.5em 0;
  font-style: italic;
  color: #666;
}

.ai-response-text.message-markdown a {
  color: #007bff;
  text-decoration: underline;
}

.ai-response-text.message-markdown table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.5em 0;
}

.ai-response-text.message-markdown th,
.ai-response-text.message-markdown td {
  border: 1px solid #ddd;
  padding: 0.5em;
  text-align: left;
}

.ai-response-text.message-markdown th {
  background-color: #f2f2f2;
  font-weight: bold;
}

.ai-response-text.message-markdown hr {
  border: none;
  border-top: 1px solid #ddd;
  margin: 1em 0;
}

.connection-error {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #ff4444;
  color: white;
  padding: 10px 20px;
  border-radius: 5px;
  z-index: 1000;
  animation: slideDown 0.3s ease-out;
}

.error-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.error-icon {
  font-size: 16px;
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
  animation: fadeIn 0.3s ease-out;
}

.dialog-container {
  position: relative;
  animation: scaleIn 0.3s ease-out;
}

.dialog-close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  background-color: rgba(0, 0, 0, 0.1);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 14px;
  transition: background-color 0.2s;
  z-index: 1001;
}

.dialog-close:hover {
  background-color: rgba(0, 0, 0, 0.2);
  color: #333;
}

.icon {
  font-style: normal;
}

@keyframes slideDown {
  from {
    transform: translateX(-50%) translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateX(-50%) translateY(0);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .app-header {
    padding: 10px 15px;
  }
  
  .header-left {
    gap: 10px;
  }
  
  .app-title {
    font-size: 18px;
  }
  
  .header-center {
    padding: 0 10px;
  }
  
  .app-subtitle {
    font-size: 12px;
  }
  
  .sidebar {
    flex: 0 0 280px;
  }
  
  .sidebar.collapsed {
    margin-left: -280px;
  }
  
  .messages-container {
    padding: 15px 0;
  }
  
  .welcome-content {
    padding: 0 10px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .chat-message {
    padding: 0 10px;
  }
  
  .toolbar-btn {
    padding: 6px 8px;
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .sidebar {
    flex: 0 0 100vw;
    position: absolute;
    top: 60px;
    bottom: 0;
    z-index: 100;
  }
  
  .sidebar.collapsed {
    margin-left: -100vw;
  }
  
  .header-center {
    display: none;
  }
  
  .app-title {
    font-size: 16px;
  }
}
</style> 