# Commands

The main command is `/kaoneblock`; `/kob` is its short alias.

| Command | Description | Sender | Permission |
| --- | --- | --- | --- |
| `/kob help` | Shows commands available to the sender | Player or console | `kaoneblock.help` |
| `/kob start` | Creates a personal OneBlock below the player | Player | `kaoneblock.start` |
| `/kob stop` | Removes the personal OneBlock and resets progress | Player | `kaoneblock.stop` |
| `/kob reload` | Reloads configuration, language, and content files | Player or console | `kaoneblock.reload` |
| `/kob log [on\|off]` | Shows or changes activity logging | Player or console | `kaoneblock.log` |
| `/kob debug [on\|off]` | Shows or changes debug mode | Player or console | `kaoneblock.debug` |
| `/kob set <player> <stage>` | Sets an online player's stage and clears stage progress | Player or console | `kaoneblock.admin` |
| `/kob reset-stage` | Resets the sender's stage to the starting stage | Player | `kaoneblock.admin` |
| `/kob debugchest` | Prints the targeted chest contents to the console | Player | `kaoneblock.debug` |
| `/kob ia-status` | Shows ItemsAdder detection and load status | Player or console | `kaoneblock.debug` |
| `/kob checkdb` | Prints database field information to the console | Player or console | `kaoneblock.admin` |

The stage argument for `/kob set` accepts either `normal` or `normal.yml`. The target player must be online and must already own a OneBlock.
