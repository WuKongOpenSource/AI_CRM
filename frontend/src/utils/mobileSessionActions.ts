export type MobileSessionMenuPositionInput = {
  clientX: number
  clientY: number
  viewportWidth: number
  viewportHeight: number
  menuWidth?: number
  menuHeight?: number
  margin?: number
}

export type MobileSessionMenuPosition = {
  left: number
  top: number
}

const DEFAULT_MENU_WIDTH = 180
const DEFAULT_MENU_HEIGHT = 132
const DEFAULT_MARGIN = 12

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

export function getMobileSessionMenuPosition(
  input: MobileSessionMenuPositionInput
): MobileSessionMenuPosition {
  const menuWidth = input.menuWidth ?? DEFAULT_MENU_WIDTH
  const menuHeight = input.menuHeight ?? DEFAULT_MENU_HEIGHT
  const margin = input.margin ?? DEFAULT_MARGIN
  const maxLeft = Math.max(margin, input.viewportWidth - menuWidth - margin)
  const maxTop = Math.max(margin, input.viewportHeight - menuHeight - margin)

  return {
    left: Math.round(clamp(input.clientX, margin, maxLeft)),
    top: Math.round(clamp(input.clientY, margin, maxTop)),
  }
}
