/** 滑块拖动轨迹采样点（服务端逻辑坐标，t 为相对开始时间毫秒） */
export interface SliderTrackEvent {
  x: number
  y: number
  t: number
}

/** 滑块验证通过后提交给后端的参数 */
export interface SliderVerifyPayload {
  token: string
  offsetX: number
}

export function sliderVerifyToRequest(payload: SliderVerifyPayload): { uuid: string; code: string } {
  return { uuid: payload.token, code: String(payload.offsetX) }
}
