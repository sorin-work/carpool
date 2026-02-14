<script setup lang="ts">
import { computed } from 'vue'

import { avatarHue, initials } from '@/utils/initials'

const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 32 })

const style = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.round(props.size * 0.4)}px`,
  background: `hsl(${avatarHue(props.name)} 55% 88%)`,
  color: `hsl(${avatarHue(props.name)} 45% 28%)`,
}))
</script>

<template>
  <span class="avatar" :style="style" :title="name" aria-hidden="true">{{ initials(name) }}</span>
</template>

<style scoped>
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: 700;
  flex-shrink: 0;
}
</style>
