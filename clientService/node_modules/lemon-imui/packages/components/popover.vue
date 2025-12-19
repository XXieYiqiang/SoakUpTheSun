<script>
const popoverCloseQueue = [];
import contextmenu from "../directives/contextmenu";
const triggerEvents = {
  hover(el) {},
  focus(el) {
    el.addEventListener("focus", e => {
      this.changeVisible();
    });
    el.addEventListener("blur", e => {
      this.changeVisible();
    });
  },
  click(el) {
    el.addEventListener("click", e => {
      e.stopPropagation();
      contextmenu.hide();
      this.changeVisible();
    });
  },
  contextmenu(el) {
    el.addEventListener("contextmenu", e => {
      e.preventDefault();
      this.changeVisible();
    });
  }
};
export default {
  name: "LemonPopover",
  props: {
    trigger: {
      type: String,
      default: "click",
      validator(val) {
        return Object.keys(triggerEvents).includes(val);
      }
    }
  },
  data() {
    return {
      popoverStyle: {},
      visible: false
    };
  },
  created() {
    document.addEventListener("click", this._documentClickEvent);
    popoverCloseQueue.push(this.close);
  },
  mounted() {
    triggerEvents[this.trigger].call(this, this.$slots.default[0].elm);
  },
  render() {
    return (
      <span style="position:relative">
        <transition name="lemon-slide-top">
          {this.visible && (
            <div
              class="lemon-popover"
              ref="popover"
              style={this.popoverStyle}
              on-click={e => e.stopPropagation()}
            >
              <div class="lemon-popover__content">{this.$slots.content}</div>
              <div class="lemon-popover__arrow" />
            </div>
          )}
        </transition>
        {this.$slots.default}
      </span>
    );
  },
  destroyed() {
    document.removeEventListener("click", this._documentClickEvent);
  },
  computed: {},
  watch: {
    async visible(val) {
      if (val) {
        await this.$nextTick();
        const defaultEl = this.$slots.default[0].elm;
        const contentEl = this.$refs.popover;

        this.popoverStyle = {
          top: `-${contentEl.offsetHeight + 10}px`,
          left: `${defaultEl.offsetWidth / 2 - contentEl.offsetWidth / 2}px`
        };
      }
    }
  },
  methods: {
    _documentClickEvent(e) {
      e.stopPropagation();
      if (this.visible) this.close();
    },
    changeVisible() {
      this.visible ? this.close() : this.open();
    },
    open() {
      this.closeAll();
      this.visible = true;
    },
    closeAll() {
      popoverCloseQueue.forEach(callback => callback());
    },
    close() {
      this.visible = false;
    }
  }
};
</script>
<style lang="stylus">
@import '~styles/utils/index'
+b(lemon-popover)
  border 1px solid #eee
  border-radius 4px
  font-size 14px
  font-variant tabular-nums
  line-height 1.5
  color rgba(0, 0, 0, 0.65)
  z-index 10
  background-color #fff
  border-radius 4px
  box-shadow 0 2px 8px rgba(0, 0, 0, 0.08)
  position absolute
  transform-origin 50% 150%
  +e(content)
    padding 15px
    box-sizing border-box
    position relative
    z-index 1
  +e(arrow)
    left 50%
    transform translateX(-50%) rotate(45deg)
    position absolute
    z-index 0
    bottom -4px
    box-shadow 3px 3px 7px rgba(0, 0, 0, 0.07)
    width 8px
    height 8px
    background #fff
.lemon-slide-top-leave-active ,.lemon-slide-top-enter-active
  transition all .2s cubic-bezier(0.645, 0.045, 0.355, 1)
.lemon-slide-top-enter, .lemon-slide-top-leave-to
  transform translateY(-10px) scale(.8)
  opacity 0
</style>
