/**
 * Custom serve script that correctly serves both Docusaurus pages and static
 * Dokka HTML when using a non-root baseUrl (e.g., /flixelgdx/).
 *
 * Docusaurus's built-in "docusaurus serve" passes cleanUrls:true to
 * serve-handler, which generates 301 redirects that omit the baseUrl prefix.
 * Dokka HTML directories with dots in their names also break under cleanUrls.
 *
 * This script:
 *  - Serves /dokka-html/** directly from disk (no cleanUrls redirect chain)
 *  - Delegates all other paths to serve-handler with cleanUrls:true
 *  - Fixes serve-handler redirect Location headers to include the baseUrl
 *
 * Usage:  node serve-fixed.cjs [--port 3000]
 */
const http = require('http');
const path = require('path');
const fs = require('fs');

const args = process.argv.slice(2);
let port = 3000;
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--port' && args[i + 1]) {
    port = parseInt(args[i + 1], 10);
  }
}

const outDir = path.resolve(__dirname, 'build');
const baseUrl = process.env.DOCUSAURUS_BASE_URL || '/flixelgdx/';
const baseNoSlash = baseUrl.replace(/\/$/, '');

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.mjs': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject',
  '.map': 'application/json',
};

function serveStatic(res, filePath) {
  fs.stat(filePath, (err, stats) => {
    if (err) {
      res.writeHead(404);
      res.end('Not found');
      return;
    }
    const target = stats.isDirectory()
      ? path.join(filePath, 'index.html')
      : filePath;
    fs.readFile(target, (err2, data) => {
      if (err2) {
        res.writeHead(404);
        res.end('Not found');
        return;
      }
      const ext = path.extname(target).toLowerCase();
      res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' });
      res.end(data);
    });
  });
}

async function start() {
  const serveHandler = (await import('serve-handler')).default;

  const server = http.createServer((req, res) => {
    if (!req.url.startsWith(baseUrl)) {
      res.writeHead(302, { Location: baseUrl });
      res.end();
      return;
    }

    const relativePath = req.url.replace(baseUrl, '/');

    if (relativePath.startsWith('/dokka-html/')) {
      serveStatic(res, path.join(outDir, relativePath));
      return;
    }

    req.url = relativePath;

    const origWriteHead = res.writeHead.bind(res);
    res.writeHead = function (code) {
      if (code === 301 || code === 302) {
        for (let i = 1; i < arguments.length; i++) {
          const h = arguments[i];
          if (h && typeof h === 'object') {
            for (const key of Object.keys(h)) {
              if (key.toLowerCase() === 'location') {
                const loc = h[key];
                if (typeof loc === 'string' && loc.startsWith('/') && !loc.startsWith(baseUrl)) {
                  h[key] = baseNoSlash + loc;
                }
              }
            }
          }
        }
      }
      return origWriteHead.apply(this, arguments);
    };

    serveHandler(req, res, {
      cleanUrls: true,
      public: outDir,
      trailingSlash: false,
      directoryListing: false,
    });
  });

  server.listen(port, () => {
    console.log(`Serving "build" directory at: http://localhost:${port}${baseUrl}`);
  });
}

start();
