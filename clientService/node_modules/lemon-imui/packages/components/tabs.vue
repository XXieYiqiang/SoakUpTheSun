<script>
export default {
  name: "LemonTabs",
  props: {
    activeIndex: String
  },
  data() {
    return {
      active: this.activeIndex
    };
  },
  mounted() {
    if (!this.active) {
      this.active = this.$slots["tab-pane"][0].data.attrs.index;
    }
  },
  render() {
    const pane = [];
    const nav = [];
    this.$slots["tab-pane"].map(vnode => {
      const { tab, index } = vnode.data.attrs;
      pane.push(
        <div class="lemon-tabs-content__pane" v-show={this.active == index}>
          {vnode}
        </div>
      );
      nav.push(
        <div
          class={[
            "lemon-tabs-nav__item",
            this.active == index && "lemon-tabs-nav__item--active"
          ]}
          on-click={() => this._handleNavClick(index)}
        >
          {tab}
        </div>
      );
    });
    return (
      <div class="lemon-tabs">
        <div class="lemon-tabs-content">{pane}</div>
        <div class="lemon-tabs-nav">{nav}</div>
      </div>
    );
  },
  methods: {
    _handleNavClick(index) {
      this.active = index;
    }
  }
};
</script>
<style lang="stylus">
@import '~styles/utils/index'
pane-color = #f6f6f6
+b(lemon-tabs)
  background pane-color
+b(lemon-tabs-content)
  width 100%
  height 100%
  padding 15px
  +e(pane)
    //scrollbar-light()
    //overflow-y auto
    height 100%
    width 100%
+b(lemon-tabs-nav)
  display flex
  background #eee
  +e(item)
    line-height 38px
    padding 0 15px
    cursor pointer
    transition all .3s cubic-bezier(0.645, 0.045, 0.355, 1)
    +m(active)
      background pane-color
</style>
