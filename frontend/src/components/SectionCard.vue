<template>
  <section class="section-card">
    <div class="section-head">
      <div>
        <div v-if="eyebrow" class="section-eyebrow">{{ eyebrow }}</div>
        <h3>{{ title }}</h3>
      </div>
      <slot name="action" />
    </div>
    <p v-if="description" class="section-description">{{ description }}</p>
    <slot />
  </section>
</template>

<script setup>
defineProps({
  eyebrow: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>
