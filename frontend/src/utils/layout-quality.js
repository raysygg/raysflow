/** 返回超出视口或与同层交互控件重叠的可见元素。 */
export function collectLayoutIssues(root = document, viewportWidth = window.innerWidth) {
  const elements = [...root.querySelectorAll('[data-layout-check], button, input, select, textarea')]
    .filter(element => element.getClientRects().length && getComputedStyle(element).visibility !== 'hidden')
  const insideScrollableContainer = element => {
    let parent = element.parentElement
    while (parent && parent !== root.documentElement) {
      const style = getComputedStyle(parent)
      if (/(auto|scroll)/.test(style.overflowX) && parent.scrollWidth > parent.clientWidth + 1) return true
      parent = parent.parentElement
    }
    return false
  }
  const overflow = elements.filter(element => {
    const rect = element.getBoundingClientRect()
    const outsideViewport = rect.left < -1 || rect.right > viewportWidth + 1
    return (outsideViewport && !insideScrollableContainer(element)) || element.scrollWidth > element.clientWidth + 1
  }).map(element => ({ type: 'overflow', element }))
  const controls = elements.filter(element => element.matches('button, input, select, textarea'))
  const overlap = []
  for (let index = 0; index < controls.length; index += 1) {
    const first = controls[index].getBoundingClientRect()
    for (let other = index + 1; other < controls.length; other += 1) {
      const second = controls[other].getBoundingClientRect()
      const area = Math.max(0, Math.min(first.right, second.right) - Math.max(first.left, second.left))
        * Math.max(0, Math.min(first.bottom, second.bottom) - Math.max(first.top, second.top))
      if (area > 4) overlap.push({ type: 'overlap', elements: [controls[index], controls[other]] })
    }
  }
  return [...overflow, ...overlap]
}
