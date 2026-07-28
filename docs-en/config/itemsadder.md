# ItemsAdder

ItemsAdder is optional. KaOneBlock's vanilla blocks, chests, and entities continue to work without it.

## Installation Order

1. Install and configure ItemsAdder according to its official documentation.
2. Confirm that ItemsAdder loads its custom content successfully.
3. Install or restart KaOneBlock.
4. Run `/kob ia-status`.

`Enabled=true` means a compatible ItemsAdder installation was detected. `Loaded=true` means its custom content is ready.

## Custom Blocks

Use the `IA:` prefix in a stage's `blocks` section:

```yaml
blocks:
  'IA:my_namespace:ruby_ore': 5
```

If a custom block is unavailable for a generation, KaOneBlock safely falls back to stone.

## Custom Chest Items

Use the same prefix in an item's `material`:

```yaml
items:
  ruby:
    material: 'IA:my_namespace:ruby'
    min: 1
    max: 3
    weight: 5
```

Missing custom items are skipped instead of being replaced by barrier placeholders. After changing ItemsAdder content, finish the ItemsAdder reload first and then run `/kob reload`. A full restart is recommended on production servers to avoid plugin reload-order issues.
