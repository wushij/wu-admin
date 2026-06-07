import { get } from '@/utils/request'

export interface ServerCpu {
  name?: string
  arch?: string
  availableProcessors?: number
  systemLoadAverage?: number | null
  systemCpuPercent?: number | null
  processCpuPercent?: number | null
}

export interface ServerMemory {
  heapInit?: string
  heapUsed?: string
  heapMax?: string
  heapCommitted?: string
  nonHeapUsed?: string
  heapUsedBytes?: number
  heapMaxBytes?: number
  heapUsedPercent?: number
  physicalTotal?: string
  physicalFree?: string
  physicalUsedPercent?: number | null
}

export interface ServerJvm {
  name?: string
  vendor?: string
  version?: string
  specVersion?: string
  startTime?: string
  uptime?: string
  uptimeMillis?: number
}

export interface ServerSys {
  hostName?: string
  hostAddress?: string
  osName?: string
  osVersion?: string
  userDir?: string
  javaVersion?: string
}

export interface ServerDisk {
  path?: string
  total?: string
  free?: string
  used?: string
  usedPercent?: number
}

export interface ServerInfo {
  cpu?: ServerCpu
  memory?: ServerMemory
  jvm?: ServerJvm
  sys?: ServerSys
  disks?: ServerDisk[]
}

export function getServerInfo() {
  return get<ServerInfo>('/monitor/server/info')
}
