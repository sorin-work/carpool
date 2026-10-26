<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'

const version = __APP_VERSION__
</script>

<template>
  <AppHeader />
  <main class="container">
    <RouterView />
  </main>
  <footer class="app-footer muted">Carpool {{ version }}</footer>
</template>

<style scoped>
.app-footer {
  text-align: center;
  font-size: 0.8rem;
  padding: 0 1rem 2rem;
}
</style>
