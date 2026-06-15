import { h } from 'vue'
import { ElMessageBox } from 'element-plus'

function buildDeleteChatSessionMessage() {
  return h('div', { class: 'wk-delete-chat-dialog__body' }, [
    h('div', { class: 'wk-delete-chat-dialog__icon-wrap' }, [
      h('span', { class: 'wk-delete-chat-dialog__icon-badge', 'aria-hidden': 'true' }, '!')
    ]),
    h('div', { class: 'wk-delete-chat-dialog__text' }, [
      h('p', { class: 'wk-delete-chat-dialog__title-text' }, '确定要删除这个对话吗？'),
      h(
        'p',
        { class: 'wk-delete-chat-dialog__desc' },
        '删除后将无法恢复，相关的所有交互记录都将被永久移除。'
      )
    ])
  ])
}

/** 删除对话确认弹窗（与产品设计稿一致） */
export function confirmDeleteChatSession(): Promise<void> {
  return ElMessageBox.confirm(buildDeleteChatSessionMessage(), '删除对话', {
    customClass: 'wk-delete-chat-dialog',
    showClose: true,
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    confirmButtonClass: 'wk-delete-chat-dialog__confirm',
    cancelButtonClass: 'wk-delete-chat-dialog__cancel',
    autofocus: false,
    distinguishCancelAndClose: true
  }).then(() => undefined)
}
