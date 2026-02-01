import type { IUserInfoRes } from '@/api/types/login'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getUserInfo,
} from '@/api/login'

// 初始化状态
const userInfoState: IUserInfoRes = {
  id: -1,
  userAccount: '',
  userName: '',
  userAvatar: '/static/images/default-avatar.png',
  sex: 0,
  birthday: '',
  location: '',
  userProfile: '',
  userRole: '',
  editTime: '',
  createTime: '',
  updateTime: '',
}

export const useUserStore = defineStore(
  'user',
  () => {
    // 定义用户信息
    const userInfo = ref<IUserInfoRes>({ ...userInfoState })
    // 设置用户信息
    const setUserInfo = (val: IUserInfoRes) => {
      console.log('设置用户信息', val)
      // 若头像为空 则使用默认头像
      if (!val.userAvatar) {
        val.userAvatar = userInfoState.userAvatar
      }
      userInfo.value = val
    }
    const setUserAvatar = (userAvatar: string) => {
      userInfo.value.userAvatar = userAvatar
      console.log('设置用户头像', userAvatar)
      console.log('userInfo', userInfo.value)
    }
    // 删除用户信息
    const clearUserInfo = () => {
      userInfo.value = { ...userInfoState }
      uni.removeStorageSync('user')
    }

    /**
     * 获取用户信息
     */
    const fetchUserInfo = async () => {
      const saved = uni.getStorageSync('login_credentials')
      const username = saved?.phone || saved?.userAccount || userInfo.value.userAccount
      if (!username) {
        return userInfo.value
      }
      const res = await getUserInfo(username)
      setUserInfo(res)
      return res
    }

    return {
      userInfo,
      clearUserInfo,
      fetchUserInfo,
      setUserInfo,
      setUserAvatar,
    }
  },
  {
    persist: true,
  },
)
