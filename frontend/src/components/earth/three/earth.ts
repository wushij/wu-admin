import * as THREE from 'three'
import type { EarthGroupResult } from './types'

const TEXTURE_BASE = 'https://threejs.org/examples/textures/planets/'

export interface EarthCreateOptions {
  /** 透明背景场景：提亮地表与夜景，弥补无 Bloom 时的对比度 */
  vivid?: boolean
}

export function createEarthGroup(radius = 5, options: EarthCreateOptions = {}): EarthGroupResult {
  const vivid = options.vivid ?? false
  const group = new THREE.Group()
  const loader = new THREE.TextureLoader()

  const geometry = new THREE.SphereGeometry(radius, 128, 128)

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
