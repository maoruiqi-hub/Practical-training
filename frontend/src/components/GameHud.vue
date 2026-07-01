<template>
  <section class="game-hud" :class="{ compact }" aria-label="学生游戏状态">
    <div class="hud-brand">
      <span class="avatar-mark" aria-hidden="true">
        <img class="avatar-image" :src="characterSprites.playerKnightIdle" alt="" />
      </span>
      <div class="brand-copy">
        <p class="eyebrow">登塔试炼</p>
        <h1>{{ displayCourseName }}</h1>
      </div>
    </div>

    <div class="hud-core" :class="{ 'without-hp': !showHp }">
      <div class="level-chip">
        <span>等级 {{ safeProfile.level }}</span>
        <strong>{{ levelName }}</strong>
      </div>

      <div v-if="showHp" class="hp-block">
        <div class="hp-meta">
          <span>HP</span>
          <strong>{{ safeProfile.hp }}/{{ safeProfile.maxHp }}</strong>
        </div>
        <div class="hp-track" aria-hidden="true">
          <div class="hp-fill" :style="{ width: hpPercent + '%' }"></div>
        </div>
      </div>

      <div class="resource-chip energy">
        <span class="resource-icon" aria-hidden="true"></span>
        <small>能量</small>
        <strong>{{ safeProfile.energy }}</strong>
      </div>

      <div class="resource-chip coin">
        <span class="resource-icon" aria-hidden="true"></span>
        <small>金币</small>
        <strong>{{ safeProfile.coins }}</strong>
      </div>

      <div class="mini-stats">
        <span>攻击 <b>{{ safeProfile.atk }}%</b></span>
        <span>防御 <b>{{ safeProfile.def }}%</b></span>
        <span>经验 <b>{{ safeProfile.exp }}</b></span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { characterSprites } from '../data/gameAssetManifest'

const props = defineProps({
  profile: { type: Object, default: () => ({}) },
  courseName: { type: String, default: 'Python 程序设计' },
  compact: { type: Boolean, default: false },
  showHp: { type: Boolean, default: true }
})

const clamp = value => Math.max(0, Math.min(100, Number(value) || 0))

const safeProfile = computed(() => {
  const maxHp = Number(props.profile.maxHp || props.profile.max_hp || 100) || 100
  return {
    hp: Math.max(0, Math.min(maxHp, Number(props.profile.hp ?? 100) || 0)),
    maxHp,
    atk: clamp(props.profile.atk ?? 50),
    def: clamp(props.profile.def ?? 50),
    exp: Number(props.profile.exp) || 0,
    level: Number(props.profile.level) || 1,
    coins: Number(props.profile.coins ?? props.profile.coin) || 0,
    energy: Number(props.profile.energy ?? 5) || 0
  }
})

const hpPercent = computed(() =>
  Math.round((safeProfile.value.hp / Math.max(1, safeProfile.value.maxHp)) * 100)
)

const displayCourseName = computed(() => {
  const name = String(props.courseName || '').trim()
  return name === 'Python Program Design' ? 'Python 程序设计' : name
})

const levelName = computed(() => {
  const names = { 1: '入门', 2: '初级', 3: '进阶', 4: '熟练', 5: '精通' }
  return names[safeProfile.value.level] || '挑战者'
})
</script>

<style scoped>
.game-hud {
  position: sticky;
  top: 0;
  z-index: 40;
  display: grid;
  grid-template-columns: minmax(240px, 360px) 1fr;
  gap: 18px;
  align-items: center;
  min-height: 88px;
  padding: 14px 20px;
  color: #fff7df;
  background:
    linear-gradient(90deg, rgba(13, 9, 9, .96), rgba(46, 22, 15, .9) 48%, rgba(11, 18, 25, .96)),
    radial-gradient(circle at 28% 0%, rgba(224, 137, 54, .22), transparent 34%);
  border-bottom: 1px solid rgba(241, 181, 77, .36);
  box-shadow: 0 14px 28px rgba(0, 0, 0, .38);
}

