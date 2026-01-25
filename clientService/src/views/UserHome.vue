<template>
  <div class="user-dashboard-wrapper">
    <header class="dashboard-header user-info-card">
      <div class="user-avatar">
        <img :src="userInfo.avatarUrl" style="width:82px;height:82px;" alt="用户头像" />
      </div>
      <div class="user-details">
        <h1 class="user-name">✨ {{ userInfo.username }}</h1>
        <p class="user-intro">{{ userInfo.introduction }}</p>
        <div class="meta-info">
          <span class="meta-item">🌐 IP: {{ userInfo.lastLoginIp }}</span>
          <span class="meta-item">⏱️ 最近登录: {{ userInfo.lastLoginTime }}</span>
          <span class="meta-item">🗓️ 注册天数: {{ userInfo.registrationDays }} 天</span>
        </div>
      </div>
    </header>

    <div class="stat-grid">
      <div v-for="stat in stats" :key="stat.title" class="stat-card" :style="{ '--bg-color': stat.bgColor }">
        <span class="stat-icon">{{ stat.icon }}</span>
        <div class="stat-info">
          <p class="stat-title">{{ stat.title }}</p>
          <h2 class="stat-value">{{ stat.value }}</h2>
        </div>
      </div>
    </div>

    <div class="content-split-grid">

      <div class="model-section">
        <h3>🧠 接入模型</h3>
        <div class="model-cards-container">
          <div v-for="model in models.slice(0, 5)" :key="model.name" class="model-card">
            <div class="model-header">
              <h4 class="model-name">{{ model.icon }} {{ model.name }}</h4>
              <span :class="['model-type-tag', `tag-${model.type}`]">{{ model.typeText }}</span>
            </div>
            <p class="model-advantage">{{ model.advantage }}</p>
          </div>
        </div>
        <button class="detail-button" @click="viewModelDetails">查看全部模型详情</button>
      </div>

      <div class="log-section">
        <h3>📢 操作日志</h3>
        <div class="log-list-wrapper">
          <ul class="activity-logs">
            <li v-for="log in activityLogs.slice(0, 10)" :key="log.time + log.message" class="log-item">
              <div class="log-indicator"></div>
              <span class="log-time">{{ log.time }}</span>
              <span class="log-message">{{ log.message }}</span>
            </li>
          </ul>
        </div>

        <button class="detail-button" @click="viewLogDetails" style="margin-top: 10px;">查看完整日志记录</button>
      </div>

    </div>
  </div>
</template>

<script>
export default {
  name: 'UserDashboard',

  data() {
    return {
      // 用户信息
      userInfo: {
        avatarUrl: 'https://mjzjcdn.heycross.com/240601/u/ddwo6zwjgu80/b59855eb-9b0c-4291-9be2-56237c23b85f.jpg',
        username: 'AI_PowerUser_V5',
        introduction: '一名热爱数据分析和知识管理的忠实用户。专注于提升效率。',
        lastLoginIp: '192.168.1.100',
        lastLoginTime: new Date().toLocaleString(),
        registrationDays: 480,
      },

      // 统计数据
      stats: [
        { title: '知识库数量', value: '12 个', icon: '📚', bgColor: '#1e3a8a', },
        { title: '总文件数量', value: '1,560 份', icon: '📄', bgColor: '#440348', },
        { title: '已使用空间', value: '45.2 GB', icon: '💾', bgColor: '#b45309', },
        { title: '平均活跃度', value: '95%', icon: '🔥', bgColor: '#166534', },
      ],

      // 模型数据 (新增 type 和 typeText 字段)
      models: [
        {
          name: 'GPT-3.5 Turbo',
          icon: '💡',
          type: 'llm',
          typeText: '大语言模型',
          advantage: '通用语言理解和代码生成，响应速度快，成本效益高。',
        },
        {
          name: 'LLaMA-3 8B',
          icon: '🧠',
          type: 'embedding',
          typeText: '知识库向量',
          advantage: '专为本地知识库检索优化，保障数据隐私和安全性。',
        },
        {
          name: 'DALL·E 3',
          icon: '🎨',
          type: 'image',
          typeText: '图像生成',
          advantage: '高质量图像生成和编辑，适合设计和创意工作流。',
        },
        {
          name: 'BERT Base',
          icon: '🔍',
          type: 'search',
          typeText: '语义检索',
          advantage: '深度理解用户查询意图，提供高度相关的搜索结果和问答服务。',
        },
        {
          name: 'Stable Diffusion XL',
          icon: '🖼️',
          type: 'image',
          typeText: '图像生成',
          advantage: '开源且灵活的图像生成模型，支持精细化风格定制和训练。',
        }
      ],

      // 操作日志数据 (初始)
      activityLogs: [
        { time: '11:00 AM', message: '使用 **Claude 3 Opus** 进行了 15 次复杂文本总结。' }, // 新增
        { time: '10:45 AM', message: '成功创建了新的知识库：**【营销创意素材】**。' }, // 新增
        { time: '10:30 AM', message: '在【项目文档库】新增了 5 份文件。' },
        { time: '10:15 AM', message: '更新了个人介绍和头像。' },
        { time: '9:45 AM', message: '分享了知识库【技术笔记】给同事。' },
        { time: '9:30 AM', message: '修改了知识库【技术笔记】的访问权限，设置为只读。' }, // 新增
        { time: '9:00 AM', message: '完成了本周活跃度任务。' },
        { time: '8:30 AM', message: '使用 **DALL·E 3** 生成了 3 张海报草稿。' },
        { time: '8:00 AM', message: '登录时使用了 **二步验证**。' }, // 新增
      ],

      // 日志加载状态 (用于支持多数据加载)
      logLoading: false,
      hasMoreLogs: true,
    };
  },

  created() {
    console.log('UserDashboard component created.');
  },

  mounted() {
    // 确保显示最新登录时间
    this.userInfo.lastLoginTime = new Date().toLocaleString();
    console.log('UserDashboard component mounted.');
  },

  methods: {
    /**
     * ❗ 新增方法: 查看全部模型详情
     */
    viewModelDetails() {
      alert('跳转至全部模型配置页面...');
      // 实际应用中：this.$router.push('/settings/models');
    },

    /**
     * ❗ 新增方法: 查看完整日志记录
     */
    viewLogDetails() {
      alert('跳转至完整操作日志页面...');
      // 实际应用中：this.$router.push('/user/logs');
    },
    // 模拟异步加载更多日志
    loadMoreLogs() {
      if (this.logLoading || !this.hasMoreLogs) {
        return;
      }

      this.logLoading = true;
      console.log('开始加载更多日志...');

      // 模拟 API 请求延迟
      setTimeout(() => {
        const newLogs = [
          { time: '前天', message: '首次上传了【个人简历 V2.0】文件。' },
          { time: '前天', message: '将 **GPT-3.5 Turbo** 设置为默认模型。' },
          { time: '前天', message: '修改了密码。' },
        ];

        // 追加新数据
        this.activityLogs = [...this.activityLogs, ...newLogs];
        this.logLoading = false;

        // 模拟加载完所有数据
        this.hasMoreLogs = false;

        console.log('日志加载完成。');

      }, 1000); // 1秒延迟
    }
  },
};
</script>

