# Stages and Blocks

Each stage is a YAML file under `plugins/KaOneBlock/blocks/`. File names may contain only letters, numbers, underscores, and hyphens.

```yaml
amount: 500
message: '&aYou entered the Plains stage'
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

## Fields

| Field | Description |
| --- | --- |
| `amount` | Breaks required in this stage; minimum is 1 |
| `message` | Message sent on entry; supports `&` color codes |
| `next` | Next stage file name; leave empty for a final stage |
| `entity_pack` | Entity pack file from `entities/` |
| `entity_chance` | Chance of an entity event per break, from `0` to `1` |
| `chests` | Chest configuration names and their individual chances |
| `blocks` | Vanilla block names and relative weights |

Chances are decimals: `0.05` means 5%. The entity chance plus all chest chances should not exceed `1`; the remaining chance produces a normal block.

Block values are relative weights, not percentages. In the example, `STONE` appears more often than `DIAMOND_ORE`.

Quote ItemsAdder block keys:

```yaml
blocks:
  'IA:my_namespace:my_block': 5
```

After adding a stage, point the previous stage's `next` value to the new file and run `/kob reload`.
