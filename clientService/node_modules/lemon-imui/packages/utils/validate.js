export function isPlainObject(obj) {
  return Object.prototype.toString.call(obj) === "[object Object]";
}
export function isString(str) {
  return typeof str == "string";
}
export function isToday(time) {
  return new Date().getTime() - time < 86400000;
}
export function isEmpty(obj) {
  if (!obj) return true;
  if (Array.isArray(obj) && obj.length == 0) return true;
  if (isPlainObject(obj) && Object.values(obj).length == 0) return true;
  return false;
}
export function isUrl(str) {
  const reg =
    "^((https|http|ftp|rtsp|mms)?://)" +
    "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" + //ftp的user@
    "(([0-9]{1,3}.){3}[0-9]{1,3}" + // IP形式的URL- 199.194.52.184
    "|" + // 允许IP和DOMAIN（域名）
    "([0-9a-z_!~*'()-]+.)*" + // 域名- www.
    "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]." + // 二级域名
    "[a-z]{2,6})" + // first level domain- .com or .museum
    "(:[0-9]{1,4})?" + // 端口- :80
    "((/?)|" + // 如果没有文件名，则不需要斜杠
    "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";
  return new RegExp(reg).test(str) ? true : false;
}

export function isFunction(val) {
  return val && typeof val === "function";
}

export function isEng(val) {
  return /^[A-Za-z]+$/.test(val);
}
