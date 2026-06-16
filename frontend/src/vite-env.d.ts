/// <reference types="vite/client" />

declare module '*.png' {
  const src: string
  export default src
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

/** @vue-office/* postinstall often skips copying lib/index.js under pnpm — we import lib/v3/* directly */
declare module '@vue-office/docx/lib/v3/index.js' {
  const VueOfficeDocx: {
    install?: (vue: unknown) => void
    src: string | ArrayBuffer | Blob
    requestOptions?: unknown
    options?: unknown
  }
  export default VueOfficeDocx
}

declare module '@vue-office/excel/lib/v3/index.js' {
  export interface VueOfficeExcelOptions {
    minColLength?: number
    minRowLength?: number
    showContextmenu?: boolean
  }
  const VueOfficeExcel: {
    install?: (vue: unknown) => void
    src: string | ArrayBuffer | Blob
    requestOptions?: unknown
    options?: VueOfficeExcelOptions
  }
  export default VueOfficeExcel
}

declare module '@vue-office/pptx/lib/v3/index.js' {
  const VueOfficePptx: {
    install?: (vue: unknown) => void
    src: string | ArrayBuffer | Blob
    rerender?: () => unknown
    requestOptions?: unknown
    options?: unknown
  }
  export default VueOfficePptx
}

interface ImportMetaEnv {
  /** 后端 API 根地址；可留空，未设置时等同 '' */
  readonly VITE_API_BASE_URL?: string
  /** 数据同步服务 API 根地址；默认使用 /syncapi 代理 */
  readonly VITE_SYNC_API_BASE_URL?: string
  /** Vue 前端项目版本号；由 Vite 从 package.json 注入 */
  readonly VITE_APP_VERSION?: string
  /** Capacitor 原生壳版本清单地址；未配置时跳过热更新检查 */
  readonly VITE_UPDATE_VERSION_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
