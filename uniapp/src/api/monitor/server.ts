import { get } from '@/utils/request'
import type { ServerInfo } from '@/types/system'

export function getServerInfo() {
  return get<ServerInfo>('/monitor/server/info')
}
