# Chest Loot

Chest configurations are stored in `plugins/KaOneBlock/chests/`. Names used under a stage's `chests` section must match the chest file name without `.yml`.

## Basic Example

```yaml
name: '&6Supply Chest'
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
    name: '&bPioneer Pickaxe'
    lore:
      - '&7Keep mining new blocks'
    enchantments:
      EFFICIENCY: 2
```

## Chest and Item Fields

| Field | Description |
| --- | --- |
| `name` | Chest display name; supports `&` color codes |
| `amount.min` / `amount.max` | Entries drawn from the global pool, limited to 0 through 27 |
| `material` | Vanilla material or an ItemsAdder item ID prefixed with `IA:` |
| `min` / `max` | Stack amount generated for this entry |
| `weight` | Relative weight within the same pool; must be positive |
| `slot` | Fixed slot from `0` through `26`; use `-1` for a random empty slot |
| `name` / `lore` | Custom item name and description |
| `enchantments` | Direct enchantments and levels |
| `stored-enchantments` | Stored enchantments for enchanted books |
| `potion-type` | Base potion type |
| `custom-effects` | Custom potion effects |

A file may use `groups` to create multiple independent pools. Each group has its own `min`, `max`, and `items`:

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

If a fixed slot is already occupied, the item moves to a random empty slot. Remaining rewards are skipped when the chest is full.
