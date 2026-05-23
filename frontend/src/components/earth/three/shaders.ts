/** Fresnel 边缘辉光 — 中心透明、边缘发光 */
export const fresnelVertexShader = /* glsl */ `
  uniform vec3 viewVector;
  uniform float c;
  uniform float p;
  varying float intensity;

  void main() {
    vec3 vNormal = normalize(normalMatrix * normal);
    vec3 vNormel = normalize(normalMatrix * viewVector);
    intensity = pow(c - dot(vNormal, vNormel), p);
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
  }
`

/** 大气边缘辉光 — 模拟瑞利散射的薄层淡蓝轮廓 */
export const fresnelFragmentShader = /* glsl */ `
  uniform vec3 glowColor;
  uniform float opacityScale;
  varying float intensity;

  void main() {
    float edge = clamp(intensity, 0.0, 1.0);
    edge = pow(edge, 1.8);
    vec3 glow = glowColor * edge;
    gl_FragColor = vec4(glow, edge * opacityScale);
  }
`
