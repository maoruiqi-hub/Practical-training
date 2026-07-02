<template>
  <el-dialog
    v-model="visible"
    width="460px"
    class="supply-dialog"
    title="补给营地"
    append-to-body
  >
    <div class="supply-body">
      <div class="supply-token" aria-hidden="true"></div>
      <div>
        <p class="supply-title">火堆旁的短暂休整</p>
        <p class="supply-text">
          当前 HP 为 {{ profile?.hp ?? 0 }}。使用补给会记录到游戏事件中，并刷新角色属性。
        </p>
      </div>
    </div>

    <div class="supply-actions">
      <el-button
        class="camp-button primary"
        :loading="loading"
        @click="useSupply('hp_potion')"
      >
        使用生命药水
      </el-button>
      <el-button class="camp-button" @click="dismiss">暂不需要</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { sendGameEvent } from '../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  studentId: { type: [String, Number], required: true },
  courseId: { type: [String, Number], required: true },
  currentKpId: { type: [String, Number], default: '' },
  profile: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'used'])
const loading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const dismiss = () => {
  visible.value = false
}

const useSupply = async supplyType => {
  loading.value = true
  try {
    const res = await sendGameEvent(props.studentId, {
      course_id: props.courseId,
      event_type: 'supply_used',
      supply_type: supplyType,
      target_kp_id: props.currentKpId
    })
    if (res.data.code === 200) {
      ElMessage.success('补给已使用')
      emit('used', res.data.data)
      visible.value = false
    } else {
      ElMessage.error(res.data.msg || '补给使用失败')
    }
  } catch {
    ElMessage.error('补给使用失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
:deep(.supply-dialog) {
  background: #20130f;
}

.supply-body {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 16px;
  align-items: center;
}

.supply-token {
  width: 72px;
  height: 88px;
  border-radius: 10px;
  background:
    radial-gradient(circle at 50% 22%, #ffe5a3 0 12px, transparent 13px),
    linear-gradient(145deg, #74261d, #1b1110 72%);
  border: 2px solid #d79b4b;
  box-shadow: 0 14px 24px rgba(0, 0, 0, .38), inset 0 0 0 4px rgba(255, 236, 188, .08);
}

.supply-title {
  margin: 0 0 8px;
  color: #3b2419;
  font-size: 18px;
  font-weight: 800;
}

.supply-text {
  margin: 0;
  color: #6d5645;
  line-height: 1.7;
}

.supply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
}

.camp-button {
  min-height: 44px;
}

.camp-button.primary {
  color: #fff6d6;
  background: linear-gradient(180deg, #b75b28, #74311f);
  border-color: #d49b51;
}
</style>
