import { build } from 'esbuild';
import { cpSync, mkdirSync, rmSync } from 'node:fs';
rmSync('dist', { recursive: true, force: true });
mkdirSync('dist/fonts', { recursive: true });
await build({ entryPoints: ['src/app.js'], bundle: true, minify: true, format: 'iife', target: 'es2020', outfile: 'dist/app.js', legalComments: 'none' });
for (const f of ['index.html', 'style.css', 'icon.svg']) cpSync('src/' + f, 'dist/' + f);
cpSync('fonts/RobotoFlex-latin.woff2', 'dist/fonts/RobotoFlex-latin.woff2');
console.log('built dist/');
