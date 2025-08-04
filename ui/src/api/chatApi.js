import axios from 'axios'

// 配置axios基础URL
const API_BASE_URL = 'http://localhost:8081/api'

// 配置axios默认设置
axios.defaults.timeout = 10000
axios.defaults.headers.post['Content-Type'] = 'application/json'

/**
 * 使用 SSE 方式调用聊天接口
 * @param {string} conversationId 会话ID
 * @param {string} message 用户消息
 * @param {string} userId 用户ID
 * @param {Function} onMessage 接收消息的回调函数
 * @param {Function} onError 错误处理回调函数
 * @param {Function} onClose 连接关闭回调函数
 * @returns {EventSource} 返回 EventSource 对象，用于手动关闭连接
 */
export function chatWithSSE(conversationId, message, userId = 'default_user', onMessage, onError, onClose) {
    // 构建URL参数
    const params = new URLSearchParams({
        memoryId: conversationId, // 兼容旧版本参数名
        conversationId: conversationId,
        userId: userId,
        message: message
    })
    
    // 创建 EventSource 连接
    const eventSource = new EventSource(`${API_BASE_URL}/ai/chat?${params}`)
    
    // 处理接收到的消息
    eventSource.onmessage = function(event) {
        try {
            const data = event.data
            if (data && data.trim() !== '') {
                onMessage(data)
            }
        } catch (error) {
            console.error('解析消息失败:', error)
            onError && onError(error)
        }
    }
    
    // 处理错误
    eventSource.onerror = function(error) {
        console.log('SSE 连接状态:', eventSource.readyState)
        // 只有在连接状态不是正常关闭时才报错
        if (eventSource.readyState !== EventSource.CLOSED) {
            console.error('SSE 连接错误:', error)
            onError && onError(error)
        } else {
            console.log('SSE 连接正常结束')
        }
        
        // 确保连接关闭
        if (eventSource.readyState !== EventSource.CLOSED) {
            eventSource.close()
        }
    }
    
    // 处理连接关闭
    eventSource.onclose = function() {
        console.log('SSE 连接已关闭')
        onClose && onClose()
    }
    
    return eventSource
}

/**
 * 普通聊天接口（非流式）
 * @param {string} conversationId 会话ID
 * @param {string} message 用户消息
 * @param {string} userId 用户ID
 * @returns {Promise<Object>} 返回聊天响应
 */
export async function chatSync(conversationId, message, userId = 'default_user') {
    try {
        const response = await axios.post(`${API_BASE_URL}/ai/chat`, {
            conversationId,
            message,
            userId
        })
        return response.data
    } catch (error) {
        console.error('同步聊天请求失败:', error)
        throw error
    }
}

/**
 * 创建新会话
 * @param {string} userId 用户ID
 * @param {string} title 会话标题
 * @returns {Promise<Object>} 返回创建的会话信息
 */
export async function createConversation(userId, title) {
    try {
        const response = await axios.post(`${API_BASE_URL}/ai/conversation`, {
            userId,
            title
        })
        return response.data
    } catch (error) {
        console.error('创建会话失败:', error)
        throw error
    }
}

/**
 * 获取用户会话列表
 * @param {string} userId 用户ID
 * @param {number} page 页码
 * @param {number} size 每页大小
 * @returns {Promise<Object>} 返回会话列表
 */
export async function getUserConversations(userId, page = 0, size = 20) {
    try {
        const response = await axios.get(`${API_BASE_URL}/ai/conversations`, {
            params: {
                userId,
                page,
                size
            }
        })
        return response.data
    } catch (error) {
        console.error('获取会话列表失败:', error)
        throw error
    }
}

/**
 * 添加文档到RAG知识库
 * @param {string} filePath 文件路径
 * @returns {Promise<Object>} 返回添加结果
 */
export async function addDocumentToRAG(filePath) {
    try {
        const response = await axios.post(`${API_BASE_URL}/ai/rag/document`, {
            filePath
        })
        return response.data
    } catch (error) {
        console.error('添加文档失败:', error)
        throw error
    }
}

/**
 * 重新加载RAG文档
 * @returns {Promise<Object>} 返回重新加载结果
 */
export async function reloadRAGDocuments() {
    try {
        const response = await axios.post(`${API_BASE_URL}/ai/rag/reload`)
        return response.data
    } catch (error) {
        console.error('重新加载文档失败:', error)
        throw error
    }
}

/**
 * 检查后端服务是否可用
 * @returns {Promise<boolean>} 返回服务是否可用
 */
export async function checkServiceHealth() {
    try {
        const response = await axios.get(`${API_BASE_URL}/ai/health`, {
            timeout: 5000
        })
        return response.status === 200
    } catch (error) {
        console.error('服务健康检查失败:', error)
        return false
    }
} 