import Vue from 'vue'
import Vuex from 'vuex'
import common from './module/common' //  公共模块

import messageDispatcher from './module/messageDispatcher' // ✅ 消息分发器

import user from './module/user'

Vue.use(Vuex)

export default new Vuex.Store({
	state: {
		//
	},
	getters: {
	
	},
	mutations: {
		//
	},
	actions: {
		//
	},
	modules: {
		common,
		user,
		messageDispatcher    // ✅ 注册消息分发器模块
	}
})
