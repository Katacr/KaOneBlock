# 配置指南

KaOneBlock 首次启动后会在 `plugins/KaOneBlock/` 中释放可编辑文件：

| 路径 | 用途 |
| --- | --- |
| `config.yml` | 语言、日志、初始阶段和旧数据迁移设置 |
| `blocks/` | 阶段、方块池和特殊事件概率 |
| `chests/` | 宝箱名称、奖励数量和物品池 |
| `entities/` | 各阶段可生成的生物与装备 |
| `lang/` | 玩家可见消息 |

修改 YAML 文件后可使用 `/kob reload` 重新读取。请使用空格缩进，不要使用 Tab；如重载失败，请先检查控制台中的文件名和错误位置。

`data.db` 保存玩家数据，不应使用文本编辑器打开或手动修改。编辑配置前建议保留备份。

## 继续阅读

- [主配置 config.yml](main.md)
- [阶段与方块](stages.md)
- [宝箱奖励](chests.md)
- [生物包](entities.md)
- [ItemsAdder](itemsadder.md)
