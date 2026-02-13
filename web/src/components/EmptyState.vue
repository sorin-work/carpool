<script setup lang="ts">
defineProps<{ title: string }>()
</script>

<template>
  <div class="empty">
    <p class="title">{{ title }}</p>
    <p class="muted"><slot /></p>
  </div>
</template>

<style scoped>
.empty {
  text-align: center;
  padding: 2rem 1rem;
  border: 1px dashed var(--border);
  border-radius: var(--radius);
}

.title {
  font-weight: 600;
  margin: 0 0 0.25rem;
}

.muted {
  margin: 0;
}
</style>
