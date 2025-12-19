<script>
export default {
  name: "LemonAvatar",
  inject: ["IMUI"],
  props: {
    src: String,
    icon: {
      type: String,
      default: "lemon-icon-people",
    },
    circle: {
      type: Boolean,
      default() {
        return this.IMUI ? this.IMUI.avatarCricle : false;
      },
    },
    size: {
      type: Number,
      default: 32,
    },
  },
  data() {
    return {
      imageFinishLoad: true,
    };
  },
  render() {
    return (
      <span
        style={this.style}
        class={["lemon-avatar", { "lemon-avatar--circle": this.circle }]}
        on-click={e => this.$emit("click", e)}
      >
        {(this.imageFinishLoad || !this.src) && <i class={this.icon} />}
        <img src={this.src} onLoad={this._handleLoad} />
      </span>
    );
  },
  computed: {
    style() {
      const size = `${this.size}px`;
      return {
        width: size,
        height: size,
        lineHeight: size,
        fontSize: `${this.size / 2}px`,
      };
    },
  },
  methods: {
    _handleLoad() {
      this.imageFinishLoad = false;
    },
  },
};
</script>
<style lang="stylus">
@import '~styles/utils/index'
+b(lemon-avatar)
  font-variant tabular-nums
  line-height 1.5
  box-sizing border-box
  margin 0
  padding 0
  list-style none
  display inline-block
  text-align center
  background #ccc
  color rgba(255,255,255,0.7)
  white-space nowrap
  position relative
  overflow hidden
  vertical-align middle
  border-radius 4px
  +m(circle)
    border-radius 50%
  img
    width 100%
    height 100%
    display block
</style>
