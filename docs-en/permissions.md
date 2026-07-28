# Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `kaoneblock.help` | Everyone | View help |
| `kaoneblock.start` | Everyone | Create a personal OneBlock |
| `kaoneblock.stop` | Everyone | Remove a personal OneBlock |
| `kaoneblock.reload` | OP | Reload plugin configuration |
| `kaoneblock.log` | OP | Manage activity logging |
| `kaoneblock.debug` | OP | Debug mode, chest diagnostics, and ItemsAdder status |
| `kaoneblock.admin` | OP | Set or reset stages, inspect database fields, and inherit reload, log, and debug permissions |

With LuckPerms, basic permissions can be assigned to the default group according to your server policy:

```text
/lp group default permission set kaoneblock.help true
/lp group default permission set kaoneblock.start true
/lp group default permission set kaoneblock.stop true
```

These three permissions are already available to everyone by default. The example is useful if another permission setup overrides plugin defaults.
