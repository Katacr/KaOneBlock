# 权限列表

| 权限 | 默认值 | 用途 |
| --- | --- | --- |
| `kaoneblock.help` | 所有人 | 查看帮助 |
| `kaoneblock.start` | 所有人 | 创建个人 OneBlock |
| `kaoneblock.stop` | 所有人 | 移除个人 OneBlock |
| `kaoneblock.reload` | OP | 重载插件配置 |
| `kaoneblock.log` | OP | 管理活动日志 |
| `kaoneblock.debug` | OP | 调试模式、宝箱诊断和 ItemsAdder 状态 |
| `kaoneblock.admin` | OP | 设置或重置阶段、查看数据库信息，并包含重载、日志和调试权限 |

如果服务器使用 LuckPerms，可按服务器规则将玩家基础权限加入默认组。例如：

```text
/lp group default permission set kaoneblock.help true
/lp group default permission set kaoneblock.start true
/lp group default permission set kaoneblock.stop true
```

这三个权限已默认向所有玩家开放；示例主要用于服务器曾经覆盖默认权限的情况。
