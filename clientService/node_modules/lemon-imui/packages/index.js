import Contextmenu from "./directives/contextmenu";
import LemonTabs from "./components/tabs";
import LemonPopover from "./components/popover";
import LemonButton from "./components/button";
import LemonBadge from "./components/badge";
import LemonAvatar from "./components/avatar";
import LemonContact from "./components/contact";
import LemonEditor from "./components/editor";
import LemonMessages from "./components/messages";
import LemonMessageBasic from "./components/message/basic";
import LemonMessageText from "./components/message/text";
import lemonMessageImage from "./components/message/image";
import lemonMessageFile from "./components/message/file";
import lemonMessageEvent from "./components/message/event";

import LemonIMUI from "./components/index";
import "./styles/common/index.styl";
const version = "1.4.2";
const components = [
  LemonIMUI,
  LemonContact,
  LemonMessages,
  LemonEditor,
  LemonAvatar,
  LemonBadge,
  LemonButton,
  LemonPopover,
  LemonTabs,
  LemonMessageBasic,
  LemonMessageText,
  lemonMessageImage,
  lemonMessageFile,
  lemonMessageEvent
];
const install = (Vue) => {
  Vue.directive("LemonContextmenu", Contextmenu);
  components.forEach(component => {
    Vue.component(component.name, component);
  });
};

if (typeof window !== "undefined" && window.Vue) {
  install(window.Vue);
}

export default {
  version,
  install
};
