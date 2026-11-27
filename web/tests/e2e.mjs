// End-to-end checks against a TEST server (never run this against your real data).
// Usage: node tests/e2e.mjs <baseUrl>
import { chromium } from 'playwright-core';
const base = process.argv[2];
const browser = await chromium.launch({ executablePath: process.env.CHROME || '/usr/bin/chromium-browser', args: ['--no-sandbox'] });
let failed = 0;
const ok = (cond, msg) => { console.log((cond ? 'PASS ' : 'FAIL ') + msg); if (!cond) failed++; };
const phone = { viewport: { width: 412, height: 860 }, deviceScaleFactor: 1, hasTouch: true, isMobile: true };
const errors = [];
async function open(opts = phone) {
  const ctx = await browser.newContext(opts); const p = await ctx.newPage();
  p.on('pageerror', e => errors.push(e.message)); return { ctx, p };
}
async function login(p, pw = 'password') {
  await p.goto(base); await p.fill('input[type=password]', pw); await p.click('button[type=submit]'); await p.waitForSelector('.shell');
  await p.waitForTimeout(600);
}
async function swipe(p, selector, dx) {
  const b = await p.locator(selector).boundingBox();
  const cdp = await p.context().newCDPSession(p);
  const y = b.y + b.height / 2, x0 = b.x + b.width / 2 + (dx > 0 ? -60 : 10);
  const pt = x => [{ x, y, id: 1 }];
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: pt(x0) });
  for (let i = 1; i <= 12; i++) await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: pt(x0 + dx * i / 12) });
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  await p.waitForTimeout(500);
}
const serverLists = async p => p.evaluate(async () => { const t = JSON.parse(localStorage.getItem('cl.token')); const r = await (await fetch('api.php?action=get', { headers: { Authorization: 'Bearer ' + t } })).json(); return r.json ? JSON.parse(r.json) : null; });

/* 1. tasks + touch swipes */
const A = await open(); const a = A.p;
await login(a);
await a.click('.fab:has-text("New list")'); await a.fill('.title-input', 'Swipe test');
for (const t of ['one', 'two', 'three']) { await a.click('.fab:has-text("Add task")'); await a.keyboard.type(t); }
await a.waitForTimeout(200);
ok(await a.locator('.task').count() === 3, 'three tasks created with Add task + typing');
await swipe(a, '.task >> nth=0', 260);
ok(await a.locator('.task').count() === 2 && await a.locator('.completed-head .badge').textContent() === '1', 'swipe right completes a task (moves to Completed 1)');
await swipe(a, '.task >> nth=0', -260);
ok(await a.locator('.task').count() === 1, 'swipe left deletes a task immediately');
await a.click('.completed-head'); await a.waitForTimeout(300);
ok(await a.locator('.task.done').count() === 1, 'Completed section expands to show the finished task');
await swipe(a, '.task.done >> nth=0', 260);
ok(await a.locator('.task.done').count() === 0, 'swipe right on a completed task restores it');

/* 1b. short swipes do nothing; reorder; total count */
const nBefore = await a.locator('.task').count();
await swipe(a, '.task >> nth=0', 80);
await swipe(a, '.task >> nth=0', -80);
ok(await a.locator('.task').count() === nBefore && await a.locator('.task.done').count() === 0, 'a swipe shorter than a third of the row does nothing');
ok((await a.textContent('.total-pill')).startsWith(String(await a.locator('.task').count())), 'total task count shown in the top bar');
const texts = async () => a.locator('.task textarea').evaluateAll(els => els.map(e => e.value));
const t0 = await texts();
const hb = await a.locator('.task .drag >> nth=0').boundingBox(), hb2 = await a.locator('.task .drag >> nth=1').boundingBox();
await a.mouse.move(hb.x + hb.width / 2, hb.y + hb.height / 2); await a.mouse.down();
await a.mouse.move(hb2.x + hb2.width / 2, hb2.y + hb2.height / 2 + 20, { steps: 10 }); await a.mouse.up(); await a.waitForTimeout(400);
const t1 = await texts();
ok(t1[0] === t0[1] && t1[1] === t0[0], 'dragging a task by its handle reorders it');
await a.waitForTimeout(800);
ok(await a.evaluate(() => { let v = JSON.parse(localStorage.getItem('cl.lists')); if (typeof v === 'string') v = JSON.parse(v); return v[0].items[0].t; }) === t1[0], 'new order is saved');
ok(await a.locator('.list-view .top-row').evaluate(e => getComputedStyle(e).position) === 'sticky', 'top bar stays put when scrolling');

