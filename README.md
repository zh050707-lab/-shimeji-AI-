# AI 桌宠 (Shimeji-ee + AI Chat)

这是一个基于 [Shimeji-ee](https://code.google.com/archive/p/shimeji-ee/) 项目的修改版，为其增加了 AI 对话功能。

![Shimeji](img/profile.png) ## ✨ 新功能：AI 对话

* **智能互动：** 通过右键菜单选择 "AI 对话"，即可与你的桌宠进行聊天。
* **可扩展：** 基于接口设计，可以轻松替换不同的 AI 服务（如 OpenAI, Gemini, 或其他 LLM）。

## 🚀 如何构建

本项目使用 Java 和 Ant 构建。

1.  **环境要求：**
    * JDK 8 或更高版本
    * [Apache Ant](https://ant.apache.org/)

2.  **编译：**
    在项目根目录（`Shimeji源码` 目录）运行 Ant：
    ```bash
    ant
    ```

3.  **运行：**
    编译完成后，会在 `dist`（或 `build`）目录下生成 `Shimeji-ee.jar`。运行它：
    ```bash
    java -jar dist/Shimeji-ee.jar
    ```


## 📜 许可证 (License)

本项目基于 Shimeji-ee 的原始许可证。详情请查看 `licence.txt` 和 `originallicence.txt` 文件。