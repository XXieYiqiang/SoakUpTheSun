const path = require("path");
function resolve(dir) {
  return path.join(__dirname, "", dir);
}
module.exports = {
  pages: {
    index: {
      entry: "examples/main.js",
      template: "public/index.html",
      filename: "index.html"
    }
  },
  publicPath:'',
  productionSourceMap:false,
  configureWebpack: {
    resolve: {
      alias: {
        components: resolve("packages/components"),
        mixins: resolve("packages/mixins"),
        styles: resolve("packages/styles"),
        utils: resolve("packages/utils")
      }
    }
  }
};
