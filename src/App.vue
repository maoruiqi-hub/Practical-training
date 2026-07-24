<template>
  <router-view />
</template>

<script setup>
</script>

<style>
html,
body,
#app {
  min-height: 100%;
  background: #050609;
}

body {
  margin: 0;
  font-family: 'Microsoft YaHei', sans-serif;
}

* {
  box-sizing: border-box;
}

/* Keep Element Plus loading states consistent with the dark student game UI. */
.tower-page,
.floor-page,
.run-room-dialog,
.treasure-room-dialog,
.shop-room-dialog,
.rest-room-dialog,
.supply-dialog {
  --el-mask-color: rgba(5, 6, 9, 0.72);
  --el-skeleton-color: rgba(248, 237, 207, 0.07);
  --el-skeleton-to-color: rgba(218, 154, 77, 0.13);
}

.tower-page .el-loading-mask,
.floor-page .el-loading-mask {
  backdrop-filter: blur(2px);
}

.game-loading-indicator {
  min-height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: rgba(248, 237, 207, 0.72);
}

.game-loading-indicator.compact {
  min-height: 72px;
}

.game-loading-indicator span {
  width: 24px;
  height: 24px;
  border: 2px solid rgba(248, 237, 207, 0.18);
  border-top-color: #d99a4d;
  border-radius: 50%;
  animation: game-loading-spin 0.8s linear infinite;
}

.game-loading-indicator p {
  margin: 0;
  font-size: 13px;
}

@keyframes game-loading-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .game-loading-indicator span {
    animation: none;
  }
}
</style>
