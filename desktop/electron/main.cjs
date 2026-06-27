'use strict'
const { app, BrowserWindow, Menu, Tray, dialog, shell, ipcMain } = require('electron')
const path = require('path')
const fs = require('fs')
const os = require('os')
const http = require('http')
const { spawn, spawnSync } = require('child_process')
const { createServer } = require('./server.cjs')

// ---- config -----------------------------------------------------------------
// mode 'remote' : proxy to a configurable backend URL (default the docker nginx entry)
// mode 'local'  : spawn the bundled backend jar (embedded Postgres + Redis + local files)
const DEFAULT_TARGET = 'http://localhost'
const LOCAL_BACKEND_URL = 'http://127.0.0.1:8088'
const configFile = () => path.join(app.getPath('userData'), 'config.json')

function loadConfig() {
  const envUrl = process.env.CRM_SERVER_URL && process.env.CRM_SERVER_URL.trim()
  const envMode = process.env.CRM_BACKEND_MODE && process.env.CRM_BACKEND_MODE.trim()
  let cfg = { target: DEFAULT_TARGET, mode: 'remote' }
  let fileExisted = true
  try {
    cfg = { ...cfg, ...JSON.parse(fs.readFileSync(configFile(), 'utf8')) }
  } catch (_) {
    fileExisted = false
  }
  if (envUrl) cfg.target = envUrl
  if (envMode) cfg.mode = envMode
  cfg.mode = cfg.mode === 'local' ? 'local' : 'remote'
  cfg.target = normalizeUrl(cfg.target) || DEFAULT_TARGET
  // first launch with no saved config (and mode not forced by env) -> let the user choose a mode
  isFirstRun = !fileExisted && !envUrl && !envMode
  return cfg
}

function saveConfig(cfg) {
  try {
    fs.writeFileSync(configFile(), JSON.stringify(cfg, null, 2), 'utf8')
  } catch (e) {
    console.error('failed to save config', e)
  }
}

