<template>
  <div class="structured-editor schedule-builder">
    <div class="structured-grid">
      <label><span>时区</span><select v-model="draft.timezone"><option v-for="zone in timezones" :key="zone" :value="zone">{{ zone }}</option></select></label>
      <label><span>执行频率</span><select v-model="draft.frequency"><option value="daily">每天</option><option value="weekdays">工作日</option><option value="hourly">每小时</option><option value="weekly">每周</option></select></label>
      <label v-if="draft.frequency !== 'hourly'"><span>执行时间</span><input v-model="draft.time" type="time" /></label>
      <label v-if="draft.frequency === 'weekly'"><span>执行星期</span><select v-model="draft.weekday"><option v-for="day in weekdays" :key="day.value" :value="day.value">{{ day.label }}</option></select></label>
    </div>
    <p class="condition-preview">下次执行：{{ nextRunLabel }}</p>
    <details class="advanced-diagnostic"><summary>开发者诊断：高级 cron</summary><input v-model.trim="draft.advancedCron" placeholder="仅在需要时填写 cron 表达式" :aria-invalid="Boolean(cronError)" /><small v-if="cronError" class="structured-error">{{ cronError }}</small><small v-else>表达式已通过基础格式校验，普通用户不需要使用此模式。</small></details>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
const props = defineProps({ modelValue: { type: Object, default: () => ({}) } })
const emit = defineEmits(['update:modelValue'])
const timezones = ['Asia/Shanghai', 'Asia/Tokyo', 'UTC', 'Europe/London', 'America/Los_Angeles']
const weekdays = [{ value: 'MON', label: '周一' }, { value: 'TUE', label: '周二' }, { value: 'WED', label: '周三' }, { value: 'THU', label: '周四' }, { value: 'FRI', label: '周五' }, { value: 'SAT', label: '周六' }, { value: 'SUN', label: '周日' }]
const draft = reactive({ timezone: 'Asia/Shanghai', frequency: 'daily', time: '09:00', weekday: 'MON', advancedCron: '', ...props.modelValue })
watch(draft, value => emit('update:modelValue', { ...value }), { deep: true })
const cronError = computed(() => { if (!draft.advancedCron) return ''; const fields = draft.advancedCron.split(/\s+/).filter(Boolean); return fields.length < 5 || fields.length > 7 ? 'cron 需要包含 5 到 7 段时间字段。' : fields.some(field => !/^[0-9*/?,\-A-Z]+$/i.test(field)) ? 'cron 包含无法识别的字符。' : '' })
const nextRunLabel = computed(() => draft.advancedCron ? (cronError.value ? '高级 cron 待修正' : '使用高级 cron，已通过基础校验') : `${draft.timezone} · ${draft.frequency === 'hourly' ? '每小时整点' : `${draft.time} ${draft.frequency === 'weekdays' ? '工作日' : draft.frequency === 'weekly' ? weekdays.find(day => day.value === draft.weekday)?.label : '每天'}`}`)
</script>

<style scoped src="./structured-editor.css"></style>
