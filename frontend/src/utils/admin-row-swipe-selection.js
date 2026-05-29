import { onBeforeUnmount, watch } from 'vue'

const SWIPE_THRESHOLD = 56
const MAX_SWIPE_OFFSET = 82
const DRAG_CLASS = 'admin-select-table--dragging'

export function useAdminRowSwipeSelection(tableRef, rowsRef, selectedRowsRef) {
  let startX = 0
  let startY = 0
  let startRow = null
  let pointerId = null
  let cleanup = null

  function rowFromEvent(event) {
    return event.target?.closest?.('.el-table__body-wrapper tr.el-table__row')
  }

  function rowIndex(root, rowEl) {
    const rows = [...root.querySelectorAll('.el-table__body-wrapper tbody tr.el-table__row')]
    return rows.indexOf(rowEl)
  }

  function isSelected(row) {
    return selectedRowsRef.value.some((item) => item?.id === row?.id)
  }

  function onPointerDown(event) {
    if (event.button !== undefined && event.button !== 0) return
    if (event.target?.closest?.('button,a,.el-button,.el-checkbox,.el-input,.el-select')) return
    const rowEl = rowFromEvent(event)
    if (!rowEl) return
    startRow = rowEl
    pointerId = event.pointerId
    startX = event.clientX
    startY = event.clientY
    rowEl.setPointerCapture?.(event.pointerId)
    rowEl.classList.add(DRAG_CLASS)
  }

  function resetRow(rowEl) {
    rowEl?.classList.remove(DRAG_CLASS)
    if (rowEl?.style) {
      rowEl.style.removeProperty('--admin-row-swipe-x')
    }
  }

  function onPointerMove(event) {
    if (!startRow || (pointerId !== null && event.pointerId !== pointerId)) return
    const deltaX = Math.max(0, event.clientX - startX)
    const deltaY = Math.abs(event.clientY - startY)
    if (deltaX <= 4 || deltaY > 36) return
    event.preventDefault()
    startRow.style.setProperty('--admin-row-swipe-x', `${Math.min(deltaX, MAX_SWIPE_OFFSET)}px`)
  }

  function onPointerUp(event) {
    if (!startRow || (pointerId !== null && event.pointerId !== pointerId)) return
    const rowEl = startRow
    startRow = null
    pointerId = null
    rowEl.releasePointerCapture?.(event.pointerId)
    resetRow(rowEl)
    const deltaX = event.clientX - startX
    const deltaY = Math.abs(event.clientY - startY)
    if (deltaX < SWIPE_THRESHOLD || deltaY > 32) return

    const table = tableRef.value
    const root = table?.$el
    const index = root ? rowIndex(root, rowEl) : -1
    const row = index >= 0 ? rowsRef.value[index] : null
    if (!row || !table?.toggleRowSelection) return
    table.toggleRowSelection(row, !isSelected(row))
  }

  function onPointerCancel() {
    resetRow(startRow)
    startRow = null
    pointerId = null
  }

  function attach(root) {
    cleanup?.()
    cleanup = null
    if (!root) return
    root.addEventListener('pointerdown', onPointerDown)
    root.addEventListener('pointermove', onPointerMove)
    root.addEventListener('pointerup', onPointerUp)
    root.addEventListener('pointercancel', onPointerCancel)
    root.addEventListener('pointerleave', onPointerCancel)
    cleanup = () => {
      root.removeEventListener('pointerdown', onPointerDown)
      root.removeEventListener('pointermove', onPointerMove)
      root.removeEventListener('pointerup', onPointerUp)
      root.removeEventListener('pointercancel', onPointerCancel)
      root.removeEventListener('pointerleave', onPointerCancel)
    }
  }

  const stopWatchingTable = watch(() => tableRef.value?.$el, attach, {
    flush: 'post',
    immediate: true
  })

  onBeforeUnmount(() => {
    stopWatchingTable()
    cleanup?.()
  })
}
