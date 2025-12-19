import Vue from "vue";
import App from "./App.vue";
import LemonIMUI from "../packages";
Vue.use(LemonIMUI);

Vue.config.productionTip = false;

new Vue({
  render: h => h(App)
}).$mount("#app");
