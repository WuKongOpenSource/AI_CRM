declare module 'vue' {
  export interface GlobalComponents {
    WkIcon: typeof import('@/components/common/WkIcon.vue')['default']
  }
}

export {}
