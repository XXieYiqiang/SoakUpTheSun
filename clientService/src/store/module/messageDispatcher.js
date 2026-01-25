/**
 * 📨 消息分发器模块
 * - WebSocket 收到的消息将统一由这里分类分发
 * - 支持私聊、群聊、系统通知、全站广播、ping 等类型
 */

// store/modules/messageDispatcher.js
import WebSocketService from '@/plugins/ws'; // ✅ 正确
export default {
  namespaced: true,

  actions: {
    /**
     * 🔧 消息总入口
     * @param {Object} message WebSocket 接收到的原始消息对象
     */
    dispatch({ dispatch }, content) {

      const { cmd, message, data } = content;

      console.log(`[Dispatcher] 📩 收到消息类型: ${cmd}`, data);

      switch (cmd) {
        case 111111:
          console.log("响应成功：" + message);
          break
        // 响应消息
        case 100001:
          dispatch('handleChatMessage', data);
          break
        //处理查询用户列表响应
        case 1100003:
          dispatch('handleUserList', data);
        //后端推送：需要更新查询在线用户列表信息  
        case 1100005:
          dispatch('updateOnlineUserInfoList', data);
        case 'chat':
          dispatch('handleChatMessage', data);
          break;
        case 'system':
          dispatch('handleSystemMessage', data);
          break;
        case 'broadcast':
          dispatch('handleBroadcast', data);
          break;
        case 'ping':
          // 心跳响应（可选）
          console.log('[Dispatcher] 💓 收到 ping 心跳');
          break;
        default:
          console.warn('[Dispatcher] ⚠️ 未知消息类型:', cmd, data);
      }
    },
    /**
     * 处理用户列表查询
     */
    handleUserList({ commit, rootState }, data) {
      console.log('[Dispatcher] ✅ 接收到用户列表:', data);
      commit('message/SET_USER_LIST', data, { root: true });
    },
    /**
     * 💬 处理私聊 / 群聊消息
     * - 判断是当前聊天对象就直接展示
     * - 否则增加未读数
     */
    handleChatMessage({ commit, rootState }, data) {
      const currentId = rootState.chat?.currentContactId;
      const contactId = data.targetType === 'user' ? data.senderId : data.receiverId;

      console.log(`[Dispatcher] 💬 来自 ${data.targetType === 'user' ? '私聊' : '群聊'} 消息: ${contactId}`, data);

      // 添加消息到 Vuex message 模块
      commit('message/ADD_MESSAGE', { contactId, message: data }, { root: true });

      // 当前正在聊天窗口不是该会话，增加未读数
      if (contactId !== currentId) {
        console.log(`[Dispatcher] 🔴 ${contactId} 不是当前窗口，增加未读数`);
        commit('message/INCREMENT_UNREAD', contactId, { root: true });
      } else {
        console.log(`[Dispatcher] 🟢 当前窗口消息，自动已读`);
        commit('message/RESET_UNREAD', contactId, { root: true });
      }
    },
    /**
     * 推送更新：查询用户列表
     */
    updateOnlineUserInfoList({ commit, rootState }, data) {
      console.log('推送更新查询用户列表')
      const webService = new WebSocketService(this.$store);
      webService.send({
        cmd: 100003,
        data: {}
      });
    },
    /**
     * 📢 处理系统通知（如账号异常、审核成功等）
     */
    handleSystemMessage({ commit }, data) {
      // 这里可扩展为保存到系统通知表、或直接显示 UI 通知
      console.info('[Dispatcher] 📢 系统通知:', data.title || data.content);
      // 示例：可集成 notification/toast 插件提示
      // this._vm.$notify({ title: data.title, message: data.content });
    },

    /**
     * 🌍 处理全站广播消息（如系统维护提醒）
     */
    handleBroadcast({ commit }, data) {
      // 通常用于不持久化的即时提示
      console.info('[Dispatcher] 🌍 广播消息:', data);
      // 示例：使用全局弹窗组件显示
      // this._vm.$toast({ message: data.content, type: data.level || 'info' });
    }
  }
};