/* 2. list color dialog */
const before = await a.evaluate(() => getComputedStyle(document.querySelector('.list-view')).getPropertyValue('--primary'));
await a.click('[aria-label="List menu"]'); await a.click('.menu >> text=Change color');
await a.waitForSelector('dialog[open]');
await a.locator('dialog .swatch').nth(3).click(); await a.waitForTimeout(200);
await a.click('dialog >> text=Done'); await a.waitForTimeout(900);
const after = await a.evaluate(() => getComputedStyle(document.querySelector('.list-view')).getPropertyValue('--primary'));
ok(before !== after, `list color changes the list theme (${before} -> ${after})`);
const sl = await serverLists(a);
ok(sl && sl[0].argb === (0xFF3FA34D | 0), 'list color saved to server as signed ARGB int (same format as the Android app): ' + (sl && sl[0].argb));
ok(sl && sl[0].items.length === 2 && 't' in sl[0].items[0] && 'c' in sl[0].items[0], 'task format {t,c} matches the Android app');

/* 3. theme settings */
await a.click('.back-btn'); await a.click('[aria-label=Settings]'); await a.waitForSelector('.seg');
await a.click('.seg >> text=Dark'); await a.waitForTimeout(300);
ok(await a.evaluate(() => document.documentElement.style.colorScheme) === 'dark', 'Dark appearance applied');
await a.click('.switch'); await a.waitForTimeout(300);
ok(await a.evaluate(() => getComputedStyle(document.querySelector('.main-pane')).backgroundColor) === 'rgb(0, 0, 0)', 'Pure black background applied');
const pBefore = await a.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--primary'));
await a.locator('.card .swatch').nth(4).click(); await a.waitForTimeout(200);
const pAfter = await a.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--primary'));
ok(pBefore !== pAfter, `theme color changes the app theme (${pBefore} -> ${pAfter})`);
await a.reload(); await a.waitForSelector('.shell');
ok(await a.evaluate(() => JSON.parse(localStorage.getItem('cl.settings')).black === true), 'theme settings persist across reloads');

/* 4. second browser sees the same lists (sync) */
const B = await open({ viewport: { width: 1200, height: 800 } }); const b = B.p;
await login(b);
ok(await b.locator('.list-card .name', { hasText: 'Swipe test' }).count() === 1, 'a second browser signs in and sees the list');
await b.locator('.list-card').first().click(); await b.waitForSelector('.task');
await b.locator('.task textarea').first().click(); await b.keyboard.type(' from B'); await b.waitForTimeout(1500);
await a.evaluate(() => document.dispatchEvent(new Event('visibilitychange'))); await a.waitForTimeout(1200);
await a.click('.back-btn').catch(() => {});
await a.waitForTimeout(300);
const t = (await serverLists(a))[0].items.map(i => i.t);
ok(t.some(x => x.includes('from B')), 'edits from the second browser reach the server: ' + JSON.stringify(t));

/* 5. change password */
await a.goto(base + '#/settings'); await a.waitForSelector('.seg');
await a.click('text=Change password'); await a.waitForTimeout(200);
const inputs = a.locator('form, .card').locator('input[type=password]');
await inputs.nth(0).fill('wrong'); await inputs.nth(1).fill('newpass1'); await inputs.nth(2).fill('newpass1');
await a.click('text=Save new password'); await a.waitForSelector('.msg.err', { timeout: 5000 });
ok((await a.textContent('.msg')).includes('wrong'), 'wrong current password is refused');
await inputs.nth(0).fill('password'); await inputs.nth(1).fill('abc'); await inputs.nth(2).fill('abc');
await a.click('text=Save new password'); await a.waitForTimeout(300);
ok((await a.textContent('.msg')).includes('at least 6'), 'short new password is refused');
await inputs.nth(1).fill('newpass1'); await inputs.nth(2).fill('newpass2');
await a.click('text=Save new password'); await a.waitForTimeout(300);
ok((await a.textContent('.msg')).includes("don't match"), 'mismatched confirmation is refused');
await inputs.nth(2).fill('newpass1'); await a.click('text=Save new password'); await a.waitForTimeout(800);
ok((await a.textContent('.msg')).includes('Password changed'), 'password changes from inside the web app');
await b.evaluate(() => document.dispatchEvent(new Event('visibilitychange'))); await b.waitForTimeout(1500);
ok(await b.locator('.login').count() === 1, 'other signed-in browser is signed out after the change');
await login(b, 'newpass1');
ok(await b.locator('.shell').count() === 1, 'new password works for signing in');
const bad = await (await b.context().request.post(base + 'api.php?action=login', { data: { password: 'password' } })).status();
ok(bad === 401, 'old password no longer works');

/* 6. sign out clears local copy */
await a.click('text=Sign out'); await a.waitForSelector('.login');
ok(await a.evaluate(() => localStorage.getItem('cl.lists') === null && localStorage.getItem('cl.token') === null), 'sign out clears the cached lists and token');

console.log(errors.length ? 'page errors: ' + errors.join('; ') : 'no page errors');
console.log(failed ? `${failed} FAILED` : 'ALL PASSED');
await browser.close(); process.exit(failed ? 1 : 0);
