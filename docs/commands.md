# 命令列表

主命令为 `/kaoneblock`，可使用简写 `/kob`。

| 命令 | 说明 | 执行者 | 权限 |
| --- | --- | --- | --- |
| `/kob help` | 显示有权使用的命令 | 玩家或控制台 | `kaoneblock.help` |
| `/kob start` | 在脚下创建个人 OneBlock | 玩家 | `kaoneblock.start` |
| `/kob stop` | 移除个人 OneBlock 并重置进度 | 玩家 | `kaoneblock.stop` |
| `/kob reload` | 重载配置、语言和内容文件 | 玩家或控制台 | `kaoneblock.reload` |
| `/kob log [on\|off]` | 查看或切换活动日志 | 玩家或控制台 | `kaoneblock.log` |
| `/kob debug [on\|off]` | 查看或切换调试模式 | 玩家或控制台 | `kaoneblock.debug` |
| `/kob set <玩家> <阶段>` | 设置在线玩家的阶段并清零阶段进度 | 玩家或控制台 | `kaoneblock.admin` |
| `/kob reset-stage` | 将自己的阶段重置为初始阶段 | 玩家 | `kaoneblock.admin` |
| `/kob debugchest` | 把所看宝箱内容输出到控制台 | 玩家 | `kaoneblock.debug` |
| `/kob ia-status` | 查看 ItemsAdder 检测与加载状态 | 玩家或控制台 | `kaoneblock.debug` |
| `/kob checkdb` | 把数据库字段信息输出到控制台 | 玩家或控制台 | `kaoneblock.admin` |

`/kob set` 的阶段参数可写 `normal` 或 `normal.yml`。目标玩家必须在线且已经创建 OneBlock。
