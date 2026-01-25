export default {
	state: {
		screenWidth: document.body.clientWidth //  屏幕宽度
	},
	mutations: {
		/**
		 * 改变屏幕宽度
		 * @description 表格显示列保存在 Vuex 和 cookie 中
		 * @param {object} state Vuex 的 state 对象
		 * @param {[]} data 屏幕宽度
		 */
		changeScreenWidth(state, data) {
			state.screenWidth = data
		}
	}
}
