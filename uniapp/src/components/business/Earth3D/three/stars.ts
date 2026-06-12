import * as THREE from 'three'

export function createStars(count = 12000, spread = 2200) {
  const geometry = new THREE.BufferGeometry()
  const positions = new Float32Array(count * 3)

  for (let i = 0; i < count; i++) {
    positions[i * 3] = (Math.random() - 0.5) * spread
    positions[i * 3 + 1] = (Math.random() - 0.5) * spread
    positions[i * 3 + 2] = (Math.random() - 0.5) * spread
  }

  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))

  const material = new THREE.PointsMaterial({
    color: 0xffffff,
    size: 0.65,
    sizeAttenuation: true,
    transparent: true,
    opacity: 0.85,
  })

  return new THREE.Points(geometry, material)
}
