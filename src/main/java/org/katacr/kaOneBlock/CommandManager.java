package org.katacr.kaOneBlock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class CommandManager implements TabExecutor {
    private final KaOneBlock plugin;
    private final List<String> subCommands = Arrays.asList("help", "log", "reload", "start", "stop", "debug", "ia-status", "debugchest", "reset-stage", "set");

    public CommandManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 处理 /kaoneblock 和 /kob 命令
        if (cmd.getName().equalsIgnoreCase("kaoneblock") || cmd.getName().equalsIgnoreCase("kob")) {
            // 如果没有参数或"help"参数，显示帮助
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                if (!sender.hasPermission("kaoneblock.help")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }
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
            if (args[0].equalsIgnoreCase("set")) {
                if (!sender.hasPermission("kaoneblock.admin")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /kob set <玩家> <阶段>");
                    return true;
                }

                // 获取玩家
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "玩家 " + args[1] + " 不在线或不存在");
                    return true;
                }

                // 确保玩家数据存在
                if (!plugin.getDatabaseManager().hasBlock(targetPlayer.getUniqueId())) {
                    sender.sendMessage(ChatColor.RED + "玩家 " + args[1] + " 还没有生成方块，请先使用 /kob start");
                    return true;
                }

                // 获取阶段文件名（确保有扩展名）
                String stageFile = args[2];
                if (!stageFile.endsWith(".yml")) {
                    stageFile += ".yml";
                }

                // 检查阶段文件是否存在
                File stageConfigFile = new File(plugin.getDataFolder(), "blocks/" + stageFile);
                if (!stageConfigFile.exists()) {
                    sender.sendMessage(ChatColor.RED + "阶段配置文件不存在: " + stageFile);
                    // 列出可用阶段
                    StringBuilder availableStages = new StringBuilder(ChatColor.YELLOW + "可用阶段: ");
                    File blocksDir = new File(plugin.getDataFolder(), "blocks");
                    if (blocksDir.exists()) {
                        File[] files = blocksDir.listFiles((dir, name) -> name.endsWith(".yml"));
                        if (files != null) {
                            for (File file : files) {
                                String fileName = file.getName().replace(".yml", "");
                                availableStages.append(fileName).append(", ");
                            }
                        }
                    }
                    sender.sendMessage(availableStages.toString());
                    return true;
                }

                // 强制设置玩家阶段
                plugin.getStageManager().setPlayerStage(targetPlayer, stageFile);

                sender.sendMessage(ChatColor.GREEN + "已设置玩家 " + targetPlayer.getName() + " 的阶段为: " + stageFile);
                return true;
            }

            // 开始命令 - 在玩家腿部位置生成方块
            if (args[0].equalsIgnoreCase("start")) {
                if (!sender.hasPermission("kaoneblock.start")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
                    return true;
                }

                // 初始化玩家进度 - 这会发送初始阶段消息
                plugin.getStageManager().initPlayerProgress(player);

                // 生成起始方块
                plugin.getBlockGenerator().generateBlockAtPlayerLocation(player);
                return true;
            }

            // 停止命令 - 移除玩家生成的方块
            if (args[0].equalsIgnoreCase("stop")) {
                if (!sender.hasPermission("kaoneblock.stop")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
                    return true;
                }

                plugin.getBlockGenerator().removePlayerBlock(player);
                return true;
            }

            // 日志命令
            if (args[0].equalsIgnoreCase("log")) {
                if (!sender.hasPermission("kaoneblock.log")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    // 显示当前日志状态
                    boolean logEnabled = plugin.getConfig().getBoolean("log", true);
                    String message = logEnabled ? "logging-enabled" : "logging-disabled";
                    sender.sendMessage(plugin.getLanguageManager().getMessage(message));
                    return true;
                }

                if (args[1].equalsIgnoreCase("on")) {
                    plugin.getConfig().set("log", true);
                    plugin.saveConfig();
                    plugin.getLogManager().setEnabled(true);
                    sender.sendMessage(plugin.getLanguageManager().getMessage("logging-enabled"));
                    return true;
                }

                if (args[1].equalsIgnoreCase("off")) {
                    plugin.getConfig().set("log", false);
                    plugin.saveConfig();
                    plugin.getLogManager().setEnabled(false);
                    sender.sendMessage(plugin.getLanguageManager().getMessage("logging-disabled"));
                    return true;
                }
            }
            // 在 CommandManager 中添加 reset-stage 命令
            if (args[0].equalsIgnoreCase("reset-stage")) {
                if (!sender.hasPermission("kaoneblock.admin")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
                    return true;
                }

                // 重置玩家阶段
                plugin.getStageManager().resetPlayerStage(player, "normal.yml");
                player.sendMessage(plugin.getLanguageManager().getMessage("stage-reset"));
                return true;
            }
            // 添加 debugchest 命令
            if (args[0].equalsIgnoreCase("debugchest")) {
                if (!sender.hasPermission("kaoneblock.debug")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
                    return true;
                }

                // 获取玩家指向的方块
                Block target = player.getTargetBlockExact(5);
                if (target != null && target.getState() instanceof Chest chest) {
                    plugin.getEnhancedChestManager().debugChestContents(chest);
                    player.sendMessage(ChatColor.GREEN + "宝箱内容已输出到控制台");
                } else {
                    player.sendMessage(ChatColor.RED + "请看向一个宝箱");
                }
                return true;
            }

            // 调试命令
            if (args[0].equalsIgnoreCase("debug")) {
                if (!sender.hasPermission("kaoneblock.debug")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    // 显示当前调试状态
                    boolean debugEnabled = plugin.isDebugEnabled();
                    String message = debugEnabled ? "debug-enabled" : "debug-disabled";
                    sender.sendMessage(plugin.getLanguageManager().getMessage(message));
                    return true;
                }

                if (args[1].equalsIgnoreCase("on")) {
                    plugin.getConfig().set("debug", true);
                    plugin.saveConfig();
                    plugin.debugMode = true;
                    sender.sendMessage(plugin.getLanguageManager().getMessage("debug-enabled"));
                    return true;
                }

                if (args[1].equalsIgnoreCase("off")) {
                    plugin.getConfig().set("debug", false);
                    plugin.saveConfig();
                    plugin.debugMode = false;
                    sender.sendMessage(plugin.getLanguageManager().getMessage("debug-disabled"));
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("checkdb")) {
                if (!sender.hasPermission("kaoneblock.admin")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                plugin.getDatabaseManager().checkTableStructure();
                sender.sendMessage(ChatColor.GREEN + "已检查数据库表结构");
                return true;
            }
            // 添加 ItemsAdder 状态命令
            if (args[0].equalsIgnoreCase("ia-status")) {
                if (!sender.hasPermission("kaoneblock.debug")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }

                boolean enabled = plugin.isItemsAdderEnabled();
                boolean loaded = plugin.isItemsAdderLoaded();

                // 获取原始消息
                String rawMessage = plugin.getLanguageManager().getMessage("ia-status");

                // 手动替换变量
                String message = rawMessage.replace("%enabled%", String.valueOf(enabled)).replace("%loaded%", String.valueOf(loaded));

                sender.sendMessage(message);
                return true;
            }

            // 未知命令
            sender.sendMessage(plugin.getLanguageManager().getMessage("unknown-command"));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
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
            } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                // set 命令的玩家补全
                String partialName = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String playerName = player.getName();
                    if (playerName.toLowerCase().startsWith(partialName)) {
                        completions.add(playerName);
                    }
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                // set 命令的阶段补全
                String partialStage = args[2].toLowerCase();
                List<String> stageFiles = getAvailableStageFiles();
                for (String stage : stageFiles) {
                    if (stage.toLowerCase().startsWith(partialStage)) {
                        completions.add(stage);
                    }
                }
            } else if (args.length == 2) {
                // 其他命令的补全（如 log, debug）
                if (args[0].equalsIgnoreCase("log") || args[0].equalsIgnoreCase("debug")) {
                    List<String> options = Arrays.asList("on", "off");
                    for (String option : options) {
                        if (option.startsWith(args[1].toLowerCase())) {
                            completions.add(option);
                        }
                    }
                }
            }
        }

        return completions;
    }

    /**
     * 获取所有可用的阶段文件名（不含扩展名）
     */
    private List<String> getAvailableStageFiles() {
        List<String> stageFiles = new ArrayList<>();
        File blocksDir = new File(plugin.getDataFolder(), "blocks");
        if (blocksDir.exists() && blocksDir.isDirectory()) {
            File[] files = blocksDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName().replace(".yml", "");
                    stageFiles.add(fileName);
                }
            }
        }
        return stageFiles;
    }

    private void showHelp(CommandSender sender) {
        LanguageManager languageManager = plugin.getLanguageManager();
        sender.sendMessage(languageManager.getMessage("help-header"));

        // 所有玩家都能看到的命令
        sender.sendMessage(languageManager.getMessage("help-help"));
        sender.sendMessage(languageManager.getMessage("help-start"));
        sender.sendMessage(languageManager.getMessage("help-stop"));

        // 仅 OP 玩家能看到的命令
        if (sender.hasPermission("kaoneblock.reload") || sender.isOp()) {
            sender.sendMessage(languageManager.getMessage("help-reload"));
        }

        if (sender.hasPermission("kaoneblock.log") || sender.isOp()) {
            sender.sendMessage(languageManager.getMessage("help-log"));
        }

        if (sender.hasPermission("kaoneblock.debug") || sender.isOp()) {
            sender.sendMessage(languageManager.getMessage("help-debug"));
            sender.sendMessage(languageManager.getMessage("help-debugchest"));
            sender.sendMessage(languageManager.getMessage("help-ia-status"));
        }

        sender.sendMessage(languageManager.getMessage("help-more"));
    }
}