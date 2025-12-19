import {clearHtml } from 'utils';
export default {
  file(message) {
    return "[文件]";
  },
  image(message) {
    return "[图片]";
  },
  text(message) {
    return this.emojiNameToImage(clearHtml(message.content));
  },
  event(message){
    return '[通知]';
  },
};
