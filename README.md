# CodeClinic编程错题智能解析助手

这是一个可直接运行的Spring Boot单体应用，包含REST API和中文Web工作台。默认连接本机MySQL的`code_clinic`数据库；默认开启本地模拟AI，配置`DEEPSEEK_API_KEY`并关闭`AI_MOCK_ENABLED`后即可调用DeepSeek兼容接口。

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

核心接口包括用户注册登录、错题增删改查、分页搜索、AI分析、重新分析、分析历史、知识点列表和学习统计。所有需要登录的接口使用`Authorization: Bearer <token>`，错题查询会按当前用户隔离。

## GitHub自动同步

项目已绑定`https://github.com/MoonLight-sweet/SpringBootBC.git`。本地Git已配置提交后自动执行推送；网络或登录状态异常时，提交仍会保留在本地，网络恢复后再次提交即可触发同步。
