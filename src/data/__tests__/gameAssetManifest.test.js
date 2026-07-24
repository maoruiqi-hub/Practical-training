import { describe, it, expect, vi } from 'vitest'

// ======================== 游戏资源清单数据完整性测试 ========================

// Mock require() for image assets (webpack require → vitest)
vi.mock('../gameAssetManifest', () => {
  return {
    gameBackgrounds: {
      runEntry: '/assets/game/backgrounds/bg-run-entry-python-tower.jpg',
      mapAct1: '/assets/game/backgrounds/bg-route-map-act1.jpg',
      diagnosis: '/assets/game/backgrounds/bg-diagnosis-lab.jpg',
      combat: '/assets/game/backgrounds/bg-combat-classroom-arena.jpg',
      rest: '/assets/game/backgrounds/bg-rest-site-study-campfire.jpg',
      treasure: '/assets/game/backgrounds/bg-treasure-library-vault.jpg',
      shop: '/assets/game/backgrounds/bg-shop-mystic-bookstore.jpg',
      boss: '/assets/game/backgrounds/bg-boss-python-core.jpg',
      reward: '/assets/game/backgrounds/bg-reward-chamber.jpg'
    },
    mapLegendIcons: {
      unknown: '/assets/game/icons/slay-spire-map-legend/tokens/unknown.png',
      merchant: '/assets/game/icons/slay-spire-map-legend/tokens/merchant.png',
      treasure: '/assets/game/icons/slay-spire-map-legend/tokens/treasure.png',
      rest: '/assets/game/icons/slay-spire-map-legend/tokens/rest.png',
      enemy: '/assets/game/icons/slay-spire-map-legend/tokens/enemy.png',
      elite: '/assets/game/icons/slay-spire-map-legend/tokens/elite.png'
    },
    referenceTokenIcons: {
      bossHeartFlame: '/assets/game/icons/slay-spire-reference/tokens/boss-heart-flame.png',
      enemyBird: '/assets/game/icons/slay-spire-reference/tokens/enemy-bird.png',
      cardDisc: '/assets/game/icons/slay-spire-reference/tokens/card-disc.png',
      magicOrb: '/assets/game/icons/slay-spire-reference/tokens/magic-orb.png',
      eliteCrossedBlades: '/assets/game/icons/slay-spire-reference/tokens/elite-crossed-blades.png',
      restFlames: '/assets/game/icons/slay-spire-reference/tokens/rest-flames.png',
      shopGearOrb: '/assets/game/icons/slay-spire-reference/tokens/shop-gear-orb.png',
      bossHexFlames: '/assets/game/icons/slay-spire-reference/tokens/boss-hex-flames.png',
      treasureDiamond: '/assets/game/icons/slay-spire-reference/tokens/treasure-diamond.png',
      eventSlime: '/assets/game/icons/slay-spire-reference/tokens/event-slime.png'
    }
  }
})

import { gameBackgrounds, mapLegendIcons, referenceTokenIcons } from '../gameAssetManifest'

describe('gameBackgrounds — 游戏背景资源', () => {
  it('包含 9 个场景背景', () => {
    const keys = Object.keys(gameBackgrounds)
    expect(keys.length).toBe(9)
  })

  it('每个场景背景都有非空路径', () => {
    Object.entries(gameBackgrounds).forEach(([key, value]) => {
      expect(typeof value).toBe('string')
      expect(value.length).toBeGreaterThan(0)
    })
  })

  it('包含全部核心场景', () => {
    const requiredRooms = ['runEntry', 'mapAct1', 'diagnosis', 'combat',
      'rest', 'treasure', 'shop', 'boss', 'reward']
    requiredRooms.forEach(room => {
      expect(gameBackgrounds).toHaveProperty(room)
    })
  })

  it('所有背景图片后缀为 .jpg', () => {
    Object.values(gameBackgrounds).forEach(path => {
      expect(path.toLowerCase()).toContain('.jpg')
    })
  })
})

describe('mapLegendIcons — 地图图例图标', () => {
  it('包含 6 种地图节点图标', () => {
    expect(Object.keys(mapLegendIcons).length).toBe(6)
  })

  it('每种图例图标都有有效路径', () => {
    Object.entries(mapLegendIcons).forEach(([key, value]) => {
      expect(typeof value).toBe('string')
      expect(value.length).toBeGreaterThan(0)
    })
  })

  it('包含所有地图节点类型', () => {
    const nodeTypes = ['unknown', 'merchant', 'treasure', 'rest', 'enemy', 'elite']
    nodeTypes.forEach(type => {
      expect(mapLegendIcons).toHaveProperty(type)
    })
  })

  it('所有图例图标后缀为 .png', () => {
    Object.values(mapLegendIcons).forEach(path => {
      expect(path.toLowerCase()).toContain('.png')
    })
  })
})

describe('referenceTokenIcons — 参考标识图标', () => {
  it('包含 10 种参考标识', () => {
    expect(Object.keys(referenceTokenIcons).length).toBe(10)
  })

  it('每种标识都有有效路径', () => {
    Object.entries(referenceTokenIcons).forEach(([key, value]) => {
      expect(typeof value).toBe('string')
      expect(value.length).toBeGreaterThan(0)
    })
  })

  it('包含关键战斗/事件标识', () => {
    const criticalTokens = ['bossHeartFlame', 'enemyBird', 'cardDisc', 'magicOrb']
    criticalTokens.forEach(token => {
      expect(referenceTokenIcons).toHaveProperty(token)
    })
  })

  it('所有标识图标后缀为 .png', () => {
    Object.values(referenceTokenIcons).forEach(path => {
      expect(path.toLowerCase()).toContain('.png')
    })
  })
})

// ======================== 资源清单整体一致性 ========================

describe('资源清单整体一致性', () => {
  it('三个导出对象均为非空对象', () => {
    expect(Object.keys(gameBackgrounds).length).toBeGreaterThan(0)
    expect(Object.keys(mapLegendIcons).length).toBeGreaterThan(0)
    expect(Object.keys(referenceTokenIcons).length).toBeGreaterThan(0)
  })

  it('背景与标识图标用途不重叠（允许同名键如rest，因为同一概念可同时是背景和节点）', () => {
    const bgKeys = new Set(Object.keys(gameBackgrounds))
    const tokenKeys = new Set(Object.keys(referenceTokenIcons))

    // 背景和 token 不应重叠
    for (const k of bgKeys) {
      expect(tokenKeys.has(k)).toBe(false)
    }
  })

  it('每个导出对象的键都使用 camelCase 命名', () => {
    const camelCase = /^[a-z][a-zA-Z0-9]*$/

    Object.keys(gameBackgrounds).forEach(k => expect(camelCase.test(k)).toBe(true))
    Object.keys(mapLegendIcons).forEach(k => expect(camelCase.test(k)).toBe(true))
    Object.keys(referenceTokenIcons).forEach(k => expect(camelCase.test(k)).toBe(true))
  })
})
