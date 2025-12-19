export default class MemoryCache {
  constructor() {
    this.table = {};
  }
  get(key) {
    return key ? this.table[key] : this.table;
  }
  set(key, val) {
    this.table[key] = val;
  }
  // setOnly(key, val) {
  //   if (!this.has(key)) this.set(key, val);
  // }
  remove(key) {
    if (key) {
      delete this.table[key];
    } else {
      this.table = {};
    }
  }
  has(key) {
    return !!this.table[key];
  }
}
