<script>
import { hoursTimeFormat } from "utils";
import { isString } from "utils/validate";
import contextmenu from "../directives/contextmenu";
export default {
  name: "LemonMessages",
  components: {},
  props: {
    //是否隐藏消息发送人昵称
    hideName: Boolean,
    //是否隐藏显示消息时间
    hideTime: Boolean,
    reverseUserId: [String, Number],
    timeRange: {
      type: Number,
      default: 1,
    },
    timeFormat: {
      type: Function,
      default(val) {
        return hoursTimeFormat(val);
      },
    },
    loadingText: {
      type: [String, Function],
    },
    loadendText: {
      type: [String, Function],
      default: "暂无更多消息",
    },
    messages: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    this._lockScroll = false;
    return {
      _loading: false,
      _loadend: false,
    };
  },
  render() {
    return (
      <div class="lemon-messages" ref="wrap" on-scroll={this._handleScroll}>
        <div
          class={[
            "lemon-messages__load",
            `lemon-messages__load--${this._loadend ? "end" : "ing"}`,
          ]}
        >
          <span class="lemon-messages__loadend">
            {isString(this.loadendText) ? this.loadendText : this.loadendText()}
          </span>
          <span class="lemon-messages__loading">
            {this.loadingText ? (
              isString(this.loadingText) ? (
                this.loadingText
              ) : (
                this.loadingText()
              )
            ) : (
              <i class="lemon-icon-loading lemonani-spin" />
            )}
          </span>
        </div>
        {this.messages.map((message, index) => {
          const node = [];
          const tagName = `lemon-message-${message.type}`;
          const prev = this.messages[index - 1];
          if (
            prev &&
            this.msecRange &&
            message.sendTime - prev.sendTime > this.msecRange
          ) {
            node.push(
              <lemon-message-event
                attrs={{
                  message: {
                    id: "__time__",
                    type: "event",
                    content: hoursTimeFormat(message.sendTime),
                  },
                }}
              />,
            );
          }

          let attrs;
          if (message.type == "event") {
            attrs = { message: message };
          } else {
            attrs = {
              timeFormat: this.timeFormat,
              message: message,
              reverse: this.reverseUserId == message.fromUser.id,
              hideTime: this.hideTime,
              hideName: this.hideName,
            };
          }
          node.push(<tagName ref="message" refInFor={true} attrs={attrs} />);
          return node;
        })}
      </div>
    );
  },
  computed: {
    msecRange() {
      return this.timeRange * 1000 * 60;
    },
  },
  watch: {},
  methods: {
    loaded() {
      this._loadend = true;
      this.$forceUpdate();
    },
    resetLoadState() {
      this._lockScroll = true;
      this._loading = false;
      this._loadend = false;
      setTimeout(() => {
        this._lockScroll = false;
      }, 200);
    },
    async _handleScroll(e) {
      if (this._lockScroll) return;
      const { target } = e;
      contextmenu.hide();
      if (
        target.scrollTop == 0 &&
        this._loading == false &&
        this._loadend == false
      ) {
        this._loading = true;
        await this.$nextTick();
        const hst = target.scrollHeight;

        this.$emit("reach-top", async isEnd => {
          await this.$nextTick();
          target.scrollTop = target.scrollHeight - hst;
          this._loading = false;
          this._loadend = !!isEnd;
        });
      }
    },
    async scrollToBottom() {
      await this.$nextTick();
      const { wrap } = this.$refs;
      if (wrap) {
        wrap.scrollTop = wrap.scrollHeight;
      }
    },
  },
  created() {},
  mounted() {},
};
</script>
<style lang="stylus">
@import '~styles/utils/index'
+b(lemon-messages)
  height 400px
  overflow-x hidden
  overflow-y auto
  scrollbar-light()
  padding 10px 15px
  +e(time)
    text-align center
    font-size 12px
  +e(load)
    user-select none
    font-size 12px
    text-align center
    color #999
    line-height 30px
    .lemon-messages__loading
    .lemon-messages__loadend
      display none
    +m(ing)
      .lemon-icon-loading
        font-size 22px
      .lemon-messages__loading
        display block
    +m(end)
      .lemon-messages__loadend
        display block
</style>