<style scoped>
.user-avatar{
  width: 80px;
  height: 80px;
}
/* --- 基础配置 --- */
.user-dashboard-wrapper {
  min-height: 100vh;
  padding: 40px 60px;
  background-color: #0f172a;
  color: #e2e8f0;
  font-family: 'Segoe UI', Roboto, Helvetica, sans-serif;
}

/* --- 头部和统计网格样式 --- */
.dashboard-header {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 40px;
  display: flex;
  align-items: center;
  gap: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.stat-card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 15px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  position: relative;
  overflow: hidden;
  border-left: 5px solid var(--bg-color);
}

.stat-icon {
  font-size: 36px;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
}

.stat-title {
  font-size: 14px;
  color: #94a3b8;
  margin: 0 0 4px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  color: #f1f5f9;
}

/* --- 内容分割网格 --- */
.content-split-grid {
  display: grid;
  /* 左边模型区占 1 份，右边日志区占 2 份 */
  grid-template-columns: 1fr 2fr;
  gap: 24px;
}

.model-section {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);

  /* ❗ 设置相对定位，以便内部元素布局 */
  position: relative; 
  /* ❗ 确保整个卡片有足够的最小高度 */
  height: 600px;

  /* ❗ 使用 Flex 布局垂直排列内容 */
  display: flex;
  flex-direction: column;
}

.log-section {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);

  /* ❗ 设置相对定位，以便内部元素布局 */
  position: relative; 
  /* ❗ 确保整个卡片有足够的最小高度 */
  height: 600px;

  /* ❗ 使用 Flex 布局垂直排列内容 */
  display: flex;
  flex-direction: column;
}

.model-section h3,
.log-section h3 {
  font-size: 20px;
  font-weight: 600;
  color: #cbd5e1;
  margin-top: 0;
  margin-bottom: 20px;
  border-bottom: 1px solid #334155;
  padding-bottom: 10px;
  flex-shrink: 0;
}

/* --- 优化后的模型卡片样式 --- */
.model-cards-container {
  display: flex;
  flex-direction: column;
  gap: 15px;

  /* ❗ 启用滚动功能 */
  overflow-y: auto;
  
  /* ❗ 自动填充可用高度 */
  flex-grow: 1; 
  
  /* ❗ 增加内边距以避免滚动条紧贴内容 */
  padding-right: 15px;
}

.model-card {
  background: #1f2a3a;
  padding: 18px 20px;
  border-radius: 10px;
  border: 1px solid #334155;
  transition: transform 0.3s, box-shadow 0.3s;
  position: relative;
}

.model-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.15);
}

.model-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.model-name {
  font-size: 17px;
  font-weight: 700;
  color: #ffffff;
  margin: 0;
}

.model-type-tag {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 600;
  text-transform: uppercase;
}

