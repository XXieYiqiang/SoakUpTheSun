<template>
    <div class="landing-page">
        <div class="tech-bg">
            <div class="grid-overlay"></div>
            <div class="glow-point p1"></div>
            <div class="glow-point p2"></div>
        </div>

        <nav class="nav-bar">
            <div class="logo">
                <span class="logo-icon">☀️</span>
                <span class="logo-text">SoakUpTheSun</span>
            </div>
            <div class="nav-links">
                <a href="#features">核心功能</a>
                <a href="#architecture">架构体系</a>
                <a href="#tech-stack">技术栈</a>
            </div>
        </nav>

        <header class="hero">
            <div class="hero-content">
                <div class="badge">V2.5 高并发微服务架构</div>
                <h1 class="main-title">云端借眼，温暖视界</h1>
                <p class="sub-title">
                    基于 <strong>Go SFU 实时流媒体</strong> 与 <strong>AI 视觉分析</strong> 的全栈无障碍云辅助平台
                </p>
                <div class="cta-group">
                    <button class="btn-cyan" @click="toJoin">立即开启协助</button>
                    <button class="btn-ghost">查看架构图</button>
                </div>
            </div>

            <div class="mock-terminal">
                <div class="terminal-header">
                    <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                    <span class="terminal-title">SFU Node-01 Status</span>
                </div>
                <div class="terminal-body">
                    <p><span class="cyan">></span> [WebRTC] Signal: Connected</p>
                    <p><span class="cyan">></span> [Redis] Hot Pool: 1,284 Active Volunteers</p>
                    <p><span class="cyan">></span> [AI] OCR Engine: Ready (Latency: 45ms)</p>
                    <div class="pulse-line"></div>
                </div>
            </div>
        </header>

        <section id="features" class="section">
            <h2 class="section-title">核心功能亮点</h2>
            <div class="feature-grid">
                <div class="feature-card">
                    <div class="icon">⚡️</div>
                    <h3>毫秒级热池匹配</h3>
                    <p>利用 Redis Set 维护实时在线志愿者池，结合 ES 权重打分，确保 99% 接通率。</p>
                </div>
                <div class="feature-card active">
                    <div class="icon">🎥</div>
                    <h3>自研 Go SFU 服务</h3>
                    <p>基于 Pion 深度定制，WebRTC 转发，端到端延迟控制在 200ms 以内。</p>
                </div>
                <div class="feature-card">
                    <div class="icon">🛡️</div>
                    <h3>短链防御体系</h3>
                    <p>布隆过滤器 + 令牌桶限流，阻断 99% 恶意遍历攻击。</p>
                </div>
                <div class="feature-card">
                    <div class="icon">💰</div>
                    <h3>Lua 原子扣减</h3>
                    <p>高性能秒杀逻辑，杜绝公益奖品超卖现象。</p>
                </div>
            </div>
        </section>

        <section id="tech-stack" class="section dark-section">
            <h2 class="section-title">全栈技术架构</h2>
            <div class="tech-table-wrapper">
                <table class="tech-table">
                    <thead>
                        <tr>
                            <th>领域</th>
                            <th>技术选型</th>
                            <th>核心作用</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="(item, index) in techStack" :key="index">
                            <td class="tech-label">{{ item.field }}</td>
                            <td class="tech-val">{{ item.tech }}</td>
                            <td class="tech-desc">{{ item.effect }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <footer class="footer">
            <p>© 2024 SoakUpTheSun - 技术驱动公益</p>
            <div class="social-links">
                <span>GitHub Star ⭐</span>
                <span>Contact: a1103938364@gmail.com</span>
            </div>
        </footer>
    </div>
</template>

<script>
export default {
    data() {
        return {
            techStack: [
                { field: '音视频', tech: 'Go (Golang) + Pion', effect: 'WebRTC 信令与媒体转发' },
                { field: '服务治理', tech: 'Spring Cloud Alibaba', effect: 'Nacos 注册/配置中心' },
                { field: 'AI 计算', tech: 'Python + OpenCV', effect: 'OCR/物体识别模型自部署' },
                { field: '消息队列', tech: 'RocketMQ', effect: '任务异步分发与流量削峰' },
                { field: '搜索/分析', tech: 'Elasticsearch 7.x', effect: '志愿者多维匹配权重打分' }
            ]
        }
    },
    methods: {
        toJoin() {
            this.$router.push('/join');
        }
    }
}
</script>

<style scoped>
/* 颜色变量定义 */
:root {
    --bg-dark: #060c14;
    --card-bg: rgba(16, 24, 39, 0.8);
    --cyan: #00f2ff;
    --text-main: #ffffff;
    --text-dim: #8a99af;
}

.landing-page {
    background-color: #060c14;
    color: #fff;
    min-height: 100vh;
    position: relative;
    overflow-x: hidden;
}

/* 背景动效 */
.tech-bg {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 0;
}

.grid-overlay {
    width: 100%;
    height: 100%;
    background-image: linear-gradient(rgba(0, 242, 255, 0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(0, 242, 255, 0.03) 1px, transparent 1px);
    background-size: 50px 50px;
}

.glow-point {
    position: absolute;
    width: 400px;
    height: 400px;
    border-radius: 50%;
    filter: blur(100px);
    opacity: 0.15;
    background: #00f2ff;
}

.p1 {
    top: -100px;
    right: -100px;
}

.p2 {
    bottom: -100px;
    left: -100px;
    background: #3498db;
}

/* 导航栏 */
.nav-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 8%;
    background: rgba(6, 12, 20, 0.8);
    backdrop-filter: blur(10px);
    position: sticky;
    top: 0;
    z-index: 100;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.logo {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: bold;
    font-size: 20px;
}

.logo-icon {
    font-size: 24px;
}

.nav-links {
    display: flex;
    gap: 30px;
    align-items: center;
}

.nav-links a {
    color: #8a99af;
    text-decoration: none;
    font-size: 14px;
    transition: 0.3s;
}

.nav-links a:hover {
    color: #00f2ff;
}

/* Hero Section */
.hero {
    padding: 100px 8% 60px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    position: relative;
    z-index: 1;
}

.hero-content {
    max-width: 600px;
}

.badge {
    display: inline-block;
    padding: 5px 15px;
    background: rgba(0, 242, 255, 0.1);
    color: #00f2ff;
    border: 1px solid #00f2ff;
    border-radius: 20px;
    font-size: 12px;
    margin-bottom: 20px;
}

.main-title {
    font-size: 56px;
    line-height: 1.2;
    margin-bottom: 20px;
    font-weight: 800;
}

.sub-title {
    font-size: 18px;
    color: #8a99af;
    margin-bottom: 40px;
    line-height: 1.6;
}

/* 按钮样式 */
.btn-cyan {
    background: #00f2ff;
    color: #000;
    border: none;
    padding: 15px 35px;
    border-radius: 8px;
    font-weight: bold;
    cursor: pointer;
    box-shadow: 0 0 20px rgba(0, 242, 255, 0.4);
    transition: 0.3s;
}

.btn-cyan:hover {
    transform: translateY(-2px);
    box-shadow: 0 0 30px rgba(0, 242, 255, 0.6);
}


.btn-ghost {
    margin-left: 20px;
    background: #00f2ff;
    color: #000;
    border: none;
    padding: 15px 35px;
    border-radius: 8px;
    font-weight: bold;
    cursor: pointer;
    box-shadow: 0 0 20px rgba(0, 242, 255, 0.4);
    transition: 0.3s;
}

.btn-ghost:hover {
    transform: translateY(-2px);
    box-shadow: 0 0 30px rgba(0, 242, 255, 0.6);
}

/* 模拟终端 */
.mock-terminal {
    width: 450px;
    background: #0d1624;
    border-radius: 12px;
    border: 1px solid rgba(0, 242, 255, 0.2);
    overflow: hidden;
    box-shadow: 0 30px 60px rgba(0, 0, 0, 0.5);
}

.terminal-header {
    background: #1a2638;
    padding: 10px 15px;
    display: flex;
    align-items: center;
    gap: 8px;
}

.dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #ff5f56;
}

.dot:nth-child(2) {
    background: #ffbd2e;
}

.dot:nth-child(3) {
    background: #27c93f;
}

.terminal-title {
    font-size: 12px;
    color: #8a99af;
    margin-left: 10px;
}

.terminal-body {
    padding: 20px;
    font-family: monospace;
    font-size: 13px;
    color: #fff;
}

.cyan {
    color: #00f2ff;
}

.pulse-line {
    height: 2px;
    width: 100%;
    background: #00f2ff;
    opacity: 0.3;
    margin-top: 20px;
    animation: pulse 2s infinite;
}

/* 功能网格 */
.section {
    padding: 80px 8%;
    position: relative;
    z-index: 1;
}

.section-title {
    text-align: center;
    font-size: 32px;
    margin-bottom: 50px;
}

.feature-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); /* 稍微加宽最小宽度 */
    gap: 30px; /* 增加间距，减少拥挤感 */
}

