import { computed, ref, watch } from 'vue'

export type ThemePreference = 'light' | 'dark' | 'system'
export type ResolvedTheme = 'light' | 'dark'

const THEME_STORAGE_KEY = 'wk-ai-crm-theme'
const VALID_THEME_PREFERENCES: ThemePreference[] = ['light', 'dark', 'system']

const themePreference = ref<ThemePreference>('system')
const systemPrefersDark = ref(false)
const initialized = ref(false)

let mediaQuery: MediaQueryList | null = null

function readStoredThemePreference(): ThemePreference {
  if (typeof window === 'undefined') return 'system'
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY)
  return VALID_THEME_PREFERENCES.includes(stored as ThemePreference)
    ? (stored as ThemePreference)
    : 'system'
}

const resolvedTheme = computed<ResolvedTheme>(() => {
  if (themePreference.value === 'system') {
    return systemPrefersDark.value ? 'dark' : 'light'
  }
  return themePreference.value
})

const isDark = computed(() => resolvedTheme.value === 'dark')

function applyTheme(theme: ResolvedTheme) {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  root.classList.toggle('dark', theme === 'dark')
  root.dataset.theme = theme
  root.style.colorScheme = theme
}

function setupTheme() {
  if (initialized.value || typeof window === 'undefined') return
  initialized.value = true

  themePreference.value = readStoredThemePreference()
  mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  systemPrefersDark.value = mediaQuery.matches

  const handleMediaChange = (event: MediaQueryListEvent) => {
    systemPrefersDark.value = event.matches
    themePreference.value = 'system'
    window.localStorage.setItem(THEME_STORAGE_KEY, 'system')
  }

  if (typeof mediaQuery.addEventListener === 'function') {
    mediaQuery.addEventListener('change', handleMediaChange)
  } else {
    mediaQuery.addListener(handleMediaChange)
  }

  watch(resolvedTheme, applyTheme, { immediate: true })
}

export function useTheme() {
  setupTheme()

  function setThemePreference(preference: ThemePreference) {
    themePreference.value = preference
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(THEME_STORAGE_KEY, preference)
    }
  }

  function toggleTheme() {
    setThemePreference(isDark.value ? 'light' : 'dark')
  }

  return {
    isDark,
    resolvedTheme,
    themePreference,
    setThemePreference,
    toggleTheme,
  }
}
