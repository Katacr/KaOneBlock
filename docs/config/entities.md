# 生物包

生物包位于 `plugins/KaOneBlock/entities/`。阶段文件通过 `entity_pack` 引用文件名，通过 `entity_chance` 控制触发概率。

```yaml
list:
  armored_zombie:
    name: '&c装甲僵尸'
    type: ZOMBIE
    weight: 10
    armors:
      helmet: IRON_HELMET
      chestplate: IRON_CHESTPLATE
      leggings: IRON_LEGGINGS
      boots: IRON_BOOTS
      mainhand: IRON_SWORD
      offhand: SHIELD
```

## 字段说明

| 字段 | 说明 |
| --- | --- |
| `list` 下的名称 | 配置内唯一 ID，可自行命名 |
| `name` | 生物头顶显示名称，支持 `&` 颜色代码 |
| `type` | Bukkit 生物类型，例如 `ZOMBIE`、`SKELETON`、`BLAZE` |
| `weight` | 同一生物包中的相对权重，必须为正数才会被抽中 |
| `armors` | 可选装备部分 |

支持的装备键为 `helmet`、`chestplate`、`leggings`、`boots`、`mainhand` 和 `offhand`。材质名必须适用于服务器版本。

某些生物不会显示或使用装备；这由 Minecraft 本身的生物行为决定。修改后执行 `/kob reload`。
