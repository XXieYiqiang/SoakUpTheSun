<script>
export default {
  name: "lemonMessageImage",
  inheritAttrs: false,
  render() {
    return (
      <lemon-message-basic
        class="lemon-message-image"
        props={{ ...this.$attrs }}
        scopedSlots={{
          content: props => <img src={props.content} />
        }}
      />
    );
  }
};
</script>
<style lang="stylus">
@import '~styles/utils/index'
+b(lemon-message-image)
  +b(lemon-message)
    +e(content)
      padding 0
      cursor pointer
      overflow hidden
      img
        max-width 100%
        min-width 100px
        display block
</style>
