# 开发约定（Conventions）

本目录是 **跨 AI / 人工** 共用的编码规范（与具体 IDE 无关）。

| 文件 | 说明 |
|------|------|
| [java-jdk7-style.md](./java-jdk7-style.md) | Java JDK 1.7 语法风格（菱形/`<?>`、包装类拆箱、避免 Java 8+ 语法） |

## 各 AI 怎么用

| 工具 | 怎么吃到约定 |
|------|----------------|
| **任意 Agent** | 读仓库根目录 [`AGENTS.md`](../../AGENTS.md)，再按需打开本目录 |
| **Cursor** | 自动加载 `.cursor/rules/*.mdc`；与本文保持同步 |
| **GitHub Copilot** | 见 [`.github/copilot-instructions.md`](../../.github/copilot-instructions.md) |
| **Claude Code 等** | 多数会读 `AGENTS.md`；也可在对话里 `@docs/conventions` |
| **其他** | 把 `docs/conventions/` 或 `AGENTS.md` 加入该产品的「项目说明 / 自定义指令」 |

修改规范时：**先改本目录**，再同步 `.cursor/rules/` 与 `AGENTS.md` 中的摘要（如有）。
