import type * as THREE from 'three'

export interface LatLng {
  lat: number
  lng: number
}

export interface Position3D {
  x: number
  y: number
  z: number
}

export interface EarthGroupResult {
  group: THREE.Group
  earth: THREE.Mesh<THREE.SphereGeometry, THREE.MeshStandardMaterial>
  material: THREE.MeshStandardMaterial
  radius: number
}

export interface GlowMeshResult {
  glowMesh: THREE.Mesh<THREE.SphereGeometry, THREE.ShaderMaterial>
  glowMaterial: THREE.ShaderMaterial
  update: () => void
}

export interface SpinOption {
  label: string
  value: string
}
