export interface LatLng {
  lat: number
  lng: number
}

export interface Position3D {
  x: number
  y: number
  z: number
}

/** Three.js 对象在移动端仅 H5 使用，类型以 any 规避 TS 4.9 与 three 声明不兼容 */
export interface EarthGroupResult {
  group: any
  earth: any
  material: any
  radius: number
}

export interface GlowMeshResult {
  glowMesh: any
  glowMaterial: any
  update: () => void
}
