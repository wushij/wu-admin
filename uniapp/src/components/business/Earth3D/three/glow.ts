import * as THREE from 'three'
import { fresnelVertexShader, fresnelFragmentShader } from './shaders'
import type { GlowMeshResult } from './types'

export const ATMOSPHERE_GLOW_COLOR = 0x4a9fd4

export interface GlowCreateOptions {
  opacityScale?: number
  rimPower?: number
  segments?: number
}

export function createGlowMesh(
  radius: number,
  camera: any,
  color: any = ATMOSPHERE_GLOW_COLOR,
  options: GlowCreateOptions = {},
): GlowMeshResult {
  const opacityScale = options.opacityScale ?? 0.32
  const rimPower = options.rimPower ?? 8.5
  const segments = options.segments ?? 96
  const glowGeometry = new THREE.SphereGeometry(radius * 1.012, segments, segments)

  const glowMaterial = new THREE.ShaderMaterial({
    uniforms: {
      c: { value: 0.28 },
      p: { value: rimPower },
      glowColor: { value: new THREE.Color(color) },
      viewVector: { value: new THREE.Vector3() },
      opacityScale: { value: opacityScale },
    },
    vertexShader: fresnelVertexShader,
    fragmentShader: fresnelFragmentShader,
    side: THREE.BackSide,
    blending: THREE.AdditiveBlending,
    transparent: true,
    depthWrite: false,
  })

  const glowMesh = new THREE.Mesh(glowGeometry, glowMaterial)
  const worldPos = new THREE.Vector3()

  const update = () => {
    glowMaterial.uniforms.viewVector.value
      .copy(camera.position)
      .sub(glowMesh.getWorldPosition(worldPos))
  }

  update()

  return { glowMesh, glowMaterial, update }
}
