<template>
  <BattleRoom
    :kp-id="kpId"
    :course-id="courseId"
    :student-id="studentId"
    :task-no="taskNo"
    :floor-name="floorName"
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
  floorName: { type: String, default: 'Boss 楼层' }
})

defineEmits(['boss-end', 'profile-refresh'])
</script>
