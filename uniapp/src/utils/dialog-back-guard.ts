import {
  dialogState,
  cancelActionSheet,
  resolveConfirm,
  consumeSuppressedPopstate,
} from '@/store/dialog'
import {
  handleGlobalBackPress,
  handleShallowStackPopstate,
  installH5ShallowStackTrapIfNeeded,
  patchNavigateBackFail,
  markNativeNavigateBack,
} from '@/utils/navigate-back'

export function isAppDialogOpen() {
  return dialogState.confirmVisible || dialogState.actionSheetVisible
}

export function closeTopAppDialog() {
  if (dialogState.actionSheetVisible) {
    cancelActionSheet()
    return true
  }
  if (dialogState.confirmVisible) {
    resolveConfirm(false)
    return true
  }
  return false
}

let h5PopStateBound = false

function bindH5PopStateGuard() {
  // #ifdef H5
  if (h5PopStateBound || typeof window === 'undefined') return
  h5PopStateBound = true

  window.addEventListener(
    'popstate',
    () => {
      if (consumeSuppressedPopstate()) return
      if (isAppDialogOpen()) {
        closeTopAppDialog()
        installH5ShallowStackTrapIfNeeded()
        return
      }
      handleShallowStackPopstate()
    },
    true,
  )
  // #endif
}

export function setupDialogBackGuard() {
  bindH5PopStateGuard()

  uni.addInterceptor('navigateBack', {
    invoke(args) {
      if (closeTopAppDialog()) return false
      if (handleGlobalBackPress()) return false
      patchNavigateBackFail(args)
      markNativeNavigateBack()
      return true
    },
  })
}
