# CodeClinic编程错题智能解析助手

　　这是一个可直接运行的Spring Boot单体应用，包含REST API和桌面端中文Web工作台。工作台提供浅色与深色两套界面，不包含移动端应用或移动端专用导航。项目只使用MySQL，不包含H2依赖或本地文件数据库配置。默认连接本机MySQL的`code_clinic`数据库，并通过Ollama调用本机`qwen3:8b`生成真实AI分析；也可以通过环境变量切换到DeepSeek兼容接口。

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

　　启动前必须通过`MYSQL_USERNAME`和`MYSQL_PASSWORD`设置MySQL账号，项目不再提供固定的root口令。首次启动前需要创建数据库：

```sql
CREATE DATABASE code_clinic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

　　首次启动会执行`schema-mysql.sql`建表。建表完成后，可改用仅有本库数据读写权限的账号，并设置`SQL_INIT_MODE=never`，避免运行期间继续要求建表权限。为保证登录状态在服务重启后继续有效，应设置至少32字节的随机`JWT_SECRET`；未设置时系统每次启动生成临时密钥，重启会使已有登录状态失效。

## AI配置

```powershell
$env:DEEPSEEK_BASE_URL="https://api.deepseek.com"
$env:DEEPSEEK_API_KEY="你的密钥"
$env:DEEPSEEK_MODEL="deepseek-chat"
$env:AI_MOCK_ENABLED="false"
```

　　核心接口包括用户注册登录、找回密码、错题增删改查、分页搜索、AI分析、重新分析、分析历史、分析结果人工核验、错题复习记录、知识点列表和学习统计。所有需要登录的接口使用`Authorization: Bearer <token>`，错题查询会按当前用户隔离。

## 找回密码配置

　　找回密码采用注册邮箱验证码。系统生成6位验证码后，通过配置的发件邮箱发送到用户注册邮箱；数据库只保存验证码的加密结果，验证码不会出现在接口响应、页面或日志中。验证码10分钟后失效，成功重置后立即作废。

　　本机直接运行时，可以在找回密码页打开配置区域。启动前先设置至少32字节的随机`MAIL_SETUP_TOKEN`，然后填写管理口令、QQ/网易/Gmail/Outlook发件邮箱及其SMTP授权码。系统先连接邮箱服务器验证配置，再发送验证码。发件邮箱和授权码只保存在本次服务运行的内存中，服务停止后立即失效，不会写入数据库、日志或Git仓库。该配置入口只接受从运行服务的本机发起的请求。

　　服务器部署或需要重启后继续使用时，应在启动前通过环境变量配置一个支持SMTP发送的邮箱账号和邮箱授权码。下面以使用465端口的邮箱服务为例，`SPRING_MAIL_PASSWORD`填写邮箱设置页面生成的授权码，不是邮箱登录密码：

```powershell
$env:SPRING_MAIL_HOST="smtp.example.com"
$env:SPRING_MAIL_PORT="465"
$env:SPRING_MAIL_USERNAME="你的邮箱账号"
$env:SPRING_MAIL_PASSWORD="你的邮箱授权码"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE="true"
$env:MAIL_FROM="你的发件邮箱"
mvn spring-boot:run
```

　　如果邮箱服务要求587端口，应将`SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE`删除，并设置`SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE="true"`。申请验证码接口对注册与未注册邮箱返回相同提示，不会把验证码退回页面。连续猜错5次后该验证码失效；请确认发件服务可用再申请。

## 复习与实验

　　错题复习页记录每次“已经记住”或“还需练习”，并安排1、3、7、14、30天后的回顾。分析页允许保存人工判断及依据。页面显示的参考代码尚未由系统自动运行，因此不能仅凭JSON结构完整就认定修改正确。

　　[实验说明](experiments/README.md)提供150条可追溯Java候选案例、两种提示词对照工具和300行空白人工评分表。只有实际完成标注并运行对应测试的案例，才可在论文中计为已核验案例。AI默认每位用户每天最多20次分析、全局同时最多3次；可通过`AI_DAILY_LIMIT`和`AI_GLOBAL_CONCURRENCY`调整。当前额度按单个应用进程计数，多个实例部署时需要共享计数服务。

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

　　项目已绑定`https://github.com/MoonLight-sweet/SpringBootBC.git`。本地Git已配置提交后自动执行推送；Git传输线路临时不可用时会自动重试，后台任务也会每分钟补推尚未上传的提交。项目不保存GitHub访问令牌。

　　Windows登录后会启动`CodeClinic-GitHub-AutoSync`计划任务。项目文件稳定20秒后自动运行测试，测试通过则创建提交并推送到当前GitHub分支；测试失败时保留修改但不提交。运行日志保存在`output/auto-sync/auto-sync.log`。

　　如需临时停止自动同步，可在PowerShell中执行`Stop-ScheduledTask -TaskName "CodeClinic-GitHub-AutoSync"`；再次启动时执行`Start-ScheduledTask -TaskName "CodeClinic-GitHub-AutoSync"`。