/* 模型类型标签颜色定义 */
.tag-llm {
  background-color: #3b82f6;
  /* Blue */
  color: #e0f2fe;
}

.tag-embedding {
  background-color: #10b981;
  /* Green */
  color: #ecfdf5;
}

.tag-image {
  background-color: #f97316;
  /* Orange */
  color: #fff7ed;
}

.model-advantage {
  font-size: 13px;
  color: #a0aec0;
  margin: 0;
  line-height: 1.5;
}

/* --- 优化后的操作日志样式 (时间轴风格) --- */
.activity-logs {
  list-style: none;
  padding: 0;
  margin: 0;
}

.activity-logs li {
  display: flex;
  align-items: flex-start;
  padding: 12px 0 12px 20px;
  position: relative;
}

/* 日志项之间的点状连接线 */
.activity-logs li:not(:last-child)::before {
  content: '';
  position: absolute;
  top: 0;
  left: 3px;
  height: 100%;
  width: 1px;
  background-color: #475569;
  z-index: 0;
}

/* 日志点的指示器 */
.log-indicator {
  position: absolute;
  left: 0;
  top: 18px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #3b82f6;
  z-index: 1;
  border: 2px solid #1e293b;
}

.log-time {
  font-size: 13px;
  color: #94a3b8;
  width: 100px;
  flex-shrink: 0;
}

.log-message {
  font-size: 14px;
  color: #e2e8f0;
  line-height: 1.4;
  border-bottom: 1px dotted #334155;
  flex-grow: 1;
  padding-bottom: 12px;
}

.activity-logs li:last-child .log-message {
  border-bottom: none;
}

/* --- 加载更多按钮样式 --- */
.load-more-button {
  width: 100%;
  padding: 10px 15px;
  margin-top: 20px;
  background-color: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: background-color 0.2s, opacity 0.2s;
  display: flex;
  justify-content: center;
  align-items: center;
}

.load-more-button:hover:not(:disabled) {
  background-color: #2563eb;
}

.load-more-button:disabled {
  background-color: #475569;
  cursor: not-allowed;
  opacity: 0.7;
}

/* 加载动画 (Spinner) */
.loading-spinner {
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top: 3px solid #ffffff;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}


/* --- 新增的详情按钮样式 --- */
.detail-button {
  width: 100%;
  padding: 10px 15px;
  margin-top: 20px;
  /* 使用稍暗的颜色作为区分 */
  background-color: #475569;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: background-color 0.2s;
  text-align: center;
}

.detail-button:hover {
  background-color: #64748b;
}

/* 保持 load-more-button 和 detail-button 都有 100% 宽度 */
.load-more-button {
  /* ... (原样式不变) */
  margin-top: 20px;
  /* 确保它在日志列表下方有间距 */
}

/* 确保日志区在 load-more-button 下方有额外的间距来放置 detail-button */
.log-section .detail-button {
  margin-top: 10px;
  /* 调整与上方 "加载更多" 按钮的间距 */
}

.log-list-wrapper {
  /* ❗ 启用滚动功能 */
  overflow-y: auto;
  
  /* ❗ 自动填充可用高度 */
  flex-grow: 1;
  
  /* ❗ 设置一个最大高度，确保滚动条出现 */
  max-height: 500px; /* 根据实际需求调整此值 */

  /* ❗ 增加内边距以避免滚动条紧贴内容 */
  padding-right: 15px; 
}

.model-cards-container::-webkit-scrollbar,
.log-list-wrapper::-webkit-scrollbar {
  width: 8px;
}
.model-cards-container::-webkit-scrollbar-thumb,
.log-list-wrapper::-webkit-scrollbar-thumb {
  background-color: #475569;
  border-radius: 4px;
}
.model-cards-container::-webkit-scrollbar-track,
.log-list-wrapper::-webkit-scrollbar-track {
  background-color: #1e293b;
}


.user-details {
  display: flex; /* 启用 Flex 布局 */
  flex-direction: column; /* 垂直排列子元素 */
  gap: 8px; /* 增加 h1, p, meta-info 之间的垂直间距 */
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin: 0; /* 清除默认 margin */
}

.user-intro {
  font-size: 15px;
  color: #94a3b8;
  margin: 0 0 10px 0; /* 增加简介和下方元信息之间的距离 */
}

/* 2. 优化 meta-info 的布局和元素间距 */
.meta-info {
  /* ❗ 确保元信息项在一行内，并使用间距 */
  display: flex;
  flex-wrap: wrap; /* 防止在窄屏上溢出 */
  gap: 20px; /* ❗ 关键：增加项目之间的水平间距，使其不紧贴 */
  padding-top: 5px; /* 在元信息上方增加一点垂直间距 */
  border-top: 1px solid #334155; /* 添加一条分割线，区分简介和元信息 */
  padding-bottom: 5px;
}

.meta-item {
  font-size: 13px;
  color: #cbd5e1;
  /* 增加右侧间距以确保视觉分离，虽然有 gap，但有时辅助 margin 更有用 */
  /* margin-right: 20px; */ 
}
</style>
