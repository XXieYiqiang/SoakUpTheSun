import { isPlainObject, isFunction } from "utils/validate";
export function messageToHtml() {}
export function messageToText() {}
/**
 * 使用某个组件上的作用域插槽
 * @param {VueComponent} inject
 * @param {String} slotName
 * @param {Node} defaultElement
 * @param {Object} props
 */
export function useScopedSlot(slot, def, props) {
  return slot ? slot(props) : def;
}
export function padZero(val) {
  return val < 10 ? `0${val}` : val;
}
export function hoursTimeFormat(t) {
  const date = new Date(t);
  const nowDate = new Date();
  const Y = t => {
    return t.getFullYear();
  };
  const MD = t => {
    return `${t.getMonth() + 1}-${t.getDate()}`;
  };
  const dateY = Y(date);
  const nowDateY = Y(nowDate);

  let format;
  if (dateY !== nowDateY) {
    format = "y年m月d日 h:i";
  } else if (`${dateY}-${MD(date)}` === `${nowDateY}-${MD(nowDate)}`) {
    format = "h:i";
  } else {
    format = "m月d日 h:i";
  }
  return timeFormat(t, format);
}
export function timeFormat(t, format) {
  if (!format) format = "y-m-d h:i:s";
  if (t) t = new Date(t);
  else t = new Date();
  const formatArr = [
    t.getFullYear().toString(),
    padZero((t.getMonth() + 1).toString()),
    padZero(t.getDate().toString()),
    padZero(t.getHours().toString()),
    padZero(t.getMinutes().toString()),
    padZero(t.getSeconds().toString()),
  ];
  const reg = "ymdhis";
  for (let i = 0; i < formatArr.length; i++) {
    format = format.replace(reg.charAt(i), formatArr[i]);
  }
  return format;
}

export function funCall(event, callback) {
  if (isFunction(event)) {
    event(() => {
      callback();
    });
  } else {
    callback();
  }
}
/**
 * 获取数组相交的值组成新数组
 * @param {Array} a
 * @param {Array} b
 */
export function arrayIntersect(a, b) {
  return a.filter(x => b.includes(x));
}
//清除字符串内的所有HTML标签
export function clearHtml(str) {
  return str.replace(/<.*?>/gi, "");
}
//清除字符串内的所有HTML标签，除了IMG
export function clearHtmlExcludeImg(str) {
  return str.replace(/<(?!img).*?>/gi, "");
}
export function error(text) {
  throw new Error(text);
}
export function cloneDeep(obj) {
  const newobj = { ...obj };
  for (const key in newobj) {
    const val = newobj[key];
    if (isPlainObject(val)) {
      newobj[key] = cloneDeep(val);
    }
  }
  return newobj;
}

export function mergeDeep(o1, o2) {
  for (const key in o2) {
    if (isPlainObject(o1[key])) {
      o1[key] = mergeDeep(o1[key], o2[key]);
    } else {
      o1[key] = o2[key];
    }
  }
  return o1;
}

export function formatByte(value) {
  if (null == value || value == "") {
    return "0 Bytes";
  }
  var unitArr = ["B", "K", "M", "G", "T", "P", "E", "Z", "Y"];
  var index = 0;
  var srcsize = parseFloat(value);
  index = Math.floor(Math.log(srcsize) / Math.log(1024));
  var size = srcsize / Math.pow(1024, index);
  size = parseFloat(size.toFixed(2));
  return size + unitArr[index];
}

export function generateUUID() {
  var d = new Date().getTime();
  if (window.performance && typeof window.performance.now === "function") {
    d += performance.now(); //use high-precision timer if available
  }
  var uuid = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(
    c,
  ) {
    var r = (d + Math.random() * 16) % 16 | 0;
    d = Math.floor(d / 16);
    return (c == "x" ? r : (r & 0x3) | 0x8).toString(16);
  });
  return uuid;
}
