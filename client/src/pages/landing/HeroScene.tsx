import { useMemo, useRef } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import { ContactShadows, Float, RoundedBox } from '@react-three/drei'
import type { Group, Mesh } from 'three'

/* 세 업종 매장 블록 사이로 재고 상자가 오간다 (요청 → 승인 → 출고 → 입고). 글자는 넣지 않는다 */
const STORES: { position: [number, number, number]; color: string; size: [number, number, number] }[] = [
  { position: [-3.2, 0, 0.4], color: '#0071e3', size: [1.9, 1.3, 1.6] },
  { position: [0, 0, -0.6], color: '#af52de', size: [2.1, 1.7, 1.7] },
  { position: [3.2, 0, 0.4], color: '#ff9500', size: [1.8, 1.1, 1.6] },
]

const ROUTES: { from: number; to: number; color: string; offset: number; speed: number }[] = [
  { from: 1, to: 0, color: '#0a84ff', offset: 0.0, speed: 0.17 },
  { from: 2, to: 1, color: '#ff9f0a', offset: 0.42, speed: 0.15 },
  { from: 0, to: 2, color: '#bf5af2', offset: 0.7, speed: 0.13 },
]

function StoreBlock({ position, color, size }: (typeof STORES)[number]) {
  const [w, h, d] = size
  return (
    <group position={position}>
      <RoundedBox args={[w, h, d]} radius={0.22} smoothness={6} position={[0, h / 2, 0]} castShadow>
        <meshStandardMaterial color={color} roughness={0.35} metalness={0.05} />
      </RoundedBox>
      {/* 지붕 띠: 매장 간판 느낌 */}
      <RoundedBox args={[w * 0.92, 0.12, d * 0.92]} radius={0.05} smoothness={4} position={[0, h + 0.08, 0]}>
        <meshStandardMaterial color="#ffffff" roughness={0.5} />
      </RoundedBox>
    </group>
  )
}

/** 포물선을 따라 움직이는 재고 상자. 도착하면 처음으로 돌아간다 */
function ItemCube({ from, to, color, offset, speed }: (typeof ROUTES)[number]) {
  const ref = useRef<Mesh>(null)
  const a = STORES[from]
  const b = STORES[to]
  const start = useMemo(() => [a.position[0], a.size[1] + 0.45, a.position[2]] as const, [a])
  const end = useMemo(() => [b.position[0], b.size[1] + 0.45, b.position[2]] as const, [b])

  useFrame(({ clock }) => {
    const mesh = ref.current
    if (!mesh) return
    const t = (clock.elapsedTime * speed + offset) % 1
    const ease = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
    mesh.position.x = start[0] + (end[0] - start[0]) * ease
    mesh.position.z = start[2] + (end[2] - start[2]) * ease
    mesh.position.y = start[1] + (end[1] - start[1]) * ease + Math.sin(ease * Math.PI) * 1.6
    mesh.rotation.x = t * Math.PI * 2
    mesh.rotation.y = t * Math.PI * 1.5
    const scale = 0.85 + Math.sin(ease * Math.PI) * 0.15
    mesh.scale.setScalar(scale)
  })

  return (
    <mesh ref={ref} castShadow>
      <boxGeometry args={[0.38, 0.38, 0.38]} />
      <meshStandardMaterial color={color} roughness={0.3} />
    </mesh>
  )
}

function Scene() {
  const group = useRef<Group>(null)
  useFrame(({ clock }) => {
    if (group.current) group.current.rotation.y = Math.sin(clock.elapsedTime * 0.12) * 0.18
  })

  return (
    <>
      {/* 외부 HDR 을 받지 않도록 조명만으로 밝힌다 (오프라인·폐쇄망에서도 뜬다) */}
      <ambientLight intensity={0.7} />
      <hemisphereLight args={['#ffffff', '#dfe3ea', 0.6]} />
      <directionalLight position={[5, 8, 4]} intensity={1.4} castShadow shadow-mapSize={[1024, 1024]} />
      <directionalLight position={[-6, 4, -3]} intensity={0.5} />
      <group ref={group} position={[0, -0.9, 0]}>
        <Float speed={1.2} rotationIntensity={0} floatIntensity={0.25}>
          {STORES.map((store, index) => (
            <StoreBlock key={index} {...store} />
          ))}
        </Float>
        {ROUTES.map((route, index) => (
          <ItemCube key={index} {...route} />
        ))}
        {/* 바닥: 밝은 무대 */}
        <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.01, 0]} receiveShadow>
          <circleGeometry args={[6, 48]} />
          <meshStandardMaterial color="#f5f5f7" roughness={1} />
        </mesh>
        <ContactShadows position={[0, 0, 0]} opacity={0.35} scale={12} blur={2.4} far={3} />
      </group>
    </>
  )
}

/** 랜딩 3D 히어로. 따로 받는 청크라 첫 화면 번들에는 three 가 들어가지 않는다 */
export default function HeroScene() {
  return (
    <Canvas
      shadows
      dpr={[1, 1.5]}
      camera={{ position: [0, 3.2, 9], fov: 34 }}
      gl={{ antialias: true, alpha: true, powerPreference: 'low-power' }}
      style={{ width: '100%', height: '100%' }}
    >
      <Scene />
    </Canvas>
  )
}
