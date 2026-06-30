<template>
  <section class="reward-room" :style="roomStyle" aria-label="战斗奖励">
    <div class="reward-scrim">
      <header class="reward-header">
        <p class="kicker">Reward Draft</p>
        <h2>选择一项战利品</h2>
        <p>
          本场正确率 {{ Math.round((battleResult.correctRate || 0) * 100) }}%，选择一个奖励强化后续路线。
        </p>
      </header>

      <div class="reward-grid">
        <button
          v-for="reward in rewards"
          :key="reward.id"
          type="button"
          class="reward-card"
          :class="reward.rarity"
          @click="pickReward(reward)"
        >
          <span class="reward-cost">{{ reward.badge }}</span>
          <div class="reward-art" aria-hidden="true">
            <el-icon>
              <component :is="reward.icon" />
            </el-icon>
          </div>
          <strong>{{ reward.name }}</strong>
          <small>{{ reward.typeLabel }}</small>
          <p>{{ reward.description }}</p>
        </button>
      </div>

      <footer class="reward-actions">
        <el-button class="ghost-button" @click="skipReward">跳过，换 10 金币</el-button>
      </footer>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { Coin, FirstAidKit, Lightning, Lock, MagicStick } from '@element-plus/icons-vue'
import { gameBackgrounds } from '../data/gameAssetManifest'

const props = defineProps({
  battleResult: { type: Object, default: () => ({}) },
  floorName: { type: String, default: '当前知识点' },
  roomType: { type: String, default: 'battle' }
})

const emit = defineEmits(['reward-picked'])

const roomStyle = computed(() => ({
  backgroundImage: `linear-gradient(180deg, rgba(8, 10, 14, .2), rgba(8, 10, 14, .78)), url(${gameBackgrounds.reward})`
}))

const rewards = computed(() => {
  const strongRun = Number(props.battleResult.correctRate || 0) >= 0.9
  const elite = props.roomType === 'elite' || props.roomType === 'boss'
  return [
    {
      id: 'trace-variable',
      name: '变量追踪',
      type: 'card',
      typeLabel: '知识卡',
      rarity: strongRun ? 'rare' : 'common',
      badge: '1',
      icon: MagicStick,
      description: `下次遇到 ${props.floorName} 相关题目时，可排除一个干扰项。`
    },
    {
      id: 'indent-shield',
      name: '缩进护盾',
      type: 'card',
      typeLabel: '防御卡',
      rarity: 'common',
      badge: '1',
      icon: Lock,
      description: '下一场战斗答错时减少 10 点伤害，适合稳住血量。'
    },
    {
      id: elite ? 'focus-relic' : 'recover-pack',
      name: elite ? '专注徽章' : '复习补给',
      type: elite ? 'relic' : 'heal',
      typeLabel: elite ? '遗物' : '补给',
      rarity: elite ? 'rare' : 'common',
      badge: elite ? '★' : '+',
      icon: elite ? Lightning : FirstAidKit,
      description: elite ? '本局后续战斗初始能量 +1。' : '立即恢复 15 HP，并获得一份复习提示。'
    },
    {
      id: 'coin-bundle',
      name: '金币袋',
      type: 'coin',
      typeLabel: '资源',
      rarity: 'common',
      badge: '$',
      icon: Coin,
      description: '获得 20 金币，可在商店购买提示、回血或净化错题卡。'
    }
  ].slice(0, 3)
})

const pickReward = reward => {
  emit('reward-picked', {
    reward,
    profileDelta: reward.type === 'coin' ? { coins: 20 } : reward.type === 'heal' ? { hp: 15 } : {},
    card: reward.type === 'card' ? reward : null,
    relic: reward.type === 'relic' ? reward : null
  })
}

const skipReward = () => {
  emit('reward-picked', {
    reward: { id: 'skip-coin', name: '跳过奖励', type: 'coin' },
    profileDelta: { coins: 10 }
  })
}
</script>

<style scoped>
.reward-room {
  min-height: 620px;
  overflow: hidden;
  border: 1px solid rgba(232, 184, 92, .28);
  border-radius: 8px;
  color: #fff3d6;
  background-position: center;
  background-size: cover;
  box-shadow: 0 24px 70px rgba(0, 0, 0, .48);
}

.reward-scrim {
  display: grid;
  align-content: center;
  min-height: inherit;
  padding: 32px;
  background:
    radial-gradient(circle at 50% 44%, rgba(242, 188, 89, .18), transparent 34%),
    linear-gradient(90deg, rgba(6, 8, 12, .62), rgba(6, 8, 12, .18), rgba(6, 8, 12, .62));
}

.reward-header {
  max-width: 760px;
  margin: 0 auto 24px;
  text-align: center;
}

.kicker {
  margin: 0 0 6px;
  color: #e3ad5d;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  color: #fff7de;
  font-size: 34px;
}

.reward-header p {
  margin: 10px 0 0;
  color: #e5d1aa;
}

.reward-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 260px));
  justify-content: center;
  gap: 18px;
}

.reward-card {
  position: relative;
  display: grid;
  min-height: 360px;
  padding: 18px;
  border: 2px solid rgba(233, 184, 92, .42);
  border-radius: 8px;
  color: #fff4d2;
  text-align: left;
  background:
    linear-gradient(180deg, rgba(250, 231, 184, .18), rgba(42, 24, 20, .92)),
    linear-gradient(145deg, #63301f, #17100f);
  box-shadow: 0 18px 34px rgba(0, 0, 0, .42);
  cursor: pointer;
  transition: transform .18s ease-out, border-color .18s ease-out, filter .18s ease-out;
}

.reward-card:hover {
  transform: translateY(-8px);
  border-color: #f0c66b;
  filter: brightness(1.08);
}

.reward-card.rare {
  border-color: rgba(76, 214, 206, .72);
}

.reward-cost {
  position: absolute;
  top: 12px;
  left: 12px;
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 50%;
  color: #331b10;
  font-weight: 900;
  background: radial-gradient(circle, #ffe9a8, #d28f39);
  box-shadow: 0 0 0 3px rgba(55, 27, 17, .8);
}

.reward-art {
  display: grid;
  height: 128px;
  margin: 28px 0 12px;
  place-items: center;
  border: 1px solid rgba(255, 241, 197, .18);
  border-radius: 8px;
  color: #f6d284;
  background: radial-gradient(circle, rgba(247, 208, 121, .2), rgba(255, 255, 255, .04));
}

.reward-art .el-icon {
  font-size: 64px;
}

.reward-card strong {
  color: #fff7dd;
  font-size: 22px;
}

.reward-card small {
  margin-top: 4px;
  color: #e3ad5d;
  font-weight: 800;
}

.reward-card p {
  margin: 14px 0 0;
  color: #e4d0a8;
  line-height: 1.7;
}

.reward-actions {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

.ghost-button {
  min-height: 44px;
  border-color: rgba(238, 181, 91, .44);
  border-radius: 6px;
  color: #f8ebcb;
  background: rgba(255, 255, 255, .08);
}

@media (max-width: 900px) {
  .reward-grid {
    grid-template-columns: 1fr;
  }
  .reward-card {
    min-height: 260px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .reward-card {
    transition: none;
  }
  .reward-card:hover {
    transform: none;
  }
}
</style>
