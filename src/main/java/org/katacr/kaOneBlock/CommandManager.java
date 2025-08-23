package org.katacr.kaOneBlock;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class CommandManager implements TabExecutor {
    private final KaOneBlock plugin;
    private final List<String> subCommands = Arrays.asList("help", "info", "reload", "start", "stop", "history");

    public CommandManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String label, @Nonnull String[] args) {
        // 处理 /kaoneblock 和 /kob 命令
        if (cmd.getName().equalsIgnoreCase("kaoneblock") || cmd.getName().equalsIgnoreCase("kob")) {
            // 如果没有参数或"help"参数，显示帮助
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                showHelp(sender);
                return true;
            }

            // 重载命令
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("kaoneblock.reload")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                try {
                    plugin.reloadPlugin();
                    sender.sendMessage(plugin.getLanguageManager().getMessage("config-reloaded"));
                } catch (Exception e) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("reload-error"));
                    plugin.getLogger().log(Level.SEVERE, "Error reloading config: ", e);
                }
                return true;
            }

            // 信息命令
            if (args[0].equalsIgnoreCase("info")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("info-message"));
                return true;
            }

            // 开始命令 - 在玩家腿部位置生成方块
            if (args[0].equalsIgnoreCase("start")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
                    return true;
                }

                if (!sender.hasPermission("kaoneblock.start")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                // 使用方块生成器在玩家位置生成方块
                plugin.getBlockGenerator().generateBlockAtPlayerLocation(player);
                return true;
            }

            // 停止命令
            if (args[0].equalsIgnoreCase("stop")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
                    return true;
                }

                if (!sender.hasPermission("kaoneblock.stop")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                // 移除玩家在当前世界生成的方块
                plugin.getBlockGenerator().removePlayerBlockInCurrentWorld(player);
                return true;
            }

            // 历史记录查询命令
            if (args[0].equalsIgnoreCase("history")) {
                if (!sender.hasPermission("kaoneblock.history")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                // 这里可以添加查询历史记录的逻辑
                sender.sendMessage(plugin.getLanguageManager().getMessage("history-feature-coming-soon"));
                return true;
            }

            // 未知命令
            sender.sendMessage(plugin.getLanguageManager().getMessage("unknown-command"));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String label, @Nonnull String[] args) {
        List<String> completions = new ArrayList<>();

        // 处理 /kaoneblock 和 /kob 命令的标签补全
        if (cmd.getName().equalsIgnoreCase("kaoneblock") || cmd.getName().equalsIgnoreCase("kob")) {
            if (args.length == 1) {
                // 子命令的标签补全
                for (String subCommand : subCommands) {
                    if (subCommand.startsWith(args[0].toLowerCase())) {
                        completions.add(subCommand);
                    }
                }
            }
        }

        return completions;
    }

    private void showHelp(@Nonnull CommandSender sender) {
        LanguageManager languageManager = plugin.getLanguageManager();
        sender.sendMessage(languageManager.getMessage("help-header"));
        sender.sendMessage(languageManager.getMessage("help-help"));
        sender.sendMessage(languageManager.getMessage("help-info"));
        sender.sendMessage(languageManager.getMessage("help-reload"));
        sender.sendMessage(languageManager.getMessage("help-start"));
        sender.sendMessage(languageManager.getMessage("help-stop"));
        sender.sendMessage(languageManager.getMessage("help-history"));
        sender.sendMessage(languageManager.getMessage("help-more"));
    }
}