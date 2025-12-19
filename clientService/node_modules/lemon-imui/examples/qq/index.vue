<script>
import UserData from "../database/user";
import ContactsData from "../database/contacts";
import MessagesData from "../database/messages";
import EmojiData from "../database/emoji";
export default {
  name: "QqImui",
  components: {},
  data() {
    return {};
  },
  render() {
    return (
      <div class="qq-lemon-imui">
        <lemon-imui
          class="lemon-slot"
          user={UserData}
          width={900}
          avatar-cricle
          hide-message-name
          hide-message-time
          ref="IMUI"
          on={{
            "pull-messages": this.handlePullMessages,
            "change-contact": this.handleChangeContact,
            send: this.handleSend,
          }}
          scopedSlots={{
            "message-title": contact => {
              return (
                <div>
                  <div style="display:flex;justify-content:space-between">
                    <span>{contact.displayName}</span>
                    <span style="font-size:12px;">
                      <span>打开抽屉：</span>
                      <span
                        class="cursor:pointer;"
                        on-click={() => this.openDrawer("right")}
                      >
                        右侧 |{" "}
                      </span>
                      <span
                        class="cursor:pointer;"
                        on-click={() => this.openDrawer("rightInside")}
                      >
                        右侧内部 |{" "}
                      </span>
                      <span
                        class="cursor:pointer;"
                        on-click={() => this.openDrawer("center")}
                      >
                        居中
                      </span>
                    </span>
                  </div>
                  {contact.isGroup && (
                    <div class="slot-group-menu">
                      <span>聊天</span>
                      <span>公告</span>
                      <span>相册</span>
                      <span>文件</span>
                      <span>活动</span>
                      <span
                        v-lemon-contextmenu_click={[
                          {
                            text: "操作一",
                            click(e, instance, hide) {
                              hide();
                            },
                          },
                          {
                            text: "操作二",
                          },
                        ]}
                      >
                        设置(左键弹出菜单)
                      </span>
                    </div>
                  )}
                </div>
              );
            },
            "sidebar-contact-fixedtop": contact => {
              return (
                <div class="slot-contact-fixedtop">
                  <input class="slot-search" placeholder="搜索通讯录" />
                </div>
              );
            },
            "message-side": contact => {
              if (contact.isGroup) {
                return (
                  <div class="slot-group">
                    <div class="slot-group-title">群通知</div>
                    <div class="slot-group-notice">
                      进群请改备注，格式，工作地点-姓名，请大家配合谢谢
                    </div>
                    <div class="slot-group-title">群成员</div>
                    <div class="slot-group-panel">
                      <input class="slot-search" placeholder="搜索群成员" />
                      <div class="slot-group-member">河南-96-十里青山</div>
                      <div class="slot-group-member">河南-96-十里青山</div>
                      <div class="slot-group-member">河南-96-十里青山</div>
                      <div class="slot-group-member">河南-96-十里青山</div>
                      <div class="slot-group-member">河南-96-十里青山</div>
                      <div class="slot-group-member">河南-96-十里青山</div>
                      <div class="slot-group-member">河南-96-十里青山</div>
                      <div class="slot-group-member">河南-96-十里青山</div>
                      <div class="slot-group-member">河南-96-十里青山</div>
                      <div class="slot-group-member">河南-96-十里青山</div>
                    </div>
                  </div>
                );
              }
            },
          }}
        />
      </div>
    );
  },
  computed: {},
  watch: {},
  created() {},
  mounted() {
    const IMUI = this.$refs.IMUI;
    IMUI.initContacts(ContactsData);
    IMUI.initEmoji(EmojiData);
    IMUI.changeContact(13);
  },
  methods: {
    openDrawer(position) {
      const IMUI = this.$refs.IMUI;
      const params = {
        position,
        render: contact => {
          return (
            <div style="padding:15px">
              <h5>{contact.displayName}</h5>
              <span on-click={IMUI.closeDrawer}>关闭抽屉</span>
            </div>
          );
        },
      };
      if (position == "center") {
        params.width = "50%";
        params.height = "50%";
      } else if (position == "rightInside") {
        params.height = "90%";
        params.offsetY = "10%";
      }
      IMUI.openDrawer(params);
    },
    handlePullMessages(contact, next) {
      const { IMUI } = this.$refs;
      setTimeout(() => {
        next(MessagesData[contact.id], true);
      }, 3000);
    },
    handleChangeContact() {},
    handleSend(message, next, file) {
      console.log(message, next, file);
      setTimeout(() => {
        next();
      }, 1000);
    },
  },
};
</script>
<style lang="stylus">
.slot-group
  width 170px
  border-left 1px solid #ddd;
  height 100%
  box-sizing border-box
  padding 10px
  .slot-search
    margin 5px 0
.slot-group-notice
  color #999
  padding 6px 0
  font-size 12px
.slot-group-title
  font-size 12px
.slot-group-member
  font-size 12px
  line-height 18px
.slot-group-menu span
  display inline-block
  cursor pointer
  color #888
  margin 4px 10px 0 0
  border-bottom 2px solid transparent;
  &:hover
    color #000
    border-color #333
.slot-contact-fixedtop
  padding 10px
  border-bottom 1px solid #ddd
.slot-search
  width 100%
  box-sizing border-box
  font-size 14px
  border 1px solid #bbb
  padding 5px 10px
</style>
