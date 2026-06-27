'use strict'
const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('serverPrompt', {
  onInit: (cb) => ipcRenderer.on('prompt:init', (_e, value) => cb(value)),
  submit: (value) => ipcRenderer.send('prompt:submit', value),
  cancel: () => ipcRenderer.send('prompt:cancel')
})
