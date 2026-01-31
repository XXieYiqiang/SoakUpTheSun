import type { IAuthLoginRes, ICaptcha, IDoubleTokenRes, IUpdateInfo, IUpdatePassword, IUserInfoRes } from './types/login'
import { http } from '@/http/http'


/**
 * 注册表单
 */
export interface IRegisterForm {
  userAccount: string // 账号
  userPassword: string // 密码
  userName: string // 用户名
  userAvatar: string // 头像 base64
  userProfile: string // 简介
}

/**
 * 用户注册
 * @param data 注册表单
 */
export function register(data: IRegisterForm) {
  return http.post<number>('/user/register', data)
}

/**
 * 获取验证码
 * @returns ICaptcha 验证码
 */
export function getCode() {
  return http.get<ICaptcha>('/user/getCode')
}

/**
 * 用户登录表单
 */
export interface IUserLoginForm {
  userAccount: string
  password: string
}

/**
 * 用户登录
 * @param loginForm 登录表单
 */
export function login(loginForm: any) {
  return http.post<IAuthLoginRes>('/auth/login', loginForm)
}

/**
 * 用户登录
 * @param data 登录表单
 */
export function userLogin(data: IUserLoginForm) {
  return http.post<IAuthLoginRes>('/user/login', data)
}

/**
 * 刷新token
 * @param refreshToken 刷新token
 */
export function refreshToken(refreshToken: string) {
  return http.post<IDoubleTokenRes>('/auth/refreshToken', { refreshToken })
}

/**
 * 获取用户信息
 */
export function getUserInfo(username: string) {
  return http.get<IUserInfoRes>('/user/' + username)
}

/**
 * 退出登录
 */
export function logout() {
  return http.put<void>('/user/logout')
}

/**
 * 修改用户信息
 */
export function updateInfo(data: IUpdateInfo) {
  return http.post('/user/updateInfo', data)
}

/**
 * 修改用户密码
 */
export function updateUserPassword(data: IUpdatePassword) {
  return http.post('/user/updatePassword', data)
}

/**
 * 获取微信登录凭证
 * @returns Promise 包含微信登录凭证(code)
 */
export function getWxCode() {
  return new Promise<UniApp.LoginRes>((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: res => resolve(res),
      fail: err => reject(new Error(err)),
    })
  })
}

/**
 * 微信登录
 * @param params 微信登录参数，包含code
 * @returns Promise 包含登录结果
 */
export function wxLogin(data: { code: string }) {
  return http.post<IAuthLoginRes>('/auth/wxLogin', data)
}

/**
 * 获取百度语音识别 Access Token
 */
export function getBaiduToken(params: {
  grant_type: string
  client_id: string
  client_secret: string
}) {
  return uni.request({
    url: '/audio/oauth/2.0/token',
    method: 'POST',
    data: params,
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
}

/**
 * 百度语音识别
 */
export function recognizeSpeech(data: {
  format: string
  rate: number
  channel: number
  cuid: string
  token: string
  speech: string
  len: number
}) {
  return uni.request({
    url: '/audio/server_api',
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
    },
    data,
  })
}
