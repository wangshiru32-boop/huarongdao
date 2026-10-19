# 华容道

> **来源说明**：本项目改造自上游仓库 https://github.com/DTW7607/DTW7607.github.io （作者 DTW7607），在此基础上继续加功能。
> 上游仓库未声明整体开源许可证，THIRD_PARTY_NOTICES.md 中只包含经典阵型资料的第三方 MIT 许可；公开分发前请先确认授权。
> 已把上游配置为 git remote upstream，同步方式：git fetch upstream 后执行 git log --oneline HEAD..upstream/main，再挑选需要的提交（git cherry-pick）。
>


支持经典阵型、自定义布阵、后台提示、惯性滑动、连锁推动和持续转向。

在线游玩：

- https://dtw7607.github.io/
- https://dtw7607.github.io/huarongdao/

打开 index.html 即可离线游玩；全部页面、样式与求解线程包含在单文件内。

经典阵型资料的许可说明见 THIRD_PARTY_NOTICES.md。

## 中英文支持

两个入口均在页首提供「中文 / English」按钮，可通过 Tab、Enter 或空格操作。
首次访问按浏览器语言优先顺序选择中文或英文（支持区域变体）；没有匹配语言时显示中文。
手动选择保存到 `localStorage` 的 `huarongdao.language`，刷新及两个入口之间导航后保留。
浏览器禁止存储时仍可即时切换，但无法跨刷新保存偏好。切换不会重置棋局。

## 触发区域检查页

访问 https://dtw7607.github.io/test/ 查看三角形箭头的真实触发区域。
各彩色区域对应同色箭头，区域边界直接使用游戏的点击判定坐标；提示动画不改变边界。
其他功能与根路径一致，包括中英文切换、布阵、拖动、惯性滑动、撤销和提示。
覆盖层不接收鼠标或触摸事件，布阵、拖动中及胜利时自动隐藏。

## 验证

页面无需构建，也不依赖新增运行时包。开发检查可安装测试依赖后执行：

```sh
npm install --no-save jsdom playwright
node tests/english-support.cjs
npx playwright install chromium
node tests/english-support.browser.cjs
node tests/hit-regions.browser.cjs
```

也可用 `CHROMIUM_EXECUTABLE` 指定本地 Chromium；`SCREENSHOT_DIR` 指定已存在的截图目录。
具体覆盖范围和验证限制见 [VALIDATION.md](VALIDATION.md)。

## Spring Boot 账号、积分与排行榜版

`springboot-app/` 提供 Spring Boot 3 实现：

- 游戏页内的登录、注册、退出和排行榜弹窗；
- Spring MVC REST API；
- Spring JDBC + H2 文件数据库；
- BCrypt 密码哈希、Session 轮换和 CSRF 防护；
- 服务端计算积分，每个经典阵型只保留最高分；
- Spring Boot 真实 HTTP 集成测试和游戏页浏览器测试。

原 `index.html`、`huarongdao/index.html`、`test/index.html` 未改动。

运行方式：

```powershell
cd springboot-app
mvn spring-boot:run
```

访问 `http://localhost:8080/huarongdao/`。详细说明见 [springboot-app/README.md](springboot-app/README.md)。
