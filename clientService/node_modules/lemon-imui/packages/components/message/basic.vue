<script>
import { useScopedSlot } from "utils";
export default {
  name: "lemonMessageBasic",
  inject: {
    IMUI: {
      from: "IMUI",
      default() {
        return this;
      },
    },
  },
  props: {
    contextmenu: Array,
    message: {
      type: Object,
      default: () => {
        return {};
      },
    },
    timeFormat: {
      type: Function,
      default: () => "",
    },
    reverse: Boolean,
    hideName: Boolean,
    hideTime: Boolean,
  },
  data() {
    return {};
  },
  render() {
    const { fromUser, status, sendTime } = this.message;
    const hideTitle = this.hideName == true && this.hideTime == true;
    return (
      <div
        class={[
          "lemon-message",
          `lemon-message--status-${status}`,
          {
            "lemon-message--reverse": this.reverse,
            "lemon-message--hide-title": hideTitle,
          },
        ]}
      >
        <div class="lemon-message__avatar">
          <lemon-avatar
            size={36}
            shape="square"
            src={fromUser.avatar}
            on-click={e => {
              this._emitClick(e, "avatar");
            }}
          />
        </div>
        <div class="lemon-message__inner">
          <div class="lemon-message__title">
            {this.hideName == false && (
              <span
                on-click={e => {
                  this._emitClick(e, "displayName");
                }}
              >
                {fromUser.displayName}
              </span>
            )}
            {this.hideTime == false && (
              <span
                class="lemon-message__time"
                on-click={e => {
                  this._emitClick(e, "sendTime");
                }}
              >
                {this.timeFormat(sendTime)}
              </span>
            )}
          </div>
          <div class="lemon-message__content-flex">
            <div
              v-lemon-contextmenu_message={this.IMUI.contextmenu}
              class="lemon-message__content"
              on-click={e => {
                this._emitClick(e, "content");
              }}
            >
              {useScopedSlot(this.$scopedSlots["content"], null, this.message)}
            </div>
            <div class="lemon-message__content-after">
              {useScopedSlot(
                this.IMUI.$scopedSlots["message-after"],
                null,
                this.message,
              )}
            </div>
            <div
              class="lemon-message__status"
              on-click={e => {
                this._emitClick(e, "status");
              }}
            >
              <i class="lemon-icon-loading lemonani-spin" />
              <i
                class="lemon-icon-prompt"
                title="重发消息"
                style={{
                  color: "#ff2525",
                  cursor: "pointer",
                }}
              />
            </div>
          </div>
        </div>
      </div>
    );
  },
  created() {},
  mounted() {},
  computed: {},
  watch: {},
  methods: {
    _emitClick(e, key) {
      this.IMUI.$emit("message-click", e, key, this.message, this.IMUI);
    },
  },
};
</script>
<style lang="stylus">
@import '~styles/utils/index'
arrow()
  content ' '
  position absolute
  top 6px
  width 0
  height 0
  border 4px solid transparent
+b(lemon-message)
  display flex
  padding 10px 0
  +e(time)
    color #b9b9b9
    padding 0 5px
  +e(inner)
    position relative
  +e(avatar)
    padding-right 10px
    user-select none
    .lemon-avatar
      cursor pointer
  +e(title)
    display flex
    font-size 12px
    line-height 16px
    height 16px
    padding-bottom 4px
    user-select none
    color #666
  +e(content-flex)
    display flex
  +e(content)
    font-size 14px
    line-height 20px
    padding 8px 10px
    background #fff
    border-radius 4px
    position relative
    margin 0
    img
    video
      background #e9e9e9
      height 100px
    &:before
      arrow()
      left -4px
      border-left none
      border-right-color #fff
  +e(content-after)
    display block
    width 48px
    height 36px
    padding-left 6px
    flex none
    font-size 12px
    color #aaa
    overflow hidden
    visibility hidden
  +e(status)
    position absolute
    top 23px
    right 20px
    color #aaa
    font-size 20px
    .lemon-icon-loading
    .lemon-icon-prompt
      display none
  +m(status-going)
    .lemon-icon-loading
      display inline-block
  +m(status-failed)
    .lemon-icon-prompt
      display inline-block
  +m(status-succeed)
    +e(content-after)
      visibility visible
  +m(reverse)
    flex-direction row-reverse
    +e(content-flex)
      flex-direction row-reverse
    +e(content-after)
      padding-right 6px
      padding-left 0
      text-align right
    +e(title)
      flex-direction row-reverse
    +e(status)
      left 26px
      right auto
    +e(content)
      background #35d863
      &:before
        arrow()
        left auto
        right -4px
        border-right none
        border-left-color #35d863
    +e(title)
      text-align right
    +e(avatar)
      padding-right 0
      padding-left 10px
  +m(hide-title)
    +e(avatar)
      padding-top 10px
    +e(status)
      top 14px
    +e(content)
      position relative
      top -10px
      &:before
        top 14px
</style>
