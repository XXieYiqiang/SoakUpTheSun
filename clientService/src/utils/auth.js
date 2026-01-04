
const TokenKey = 'token'
const TokenTypeKey = 'Token-Type'
const FreshTokenKey = 'Fresh-Token'
const Userinfo = 'userinfo'
const BaseUrl = 'baseUrl'
const ConnectedBluetooth = 'connectedBluetooth'


export function getToken () {
  return localStorage.getItem(TokenKey)
}
export function setToken (token) {
  return localStorage.setItem(TokenKey, token) 
}

export function setTokenType (tokenType) {
  return localStorage.setItem(TokenTypeKey, tokenType)
}

export function removeTokenTypeKey () {
  return localStorage.removeItem(TokenTypeKey)
}

export function getTokenType () {
  return localStorage.getItem(TokenTypeKey) || 'Bearer'
}

export function removeToken () {
  return localStorage.removeItem(TokenKey)
}

export function setFreshToken (freshToken) {
  return localStorage.setItem(FreshTokenKey, freshToken)
}

export function getFreshToken () {
  return localStorage.getItem(FreshTokenKey)
}

export function removeFreshToken () {
  return localStorage.removeItem(FreshTokenKey)
}

export function getUserinfo () {
  return localStorage.getItem(Userinfo)
}

export function setUserinfo (userinfo) {
  return localStorage.setItem(Userinfo, userinfo)
}

export function removeUserinfo () {
  return localStorage.removeItem(Userinfo)
}

export function getBaseUrl () {
  return localStorage.getItem(BaseUrl) || 'https://172.30.1.203:8080'
}

export function setBaseUrl (baseUrl) {
  return localStorage.setItem(BaseUrl, baseUrl)
}

export function removeBaseUrl () {
  return localStorage.removeItem(BaseUrl)
}

export function getConnectedBluetooth () {
  return localStorage.getItem(ConnectedBluetooth)
}

export function setConnectedBluetooth (connectedBluetooth) {
  return localStorage.setItem(ConnectedBluetooth, connectedBluetooth)
}

export function removeConnectedBluetooth () {
  return localStorage.removeItem(ConnectedBluetooth)
}