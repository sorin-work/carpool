<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <section>
    <h1>Hello {{ auth.user?.displayName }}</h1>
  </section>
</template>
