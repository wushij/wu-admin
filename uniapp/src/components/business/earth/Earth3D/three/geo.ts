import * as THREE from 'three'
import type { LatLng, Position3D } from './types'

export function latLngToVector3(lat: number, lng: number, radius: number) {
  const phi = ((90 - lat) * Math.PI) / 180
  const theta = ((lng + 180) * Math.PI) / 180

  return new THREE.Vector3(
    -radius * Math.sin(phi) * Math.cos(theta),
    radius * Math.cos(phi),
    radius * Math.sin(phi) * Math.sin(theta),
  )
}

export function latLngToPosition(lat: number, lng: number, radius: number): Position3D {
  const v = latLngToVector3(lat, lng, radius)
  return { x: v.x, y: v.y, z: v.z }
}

export type { LatLng }
