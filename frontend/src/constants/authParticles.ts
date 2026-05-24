import type { ISourceOptions } from '@tsparticles/engine'

const baseLayer: Pick<ISourceOptions, 'background' | 'fullScreen' | 'fpsLimit' | 'detectRetina'> = {
  background: { color: { value: 'transparent' } },
  fullScreen: { enable: false },
  fpsLimit: 120,
  detectRetina: true,
}

/** 登录/注册页粒子背景三层配置 */
export const authParticleOptions1: ISourceOptions = {
  ...baseLayer,
  particles: {
    color: { value: '#6366f1' },
    move: {
      enable: true,
      speed: 0.5,
      direction: 'none',
      outModes: { default: 'out' },
    },
    number: {
      density: { enable: true, width: 400, height: 400 },
      value: 60,
    },
    opacity: {
      value: { min: 0.1, max: 1 },
      animation: { enable: true, speed: 0.5, startValue: 'random', sync: false },
    },
    shape: { type: 'circle' },
    size: { value: { min: 0.6, max: 1.5 } },
  },
}

export const authParticleOptions2: ISourceOptions = {
  ...baseLayer,
  particles: {
    color: { value: '#a855f7' },
    move: {
      enable: true,
      speed: 0.8,
      direction: 'none',
      outModes: { default: 'out' },
    },
    number: {
      density: { enable: true, width: 400, height: 400 },
      value: 80,
    },
    opacity: {
      value: { min: 0.1, max: 1 },
      animation: { enable: true, speed: 0.8, startValue: 'random', sync: false },
    },
    shape: { type: 'circle' },
    size: { value: { min: 0.4, max: 1 } },
  },
}

export const authParticleOptions3: ISourceOptions = {
  ...baseLayer,
  particles: {
    color: { value: '#ffffff' },
    move: {
      enable: true,
      speed: 1.2,
      direction: 'none',
      outModes: { default: 'out' },
    },
    number: {
      density: { enable: true, width: 400, height: 400 },
      value: 100,
    },
    opacity: {
      value: { min: 0.1, max: 1 },
      animation: { enable: true, speed: 1.2, startValue: 'random', sync: false },
    },
    shape: { type: 'circle' },
    size: { value: { min: 0.2, max: 0.6 } },
  },
}