.game-hud.compact {
  min-height: 78px;
  padding-block: 10px;
}

.hud-brand {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.avatar-mark {
  position: relative;
  display: grid;
  width: 48px;
  height: 54px;
  place-items: center;
  border: 2px solid #e2ad57;
  border-radius: 12px 12px 7px 7px;
  background:
    radial-gradient(circle at 50% 22%, rgba(255, 230, 150, .92) 0 7px, transparent 8px),
    linear-gradient(180deg, #6d2f1f, #1c1210);
  box-shadow: inset 0 0 0 4px rgba(255, 236, 187, .08), 0 0 24px rgba(208, 88, 33, .22);
}

.avatar-image {
  width: 44px;
  height: 50px;
  object-fit: contain;
  object-position: center bottom;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, .45));
}

.brand-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 3px;
  color: #dba257;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1 {
  overflow: hidden;
  margin: 0;
  color: #fff8df;
  font-size: 22px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hud-core {
  display: grid;
  grid-template-columns: 92px minmax(180px, 1.3fr) repeat(2, minmax(90px, 116px)) minmax(220px, .8fr);
  gap: 10px;
  align-items: stretch;
}

.hud-core.without-hp {
  grid-template-columns: 92px repeat(2, minmax(90px, 116px)) minmax(220px, .8fr);
}

.level-chip,
.hp-block,
.resource-chip,
.mini-stats {
  min-height: 56px;
  border: 1px solid rgba(240, 184, 93, .28);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 239, 197, .12), rgba(70, 35, 22, .34));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .1), 0 8px 18px rgba(0, 0, 0, .2);
}

.level-chip,
.resource-chip {
  display: grid;
  align-content: center;
  padding: 8px 10px;
}

.level-chip span,
.resource-chip small,
.hp-meta span {
  color: #c7a976;
  font-size: 12px;
  font-weight: 800;
}

.level-chip strong,
.resource-chip strong,
.hp-meta strong {
  color: #fff5ce;
  font-size: 18px;
  line-height: 1.2;
}

.hp-block {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 8px 12px;
}

.hp-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.hp-track {
  height: 13px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, .1);
  border-radius: 999px;
  background: rgba(22, 10, 9, .84);
}

.hp-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #9f1d1d, #dc5c32, #f0b85d);
  transition: width .22s ease-out;
}

.resource-chip {
  position: relative;
  grid-template-columns: 30px 1fr;
  column-gap: 8px;
}

.resource-chip strong,
.resource-chip small {
  grid-column: 2;
}

.resource-icon {
  grid-row: 1 / span 2;
  align-self: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, .16), 0 0 14px rgba(237, 178, 83, .18);
}

.resource-chip.energy .resource-icon {
  clip-path: polygon(48% 0, 82% 0, 62% 38%, 92% 38%, 36% 100%, 48% 56%, 18% 56%);
  border-radius: 3px;
  background: linear-gradient(180deg, #89fff2, #2aa0a8);
}

.resource-chip.coin .resource-icon {
  background: radial-gradient(circle at 35% 30%, #fff2a8, #d9952f 62%, #7c4218);
}

.mini-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  padding: 8px;
}

.mini-stats span {
  display: grid;
  place-items: center;
  border-radius: 6px;
  color: #c7a976;
  font-size: 12px;
  font-weight: 800;
  background: rgba(255, 255, 255, .06);
}

.mini-stats b {
  color: #fff5ce;
  font-size: 15px;
}

@media (max-width: 1180px) {
  .game-hud {
    grid-template-columns: 1fr;
  }
  .hud-core {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .hud-core.without-hp {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .hp-block,
  .mini-stats {
    grid-column: span 2;
  }
}

@media (max-width: 700px) {
  .game-hud {
    padding: 12px;
  }
  .hud-brand {
    grid-template-columns: 48px minmax(0, 1fr);
  }
  h1 {
    font-size: 20px;
  }
  .hud-core {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .hud-core.without-hp {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .hp-block,
  .mini-stats {
    grid-column: 1 / -1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hp-fill {
    transition: none;
  }
}
</style>
