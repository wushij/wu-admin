import { get } from '@/core/http'
import type { ServerInfo } from '@/types/system'

export function getServerInfo() {
  return get<ServerInfo>('/monitor/server/info')
}
