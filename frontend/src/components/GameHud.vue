<template>
  <section class="game-hud" aria-label="学生游戏属性">
    <div class="hud-title">
      <span class="tower-mark" aria-hidden="true"></span>
      <div>
        <p class="eyebrow">Tower Run</p>
        <h1>{{ courseName }}</h1>
      </div>
    </div>

    <div class="hud-stats">
      <div class="level-plate">
        <span class="label">Lv.{{ safeProfile.level }}</span>
        <strong>{{ levelName }}</strong>
      </div>

      <div class="hp-block">
        <div class="hp-meta">
          <span>HP</span>
          <strong>{{ safeProfile.hp }}/100</strong>
        </div>
        <div class="hp-track" aria-hidden="true">
          <div class="hp-fill" :style="{ width: safeProfile.hp + '%' }"></div>
        </div>
      </div>

      <div class="stat-card">
        <span>ATK</span>
        <strong>{{ safeProfile.atk }}%</strong>
      </div>
      <div class="stat-card">
        <span>DEF</span>
        <strong>{{ safeProfile.def }}%</strong>
      </div>
      <div class="stat-card">
        <span>EXP</span>
        <strong>{{ safeProfile.exp }}</strong>
      </div>
      <div class="stat-card">
        <span>COIN</span>
        <strong>{{ safeProfile.coins }}</strong>
      </div>
      <div class="stat-card">
        <span>ENERGY</span>
        <strong>{{ safeProfile.energy }}</strong>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  profile: { type: Object, default: () => ({}) },
  courseName: { type: String, default: 'Python 程序设计' }
})

const clamp = value => Math.max(0, Math.min(100, Number(value) || 0))

const safeProfile = computed(() => ({
  hp: clamp(props.profile.hp),
  atk: clamp(props.profile.atk),
  def: clamp(props.profile.def),
  exp: Number(props.profile.exp) || 0,
  level: Number(props.profile.level) || 1,
  coins: Number(props.profile.coins) || 0,
  energy: Number(props.profile.energy) || 0
}))

const levelName = computed(() => {
  const names = { 1: '入门', 2: '初级', 3: '中级', 4: '熟练', 5: '精通' }
  return names[safeProfile.value.level] || '精通'
})
</script>

<style scoped>
.game-hud {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: minmax(220px, 340px) 1fr;
  gap: 18px;
  align-items: center;
  padding: 18px 22px;
  color: #fff7df;
  background:
    linear-gradient(90deg, rgba(20, 12, 10, .96), rgba(47, 22, 14, .94) 50%, rgba(16, 24, 32, .96)),
    radial-gradient(circle at 25% 0%, rgba(220, 92, 32, .28), transparent 36%);
  border-bottom: 1px solid rgba(241, 181, 77, .35);
  box-shadow: 0 14px 28px rgba(0, 0, 0, .34);
}

.hud-title {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.tower-mark {
  width: 46px;
  height: 52px;
  flex: 0 0 auto;
  border: 2px solid #f0b85d;
  border-radius: 12px 12px 6px 6px;
  background:
    linear-gradient(180deg, #6b3420, #21140f),
    repeating-linear-gradient(90deg, transparent 0 8px, rgba(255, 255, 255, .12) 8px 10px);
  box-shadow: inset 0 0 0 4px rgba(255, 236, 187, .08), 0 0 24px rgba(208, 88, 33, .25);
}

.eyebrow {
  margin: 0 0 3px;
  color: #d39b57;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #fff8df;
  font-size: 22px;
  line-height: 1.2;
}

.hud-stats {
  display: grid;
  grid-template-columns: 96px minmax(170px, 1.4fr) repeat(5, minmax(82px, 1fr));
  gap: 10px;
  align-items: stretch;
}

.level-plate,
.stat-card,
.hp-block {
  min-height: 58px;
  border: 1px solid rgba(240, 184, 93, .28);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 239, 197, .12), rgba(70, 35, 22, .36));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .1), 0 8px 18px rgba(0, 0, 0, .2);
}

.level-plate,
.stat-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 8px 10px;
}

.label,
.stat-card span,
.hp-meta span {
  color: #c7a976;
  font-size: 12px;
  font-weight: 700;
}

.level-plate strong,
.stat-card strong,
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
  height: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(24, 12, 10, .82);
  border: 1px solid rgba(255, 255, 255, .1);
}

.hp-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #9f1d1d, #dc5c32, #f0b85d);
  transition: width .22s ease-out;
}

@media (max-width: 1100px) {
  .game-hud {
    grid-template-columns: 1fr;
  }
  .hud-stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .hp-block {
    grid-column: span 2;
  }
}

@media (max-width: 640px) {
  .game-hud {
    padding: 14px;
  }
  .hud-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .hp-block {
    grid-column: span 2;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hp-fill {
    transition: none;
  }
}
</style>
