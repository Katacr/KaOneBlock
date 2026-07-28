# 宝箱奖励

宝箱配置位于 `plugins/KaOneBlock/chests/`。阶段文件中 `chests` 下使用的名称必须与宝箱文件名一致，但不写 `.yml`。

## 基础示例

```yaml
name: '&6补给宝箱'
amount:
  min: 2
  max: 5

items:
  bread:
    material: BREAD
    min: 2
    max: 6
    weight: 10
    slot: -1
  pickaxe:
    material: IRON_PICKAXE
    min: 1
    max: 1
    weight: 2
    name: '&b开拓者之镐'
    lore:
      - '&7继续挖掘新的方块'
    enchantments:
      EFFICIENCY: 2
```

## 宝箱与物品字段

| 字段 | 说明 |
| --- | --- |
| `name` | 宝箱显示名称，支持 `&` 颜色代码 |
| `amount.min` / `amount.max` | 从全局物品池抽取的条目数量，范围会限制在 0 到 27 |
| `material` | 原版材质名，或以 `IA:` 开头的 ItemsAdder 物品 ID |
| `min` / `max` | 单次生成的堆叠数量 |
| `weight` | 在同一物品池中的相对权重，必须大于 0 |
| `slot` | 固定槽位 `0` 到 `26`；`-1` 表示随机空槽 |
| `name` / `lore` | 自定义物品名称与描述 |
| `enchantments` | 直接附魔及等级 |
| `stored-enchantments` | 附魔书保存的附魔及等级 |
| `potion-type` | 药水的基础类型 |
| `custom-effects` | 药水的自定义效果 |

同一个配置也可以使用 `groups` 建立多个独立物品池，每组拥有自己的 `min`、`max` 和 `items`：

```yaml
groups:
  food:
    min: 1
    max: 2
    items:
      apple:
        material: APPLE
        min: 2
        max: 4
        weight: 10
```

固定槽位已经被其他奖励占用时，物品会改放到随机空槽；宝箱没有空位时，其余奖励不会生成。