function normalizeUrl(u) {
  if (!u || typeof u !== 'string') return ''
  let s = u.trim().replace(/\/+$/, '')
  if (!s) return ''
  if (!/^https?:\/\//i.test(s)) s = 'http://' + s
  try {
    new URL(s) // eslint-disable-line no-new
    return s
  } catch (_) {
    return ''
  }
}

// ---- state -----------------------------------------------------------------
let config = { target: DEFAULT_TARGET, mode: 'remote' }
let server = null
let serverPort = 0
let mainWindow = null
let splashWindow = null
let tray = null
let backendProc = null
let isFirstRun = false

const STATIC_DIR = path.join(__dirname, '..', 'app') // bundled SPA
const ICON = path.join(__dirname, '..', 'build', 'icon.ico')
// bundled JRE + backend jar (extraResources in packaged builds, ../resources in dev)
const RES_DIR = app.isPackaged ? process.resourcesPath : path.join(__dirname, '..', 'resources')
const JAVA_BIN = path.join(RES_DIR, 'jre', 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
const BACKEND_JAR = path.join(RES_DIR, 'backend', 'crm-desktop.jar')
const BACKEND_DATA_DIR = path.join(app.getPath('userData'), 'backend-data')

function currentBackend() {
  return config.mode === 'local'
    ? { target: LOCAL_BACKEND_URL, strip: true }
    : { target: config.target, strip: false }
}

function startServer() {
  return new Promise((resolve, reject) => {
    server = createServer({ staticDir: STATIC_DIR, getBackend: currentBackend })
    server.on('error', reject)
    server.listen(0, '127.0.0.1', () => {
      serverPort = server.address().port
      resolve(serverPort)
    })
  })
}

// ---- local backend lifecycle -----------------------------------------------
function startLocalBackend() {
  return new Promise((resolve, reject) => {
    if (!fs.existsSync(JAVA_BIN)) return reject(new Error('未找到内置 JRE：' + JAVA_BIN))
    if (!fs.existsSync(BACKEND_JAR)) return reject(new Error('未找到后端程序：' + BACKEND_JAR))
    fs.mkdirSync(path.join(BACKEND_DATA_DIR, 'logs'), { recursive: true })

    const args = [
      '-Djava.io.tmpdir=' + os.tmpdir(),
      '-jar', BACKEND_JAR,
      '--spring.profiles.active=desktop',
      '--logging.file=' + path.join(BACKEND_DATA_DIR, 'logs')
    ]
    backendProc = spawn(JAVA_BIN, args, {
      cwd: BACKEND_DATA_DIR,
      env: { ...process.env, WK_DESKTOP_DATA_DIR: BACKEND_DATA_DIR },
      windowsHide: true
    })
    const logFile = path.join(BACKEND_DATA_DIR, 'backend.log')
    const logStream = fs.createWriteStream(logFile, { flags: 'a' })
    backendProc.stdout.pipe(logStream)
    backendProc.stderr.pipe(logStream)
    backendProc.on('error', reject)
    backendProc.on('exit', (code) => {
      console.log('[backend] process exited with', code)
      backendProc = null
    })

    waitForHealth(LOCAL_BACKEND_URL + '/doc.html', 180000, logFile).then(resolve).catch(reject)
  })
}

function waitForHealth(url, timeoutMs, logFile) {
  return new Promise((resolve, reject) => {
    const start = Date.now()
    const attempt = () => {
      const req = http.get(url, (res) => {
        res.resume()
        if (res.statusCode && res.statusCode < 500) return resolve()
        schedule()
      })
      req.on('error', schedule)
      req.setTimeout(4000, () => { req.destroy(); schedule() })
    }
    const schedule = () => {
      if (!backendProc) return reject(new Error('本地后端进程已退出。日志：' + logFile))
      if (Date.now() - start > timeoutMs) return reject(new Error('本地后端启动超时。日志：' + logFile))
      setTimeout(attempt, 1500)
    }
    attempt()
  })
}

function stopLocalBackend() {
  if (!backendProc || !backendProc.pid) return
  const pid = backendProc.pid
  backendProc = null
  try {
    if (process.platform === 'win32') {
      // kill the JVM and its embedded Postgres/Redis child processes
      spawnSync('taskkill', ['/pid', String(pid), '/T', '/F'])
    } else {
      process.kill(pid, 'SIGTERM')
    }
  } catch (_) {
    /* already gone */
  }
}

// ---- windows ----------------------------------------------------------------
function showSplash() {
  splashWindow = new BrowserWindow({
    width: 420,
    height: 220,
    frame: false,
    resizable: false,
    movable: true,
    alwaysOnTop: true,
    backgroundColor: '#0f1f3d',
    icon: ICON
  })
  splashWindow.loadFile(path.join(__dirname, 'splash.html'))
}

function closeSplash() {
  if (splashWindow) {
    try { splashWindow.close() } catch (_) {}
    splashWindow = null
  }
}

function createWindow(promptServer) {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1024,
    minHeight: 680,
    title: 'AI CRM',
    icon: ICON,
    backgroundColor: '#ffffff',
    webPreferences: { contextIsolation: true, nodeIntegration: false }
  })
  mainWindow.webContents.on('before-input-event', (_event, input) => {
    if (input.type !== 'keyDown') return
    const key = (input.key || '').toLowerCase()
    if (input.control && key === 'r') mainWindow.reload()
    else if (key === 'f12') mainWindow.webContents.toggleDevTools()
  })
  mainWindow.loadURL(`http://127.0.0.1:${serverPort}/`)
  if (promptServer) {
    mainWindow.webContents.once('did-finish-load', () => openServerPrompt())
  }
  mainWindow.on('closed', () => { mainWindow = null })
}

// ---- server-address prompt --------------------------------------------------
function openServerPrompt() {
  const prompt = new BrowserWindow({
    width: 460, height: 240, parent: mainWindow || undefined, modal: true,
    resizable: false, minimizable: false, maximizable: false, title: '服务器地址设置',
    webPreferences: { contextIsolation: true, preload: path.join(__dirname, 'prompt-preload.cjs') }
  })
  prompt.setMenuBarVisibility(false)
  prompt.loadFile(path.join(__dirname, 'prompt.html'))
  prompt.webContents.on('did-finish-load', () => prompt.webContents.send('prompt:init', config.target))

  const onSubmit = (_e, value) => {
    const normalized = normalizeUrl(value)
    if (!normalized) {
      dialog.showMessageBox(prompt, { type: 'error', message: '地址无效，请填写如 http://192.168.1.10 或 http://localhost' })
      return
    }
    config.target = normalized
    saveConfig(config)
    cleanup()
    prompt.close()
    if (mainWindow) mainWindow.reload()
  }
  const onCancel = () => { cleanup(); prompt.close() }
  function cleanup() {
    ipcMain.removeListener('prompt:submit', onSubmit)
    ipcMain.removeListener('prompt:cancel', onCancel)
  }
  ipcMain.on('prompt:submit', onSubmit)
  ipcMain.on('prompt:cancel', onCancel)
  prompt.on('closed', cleanup)
}

