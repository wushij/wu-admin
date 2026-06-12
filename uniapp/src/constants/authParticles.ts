/** 登录页粒子层配置（数值对齐 PC tsparticles authParticles） */

export interface ParticleLayerConfig {
  color: string
  count: number
  /** 对应 PC move.speed */
  moveSpeed: number
  sizeMin: number
  sizeMax: number
  opacityMin: number
  opacityMax: number
  /** 对应 PC opacity.animation.speed */
  opacitySpeed: number
}

/** PC 三层：60 / 80 / 100，move 0.5 / 0.8 / 1.2 */
export const authParticleLayers: ParticleLayerConfig[] = [
  {
    color: '#6366f1',
    count: 60,
    moveSpeed: 0.5,
    sizeMin: 0.6,
    sizeMax: 1.5,
    opacityMin: 0.1,
    opacityMax: 1,
    opacitySpeed: 0.5,
  },
  {
    color: '#a855f7',
    count: 80,
    moveSpeed: 0.8,
    sizeMin: 0.4,
    sizeMax: 1,
    opacityMin: 0.1,
    opacityMax: 1,
    opacitySpeed: 0.8,
  },
  {
    color: '#ffffff',
    count: 100,
    moveSpeed: 1.2,
    sizeMin: 0.2,
    sizeMax: 0.6,
    opacityMin: 0.1,
    opacityMax: 1,
    opacitySpeed: 1.2,
  },
]

/** PC tsparticles 速度换算到 canvas 的缩放（经验值，偏慢、偏飘） */
export const PARTICLE_MOVE_SCALE = 0.22

export const PARTICLE_DENSITY_BOX = { width: 400, height: 400 }
