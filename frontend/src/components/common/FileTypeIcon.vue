<template>
  <span
    class="wk-file-type-icon"
    :class="[`wk-file-type-icon--${meta.kind}`, sizeClass]"
    :title="meta.label"
    aria-hidden="true"
  >
    <span class="wk-file-type-icon__sheet">
      <span class="wk-file-type-icon__fold" />
      <span class="wk-file-type-icon__mark">{{ meta.shortLabel }}</span>
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { resolveFileTypeIconMeta } from '@/utils/fileTypeIcon'

const props = withDefaults(defineProps<{
  fileName?: string | null
  mimeType?: string | null
  knowledgeType?: string | null
  size?: 'sm' | 'md' | 'lg'
}>(), {
  fileName: '',
  mimeType: '',
  knowledgeType: '',
  size: 'md'
})

const meta = computed(() => resolveFileTypeIconMeta({
  fileName: props.fileName,
  mimeType: props.mimeType,
  knowledgeType: props.knowledgeType
}))

const sizeClass = computed(() => `wk-file-type-icon--${props.size}`)
</script>

<style scoped>
.wk-file-type-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.wk-file-type-icon--sm {
  width: 28px;
  height: 28px;
}

.wk-file-type-icon--md {
  width: 36px;
  height: 36px;
}

.wk-file-type-icon--lg {
  width: 42px;
  height: 42px;
}

.wk-file-type-icon__sheet {
  position: relative;
  display: inline-flex;
  width: 100%;
  height: 100%;
  align-items: flex-end;
  justify-content: center;
  overflow: hidden;
  border-radius: 8px;
  background: var(--file-icon-color);
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 0.2), 0 6px 14px rgb(15 23 42 / 0.12);
}

.wk-file-type-icon__sheet::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(145deg, rgb(255 255 255 / 0.26), transparent 48%);
}

.wk-file-type-icon__fold {
  position: absolute;
  right: 0;
  top: 0;
  width: 34%;
  height: 34%;
  border-bottom-left-radius: 6px;
  background: rgb(255 255 255 / 0.34);
}

.wk-file-type-icon__mark {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 0 3px 4px;
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.wk-file-type-icon--sm .wk-file-type-icon__mark {
  padding-bottom: 3px;
  font-size: 8px;
}

.wk-file-type-icon--lg .wk-file-type-icon__mark {
  padding-bottom: 5px;
  font-size: 11px;
}

.wk-file-type-icon--word {
  --file-icon-color: #2b579a;
}

.wk-file-type-icon--excel {
  --file-icon-color: #217346;
}

.wk-file-type-icon--powerpoint {
  --file-icon-color: #d24726;
}

.wk-file-type-icon--pdf {
  --file-icon-color: #e5252a;
}

.wk-file-type-icon--image {
  --file-icon-color: #0ea5e9;
}

.wk-file-type-icon--audio {
  --file-icon-color: #7c3aed;
}

.wk-file-type-icon--markdown,
.wk-file-type-icon--text,
.wk-file-type-icon--generic {
  --file-icon-color: #64748b;
}
</style>
