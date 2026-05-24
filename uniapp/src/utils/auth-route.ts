const LOGIN_PATH = '/pages/login/index'
const LOGIN_ROUTE = 'pages/login/index'
const REGISTER_PATH = '/pages/register/index'

/** 注册页返回登录：优先回退栈内登录页，否则重启到登录（避免 H5 navigateBack 失效） */
export function navigateToLogin() {
  const pages = getCurrentPages()
  const loginIndex = pages.findIndex((p) => (p as { route?: string }).route === LOGIN_ROUTE)

  if (loginIndex >= 0 && loginIndex < pages.length - 1) {
    uni.navigateBack({
      delta: pages.length - 1 - loginIndex,
      fail: () => uni.reLaunch({ url: LOGIN_PATH }),
    })
    return
  }

  uni.reLaunch({ url: LOGIN_PATH })
}

export function navigateToRegister() {
  uni.navigateTo({ url: REGISTER_PATH })
}
