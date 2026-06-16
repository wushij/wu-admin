/** 滑块验证通过后提交给后端的参数 */
export interface SliderVerifyPayload {
  token: string
  offsetX: number
}

export function sliderVerifyToRequest(payload: SliderVerifyPayload): { uuid: string; code: string } {
  return { uuid: payload.token, code: String(payload.offsetX) }
}
