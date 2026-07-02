<template>
  <BattleRoom
    :kp-id="kpId"
    :course-id="courseId"
    :student-id="studentId"
    :task-no="taskNo"
    :floor-name="floorName"
    :initial-hp="initialHp"
    :max-hp="maxHp"
    :run-id="runId"
    :node-id="nodeId"
    room-type="boss"
    boss-mode
    @battle-end="$emit('boss-end', $event)"
    @profile-refresh="$emit('profile-refresh')"
  />
</template>

<script setup>
import BattleRoom from './BattleRoom.vue'

defineProps({
  kpId: { type: [String, Number], required: true },
  courseId: { type: [String, Number], required: true },
  studentId: { type: [String, Number], required: true },
  taskNo: { type: [String, Number], default: '' },
  floorName: { type: String, default: 'Boss 楼层' },
  initialHp: { type: Number, default: 100 },
  maxHp: { type: Number, default: 100 },
  runId: { type: [String, Number], default: '' },
  nodeId: { type: [String, Number], default: '' }
})

defineEmits(['boss-end', 'profile-refresh'])
</script>
