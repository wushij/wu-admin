/** 后端统一响应 code 表示成功 */
export function isApiSuccessCode(code: unknown): boolean {
  return code === 200 || code === 0
}
