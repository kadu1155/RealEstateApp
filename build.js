const fs = require('fs');
const path = require('path');

console.log('[Build] Preparing deployment assets...');

// Ensure public directory exists
if (!fs.existsSync('public')) {
  fs.mkdirSync('public', { recursive: true });
}

// Ensure dist directory exists
if (!fs.existsSync('dist')) {
  fs.mkdirSync('dist', { recursive: true });
}

// Copy index.html to both root and dist
if (fs.existsSync('index.html')) {
  fs.copyFileSync('index.html', path.join('public', 'index.html'));
  fs.copyFileSync('index.html', path.join('dist', 'index.html'));
  console.log('[Build] Synced index.html to /public and /dist');
}

// Copy public assets to dist as well
if (fs.existsSync('public')) {
  fs.cpSync('public', 'dist/public', { recursive: true });
}

console.log('[Build] Ready for Vercel deployment.');
