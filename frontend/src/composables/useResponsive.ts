import { ref, onMounted, onUnmounted } from 'vue'

const isMobile = ref(false)
const isTablet = ref(false)

let listenerCount = 0

function update() {
  isMobile.value = window.innerWidth < 768
  isTablet.value = window.innerWidth >= 768 && window.innerWidth < 1024
}

function handleResize() {
  update()
}

export function useResponsive() {
  onMounted(() => {
    if (listenerCount === 0) {
      update()
      window.addEventListener('resize', handleResize)
    }
    listenerCount++
  })

  onUnmounted(() => {
    listenerCount--
    if (listenerCount === 0) {
      window.removeEventListener('resize', handleResize)
    }
  })

  return { isMobile, isTablet }
}
