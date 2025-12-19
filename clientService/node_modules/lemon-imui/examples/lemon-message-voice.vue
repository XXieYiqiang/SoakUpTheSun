<script>
export default {
  name: "lemonMessageVoice",
  inheritAttrs: false,
  inject: ["IMUI"],
  render() {
    return (
      <lemon-message-basic
        class="lemon-message-voice"
        props={{ ...this.$attrs }}
        scopedSlots={{
          content: props => {
            return <span>{props.content}&nbsp;🔈</span>;
          }
        }}
      />
    );
  }
};
</script>
<style lang="stylus">
.lemon-message.lemon-message-voice
  user-select none
  .lemon-message__content
    border 2px solid #000
    font-size 12px
    cursor pointer
    &::before
      display none
</style>