// ---- mode switching ---------------------------------------------------------
function switchMode(newMode) {
  if (config.mode === newMode) return
  config.mode = newMode
  saveConfig(config)
  // cleanest: relaunch so the backend is started/stopped in the right order
  app.relaunch()
  app.exit(0)
}

// First-run: let the user pick how the backend runs. Returns 'local' | 'remote' | null(quit).
function chooseMode() {
  const idx = dialog.showMessageBoxSync({
    type: 'question',
    icon: ICON,
    title: '选择运行模式',
    message: '欢迎使用 悟空 AI CRM 桌面版',
    detail: '请选择后端的运行方式：\n\n' +
      '• 本地后端 —— 自带数据库与文件存储，数据全部保存在本机，无需服务器或 Docker（首次启动稍慢）。\n\n' +
      '• 云端服务器 —— 连接你部署的 CRM 服务器，多设备实时同步。\n\n' +
      '之后可在右下角托盘图标中随时切换。',
    buttons: ['本地后端（自带数据库）', '云端服务器', '退出'],
    defaultId: 0,
    cancelId: 2
  })
  if (idx === 2) return null
  return idx === 0 ? 'local' : 'remote'
}

function showAbout() {
  dialog.showMessageBox(mainWindow, {
    type: 'info', title: '关于', message: 'AI CRM 桌面客户端',
    detail: `版本 ${app.getVersion()}\n模式 ${config.mode === 'local' ? '本地后端（自带数据库）' : '云端服务器 ' + config.target}\nElectron ${process.versions.electron}`
  })
}

function showMainWindow() {
  if (mainWindow) {
    if (mainWindow.isMinimized()) mainWindow.restore()
    mainWindow.show(); mainWindow.focus()
  } else {
    createWindow()
  }
}

function createTray() {
  try {
    tray = new Tray(ICON)
  } catch (e) {
    console.error('tray icon load failed:', e.message)
    return
  }
  tray.setToolTip('AI CRM')
  const isLocal = config.mode === 'local'
  const items = [
    { label: '显示主窗口', click: showMainWindow },
    { label: isLocal ? '当前：本地后端（自带数据库）' : '当前：云端服务器', enabled: false },
    { type: 'separator' }
  ]
  if (isLocal) {
    items.push({ label: '切换到云端服务器…', click: () => switchMode('remote') })
  } else {
    items.push({ label: '设置服务器地址…', click: openServerPrompt })
    items.push({ label: '切换到本地后端（自带数据库）', click: () => switchMode('local') })
  }
  items.push(
    { label: '重新加载', click: () => mainWindow && mainWindow.reload() },
    { type: 'separator' },
    { label: '关于', click: showAbout },
    { label: '退出', click: () => app.quit() }
  )
  tray.setContextMenu(Menu.buildFromTemplate(items))
  tray.on('double-click', showMainWindow)
}

// ---- lifecycle --------------------------------------------------------------
const gotLock = app.requestSingleInstanceLock()
if (!gotLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore()
      mainWindow.focus()
    }
  })

  app.whenReady().then(async () => {
    config = loadConfig()
    try {
      await startServer()
    } catch (e) {
      dialog.showErrorBox('启动失败', '无法启动内嵌服务：' + e.message)
      app.quit()
      return
    }
    Menu.setApplicationMenu(null)

    // first launch: ask the user to pick local vs remote
    let promptServer = false
    if (isFirstRun) {
      const chosen = chooseMode()
      if (chosen === null) { app.quit(); return }
      config.mode = chosen
      saveConfig(config)
      promptServer = chosen === 'remote'
    }

    if (config.mode === 'local') {
      showSplash()
      try {
        await startLocalBackend()
      } catch (e) {
        closeSplash()
        const choice = dialog.showMessageBoxSync({
          type: 'error', buttons: ['切换到云端服务器', '退出'], defaultId: 0,
          title: '本地后端启动失败', message: '本地后端未能启动', detail: e.message
        })
        if (choice === 0) { switchMode('remote'); return }
        app.quit(); return
      }
      closeSplash()
    }

    createWindow(promptServer)
    createTray()

    app.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) createWindow()
    })
  })

  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit()
  })

  app.on('before-quit', stopLocalBackend)
  app.on('quit', () => {
    stopLocalBackend()
    if (server) try { server.close() } catch (_) {}
  })
}
