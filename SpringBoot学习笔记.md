# Spring Boot 学习笔记

> 从一个真实的 Spring Boot 项目入手，把核心概念和代码对照着记下来。

---

## 一、项目的目录结构

```
my-app/
├── pom.xml                              ← 依赖清单
└── src/main/
    ├── java/com/example/demo/
    │   ├── DemoApplication.java         ← 启动入口
    │   ├── controller/
    │   │   └── UserController.java      ← ① 控制器
    │   ├── service/
    │   │   ├── UserService.java         ← ② 业务（接口）
    │   │   └── impl/
    │   │       └── UserServiceImpl.java ← ② 业务（实现）
    │   ├── mapper/
    │   │   └── UserMapper.java          ← ③ 持久层（MyBatis-Plus）
    │   ├── entity/
    │   │   └── User.java                ← 数据对象
    │   └── dto/
    │       └── UserRequest.java         ← 请求体对象
    └── resources/
        └── application.yml              ← 配置文件
```

三层分工速记：

| 目录 | 层 | 职责 |
| :--- | :--- | :--- |
| `controller/` | 控制层 | 接请求 |
| `service/` | 业务层 | 算逻辑 |
| `mapper/` | 持久层 | 跟数据库打交道 |

---

## 二、网址是怎么构成的

**完整网址 = 域名 + 路径**

你在浏览器看到的 `chat.deepseek.com`，其实只是**域名**（小区名字）。真正的完整地址是：

```
https://chat.deepseek.com/a/chat/s/xxxxx
└────┬────┘ └──────┬──────┘└──────┬──────┘
   协议          域名           路径
```

---

## 三、网址怎么映射到方法（Spring MVC）

### `@RequestMapping` —— 统一加前缀

`@RequestMapping` 给这个类里所有接口的地址，统一加个前缀，例如 `/api/users`。

### `@GetMapping` 就是 `@RequestMapping` 的马甲

`@GetMapping("/api/users")` 完全等于 `@RequestMapping(value = "/api/users", method = RequestMethod.GET)`。

Spring 把常用动作都做了简写，全是 `@RequestMapping` 的马甲：

| 简写 | 等于 | 动作 |
| :--- | :--- | :--- |
| `@GetMapping` | `@RequestMapping(method = GET)` | 查 |
| `@PostMapping` | `@RequestMapping(method = POST)` | 增 |
| `@PutMapping` | `@RequestMapping(method = PUT)` | 改 |
| `@DeleteMapping` | `@RequestMapping(method = DELETE)` | 删 |

> 它们全都是 `@RequestMapping` 换了个 `method` 参数而已。

### 参数绑定

- `@PathVariable`：从 **URL 路径**里抠值（如 `/users/1` 里的 `1`）。
- `@RequestBody`：把**请求体里的 JSON** 整段转成 Java 对象（如 `{"name":"Tom"}` → `UserRequest`）。

---

## 四、DispatcherServlet —— 总调度台

`DispatcherServlet` = **总调度台**。所有请求先进它，它查映射表，把请求分派给对应的 Controller，再把结果返回。

```
你启动项目
   ↓
spring-boot-starter-web 自动把 DispatcherServlet 注册好
   ↓
它扫描到你 Controller 上的 @RequestMapping / @GetMapping
   ↓
自动建一张映射表：
   GET    /api/users/{id}   → UserController.getById
   POST   /api/users        → UserController.create
   DELETE /api/users/{id}   → UserController.delete
   ↓
请求一来，它查表，把请求转给对应方法
```

> **Spring MVC** 就是 Spring 里专门负责「处理 Web 请求」的那一整套机制。

---

## 五、Spring 核心：IoC 容器

### 对象是怎么「造」和「拿」的

```
      @Service                          @Autowired
    ┌──────────────┐                 ┌──────────────┐
    │ UserServiceImpl│ ──存──▶ 仓库 ──取──▶ │ userService  │
    └──────────────┘                 └──────────────┘
        造对象                          拿对象
```

- `@Service`：把这个类交给 Spring 管，启动时**造好对象存进容器**（存货）。
- `@Transactional`：给这个方法套个自动开关，**成功自动提交、异常自动回滚**（套事务）。

### IoC 容器是什么

Spring 的 IoC 容器（Inversion of Control Container）。从数据和结构的角度看，它本质上就是一个**管理所有 Bean 实例的、支持按类型查找和注入的「超级 Map」**。

Service 不再主动创建 `AccountDao`，而是**被动等待容器把依赖塞进来**。控制权从我（调用方）转移到了容器（框架）手里。

> 所以「反转」的是**获取依赖这件事的控制权**：从「我主动去拿 / 造」变成「你（容器）主动给我」。

---

## 六、AOP：面向切面编程

**「面向切面」** = 把日志、事务、权限这些「横跨所有类的杂活」**横着切出来，统一套**。

- 面向对象 = Object-Oriented → 按「对象」组织代码
- 面向切面 = Aspect-Oriented → 按「横切关注点」组织代码

---

## 七、MyBatis-Plus：持久层

### `BaseMapper` —— 增删改查白送

`BaseMapper` = MyBatis-Plus 的通用 Mapper 接口。继承它，**单表增删改查白送**，不用写 SQL、不用写实现类。

```java
public interface UserMapper extends BaseMapper<User>
```

> `UserMapper` 是管用户表的「仓库管理员」，它继承了 `BaseMapper`，所以自带**走一批货（`selectList`）、取一件（`selectById`）、存货（`insert`）**等全套动作。

同理：

```java
public interface UserService extends IService<User>
```

### MyBatis-Plus 是什么

MyBatis-Plus 是 MyBatis 的「增强版工具包」：原本 MyBatis 要你手写 SQL，它帮你把**单表增删改查全自动生成**，你只写复杂 SQL 就行。

Java 想查数据，必须经过四步：

```
连上数据库 → 发 SQL → 把结果拿回来 → 转成 Java 对象
```

---

## 八、一个更正

> **更正**：Spring MVC 管「怎么把返回值变成 HTTP 响应」——**JSON 只是默认的一种，不是唯一**。但老师说得都对。
