# Installation and Getting Started

## Install the Plugin

1. Make sure the server uses Java 17 or newer.
2. Use Spigot or Paper 1.18.2 or newer.
3. Place `KaOneBlock-version.jar` in the server's `plugins` folder.
4. Fully restart the server.
5. Confirm in the console that KaOneBlock enabled successfully.

Do not use the server's `/reload` command to install or update plugins. KaOneBlock's `/kob reload` command only reloads KaOneBlock configuration files.

## Upgrade from an Older Version

Back up `plugins/KaOneBlock/` before upgrading, especially `data.db` and all YAML files.

Older databases did not store world information. If existing OneBlocks are not in the world named `world`, set `legacy-world` in `config.yml` to the loaded world that actually contains those blocks **before the first start of the new version**. After migration, each record retains its own world.

The old `entitys/` folder is still readable, but custom files should be moved to `entities/`.

## Start Playing

1. Stand where the OneBlock should be created and make sure the block position below your feet is air.
2. Run `/kob start`.
3. The initial block appears below the player.
4. Keep breaking it. New blocks, chests, or creatures are generated according to the current stage.

Each player can own only one OneBlock at a time. It can be in any loaded world; KaOneBlock remembers its exact world and coordinates.

## Stop and Start Again

Run `/kob stop` to remove the current OneBlock and clear its stage progress. The player may then use `/kob start` at another location.

If the recorded world is not loaded, KaOneBlock will not risk deleting a block in another location. Load the correct world before running the stop command again.
