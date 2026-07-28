# ItemsAdder

ItemsAdder 是可选依赖。不安装时，KaOneBlock 的原版方块、宝箱和生物功能仍可正常使用。

## 安装顺序

1. 按 ItemsAdder 官方说明安装并完成资源配置。
2. 确认 ItemsAdder 能正常加载自定义内容。
3. 安装或重启 KaOneBlock。
4. 使用 `/kob ia-status` 检查状态。

状态中的 `启用=true` 表示已检测到兼容的 ItemsAdder；`加载=true` 表示自定义内容已经可以使用。

## 自定义方块

在阶段文件的 `blocks` 中使用 `IA:` 前缀：

```yaml
blocks:
  'IA:my_namespace:ruby_ore': 5
```

如果自定义方块暂时不可用，本次会安全回退为石头。

## 自定义宝箱物品

在宝箱物品的 `material` 中使用相同前缀：

```yaml
items:
  ruby:
    material: 'IA:my_namespace:ruby'
    min: 1
    max: 3
    weight: 5
```

找不到的自定义物品会被跳过，不会用屏障物品占位。修改 ItemsAdder 内容后，请先完成 ItemsAdder 的重载，再执行 `/kob reload`；生产服务器建议完整重启以避免插件重载顺序问题。
