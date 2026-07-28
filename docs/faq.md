# 常见问题

## `/kob start` 提示脚下位置被占用

OneBlock 会生成在玩家脚下一个方块的位置。请站在平台边缘或其他安全位置，确保脚下目标位置是空气，再执行命令。

## 为什么不能再次使用 `/kob start`？

每位玩家只能拥有一个 OneBlock。先使用 `/kob stop` 移除原方块，或前往原方块继续游戏。

## 为什么其他玩家不能挖这个方块？

OneBlock 与创建者绑定，只有所有者可以推进该方块的阶段。这可防止其他玩家误改进度。

## `/kob stop` 提示世界未加载

插件记录的方块位于另一个世界。请使用世界管理插件加载该世界，再重新执行命令。KaOneBlock 不会在世界缺失时猜测坐标或删除当前世界的方块。

## 升级后插件因旧数据库迁移失败而停止

检查 `config.yml` 的 `legacy-world` 是否为旧 OneBlock 实际所在世界，并确认该世界在 KaOneBlock 启动前已经加载。保留 `data.db` 备份后再重试。

## 修改配置后没有变化

确认修改的是 `plugins/KaOneBlock/` 下的文件，检查 YAML 缩进，然后执行 `/kob reload`。如果控制台报告材质、阶段或文件名无效，请先修正对应配置。

## 宝箱中的 ItemsAdder 物品没有出现

使用 `/kob ia-status` 检查两个状态是否都为 `true`，再确认物品 ID 包含正确命名空间。无效或尚未加载的自定义物品会被安全跳过。

## 数据和日志在哪里？

玩家数据位于 `plugins/KaOneBlock/data.db`，活动日志位于 `plugins/KaOneBlock/logs/`。备份时建议完整复制 `plugins/KaOneBlock/`。
