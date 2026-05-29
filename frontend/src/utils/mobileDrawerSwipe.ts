export type SwipePoint = {
  clientX: number
  clientY: number
}

export type SwipeGesture = {
  start: SwipePoint
  end: SwipePoint
}

type SwipeCloseOptions = {
  minHorizontalDistance?: number
  maxVerticalDistance?: number
  directionRatio?: number
}

const DEFAULT_MIN_HORIZONTAL_DISTANCE = 72
const DEFAULT_MAX_VERTICAL_DISTANCE = 80
const DEFAULT_DIRECTION_RATIO = 1.2

export function shouldCloseMobileDrawerFromSwipe(
  gesture: SwipeGesture,
  options: SwipeCloseOptions = {}
): boolean {
  const minHorizontalDistance = options.minHorizontalDistance ?? DEFAULT_MIN_HORIZONTAL_DISTANCE
  const maxVerticalDistance = options.maxVerticalDistance ?? DEFAULT_MAX_VERTICAL_DISTANCE
  const directionRatio = options.directionRatio ?? DEFAULT_DIRECTION_RATIO
  const deltaX = gesture.end.clientX - gesture.start.clientX
  const deltaY = gesture.end.clientY - gesture.start.clientY
  const horizontalDistance = Math.abs(deltaX)
  const verticalDistance = Math.abs(deltaY)

  return deltaX <= -minHorizontalDistance
    && verticalDistance <= maxVerticalDistance
    && horizontalDistance >= verticalDistance * directionRatio
}
