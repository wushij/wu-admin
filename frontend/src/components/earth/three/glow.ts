import * as THREE from 'three'
import { fresnelVertexShader, fresnelFragmentShader } from './shaders'
import type { GlowMeshResult } from './types'

/** 近地大气散射色（偏蓝、低饱和，非纯白高光） */
export const ATMOSPHERE_GLOW_COLOR = 0x4a9fd4

export interface GlowCreateOptions {
  opacityScale?: number
  rimPower?: number
}

export function createGlowMesh(
  radius: number,
  camera: THREE.PerspectiveCamera,
  color: THREE.ColorRepresentation = ATMOSPHERE_GLOW_COLOR,
  options: GlowCreateOptions = {},
): GlowMeshResult {
  const opacityScale = options.opacityScale ?? 0.32
  const rimPower = options.rimPower ?? 8.5
  // 贴近球面，模拟极薄大气层而非外层光晕壳
  const glowGeometry = new THREE.SphereGeometry(radius * 1.012, 128, 128)

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
