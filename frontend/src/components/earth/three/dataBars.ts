import * as THREE from 'three'
import { latLngToVector3 } from './geo'
import type { LatLng } from './types'

type RegionSampler = () => LatLng

/** 模拟大陆区域的经纬度采样权重 */
function randomLandLatLng(): LatLng {
  const regions: RegionSampler[] = [
    () => ({ lat: 20 + Math.random() * 40, lng: 70 + Math.random() * 60 }),
    () => ({ lat: 25 + Math.random() * 25, lng: -130 + Math.random() * 50 }),
    () => ({ lat: 35 + Math.random() * 30, lng: -15 + Math.random() * 45 }),
    () => ({ lat: -35 + Math.random() * 30, lng: 110 + Math.random() * 40 }),
    () => ({ lat: -10 + Math.random() * 25, lng: -75 + Math.random() * 35 }),
    () => ({ lat: Math.random() * 140 - 70, lng: Math.random() * 360 - 180 }),
  ]
  const pick = regions[Math.floor(Math.random() * regions.length)]!
  return pick()
}

export interface DataBarsCreateOptions {
  vivid?: boolean
}

export function createDataBars(radius: number, count = 420, options: DataBarsCreateOptions = {}): THREE.Group {
  const vivid = options.vivid ?? false
  const group = new THREE.Group()

  for (let i = 0; i < count; i++) {
    const { lat, lng } = randomLandLatLng()
    const pos = latLngToVector3(lat, lng, radius)
    const barHeight = Math.random() * 2.8 + 0.4

    const barGeometry = new THREE.BoxGeometry(0.028, 0.028, barHeight)
    const hue = 180 + Math.random() * 80
    const barMaterial = new THREE.MeshBasicMaterial({
      color: new THREE.Color(`hsl(${hue}, 95%, ${vivid ? 64 : 58}%)`),
      transparent: true,
      opacity: vivid ? 1 : 0.92,
    })

    const bar = new THREE.Mesh(barGeometry, barMaterial)
    bar.position.copy(pos)
    bar.lookAt(0, 0, 0)
    bar.translateZ(barHeight / 2)

    group.add(bar)
  }

  return group
}