/* .feature-card {
    background: var(--card-bg);
    padding: 30px;
    border-radius: 16px;
    border: 1px solid rgba(255, 255, 255, 0.05);
    transition: 0.3s;
} */

.feature-card {
    position: relative;
    /* 使用更深的背景色，增加与环境的对比 */
    background: linear-gradient(145deg, rgba(20, 32, 50, 0.9) 0%, rgba(10, 15, 25, 0.95) 100%);
    padding: 40px 30px;
    border-radius: 20px;
    /* 默认边框：半透明青色，若隐若现 */
    border: 1px solid rgba(0, 242, 255, 0.1);
    transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
    overflow: hidden;
    cursor: default;
    z-index: 1;
}


.feature-card.active {
    border-color: #00f2ff;
    box-shadow: 0 0 15px rgba(0, 242, 255, 0.15);
}

.feature-card:hover {
    transform: translateY(-10px);
    background: rgba(20, 30, 48, 0.9);
}

.feature-card .icon {
    font-size: 30px;
    margin-bottom: 20px;
}

.feature-card h3 {
    margin-bottom: 15px;
    color: #00f2ff;
}

.feature-card p {
    font-size: 14px;
    color: #8a99af;
    line-height: 1.5;
}

/* 技术表格 */
.tech-table-wrapper {
    overflow-x: auto;
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.05);
}

