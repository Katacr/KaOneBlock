# Entity Packs

Entity packs are stored in `plugins/KaOneBlock/entities/`. A stage selects the file through `entity_pack` and controls its event chance through `entity_chance`.

```yaml
list:
  armored_zombie:
    name: '&cArmored Zombie'
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

## Fields

| Field | Description |
| --- | --- |
| Name under `list` | Unique ID within this file; choose any safe name |
| `name` | Name shown above the creature; supports `&` color codes |
| `type` | Bukkit entity type such as `ZOMBIE`, `SKELETON`, or `BLAZE` |
| `weight` | Relative weight in the pack; it must be positive to be selected |
| `armors` | Optional equipment section |

Supported equipment keys are `helmet`, `chestplate`, `leggings`, `boots`, `mainhand`, and `offhand`. Material names must exist in the server version.

Some creatures do not display or use equipment because of their normal Minecraft behavior. Run `/kob reload` after editing a pack.
