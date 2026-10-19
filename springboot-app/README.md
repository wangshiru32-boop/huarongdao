# 华容道 Spring Boot 版

这个目录把原先的 JSP 实现改成了 **Spring Boot 3 + Spring MVC + Spring JDBC + H2**。原静态游戏 `index.html` 没有改动，Spring Boot 只在返回页面时注入账号菜单和桥接脚本。

## 当前功能

- 游戏内登录/注册弹窗，不跳转独立页面
- BCrypt 密码哈希
- Session 登录态与 HttpOnly、SameSite=Lax Cookie
- 自定义 CSRF Token，所有 POST API 校验
- H2 文件数据库，首次启动自动建表
- 通关经典阵型后提交成绩，服务端计算积分
- 游戏内积分排行榜弹窗
- 每个用户、每个经典阵型只保留最高分
- 真实 Spring Boot HTTP 集成测试
- 真实游戏页面 + Mock API 浏览器测试

## 技术结构

```text
浏览器 / 游戏页
  -> GamePageController          返回注入后的 index.html
  -> game-bridge.js              游戏内弹窗与 fetch
  -> AuthController              /api/auth/**
  -> ScoreController             /api/scores, /api/leaderboard
  -> AuthService / ScoreService  业务规则
  -> UserRepository / ScoreRepository
  -> JdbcTemplate
  -> H2 文件数据库
```

## 运行

要求 JDK 21+、Maven 3.9+。

```powershell
cd D:\华容道\springboot-app
mvn spring-boot:run
```

或者：

```powershell
cd D:\华容道\springboot-app
mvn clean package
java -jar target\huarongdao-springboot-1.0.0.jar
```

访问：

```text
http://localhost:8080/huarongdao/
```

右上角“账户”即可完成注册、登录、排行榜和退出。

数据库默认保存到：

```text
D:\华容道\springboot-app\data\huarongdao.mv.db
```

停止应用后删除该文件即可清空本地账号和成绩。

## REST API

| 方法 | 路径 | 作用 |
|---|---|---|
| POST | `/api/auth/register` | 注册并登录 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 退出并轮换 Session |
| GET | `/api/auth/session` | 读取当前登录态 |
| POST | `/api/scores` | 提交通关原始数据并计算积分 |
| GET | `/api/leaderboard` | 读取排行榜 |

所有 POST API 都需要请求头：

```text
X-CSRF-Token: 页面初始化时注入的 token
```

## 代码阅读顺序

1. `HuarongdaoApplication.java`：Spring Boot 启动入口。
2. `game/GamePageController.java`：游戏页面入口。
3. `game/GamePageRenderer.java`：读取原游戏 HTML 并注入配置。
4. `static/assets/js/game-bridge.js`：游戏内登录/注册/排行榜弹窗和 fetch。
5. `auth/AuthController.java`：认证 REST API。
6. `auth/AuthService.java`、`auth/PasswordConfig.java`：BCrypt 注册登录规则。
7. `auth/SessionAuth.java`、`auth/CsrfService.java`：Session 轮换与 CSRF。
8. `score/ScoreController.java`、`score/ScoreService.java`：记分与排行榜业务。
9. `score/ScoreRepository.java`：最佳成绩和排行榜 SQL。
10. `web/CsrfInterceptor.java`、`web/GlobalExceptionHandler.java`：统一安全和错误处理。

## 验证

```powershell
cd D:\华容道\springboot-app
mvn clean test
```

`AuthFlowTest` 会真正启动随机端口上的 Spring Boot，验证：

```text
读取游戏页 -> 注册 -> 查 Session -> 提交成绩 -> 排行榜 -> 退出 -> 再登录 -> 错误 CSRF 被拒绝
```

在仓库根目录执行前端浏览器测试：

```powershell
$env:CHROMIUM_EXECUTABLE='C:\Program Files\Google\Chrome\Application\chrome.exe'
node tests/springboot-game.browser.cjs
```

它会使用真实游戏页面和 Mock REST API 验证游戏内注册、排行榜、通关记分和退出流程。

## 安全说明

已经实现 BCrypt、Session 轮换、CSRF、HttpOnly/SameSite Cookie、参数校验、PreparedStatement/JdbcTemplate 和统一异常处理。

正式上线前仍建议补充：

- HTTPS
- 登录失败限流
- 服务端回放操作序列，进一步防伪造成绩
- 数据库迁移工具和连接池监控
- 日志脱敏、备份和隐私政策