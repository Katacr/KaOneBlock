# 阶段与方块

每个阶段是 `plugins/KaOneBlock/blocks/` 下的一个 YAML 文件。文件名只能包含英文字母、数字、下划线和连字符。

```yaml
amount: 500
message: '&a你已进入平原阶段'
next: nether
entity_pack: normal_entity
entity_chance: 0.05

chests:
  common_chest: 0.03
  advanced_chest: 0.01

blocks:
  STONE: 20
  IRON_ORE: 8
  DIAMOND_ORE: 2
```

## 字段说明

| 字段 | 说明 |
| --- | --- |
| `amount` | 在当前阶段需要挖掘的次数，最小为 1 |
| `message` | 进入阶段时发送给玩家的消息，支持 `&` 颜色代码 |
| `next` | 下一阶段文件名；留空表示最终阶段 |
| `entity_pack` | `entities/` 中使用的生物包文件名 |
| `entity_chance` | 每次挖掘触发生物事件的概率，范围为 `0` 到 `1` |
| `chests` | 宝箱配置名与各自生成概率 |
| `blocks` | 原版方块名与相对权重 |

概率使用小数，例如 `0.05` 表示 5%。实体概率与所有宝箱概率之和应不大于 `1`；剩余概率会生成普通方块。

方块值是相对权重，不是百分比。上例中 `STONE` 的权重高于 `DIAMOND_ORE`，因此更常出现。

要使用 ItemsAdder 方块，请为键加引号：

```yaml
blocks:
  'IA:my_namespace:my_block': 5
```

新增阶段后，把上一阶段的 `next` 指向新文件名，并执行 `/kob reload`。
