import service from '@/utils/request'

/**
 * 处理用户注册
 */
export const handleRgister = (data) => {
  return service({
    method: 'post',
    url: '/apis/api/user/register',
    data
  })
}

/**
 * 处理用户登录
 */
export const handleLogin = (data) => {
  return service({
    method: 'post',
    url: '/apis/api/user/login',
    data
  })
}

/**
 * 处理用户登出
 */
export const handleLogout = (params) => {
  return service({
    method: 'put',
    url: '/apis/api/user/logout',
    headers: {
      'token': params 
    }
  })
}

/**
 * 获取图片验证码
 */
export const getPicturCode = (params) => {
  return service({
    method: 'get',
    url: '/api/commonApi/captcha',
    params: params
  })
}