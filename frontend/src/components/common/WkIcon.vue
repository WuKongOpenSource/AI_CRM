<template>
  <i
    class="wk wk-icon"
    :class="`wk-icon-${wkIconClassMap[name]}`"
    :style="iconStyle"
    :aria-hidden="label ? undefined : 'true'"
    :aria-label="label"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CSSProperties } from 'vue'
import { wkIconClassMap } from './wkIcon'
import type { WkIconName } from './wkIcon'

const props = withDefaults(defineProps<{
  name: WkIconName
  size?: number | string
  /** 固定宽高（px），iconfont 在盒内水平垂直居中 */
  boxSize?: number
  color?: string
  label?: string
}>(), {
  color: 'currentColor',
  label: undefined,
})

const iconStyle = computed<CSSProperties>(() => {
  const style: CSSProperties = {
    color: props.color,
  }

  const fontSize =
    props.size !== undefined
      ? props.size
      : props.boxSize !== undefined
        ? props.boxSize
        : undefined
  if (fontSize !== undefined) {
    style.fontSize = typeof fontSize === 'number' ? `${fontSize}px` : fontSize
  }

  if (props.boxSize !== undefined) {
    const n = props.boxSize
    style.width = `${n}px`
    style.height = `${n}px`
    style.display = 'inline-flex'
    style.alignItems = 'center'
    style.justifyContent = 'center'
    style.lineHeight = 1
  }

  return style
})
</script>
