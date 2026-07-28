# Configuration

On first start, KaOneBlock creates editable files under `plugins/KaOneBlock/`:

| Path | Purpose |
| --- | --- |
| `config.yml` | Language, logging, starting stage, and legacy migration settings |
| `blocks/` | Stages, block pools, and special-event chances |
| `chests/` | Chest names, loot counts, and item pools |
| `entities/` | Creatures and equipment available to each stage |
| `lang/` | Player-facing messages |

After editing YAML, run `/kob reload`. Use spaces instead of tabs. If a reload fails, check the server console for the file name and error location.

`data.db` contains player data and should not be opened or edited as text. Keep a backup before changing configurations.

## Continue Reading

- [Main Config: config.yml](main.md)
- [Stages and Blocks](stages.md)
- [Chest Loot](chests.md)
- [Entity Packs](entities.md)
- [ItemsAdder](itemsadder.md)