.tech-table {
    width: 100%;
    border-collapse: collapse;
    background: var(--card-bg);
}

.tech-table th,
.tech-table td {
    padding: 20px;
    text-align: left;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.tech-table th {
    background: rgba(0, 242, 255, 0.05);
    color: #00f2ff;
    font-weight: 500;
}

.tech-label {
    font-weight: bold;
    color: #fff;
}

.tech-val {
    color: #00f2ff;
    font-family: monospace;
}

.tech-desc {
    color: #8a99af;
    font-size: 14px;
}

/* 动画 */
@keyframes pulse {
    0% {
        opacity: 0.2;
        transform: scaleX(0.8);
    }

    50% {
        opacity: 0.5;
        transform: scaleX(1);
    }

    100% {
        opacity: 0.2;
        transform: scaleX(0.8);
    }
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}


.footer {
    /* 核心居中逻辑 */
    display: flex;
    flex-direction: column;
    /* 纵向排列 */
    align-items: center;
    /* 水平居中 */
    justify-content: center;
    /* 垂直居中 */

    /* 布局与装饰 */
    padding: 40px 20px;
    background: rgba(6, 12, 20, 0.9);
    /* 深色半透明背景 */
    border-top: 1px solid rgba(0, 242, 255, 0.1);
    /* 顶部淡淡的青色细线 */
    position: relative;
    z-index: 1;
}

.footer p {
    color: #8a99af;
    /* 灰蓝色，不抢眼 */
    font-size: 14px;
    margin-bottom: 12px;
    letter-spacing: 1px;
}

.social-links {
    display: flex;
    gap: 20px;
    /* 两个信息点之间的间距 */
    color: rgba(255, 255, 255, 0.6);
    font-size: 13px;
}

.social-links span {
    transition: color 0.3s ease;
    cursor: default;
}

.social-links span:hover {
    color: #00f2ff;
    /* 悬停时变青色，增加交互感 */
}

.footer::after {
    content: "";
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 300px;
    height: 50px;
    background: radial-gradient(circle, rgba(0, 242, 255, 0.1) 0%, transparent 70%);
    filter: blur(20px);
    z-index: -1;
}
</style>