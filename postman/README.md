# Postman验收

（1）打开Postman，导入`CodeClinic.postman_collection.json`。

（2）按集合顺序运行请求，集合会验证注册、找回密码、新密码登录和核心业务接口，并自动保存验证码、登录凭证和错题编号。

（3）默认服务地址为`http://localhost:8080`，启动方式见项目根目录`README.md`。

本目录的`results/`由验收脚本生成，记录每个请求的HTTP状态和响应摘要。
