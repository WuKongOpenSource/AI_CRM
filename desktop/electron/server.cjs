'use strict'
/**
 * Embedded static + reverse-proxy server for the AI CRM desktop client.
 *
 * It serves the bundled Vue SPA from `staticDir` and reverse-proxies the
 * dynamic paths (/crmapi, /syncapi, /s3 and the /ws WebSocket) to the
 * configured backend origin — i.e. the deployed docker stack's nginx entry.
 *
 * Because the SPA is loaded over http://127.0.0.1:<port>, every relative URL
 * the app builds (axios baseURL `/crmapi`, the IM socket `ws://<host>/ws`,
 * MinIO previews under `/s3`) resolves to this server and gets forwarded to
 * the backend. No frontend changes are required and the backend address is
 * fully runtime-configurable.
 */
const http = require('http')
const fs = require('fs')
const path = require('path')
const httpProxy = require('http-proxy')

// Path prefixes that must be forwarded to the backend instead of served locally.
const PROXY_PREFIXES = ['/crmapi', '/syncapi', '/s3', '/ws']
// API prefixes that the deployed nginx strips. When the backend is a bare jar
// (local mode, no nginx) we must strip them ourselves before forwarding.
const STRIP_PREFIXES = ['/crmapi', '/syncapi']

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json; charset=utf-8'
}

function isProxied(url) {
  const p = (url || '/').split('?')[0]
  return PROXY_PREFIXES.some((prefix) => p === prefix || p.startsWith(prefix + '/'))
}

// Strip the /crmapi (or /syncapi) prefix so a bare backend on :8088 receives
// root-relative paths (e.g. /crmapi/auth/userInfo -> /auth/userInfo).
function stripApiPrefix(url) {
  for (const prefix of STRIP_PREFIXES) {
    if (url === prefix) return '/'
    if (url.startsWith(prefix + '/')) return url.slice(prefix.length)
  }
  return url
}

function safeJoin(root, urlPath) {
  const clean = decodeURIComponent((urlPath || '/').split('?')[0])
  const resolved = path.normalize(path.join(root, clean))
  // prevent path traversal outside the static root
  if (!resolved.startsWith(path.normalize(root))) return null
  return resolved
}

function serveStatic(staticDir, req, res) {
  let filePath = safeJoin(staticDir, req.url)
  if (!filePath) {
    res.writeHead(403)
    return res.end('Forbidden')
  }
  fs.stat(filePath, (err, stat) => {
    if (!err && stat.isDirectory()) {
      filePath = path.join(filePath, 'index.html')
    }
    fs.readFile(filePath, (readErr, data) => {
      if (readErr) {
        // SPA fallback: unknown non-asset route -> index.html
        const indexPath = path.join(staticDir, 'index.html')
        return fs.readFile(indexPath, (idxErr, idxData) => {
          if (idxErr) {
            res.writeHead(404)
            return res.end('Not found')
          }
          res.writeHead(200, { 'Content-Type': MIME['.html'] })
          res.end(idxData)
        })
      }
      const ext = path.extname(filePath).toLowerCase()
      res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' })
      res.end(data)
    })
  })
}

/**
 * @param {object} opts
 * @param {string} opts.staticDir absolute path to the bundled SPA (contains index.html)
 * @param {() => {target: string, strip: boolean}} opts.getBackend returns the current backend
 *   origin and whether the /crmapi prefix must be stripped (true for a bare local jar, false
 *   for the deployed nginx which strips it itself).
 * @returns {http.Server}
 */
function createServer(opts) {
  const { staticDir, getBackend } = opts
  const proxy = httpProxy.createProxyServer({
    changeOrigin: true,
    ws: true,
    // SSE / chat streaming must not be buffered
    selfHandleResponse: false
  })

  proxy.on('error', (err, req, res) => {
    try {
      if (res && res.writeHead && !res.headersSent) {
        res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' })
      }
      if (res && res.end) res.end('Backend unreachable: ' + err.message)
    } catch (_) {
      /* socket already gone */
    }
  })

  const server = http.createServer((req, res) => {
    if (isProxied(req.url)) {
      const backend = getBackend()
      if (backend.strip) req.url = stripApiPrefix(req.url)
      return proxy.web(req, res, { target: backend.target })
    }
    serveStatic(staticDir, req, res)
  })

  // WebSocket upgrade (IM /ws -> backend); /ws is never stripped
  server.on('upgrade', (req, socket, head) => {
    if (isProxied(req.url)) {
      const backend = getBackend()
      proxy.ws(req, socket, head, { target: backend.target })
    } else {
      socket.destroy()
    }
  })

  return server
}

module.exports = { createServer }
