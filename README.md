# @csnmbai Telegram Bot

这是一个全新、最小可运行的 Telegram 长轮询机器人，不包含旧机器人功能。

## 功能
- `/start`：发送启动欢迎消息
- `/help`：显示帮助
- 使用 Telegram Bot API 长轮询，进程会持续运行

## FadeHost 设置

环境变量：
- `TELEGRAM_BOT_TOKEN`：从 BotFather 获取的机器人 Token（不要公开）
- `TELEGRAM_BOT_USERNAME`：`csnmbai`（可选，仅用于启动日志）

Build Command:
```bash
mvn -q package
```

Start Command:
```bash
java -jar target/csnmbai-bot-1.0.0.jar
```

建议使用 Java 17 或更高版本。

## 注意
- 先确认 `TELEGRAM_BOT_TOKEN` 是新机器人 `@csnmbai` 的 Token。
- 如果同一个 Token 已在其他程序运行，可能发生轮询冲突；同一时间只运行一个轮询实例。
- 本项目启动时会调用 `deleteWebhook`，因为长轮询不能与 webhook 同时使用。
