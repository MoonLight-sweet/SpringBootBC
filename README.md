# CodeClinic编程错题智能解析助手

　　这是一个可直接运行的Spring Boot单体应用，包含REST API和桌面端中文Web工作台。工作台提供浅色与深色两套界面，不包含移动端应用或移动端专用导航。项目只使用MySQL，不包含H2依赖或本地文件数据库配置。默认连接本机MySQL的`code_clinic`数据库；默认开启本地模拟AI，配置`DEEPSEEK_API_KEY`并关闭`AI_MOCK_ENABLED`后即可调用DeepSeek兼容接口。

## 启动

```powershell
mvn spring-boot:run
```

打开<http://localhost:8080>。

## MySQL配置

```powershell
$env:MYSQL_URL="jdbc:mysql://localhost:3306/code_clinic?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="你的密码"
mvn spring-boot:run
```

　　启动前通过`MYSQL_USERNAME`和`MYSQL_PASSWORD`设置本机MySQL账号，密码不会写入项目文件。首次启动前需要创建数据库：

```sql
CREATE DATABASE code_clinic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## AI配置

```powershell
$env:AI_MOCK_ENABLED="false"
$env:DEEPSEEK_API_KEY="你的密钥"
$env:DEEPSEEK_MODEL="deepseek-chat"
```

　　核心接口包括用户注册登录、找回密码、错题增删改查、分页搜索、AI分析、重新分析、分析历史、知识点列表和学习统计。所有需要登录的接口使用`Authorization: Bearer <token>`，错题查询会按当前用户隔离。

## 找回密码配置

　　找回密码采用注册邮箱验证码，验证码的加密结果保存在MySQL中，10分钟后失效，成功重置后立即作废。默认开启开发演示模式，验证码会显示在找回密码页面，便于本机调试。

　　接入真实邮箱时关闭开发演示模式，并配置SMTP邮箱连接：

```powershell
$env:PASSWORD_RESET_DEMO_ENABLED="false"
$env:SPRING_MAIL_HOST="smtp.example.com"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="你的邮箱账号"
$env:SPRING_MAIL_PASSWORD="你的邮箱授权码"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE="true"
$env:MAIL_FROM="你的发件邮箱"
mvn spring-boot:run
```

## 用户分类与权限分布

　　当前系统按登录状态分为2类用户，不设置管理员角色。后端统一检查登录凭证，错题、分析和统计数据按当前登录用户隔离。

|用户类别|身份说明|可访问功能|不可访问功能|数据范围|
|---|---|---|---|---|
|未登录访客|尚未登录或登录凭证已失效|访问首页静态资源、注册、登录、申请密码验证码、重置密码|个人资料、错题管理、AI分析、分析历史、知识点、学习统计|无个人业务数据访问权|
|已登录用户|持有有效登录凭证的注册用户|个人资料、错题新增与查询、修改与删除、AI分析与重新分析、分析历史、知识点、学习统计、退出登录|其他用户的个人数据和错题数据|仅限本人数据|

|接口范围|未登录访客|已登录用户|权限实现方式|
|---|---:|---:|---|
|`/`、`/index.html`、`/assets/**`|允许|允许|直接开放页面和静态资源|
|`POST /api/user/register`|允许|允许|直接开放注册接口|
|`POST /api/user/login`|允许|允许|校验用户名和密码后签发登录凭证|
|`POST /api/user/password/reset-code`|允许|允许|校验注册邮箱并生成一次性验证码|
|`POST /api/user/password/reset`|允许|允许|校验邮箱、验证码和有效期后更新密码|
|`GET /api/user/info`|禁止|允许|请求必须携带有效登录凭证|
|`/api/wrong-question/**`|禁止|允许|登录后访问，并按当前用户编号过滤数据|
|`/api/analysis/**`|禁止|允许|登录后访问，并通过错题归属限制数据|
|`/api/knowledge-point/**`|禁止|允许|登录后访问|
|`/api/statistics/**`|禁止|允许|登录后访问并统计本人数据|

## GitHub自动同步

　　项目已绑定`https://github.com/MoonLight-sweet/SpringBootBC.git`。本地Git已配置提交后自动执行推送；Git传输线路不可用时，会使用Windows凭据管理器中的登录状态通过GitHub官方API同步，不在项目中保存访问令牌。

　　Windows登录后会启动`CodeClinic-GitHub-AutoSync`计划任务。项目文件稳定20秒后自动运行测试，测试通过则创建提交并推送到当前GitHub分支；测试失败时保留修改但不提交。运行日志保存在`output/auto-sync/auto-sync.log`。
