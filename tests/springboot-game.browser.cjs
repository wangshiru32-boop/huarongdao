// Spring Boot 版前端桥接测试：真实游戏页面 + Mock REST API。
// 运行：CHROMIUM_EXECUTABLE=... node tests/springboot-game.browser.cjs
const assert = require('node:assert/strict');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const {chromium} = require('playwright');

const root = path.resolve(__dirname, '..');
const bridge = fs.readFileSync(path.join(root, 'springboot-app', 'src', 'main', 'resources',
  'static', 'assets', 'js', 'game-bridge.js'), 'utf8');
const css = fs.readFileSync(path.join(root, 'springboot-app', 'src', 'main', 'resources',
  'static', 'assets', 'css', 'game-account.css'), 'utf8');
const original = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const injected = original
  .replace('</head>', '<link rel="stylesheet" href="/assets/css/game-account.css"></head>')
  .replace('</body>', `<script>
window.HUARONGDAO_SERVER={contextPath:'',loggedIn:false,username:null,csrfToken:'csrf-1'};
</script><script src="/assets/js/game-bridge.js" defer></script></body>`);

let lastAuth = null;
let lastScore = null;
let lastLogout = null;
let leaderboard = [
  {username: '关云长', totalPoints: 11840, completedPresets: 3, bestScore: 4280},
  {username: '华容老手', totalPoints: 7360, completedPresets: 2, bestScore: 3920}
];

function readBody(request) {
  return new Promise(resolve => {
    let body = '';
    request.on('data', chunk => body += chunk);
    request.on('end', () => resolve(body));
  });
}

const server = http.createServer(async (request, response) => {
  if (request.url === '/assets/css/game-account.css') {
    response.setHeader('Content-Type', 'text/css; charset=utf-8');
    response.end(css);
    return;
  }
  if (request.url === '/assets/js/game-bridge.js') {
    response.setHeader('Content-Type', 'application/javascript; charset=utf-8');
    response.end(bridge);
    return;
  }
  if (request.url === '/api/auth/register' && request.method === 'POST') {
    lastAuth = {body: JSON.parse(await readBody(request)), csrf: request.headers['x-csrf-token']};
    response.setHeader('Content-Type', 'application/json; charset=utf-8');
    response.end(JSON.stringify({ok: true, username: lastAuth.body.username, csrfToken: 'csrf-2'}));
    return;
  }
  if (request.url === '/api/leaderboard' && request.method === 'GET') {
    response.setHeader('Content-Type', 'application/json; charset=utf-8');
    response.end(JSON.stringify(leaderboard));
    return;
  }
  if (request.url === '/api/scores' && request.method === 'POST') {
    lastScore = {body: JSON.parse(await readBody(request)), csrf: request.headers['x-csrf-token']};
    leaderboard = [...leaderboard, {username: lastAuth.body.username, totalPoints: 2780, completedPresets: 1, bestScore: 2780}]
      .sort((a, b) => b.totalPoints - a.totalPoints);
    response.setHeader('Content-Type', 'application/json; charset=utf-8');
    response.end(JSON.stringify({ok: true, score: 2780, improved: true}));
    return;
  }
  if (request.url === '/api/auth/logout' && request.method === 'POST') {
    lastLogout = {csrf: request.headers['x-csrf-token']};
    response.setHeader('Content-Type', 'application/json; charset=utf-8');
    response.end(JSON.stringify({ok: true, username: null, csrfToken: 'csrf-3'}));
    return;
  }
  response.setHeader('Content-Type', 'text/html; charset=utf-8');
  response.end(injected);
});

(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_EXECUTABLE || undefined,
    headless: true
  });
  try {
    const context = await browser.newContext({viewport: {width: 1280, height: 900}, reducedMotion: 'reduce'});
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.goto(`http://127.0.0.1:${server.address().port}/`);
    await page.waitForSelector('.preset', {state: 'attached'});

    await page.locator('.server-account-toggle').click();
    await page.getByRole('menuitem', {name: '注册新账号'}).click();
    const form = page.locator('[data-auth-mode="register"]');
    await form.locator('[name="username"]').fill('Spring玩家');
    await form.locator('[name="password"]').fill('Password123!');
    await form.locator('[name="confirmPassword"]').fill('Password123!');
    await form.locator('button[type="submit"]').click();
    await page.waitForFunction(() => document.querySelector('.server-account-toggle')?.textContent.includes('Spring玩家'));
    assert.equal(lastAuth.csrf, 'csrf-1');
    assert.deepEqual(lastAuth.body, {
      username: 'Spring玩家', password: 'Password123!', confirmPassword: 'Password123!'
    });

    await page.locator('.server-account-toggle').click();
    await page.getByRole('menuitem', {name: '积分排行榜'}).click();
    await page.waitForSelector('.server-leaderboard-dialog[open]');
    assert.match(await page.locator('.server-leaderboard-dialog').innerText(), /关云长/);
    await page.locator('.server-leaderboard-dialog .server-auth-close').click();

    await page.evaluate(() => window.dispatchEvent(new CustomEvent('huarongdao:victory', {detail: {
      phase: 'won', mode: 'classic', preset: 0, moves: 81, seconds: 120
    }})));
    await page.waitForFunction(() => document.querySelector('.server-toast')?.textContent.includes('2780'));
    assert.equal(lastScore.csrf, 'csrf-2');
    assert.deepEqual(lastScore.body, {preset: 0, moves: 81, seconds: 120});

    await page.locator('.server-account-toggle').click();
    await page.getByRole('menuitem', {name: '积分排行榜'}).click();
    await page.waitForSelector('.server-leaderboard-dialog[open]');
    assert.match(await page.locator('.server-leaderboard-dialog tbody').innerText(), /Spring玩家\s+2780/);
    await page.locator('.server-leaderboard-dialog .server-auth-close').click();

    await page.locator('.server-account-toggle').click();
    await page.getByRole('menuitem', {name: '退出登录'}).click();
    await page.waitForFunction(() => document.querySelector('.server-menu')?.textContent.includes('注册新账号'));
    assert.equal(lastLogout.csrf, 'csrf-2');

    for (const width of [320, 375, 390, 768, 1280]) {
      await page.setViewportSize({width, height: 844});
      await page.waitForTimeout(50);
      const fits = await page.evaluate(() => [...document.querySelectorAll('.masthead a,.masthead button')]
        .filter(element => element.getBoundingClientRect().width)
        .every(element => {
          const bounds = element.getBoundingClientRect();
          return bounds.left >= 0 && bounds.right <= innerWidth;
        }));
      assert(fits, `Spring Boot account menu overflows at ${width}px`);
    }

    assert.deepEqual(errors, []);
    await context.close();
    console.log('PASS springboot-game.browser: modal auth, REST score, leaderboard, logout.');
  } finally {
    await browser.close();
    server.close();
  }
})().catch(error => {
  console.error(error);
  server.close();
  process.exitCode = 1;
});