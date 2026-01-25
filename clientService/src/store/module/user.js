const state = {
  token: localStorage.getItem('token') || '',
  // userInfo: (() => {
  //   try {
  //     return JSON.parse(localStorage.getItem('userInfo'));
  //   } catch (e) {
  //     console.error('解析 userInfo 失败', e);
  //     return null; // 或返回默认空对象 {}
  //   }
  // })()
};

//store.commit('user/CLEAR_USER');
const mutations = {
  SET_TOKEN(state, token) {
    state.token = token;
    localStorage.setItem('token', token);
  },
  SET_USER_INFO(state, userInfo) {
    state.userInfo = userInfo;
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
  },
  CLEAR_USER(state) {
    state.token = '';
    state.userInfo = null;
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
  }
};

//this.$store.dispatch('user/logout');
const actions = {
  login({ commit }, { token, userInfo }) {
    commit('SET_TOKEN', token);
    commit('SET_USER_INFO', userInfo);
  },
  logout({ commit,dispatch }) {
    commit('CLEAR_USER')
  }
};

// const userInfo = this.$store.getters['user/userInfo'];
const getters = {
  isLogin: state => !!state.token,
  token: state => state.token,
  userInfo: state => state.userInfo
};

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
};
