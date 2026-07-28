# Main Config: config.yml

Default configuration:

```yaml
debug: false
log: true
language: "zh_CN"
start-list: normal
legacy-world: world
```

## Options

| Option | Values | Description |
| --- | --- | --- |
| `debug` | `true` / `false` | Prints detailed diagnostics; keep disabled during normal use |
| `log` | `true` / `false` | Records block, chest, and entity generation activity |
| `language` | `zh_CN` / `en_US` | Selects the player message language |
| `start-list` | Stage file name | Starting stage for new players; use `normal` or `normal.yml` |
| `legacy-world` | World name | Assigns a world to records created by old versions without world data |

Activity logs are stored by date in `plugins/KaOneBlock/logs/`.

`legacy-world` affects only old-data migration. Before upgrading, set it to the world that actually contains the old OneBlocks and ensure that world is loaded when the plugin starts. New records store their world automatically.

Run `/kob reload` after normal changes. For legacy migration settings, edit the file before the first migration and fully restart the server.
