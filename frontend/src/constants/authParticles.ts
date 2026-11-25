import type { ISourceOptions } from '@tsparticles/engine'

const baseLayer: Pick<ISourceOptions, 'background' | 'fullScreen' | 'fpsLimit' | 'detectRetina'> = {
  background: { color: { value: 'transparent' } },
  fullScreen: { enable: false },
  fpsLimit: 60,
  detectRetina: true,
}

/** 登录/注册页低密度微光星尘层（搭配集合背景图，克制飘动不抢视觉焦点） */
export const authParticleOptions1: ISourceOptions = {
  ...baseLayer,
  particles: {
    color: { value: ['#93c5fd', '#c4b5fd'] },
    move: {
      enable: true,
      speed: 0.28,
      direction: 'none',
      outModes: { default: 'out' },
    },
    number: {
      density: { enable: true, width: 640, height: 640 },
      value: 26,
    },
    opacity: {
      value: { min: 0.12, max: 0.58 },
      animation: { enable: true, speed: 0.45, startValue: 'random', sync: false },
    },
    shape: { type: 'circle' },
    size: { value: { min: 0.6, max: 1.4 } },
  },
}

export const authParticleOptions2: ISourceOptions = {
  ...baseLayer,
  particles: {
    color: { value: ['#7dd3fc', '#e2e8f0'] },
    move: {
      enable: true,
      speed: 0.38,
      direction: 'none',
      outModes: { default: 'out' },
    },
    number: {
      density: { enable: true, width: 640, height: 640 },
      value: 22,
    },
    opacity: {
      value: { min: 0.08, max: 0.46 },
      animation: { enable: true, speed: 0.6, startValue: 'random', sync: false },
    },
    shape: { type: 'circle' },
    size: { value: { min: 0.4, max: 1.0 } },
  },
}
