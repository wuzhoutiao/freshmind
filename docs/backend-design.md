# FreshMind 一体化网站设计说明

## 1. 设计目标

FreshMind 是面向校招/初级后端岗位的学习项目。目标是让业务完整、代码容易阅读、每项技术都有明确理由，并提供一个可以直接操作的浏览器页面，而不是模拟并不存在的大规模流量。

核心链路：

```text
打开网站 → 注册/登录 → 新增库存 → 计算临期状态 → 生成菜谱候选 → Java 校验和重排 → 临期提醒
```

## 2. 分层

- Web 页面：原生 HTML、CSS、JavaScript，由 Spring Boot 静态资源目录直接托管。
- Controller：接收 HTTP 参数，不编写业务规则。
- Service：处理库存、保质期、推荐和提醒逻辑，并划分事务。
- Repository：使用 JPA 访问 MySQL。
- AI Client：通过 Spring `RestClient` 调用通义千问兼容接口，只负责生成结构化候选。
- Redis：保存登录会话、只读目录缓存和 AI 候选缓存。
- Scheduler：每天触发临期扫描，将提醒任务交给 JUC 线程池。

页面和 API 使用同一来源，浏览器以相对路径调用 `/api/v1/**`，因此不需要单独部署前端、配置 CORS 或引入 Node.js 构建链。桌面端使用侧边导航，窄屏下切换为移动端导航；库存表单、消耗、删除和菜谱详情均在页面内完成。

## 3. 数据模型

| 表 | 用途 |
| --- | --- |
| `user_accounts` | 用户账号及 BCrypt 密码摘要 |
| `ingredient_catalog` | 食材分类和默认保质期 |
| `inventory_items` | 用户库存、日期、数量和版本号 |
| `expiry_notifications` | 用户的站内临期提醒 |

菜谱由模型按需生成，不再保存 `recipes` 和 `recipe_ingredients`。旧数据库不会被 Hibernate `ddl-auto=update` 自动清理，可在备份后选择执行 `remove-legacy-recipe-tables.sql`。

所有用户库存查询都包含用户 ID。即使知道其他用户的库存 UUID，也无法读取或修改。

## 4. AI 推荐链路

### 4.1 输入收敛

推荐服务只读取当前用户的库存，并依次过滤：

1. 数量必须大于 0；
2. 过期食材不参与；
3. 名称必须能命中 `ingredient_catalog`；
4. 发送给模型时使用目录中的标准名称。

这样既减少无意义输入，也避免把任意自定义食材名称直接当成模型指令。食材数据仍会发往外部模型，因此产品层面需要明确告知用户。

### 4.2 结构化生成

`DashScopeRecipeGenerationClient` 请求 `/chat/completions`，使用 `response_format=json_object`，默认让 `qwen-plus` 生成 8 个候选。连接和读取共用可配置超时，不记录请求头、密钥或上游响应正文。

模型只负责给出名称、描述、烹饪时间、食材和步骤，不提供可信的命中率或业务分数。

### 4.3 白名单校验

`GeneratedRecipeValidator` 对整道菜做原子校验：

- 食材必须属于目录且不能重复；
- 数量必须为正并有上限，单位不能为空；
- 烹饪时间限定在 1～180 分钟；
- 每道菜要求 2～12 种食材、2～10 个非空步骤；
- 名称、描述、单位和步骤均限制长度。

任一字段不合法就丢弃整道候选，避免只修补局部后留下语义不一致的数据。

### 4.4 Java 重排

只使用未过期库存计算：

```text
matchRate = 已拥有的候选食材数 / 候选食材总数
score = matchRate + 临期命中数 × 0.20
```

匹配率低于 60% 的候选不返回。结果按 `score`、`matchRate`、名称稳定排序，并返回命中、缺失和临期食材，推荐原因可以解释，也不能被模型伪造。

### 4.5 缓存与降级

候选缓存键：

```text
ownerId : inventoryFingerprint : model : promptVersion : catalogFingerprint
```

库存指纹按稳定顺序对名称、数量、单位、到期日和临期状态做 SHA-256。接口 `limit` 不进入键，因此同一批候选可裁剪成不同数量而不重复调用模型。Redis TTL 为 15 分钟。

模型未配置、超时、HTTP 错误或 Redis 访问异常时，推荐服务使用当前目录食材生成简单降级菜谱；格式错误或全部候选未通过校验时也走同一降级。异常消息和日志不包含 API key。

Dashboard 不同步调用推荐服务，避免首页首屏受模型延迟影响；浏览器进入智能菜谱页面后再单独请求 `/recommendations`，并展示独立加载状态。

## 5. Redis 会话

登录成功后生成不可预测的随机 token：

```text
key:   session:{token}
value: userId
ttl:   2 hours
```

`SessionAuthenticationInterceptor` 从 `X-Token` 读取 token，并把 userId 写入 request attribute。Controller 不直接操作 Redis。测试和 local profile 使用内存实现。

## 6. MySQL 与并发

写操作使用 Spring 事务。库存实体带有 `@Version` 字段，Hibernate 更新时会同时校验版本：

```sql
UPDATE inventory_items
SET quantity = ?, version = version + 1
WHERE id = ? AND version = ?;
```

更新行数为 0 表示另一个事务已经修改数据，接口返回冲突提示。它解决的是数据库并发写入，不是 JVM 线程互斥。

## 7. JUC 临期任务

`ExpiryNotificationJob` 每天 08:00 扫描当天至未来两天内到期的库存。任务先把 JPA 实体转换为简单候选数据，再提交给线程池，避免在线程间传递持久化上下文。

- corePoolSize：2
- maximumPoolSize：4
- workQueue：`ArrayBlockingQueue(100)`
- rejectionHandler：`CallerRunsPolicy`
- keepAlive：60 秒

每个提醒写入独立事务。数据库唯一约束是最后一道幂等保护，多次扫描或并发插入都不会产生重复提醒。

## 8. 有意不做的事情

- 不保留 Android 客户端：网站已经覆盖完整操作链路，避免维护两套界面和两套数据访问逻辑。
- 不引入 React/Vue：当前交互规模用原生 HTML/CSS/JavaScript 即可，减少与后端主线无关的工具链。
- 不称为 Agent：没有自主规划、工具循环或多步决策，结构化单次调用更简单、更可测试。
- 不建菜谱库：候选按库存动态生成，MySQL 只保存稳定业务数据。
- 不拆微服务：当前模块边界和数据量不足以支撑拆分成本。
- 不加入 Netty：普通管理接口由 Spring MVC/Tomcat 处理即可。
- 不加入 Dubbo/ZooKeeper：没有跨服务 RPC 和服务发现需求。
- 不缓存用户库存对象：避免缓存与数据库双写一致性问题，只缓存库存摘要对应的生成结果。
- 不声称“高并发”：线程池用于批量提醒，不代表系统经过高并发压测。

