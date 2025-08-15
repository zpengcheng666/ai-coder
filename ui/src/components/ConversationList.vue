<template>
  <div class="conversation-list">
    <div class="conversation-header">
      <h3>会话列表</h3>
      <button @click="createNewConversation" class="new-conversation-btn">
        <span class="icon">+</span>
        新建会话
      </button>
    </div>

    <div class="conversation-search">
      <input
        v-model="searchKeyword"
        placeholder="搜索会话..."
        class="search-input"
      />
    </div>

    <div class="conversation-items">
      <div
        v-for="conversation in filteredConversations"
        :key="conversation.conversationId"
        :class="['conversation-item', { active: currentConversationId === conversation.conversationId }]"
        @click="selectConversation(conversation)"
      >
        <div class="conversation-info">
          <h4 class="conversation-title">{{ conversation.title || '未命名会话' }}</h4>
          <p class="conversation-time">{{ formatTime(conversation.createTime) }}</p>
        </div>
        <div class="conversation-actions">
          <button @click.stop="deleteConversation(conversation)" class="delete-btn">
            <span class="icon">🗑️</span>
          </button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">
      加载中...
    </div>

    <div v-if="error" class="error">
      {{ error }}
    </div>

    <!-- 创建会话对话框 -->
    <div v-if="showCreateDialog" class="dialog-overlay" @click="showCreateDialog = false">
      <div class="dialog" @click.stop>
        <h3>创建新会话</h3>
        <input
          v-model="newConversationTitle"
          placeholder="请输入会话标题"
          class="dialog-input"
          @keydown.enter="confirmCreateConversation"
        />
        <div class="dialog-actions">
          <button @click="showCreateDialog = false" class="cancel-btn">取消</button>
          <button @click="confirmCreateConversation" class="confirm-btn">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getUserConversations, createConversation, deleteConversation as deleteConversationApi } from '../api/chatApi.js'
import { formatTime } from '../utils/index.js'

export default {
  name: 'ConversationList',
  props: {
    userId: {
      type: String,
      default: 'default_user'
    },
    currentConversationId: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      conversations: [],
      loading: false,
      error: null,
      searchKeyword: '',
      showCreateDialog: false,
      newConversationTitle: ''
    }
  },
  computed: {
    filteredConversations() {
      if (!this.searchKeyword) {
        return this.conversations
      }
      return this.conversations.filter(conv =>
        conv.title && conv.title.toLowerCase().includes(this.searchKeyword.toLowerCase())
      )
    }
  },
  methods: {
    async loadConversations() {
      this.loading = true
      this.error = null
      try {
        const response = await getUserConversations(this.userId)
        this.conversations = response.conversations || []
      } catch (error) {
        this.error = '加载会话列表失败'
        console.error('加载会话列表失败:', error)
      } finally {
        this.loading = false
      }
    },

    selectConversation(conversation) {
      this.$emit('conversation-selected', conversation)
    },

    createNewConversation() {
      this.showCreateDialog = true
      this.newConversationTitle = ''
    },

    async confirmCreateConversation() {
      if (!this.newConversationTitle.trim()) {
        this.newConversationTitle = '新会话'
      }

      try {
        const response = await createConversation(this.userId, this.newConversationTitle)
        this.showCreateDialog = false
        this.newConversationTitle = ''
        
        // 创建成功后重新加载会话列表
        await this.loadConversations()
        
        // 通知父组件选择新创建的会话
        this.$emit('conversation-created', {
          conversationId: response.conversationId,
          title: response.title
        })
      } catch (error) {
        this.error = '创建会话失败'
        console.error('创建会话失败:', error)
      }
    },

    async deleteConversation(conversation) {
      if (!conversation || !conversation.conversationId) return
      if (!confirm(`确定要删除会话"${conversation.title}"吗？`)) return
      try {
        await deleteConversationApi(conversation.conversationId, this.userId)
        // 成功后刷新列表
        await this.loadConversations()
        // 如果删除的是当前会话，通知父组件清空选择
        if (this.currentConversationId === conversation.conversationId) {
          this.$emit('conversation-selected', null)
        }
      } catch (error) {
        this.error = '删除会话失败'
        console.error('删除会话失败:', error)
      }
    },

    formatTime(dateString) {
      return formatTime(new Date(dateString))
    }
  },

  mounted() {
    this.loadConversations()
  },

  watch: {
    userId() {
      this.loadConversations()
    }
  }
}
</script>

<style scoped>
.conversation-list {
  width: 300px;
  height: 100%;
  background-color: #f8f9fa;
  border-right: 1px solid #e1e5e9;
  display: flex;
  flex-direction: column;
}

.conversation-header {
  padding: 20px;
  border-bottom: 1px solid #e1e5e9;
  background-color: white;
}

.conversation-header h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.new-conversation-btn {
  width: 100%;
  padding: 10px 15px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  transition: background-color 0.2s;
}

.new-conversation-btn:hover {
  background-color: #0056b3;
}

.conversation-search {
  padding: 15px 20px;
  background-color: white;
  border-bottom: 1px solid #e1e5e9;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #007bff;
}

.conversation-items {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}

.conversation-item {
  padding: 15px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.conversation-item:hover {
  background-color: #f5f5f5;
}

.conversation-item.active {
  background-color: #e3f2fd;
  border-left: 3px solid #007bff;
}

.conversation-info {
  flex: 1;
}

.conversation-title {
  margin: 0 0 5px 0;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-time {
  margin: 0;
  font-size: 12px;
  color: #666;
}

.conversation-actions {
  display: flex;
  gap: 5px;
}

.delete-btn {
  padding: 5px;
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 3px;
  opacity: 0.6;
  transition: opacity 0.2s, background-color 0.2s;
}

.delete-btn:hover {
  opacity: 1;
  background-color: #ffebee;
}

.loading, .error {
  padding: 20px;
  text-align: center;
  color: #666;
  font-size: 14px;
}

.error {
  color: #d32f2f;
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
  padding: 24px;
  border-radius: 8px;
  width: 400px;
  max-width: 90vw;
}

.dialog h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #333;
}

.dialog-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  margin-bottom: 20px;
  outline: none;
  transition: border-color 0.2s;
}

.dialog-input:focus {
  border-color: #007bff;
}

.dialog-actions {
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
  background-color: #007bff;
  color: white;
}

.confirm-btn:hover {
  background-color: #0056b3;
}

.icon {
  font-style: normal;
}

/* 滚动条样式 */
.conversation-items::-webkit-scrollbar {
  width: 6px;
}

.conversation-items::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.conversation-items::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.conversation-items::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

@media (max-width: 768px) {
  .conversation-list {
    width: 100%;
    height: auto;
    max-height: 40vh;
    border-right: none;
    border-bottom: 1px solid #e1e5e9;
  }
}
</style>