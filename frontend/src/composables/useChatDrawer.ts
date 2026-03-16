import { ref } from 'vue'

const isOpen = ref(false)

export function useChatDrawer() {
  function openChatDrawer() {
    isOpen.value = true
  }

  function closeChatDrawer() {
    isOpen.value = false
  }

  return { isOpen, openChatDrawer, closeChatDrawer }
}
