<script>
export default {
  name: "LemonButton",
  props: {
    color: {
      type: String,
      default: "default"
    },
    disabled: Boolean
  },
  render() {
    return (
      <button
        class={["lemon-button", `lemon-button--color-${this.color}`]}
        disabled={this.disabled}
        type="button"
        on-click={this._handleClick}
      >
        {this.$slots.default}
      </button>
    );
  },
  methods: {
    _handleClick(e) {
      this.$emit("click", e);
    }
  }
};
</script>
<style lang="stylus">
@import '~styles/utils/index'
+b(lemon-button)
  outline none
  line-height 1.499
  display inline-block
  font-weight 400
  text-align center
  touch-action manipulation
  cursor pointer
  background-image none
  border 1px solid #ddd
  box-sizing border-box
  white-space nowrap
  padding 0 15px
  font-size 14px
  border-radius 4px
  height 32px
  user-select none
  transition all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1)
  color rgba(0, 0, 0, 0.65)
  background-color #fff
  box-shadow 0 2px 0 rgba(0, 0, 0, 0.015)
  text-shadow 0 -1px 0 rgba(0, 0, 0, 0.12)
  +m(color-default)
    &:hover:not([disabled])
      border-color #666
      color #333
    &:active
      background-color #ddd
    &[disabled]
      cursor not-allowed
      color #aaa
      background #eee
  +m(color-grey)
    background #e1e1e1
    border-color #e1e1e1
    color #666
    &:hover:not([disabled])
      border-color #bbb
</style>
