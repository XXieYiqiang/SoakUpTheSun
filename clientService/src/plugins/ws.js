/**
 * WebSocketService 封装类
 * - 自动重连
 * - 心跳机制
 * - 统一消息分发到 Vuex
 */

import store from '@/store'  // 路径根据你的项目调整
import { handleLogout } from '@/api/user'

class WebSocketService {
  constructor(store) {
    this.store = store;
    this.socket = null;
    this.userId = null;
    this.reconnectInterval = 5000;
    this.heartbeatInterval = 30000;
    this.heartbeatTimer = null;
  }

  /**
   * 建立 WebSocket 连接
   * @param {string} userId 当前登录用户 ID
   */
  connect(userId) {
    this.userId = userId;
    const url = `ws://127.0.0.1:9999/ws?userId=${encodeURIComponent(userId)}`;
    console.log(`[WebSocket][连接] 正在连接 userId=${userId} 地址: ${url}`);

    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      console.log(`[WebSocket][✅ 成功] 用户 ${userId} 连接已建立`);

      this.send({
        cmd: 100003,
        data: {}
      });

      this.startHeartbeat();
    };

    this.socket.onmessage = (event) => {
      try {
        console.log(`[WebSocket][📩 原始数据] ${event.data}`);
        const data = JSON.parse(event.data);
        console.log(`[WebSocket][📥 已解析] cmd=${data.cmd}`, data);
        this.handleMessage(data);
      } catch (err) {
        console.error('[WebSocket][❌ 解析失败]', event.data, err);
      }
    };

    this.socket.onclose = () => {
      console.warn(`[WebSocket][⚠️ 关闭] 连接已关闭，5秒后重连 userId=${userId}`);
      this.reconnect();
    };

    this.socket.onerror = (err) => {
      console.error('[WebSocket][❌ 错误] 连接异常:', err);
      this.socket.close(); // 触发 onclose -> reconnect

      // 连接失败后做登出操作，更新用户在线状态
      // 获取用户信息
      const userInfo = store.getters['user/userInfo'];

      // if (userInfo) {
      //   const param = {
      //     userId: userInfo.userId
      //   }

      //   handleLogout(param).then(res => {
      //     console.log("用户连接失败后做登出操作，更新 数据库用户在线状态")
      //   })

      //   store.dispatch('user/logout')
      // }
    };
  }

  /**
   * 自动重连
   */
  reconnect() {
    if (!this.userId) {
      console.warn('[WebSocket][⚠️ 重连取消] userId 为空');
      return;
    }

    setTimeout(() => {
      console.log(`[WebSocket][🔄 重连中] 正在尝试重新连接 userId=${this.userId}...`);
      this.connect(this.userId);
    }, this.reconnectInterval);
  }

  /**
   * 启动心跳包
   */
  startHeartbeat() {
    if (this.heartbeatTimer) clearInterval(this.heartbeatTimer);
    
    this.heartbeatTimer = setInterval(() => {
      const ping = { cmd: 100002, timestamp: Date.now(), data: {} };
      console.log('[WebSocket][🟢 心跳] 发送 ping:', ping);

      this.send(ping)

      this.send({
        cmd: 100003,
        data: {}
      });

    }, this.heartbeatInterval);


  }

  /**
   * 发送消息
   */
  send(data) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      const str = JSON.stringify(data);
      console.log('[WebSocket][📤 发送]', str);
      this.socket.send(str);
    } else {
      console.warn('[WebSocket][⚠️ 未连接] 当前连接不可用，发送失败');
    }
  }

  /**
   * 派发消息到 Vuex
   */
  handleMessage(msg) {
    console.log(`[WebSocket][📬 分发] 派发至 messageDispatcher: cmd=${msg.cmd}`);
    this.store.dispatch('messageDispatcher/dispatch', msg);
  }

  /**
   * 手动关闭连接（用于注销/切换用户）
   */
  close() {
    console.log('[WebSocket][❎ 关闭] 手动关闭连接');
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    clearInterval(this.heartbeatTimer);
  }
}

export default WebSocketService;