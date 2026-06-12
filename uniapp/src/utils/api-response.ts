export function isApiSuccessCode(code: unknown): boolean {
  return code === 200 || code === 0
}
