/*
 * 华容道 Spring Boot 账号桥接脚本。
 *
 * 功能：
 * 1. 在游戏页右上角创建账户菜单；
 * 2. 登录、注册、排行榜全部使用游戏内弹窗，不跳转独立页面；
 * 3. 通过 fetch 调用 Spring Boot REST API；
 * 4. 通关经典阵型后提交阵型、步数和用时，由服务端计算积分。
 */
(() => {
  'use strict';

  const config = window.HUARONGDAO_SERVER;
  if (!config || typeof config.contextPath !== 'string') return;

  const basePath = config.contextPath.replace(/\/$/, '');
  let toastTimer = null;
  let lastSubmittedWin = '';
  let closeAccountMenu = () => {};
  let authDialogState = null;
  let leaderboardDialogState = null;

  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text) node.textContent = text;
    return node;
  }

  function showToast(message, isError = false) {
    let toast = document.querySelector('.server-toast');
    if (!toast) {
      toast = element('div', 'server-toast');
      toast.setAttribute('role', 'status');
      document.body.append(toast);
    }
    toast.textContent = message;
    toast.classList.toggle('error', isError);
    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { toast.hidden = true; }, 4200);
  }

  function menuButton(label, action) {
    const button = element('button', '', label);
    button.type = 'button';
    button.setAttribute('role', 'menuitem');
    button.addEventListener('click', action);
    return button;
  }

  function createAccountMenu() {
    const actions = document.querySelector('.masthead .header-actions');
    if (!actions) return;
    const existing = actions.querySelector('.server-account');
    if (existing) existing.remove();

    const wrapper = element('div', 'server-account');
    const toggle = element('button', 'quiet-button server-account-toggle');
    toggle.type = 'button';
    toggle.setAttribute('aria-haspopup', 'true');
    toggle.setAttribute('aria-expanded', 'false');
    toggle.setAttribute('aria-label', config.loggedIn ? '账号菜单' : '登录与注册');

    const icon = element('span', 'account-icon', '人');
    icon.setAttribute('aria-hidden', 'true');
    const name = element('span', 'account-name',
      config.loggedIn && config.username ? config.username : '账户');
    toggle.append(icon, name);

    const menu = element('div', 'server-menu');
    menu.hidden = true;
    menu.setAttribute('role', 'menu');
    if (config.loggedIn) {
      menu.append(element('div', 'server-menu-user', `已登录：${config.username}`));
    }

    menu.append(menuButton('积分排行榜', () => openLeaderboardDialog()));
    if (config.loggedIn) {
      menu.append(menuButton('退出登录', () => {
        closeAccountMenu();
        logout();
      }));
    } else {
      menu.append(
        menuButton('登录', () => openAuthDialog('login')),
        menuButton('注册新账号', () => openAuthDialog('register'))
      );
    }

    const setOpen = (open) => {
      menu.hidden = !open;
      toggle.setAttribute('aria-expanded', String(open));
    };
    closeAccountMenu = () => setOpen(false);
    toggle.addEventListener('click', event => {
      event.stopPropagation();
      setOpen(menu.hidden);
    });
    menu.addEventListener('click', event => event.stopPropagation());

    wrapper.append(toggle, menu);
    actions.append(wrapper);
  }

  function labeledInput(labelText, name, type, autocomplete, minLength = 0) {
    const label = element('label', 'server-auth-field');
    const input = element('input');
    input.name = name;
    input.type = type;
    input.autocomplete = autocomplete;
    if (minLength) input.minLength = minLength;
    input.required = true;
    label.append(element('span', '', labelText), input);
    return {label, input};
  }

  function submitButton(label) {
    const button = element('button', 'server-auth-submit', label);
    button.type = 'submit';
    return button;
  }

  function createAuthDialog() {
    if (authDialogState) return authDialogState;

    const dialog = element('dialog', 'server-auth-dialog');
    dialog.id = 'server-auth-dialog';
    dialog.setAttribute('aria-labelledby', 'server-auth-title');

    const close = element('button', 'server-auth-close', '×');
    close.type = 'button';
    close.setAttribute('aria-label', '关闭账号窗口');
    close.addEventListener('click', () => dialog.close());

    const eyebrow = element('p', 'server-auth-eyebrow', '华容道 · 玩家账号');
    const title = element('h2', 'server-auth-title', '登录或注册');
    title.id = 'server-auth-title';
    const note = element('p', 'server-demo-note', '登录后，通关成绩会保存到服务器排行榜。');

    const tabs = element('div', 'server-auth-tabs');
    tabs.setAttribute('role', 'tablist');
    const loginTab = element('button', '', '登录');
    loginTab.type = 'button';
    loginTab.setAttribute('role', 'tab');
    const registerTab = element('button', '', '注册');
    registerTab.type = 'button';
    registerTab.setAttribute('role', 'tab');
    tabs.append(loginTab, registerTab);

    const message = element('p', 'server-auth-message');
    message.hidden = true;
    message.setAttribute('role', 'alert');

    const loginForm = element('form', 'server-auth-form');
    loginForm.dataset.authMode = 'login';
    const loginUsername = labeledInput('用户名', 'username', 'text', 'username');
    const loginPassword = labeledInput('密码', 'password', 'password', 'current-password', 8);
    loginForm.append(loginUsername.label, loginPassword.label, submitButton('登录'));

    const registerForm = element('form', 'server-auth-form');
    registerForm.dataset.authMode = 'register';
    const registerUsername = labeledInput('用户名', 'username', 'text', 'username', 3);
    registerUsername.input.maxLength = 20;
    const registerPassword = labeledInput('密码', 'password', 'password', 'new-password', 8);
    const confirmPassword = labeledInput('确认密码', 'confirmPassword', 'password', 'new-password', 8);
    registerForm.append(registerUsername.label, registerPassword.label, confirmPassword.label,
      submitButton('创建账号'));

    dialog.append(close, eyebrow, title, note, tabs, message, loginForm, registerForm);
    document.body.append(dialog);

    const setMode = mode => {
      const login = mode !== 'register';
      loginTab.setAttribute('aria-selected', String(login));
      registerTab.setAttribute('aria-selected', String(!login));
      loginForm.hidden = !login;
      registerForm.hidden = login;
      message.hidden = true;
      dialog.dataset.mode = login ? 'login' : 'register';
      const scheduleFocus = typeof requestAnimationFrame === 'function'
        ? requestAnimationFrame : callback => setTimeout(callback, 0);
      scheduleFocus(() => (login ? loginUsername.input : registerUsername.input).focus());
    };

    loginTab.addEventListener('click', () => setMode('login'));
    registerTab.addEventListener('click', () => setMode('register'));
    loginForm.addEventListener('submit', event => submitAuth(event, 'login'));
    registerForm.addEventListener('submit', event => submitAuth(event, 'register'));
    dialog.addEventListener('click', event => {
      if (event.target === dialog) dialog.close();
    });
    dialog.addEventListener('close', () => {
      loginForm.reset();
      registerForm.reset();
      message.hidden = true;
    });

    authDialogState = {dialog, message, setMode};
    return authDialogState;
  }

  function showAuthMessage(message) {
    const node = createAuthDialog().message;
    node.textContent = message;
    node.classList.add('error');
    node.hidden = false;
  }

  function openAuthDialog(mode = 'login') {
    const state = createAuthDialog();
    state.setMode(mode);
    closeAccountMenu();
    if (!state.dialog.open) state.dialog.showModal();
  }

  async function submitAuth(event, mode) {
    event.preventDefault();
    const form = event.currentTarget;
    const submit = form.querySelector('button[type="submit"]');
    const data = new FormData(form);
    const body = {
      username: String(data.get('username') || '').trim(),
      password: String(data.get('password') || '')
    };
    if (mode === 'register') body.confirmPassword = String(data.get('confirmPassword') || '');
    submit.disabled = true;

    try {
      const response = await fetch(`${basePath}/api/auth/${mode}`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'X-CSRF-Token': config.csrfToken
        },
        body: JSON.stringify(body)
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok || !payload.ok) {
        throw new Error(payload.message || `操作失败（HTTP ${response.status}）`);
      }
      config.loggedIn = true;
      config.username = payload.username;
      config.csrfToken = payload.csrfToken;
      createAuthDialog().dialog.close();
      createAccountMenu();
      showToast(mode === 'login'
        ? `登录成功，欢迎 ${payload.username}。`
        : `注册成功，欢迎 ${payload.username}。`);
    } catch (error) {
      showAuthMessage(error.message || '操作失败，请稍后重试。');
    } finally {
      submit.disabled = false;
    }
  }

  async function logout() {
    try {
      const response = await fetch(`${basePath}/api/auth/logout`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Accept': 'application/json',
          'X-CSRF-Token': config.csrfToken
        }
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok || !payload.ok) {
        throw new Error(payload.message || `退出失败（HTTP ${response.status}）`);
      }
      config.loggedIn = false;
      config.username = null;
      config.csrfToken = payload.csrfToken;
      createAccountMenu();
      showToast('已退出登录。');
    } catch (error) {
      showToast(error.message || '退出登录失败，请稍后重试。', true);
    }
  }

  function createLeaderboardDialog() {
    if (leaderboardDialogState) return leaderboardDialogState;

    const dialog = element('dialog', 'server-leaderboard-dialog');
    dialog.setAttribute('aria-labelledby', 'server-leaderboard-title');
    const close = element('button', 'server-auth-close', '×');
    close.type = 'button';
    close.setAttribute('aria-label', '关闭排行榜');
    close.addEventListener('click', () => dialog.close());

    const eyebrow = element('p', 'server-auth-eyebrow', '经典阵型 · 最佳成绩累计');
    const title = element('h2', 'server-auth-title', '积分排行榜');
    title.id = 'server-leaderboard-title';
    const note = element('p', 'server-demo-note', '每个阵型只保留最高分，总积分为各阵型最高分之和。');
    const message = element('p', 'server-auth-message');
    message.hidden = true;
    const tableWrap = element('div', 'server-leaderboard-table-wrap');
    const table = element('table', 'server-leaderboard-table');
    const thead = element('thead');
    const headRow = element('tr');
    ['名次', '玩家', '总积分', '阵型', '最高分'].forEach(label => headRow.append(element('th', '', label)));
    thead.append(headRow);
    const tbody = element('tbody');
    table.append(thead, tbody);
    tableWrap.append(table);
    dialog.append(close, eyebrow, title, note, message, tableWrap);
    document.body.append(dialog);

    dialog.addEventListener('click', event => {
      if (event.target === dialog) dialog.close();
    });

    leaderboardDialogState = {dialog, tbody, message};
    return leaderboardDialogState;
  }

  async function openLeaderboardDialog() {
    const state = createLeaderboardDialog();
    closeAccountMenu();
    state.message.hidden = true;
    state.tbody.replaceChildren(element('tr', '', '加载中…'));
    if (!state.dialog.open) state.dialog.showModal();
    try {
      const response = await fetch(`${basePath}/api/leaderboard`, {
        credentials: 'same-origin',
        headers: {'Accept': 'application/json'}
      });
      if (!response.ok) throw new Error(`读取排行榜失败（HTTP ${response.status}）`);
      const rows = await response.json();
      state.tbody.replaceChildren();
      if (!rows.length) {
        state.message.textContent = '还没有成绩，完成第一局吧。';
        state.message.hidden = false;
        return;
      }
      rows.forEach((row, index) => {
        const tr = element('tr');
        tr.append(
          element('td', 'server-rank', String(index + 1)),
          element('td', '', row.username),
          element('td', 'server-points', String(row.totalPoints)),
          element('td', '', `${row.completedPresets} / 8`),
          element('td', '', String(row.bestScore))
        );
        state.tbody.append(tr);
      });
    } catch (error) {
      state.message.textContent = error.message || '读取排行榜失败。';
      state.message.hidden = false;
    }
  }

  async function submitScore(detail) {
    if (!detail || detail.phase !== 'won') return;
    if (detail.mode !== 'classic' || !Number.isInteger(detail.preset)) return;
    const signature = `${detail.preset}:${detail.moves}:${detail.seconds}`;
    if (signature === lastSubmittedWin) return;
    lastSubmittedWin = signature;

    if (!config.loggedIn || !config.csrfToken) {
      showToast('请从右上角账户菜单登录或注册，成绩才能进入排行榜。');
      return;
    }

    try {
      const response = await fetch(`${basePath}/api/scores`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'X-CSRF-Token': config.csrfToken
        },
        body: JSON.stringify({
          preset: detail.preset,
          moves: Number(detail.moves),
          seconds: Number(detail.seconds)
        })
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.message || `保存成绩失败（HTTP ${response.status}）`);
      }
      showToast(payload.improved
        ? `成绩已保存：本阵型 ${payload.score} 分。`
        : `本次得分 ${payload.score}，未超过该阵型原来的最佳成绩。`);
    } catch (error) {
      showToast(error.message || '保存成绩失败，请稍后重试。', true);
    }
  }

  document.addEventListener('click', () => closeAccountMenu());
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') closeAccountMenu();
  });

  createAccountMenu();
  window.addEventListener('huarongdao:victory', event => submitScore(event.detail));
})();