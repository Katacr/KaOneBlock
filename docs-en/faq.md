# FAQ

## `/kob start` says the position below me is occupied

The OneBlock appears one block below the player's feet. Stand at a safe edge or another suitable position, make sure the target position is air, and run the command again.

## Why can I not run `/kob start` again?

Each player can own only one OneBlock. Use `/kob stop` to remove the old one, or return to its location and continue playing.

## Why can another player not break this block?

A OneBlock belongs to its creator. Only its owner can advance its stage, preventing other players from changing progress accidentally.

## `/kob stop` says the world is not loaded

The recorded block is in another world. Load that world with your world-management plugin and run the command again. KaOneBlock does not guess coordinates or delete a block from the current world.

## The plugin stops while migrating an old database

Check that `legacy-world` in `config.yml` is the world that actually contains the old OneBlocks, and make sure that world loads before KaOneBlock. Keep a backup of `data.db` before trying again.

## My configuration changes do not appear

Confirm that you edited files under `plugins/KaOneBlock/`, check YAML indentation, and run `/kob reload`. If the console reports an invalid material, stage, or file name, correct that value first.

## ItemsAdder loot does not appear in chests

Run `/kob ia-status` and confirm both values are `true`. Then verify the complete namespaced item ID. Invalid or not-yet-loaded custom items are safely skipped.

## Where are data and logs stored?

Player data is stored in `plugins/KaOneBlock/data.db`; activity logs are stored in `plugins/KaOneBlock/logs/`. Back up the complete `plugins/KaOneBlock/` folder.
