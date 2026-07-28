# Gameplay

## Core Loop

When a player's OneBlock is broken, KaOneBlock generates the next result at the same position. Normal results come from the current stage's block pool; special results can be loot chests or creatures.

Only the owner can break a OneBlock. Attempts by other players are blocked.

## Default Stages

The bundled configuration uses this order:

1. `normal`: Plains stage
2. `nether`: Nether stage
3. `end`: End stage

The Plains and Nether stages require 500 breaks each by default. The End stage is the default final stage and continues using its block pool.

Server administrators can change each stage's required breaks, next stage, block weights, chest chances, and entity chance.

## Chests and Creatures

- A chest replaces the normal block result and is filled from its chest configuration.
- An entity event spawns a creature above the block and still restores the next OneBlock.
- Each stage can use different chest configurations and entity packs.

## Saved Progress

KaOneBlock saves the block location, current stage, and number of breaks within that stage. A normal server restart does not reset progress.

`/kob stop` intentionally removes the OneBlock and resets that player's progress. Administrators can also change progress with `/kob reset-stage` and `/kob set`.
