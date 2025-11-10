# AI 桌宠（Shimeji-ee + AI Chat）

一个在原始 Shimeji-ee 基础上扩展的桌宠项目，增加了 AI 聊天和本地语音（TTS）功能。

## 主要特性

- **AI 对话**：通过右键菜单启动与桌宠的对话（支持多种后端 AI 服务，易于替换）。
- **本地 TTS**：在 Windows 上使用系统语音播放 AI 回复，聊天窗口提供即时开关并持久化配置。

---

## 快速开始

### 1) 环境要求

- JDK 8 或更高
- Apache Ant（用于构建）

### 2) 构建（使用 Ant）

> 本项目使用 Ant 构建脚本。下面是常用的构建步骤与说明。

在项目根目录运行：

```powershell
ant
```

说明：

- 默认 `ant` 目标会编译源码并打包项目（依据仓库中 `build.xml` 的定义）。
- 构建成功后通常会在 `dist/`（或 `target/`）目录下生成可运行的 JAR（例如 `dist/Shimeji-ee.jar`）。

常见操作：

- 仅编译：`ant compile`
- 打包发行：`ant jar` 或 `ant dist`（具体目标请参考仓库根目录的 `build.xml`）

构建后运行：

- 运行发行包（推荐）：

```powershell
java -jar dist/Shimeji-ee.jar
```

- 或直接使用项目提供的启动脚本：在 Windows 上可以运行 `1.bat`（已由构建或仓库提供）。

### 3) 开发运行

```powershell
java -cp "target/classes;lib/*" com.group_finity.mascot.Main
```

---

## AI 对话（使用说明）

- 打开桌宠的右键菜单，选择 “ChatWithAI” 或类似入口以打开 AI 聊天窗口。
- 在聊天窗口输入问题并回车，桌宠会通过配置的 AI 服务回复。
- AI 服务的配置位于 Settings -> AI（包括 API Key、endpoint、system prompt 等）。

---

## 语音（TTS）功能

- 在 Windows 下，程序会通过 PowerShell 调用 .NET 的 System.Speech 播放 AI 回复（无需额外依赖）。
- 控制入口：
  - **全局配置**：`conf/settings.properties` 中设置 `TTS=true|false`。
  - **设置窗口**：Settings -> AI 中的“启用语音（本地 TTS）”。
  - **即时开关**：AI 聊天窗口顶部的“语音”切换按钮（切换会立即生效并持久化到 `conf/settings.properties`）。

---

## 测试与排查

1. 启动程序并打开 AI 聊天。
2. 确认聊天窗口顶部的“语音”按钮为开启状态，然后发送一条测试消息，等待桌宠回复并朗读。
3. 若没有声音：
   - 检查系统语音包（Windows Settings -> Time & Language -> Speech）。
   - 检查 `conf/settings.properties` 中 `TTS` 值或聊天窗口按钮状态。
   - 检查全局静音设置（Settings 中 Sounds）。
   - 若 PowerShell 被限制，尝试以管理员运行或放宽执行策略（这受系统策略影响）。

---

## 实现细节与后续改进

- 当前 Windows 实现使用 PowerShell + System.Speech；macOS/Linux 支持可作为后续扩展（例如使用 `say` 或 `espeak`）。
- 可选改进：集成云 TTS（更自然的语音）、在 `TTSPlayer` 中加入播放队列、对 TTS 输出进行缓存以减少重复合成。

---

## 许可证

本项目基于 Shimeji-ee 的原始许可证。详情请查看 `licence.txt` 和 `originallicence.txt`。
