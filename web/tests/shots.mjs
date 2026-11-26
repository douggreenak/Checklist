// Drives the web app in headless Chromium and saves screenshots. Usage: node tests/shots.mjs <baseUrl> <outDir> [password]
import { chromium } from 'playwright-core';
import { mkdirSync } from 'node:fs';
const [base, out, pw = 'password'] = process.argv.slice(2);
mkdirSync(out, { recursive: true });
const exe = process.env.CHROME || '/usr/bin/chromium-browser';
const browser = await chromium.launch({ executablePath: exe, args: ['--no-sandbox'] });
const errors = [];
async function newPage(opts) {
  const ctx = await browser.newContext(opts);
  const page = await ctx.newPage();
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()); });
  return page;
}
const phone = { viewport: { width: 412, height: 860 }, deviceScaleFactor: 2, hasTouch: true, isMobile: true, colorScheme: 'light' };

const p = await newPage(phone);
await p.goto(base);
await p.screenshot({ path: `${out}/01-login.png` });
await p.fill('input[type=password]', 'wrong'); await p.click('button[type=submit]');
await p.waitForSelector('.msg.err:not(.hidden)');
console.log('wrong password message:', await p.textContent('.msg.err'));
await p.fill('input[type=password]', pw); await p.click('button[type=submit]');
await p.waitForSelector('.shell');
await p.waitForTimeout(800);
console.log('signed in, status ok');

// create lists and tasks
for (const [name, tasks, color] of [['Groceries', ['Milk', 'Eggs', 'Bread', 'Coffee beans'], 0], ['Work', ['Send invoice', 'Call plumber'], 1], ['Weekend', ['Hike', 'Laundry'], 2]]) {
  await p.click('.fab:has-text("New list")');
  await p.waitForSelector('.title-input');
  await p.fill('.title-input', name);
  for (const t of tasks) {
    await p.click('.fab:has-text("Add task")');
    await p.keyboard.type(t);
  }
  // Enter-to-next: pressing Enter after the last task should open another empty one
  if (name === 'Groceries') {
    const before = await p.locator('.task').count();
    await p.keyboard.press('Enter');
    await p.waitForTimeout(100);
    const after = await p.locator('.task').count();
    console.log('Enter adds next task:', before, '->', after, '| focused is empty textarea:', await p.evaluate(() => document.activeElement.tagName === 'TEXTAREA' && document.activeElement.value === ''));
    await p.keyboard.type('Butter');
  }
  await p.waitForTimeout(100);
  if (name === 'Groceries') {
    // tick two items
    await p.locator('.task .check').nth(0).click();
    await p.locator('.task .check').nth(0).click();
    await p.waitForTimeout(300);
    await p.screenshot({ path: `${out}/02-list-collapsed.png` });
    await p.click('.completed-head');
    await p.waitForTimeout(500);
    await p.screenshot({ path: `${out}/03-list-completed.png` });
  }
  await p.click('.back-btn');
  await p.waitForSelector('.list-card');
}
await p.waitForTimeout(500);
await p.screenshot({ path: `${out}/04-home.png` });

// delete via X (no confirmation dialog)
let dialogSeen = false; p.on('dialog', () => { dialogSeen = true; });
await p.locator('.list-card').nth(0).click();
await p.waitForSelector('.task');
const n0 = await p.locator('.task').count();
await p.locator('.del').first().click();
await p.waitForTimeout(200);
console.log('delete (no popup):', n0, '->', await p.locator('.task').count(), 'dialog shown:', dialogSeen);

// settings
await p.click('.back-btn'); await p.click('[aria-label=Settings]');
await p.waitForSelector('.seg');
await p.waitForTimeout(400);
await p.screenshot({ path: `${out}/05-settings.png`, fullPage: false });

// desktop dark + pure black
const d = await newPage({ viewport: { width: 1280, height: 820 }, colorScheme: 'dark' });
await d.goto(base);
await d.fill('input[type=password]', pw); await d.click('button[type=submit]');
await d.waitForSelector('.list-card'); await d.waitForTimeout(800);
await d.locator('.list-card').nth(0).click(); await d.waitForSelector('.task'); await d.waitForTimeout(600);
await d.screenshot({ path: `${out}/06-desktop-dark.png` });
const dl = await newPage({ viewport: { width: 1280, height: 820 }, colorScheme: 'light' });
await dl.goto(base); await dl.fill('input[type=password]', pw); await dl.click('button[type=submit]');
await dl.waitForSelector('.list-card'); await dl.waitForTimeout(800);
await dl.locator('.list-card').nth(1).click(); await dl.waitForSelector('.task'); await dl.waitForTimeout(600);
await dl.screenshot({ path: `${out}/07-desktop-light.png` });

console.log('page errors:', errors.length ? errors : 'none');
await browser.close();
