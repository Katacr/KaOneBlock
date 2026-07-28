# 主配置 config.yml

默认配置如下：

```yaml
debug: false
log: true
language: "zh_CN"
start-list: normal
legacy-world: world
```

## 配置项

| 配置项 | 可用值 | 说明 |
| --- | --- | --- |
| `debug` | `true` / `false` | 输出更详细的诊断信息，日常使用建议关闭 |
| `log` | `true` / `false` | 记录方块、宝箱和生物生成活动 |
| `language` | `zh_CN` / `en_US` | 选择玩家消息语言 |
| `start-list` | 阶段文件名 | 新玩家开始时使用的阶段，可写 `normal` 或 `normal.yml` |
| `legacy-world` | 世界名称 | 为旧版数据库中没有世界信息的记录指定原世界 |

日志文件位于 `plugins/KaOneBlock/logs/`，按日期保存。

`legacy-world` 只影响旧数据迁移。升级前必须填写旧 OneBlock 实际所在的世界，并确保该世界在插件启动时已加载；新建数据会自动记录世界，无需按玩家配置。

修改后执行 `/kob reload`。如果更改的是旧数据迁移设置，应在插件首次迁移前修改并完整重启服务器。
