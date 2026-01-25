/**
 * 存放工程配置项
 */
const config = {
	/**
	 * 请求后台接口 URL 代理时的上下文
	 * @description 仅适用于本地开发环境代理时使用，
	 * 生产环境需要在 nginx 配置中将 /api/ 代理到生产环境后台地址
	 * 对应 vue.config.js 中配置的 devServer.proxy 中的代理之一
	 * 若修改了此值，请同步修改 vue.config.js 中 devServer.proxy 对应的值
	 */
	baseContext: '/api',
	/**
	 * 存放 token 时的命名方式
	 */
	tokenKeyName: 'token'
}

export default config
