import * as THREE from 'three'
import type { EarthGroupResult } from './types'

const TEXTURE_BASE = '/static/earth/'

export interface EarthCreateOptions {
  vivid?: boolean
  /** 移动端降低细分以节省 GPU */
  segments?: number
}

export function createEarthGroup(radius = 5, options: EarthCreateOptions = {}): EarthGroupResult {
  const vivid = options.vivid ?? false
  const segments = options.segments ?? 96
  const group = new THREE.Group()
  const loader = new THREE.TextureLoader()

  const geometry = new THREE.SphereGeometry(radius, segments, segments)

  const earthTexture = loader.load(`${TEXTURE_BASE}earth_atmos_2048.jpg`)
  const normalTexture = loader.load(`${TEXTURE_BASE}earth_normal_2048.jpg`)
  const lightsTexture = loader.load(`${TEXTURE_BASE}earth_lights_2048.png`)

  earthTexture.colorSpace = THREE.SRGBColorSpace
  lightsTexture.colorSpace = THREE.SRGBColorSpace

  const material = new THREE.MeshStandardMaterial({
    map: earthTexture,
    normalMap: normalTexture,
    emissiveMap: lightsTexture,
    emissive: new THREE.Color(vivid ? 0x66aaff : 0x4488ff),
    emissiveIntensity: vivid ? 0.95 : 0.55,
    color: new THREE.Color(vivid ? 0x303848 : 0x1a1a28),
    roughness: vivid ? 0.78 : 0.85,
    metalness: 0.12,
  })

  const earth = new THREE.Mesh(geometry, material)
  group.add(earth)

  return { group, earth, material, radius }
}
