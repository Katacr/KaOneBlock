package org.katacr.kaOneBlock;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Dispatches KaOneBlock commands through small permission-aware handlers.
 */
public class CommandManager implements TabExecutor {
    private static final Map<String, String> SUBCOMMAND_PERMISSIONS = createSubcommandPermissions();
    private final KaOneBlock plugin;

    public CommandManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        String permission = SUBCOMMAND_PERMISSIONS.get(subcommand);
        if (permission == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("unknown-command"));
            return true;
        }
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        return switch (subcommand) {
            case "help" -> handleHelp(sender);
            case "reload" -> handleReload(sender);
            case "start" -> withPlayer(sender, this::handleStart);
            case "stop" -> withPlayer(sender, this::handleStop);
            case "log" -> handleToggle(sender, args, false);
            case "debug" -> handleToggle(sender, args, true);
            case "set" -> handleSet(sender, args);
            case "reset-stage" -> withPlayer(sender, this::handleResetStage);
            case "debugchest" -> withPlayer(sender, this::handleDebugChest);
            case "ia-status" -> handleItemsAdderStatus(sender);
            case "checkdb" -> handleDatabaseCheck(sender);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMAND_PERMISSIONS.entrySet().stream()
                    .filter(entry -> sender.hasPermission(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .filter(name -> name.startsWith(partial))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set") && sender.hasPermission("kaoneblock.admin")) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set") && sender.hasPermission("kaoneblock.admin")) {
            String partial = args[2].toLowerCase(Locale.ROOT);
            return getAvailableStageFiles().stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial))
                    .toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("log") || args[0].equalsIgnoreCase("debug"))) {
            return List.of("on", "off").stream()
                    .filter(option -> option.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }

    /**
     * Displays only commands the sender is permitted to execute.
     */
    private boolean handleHelp(CommandSender sender) {
        LanguageManager language = plugin.getLanguageManager();
        sender.sendMessage(language.getMessage("help-header"));
        sender.sendMessage(language.getMessage("help-help"));
        if (sender.hasPermission("kaoneblock.start")) {
            sender.sendMessage(language.getMessage("help-start"));
        }
        if (sender.hasPermission("kaoneblock.stop")) {
            sender.sendMessage(language.getMessage("help-stop"));
        }
        if (sender.hasPermission("kaoneblock.reload")) {
            sender.sendMessage(language.getMessage("help-reload"));
        }
        if (sender.hasPermission("kaoneblock.log")) {
            sender.sendMessage(language.getMessage("help-log"));
        }
        if (sender.hasPermission("kaoneblock.debug")) {
            sender.sendMessage(language.getMessage("help-debug"));
            sender.sendMessage(language.getMessage("help-debugchest"));
            sender.sendMessage(language.getMessage("help-ia-status"));
        }
        if (sender.hasPermission("kaoneblock.admin")) {
            sender.sendMessage(language.getMessage("help-set"));
            sender.sendMessage(language.getMessage("help-reset-stage"));
            sender.sendMessage(language.getMessage("help-checkdb"));
        }
        sender.sendMessage(language.getMessage("help-more"));
        return true;
    }

    /**
     * Reloads mutable plugin configuration with localized error handling.
     */
    private boolean handleReload(CommandSender sender) {
        try {
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getLanguageManager().getMessage("config-reloaded"));
        } catch (RuntimeException exception) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("reload-error"));
            plugin.getLogger().log(Level.SEVERE, "Failed to reload KaOneBlock", exception);
        }
        return true;
    }

    /**
     * Creates an initial OneBlock and announces its stage only after successful persistence.
     */
    private boolean handleStart(Player player) {
        if (plugin.getBlockGenerator().generateBlockAtPlayerLocation(player)) {
            plugin.getStageManager().sendCurrentStageMessage(player);
        }
        return true;
    }

    /**
     * Removes a player's OneBlock from its recorded world.
     */
    private boolean handleStop(Player player) {
        plugin.getBlockGenerator().removePlayerBlock(player);
        return true;
    }

    /**
     * Handles the shared on/off syntax for debug and activity logging.
     */
    private boolean handleToggle(CommandSender sender, String[] args, boolean debug) {
        if (args.length < 2) {
            boolean enabled = debug ? plugin.isDebugEnabled() : plugin.getConfig().getBoolean("log", true);
            sender.sendMessage(plugin.getLanguageManager().getMessage(enabled
                    ? (debug ? "debug-enabled" : "logging-enabled")
                    : (debug ? "debug-disabled" : "logging-disabled")));
            return true;
        }

        if (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("toggle-usage"));
            return true;
        }
        boolean enabled = args[1].equalsIgnoreCase("on");
        String configPath = debug ? "debug" : "log";
        plugin.getConfig().set(configPath, enabled);
        plugin.saveConfig();
        if (debug) {
            plugin.debugMode = enabled;
        } else {
            plugin.getLogManager().setEnabled(enabled);
        }
        sender.sendMessage(plugin.getLanguageManager().getMessage(enabled
                ? (debug ? "debug-enabled" : "logging-enabled")
                : (debug ? "debug-disabled" : "logging-disabled")));
        return true;
    }

    /**
     * Validates and applies an administrative stage change to an online player.
     */
    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("set-stage-usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-not-found", Map.of("player", args[1])));
            return true;
        }
        if (!plugin.getDatabaseManager().hasBlock(target.getUniqueId())) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-has-no-block", Map.of("player", target.getName())));
            return true;
        }

        try {
            String stageFile = StageConfigManager.normalizeStageFile(args[2]);
            if (!plugin.getStageManager().setPlayerStage(target, stageFile)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("invalid-stage"));
                return true;
            }
            sender.sendMessage(plugin.getLanguageManager().getMessage("stage-set", Map.of(
                    "player", target.getName(),
                    "stage", stageFile
            )));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("invalid-stage"));
        }
        return true;
    }

    /**
     * Resets a player's persisted stage to the configured starting stage.
     */
    private boolean handleResetStage(Player player) {
        String startingStage = plugin.getConfig().getString("start-list", "normal");
        if (plugin.getStageManager().resetPlayerStage(player, startingStage)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("stage-reset"));
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("invalid-stage"));
        }
        return true;
    }

    /**
     * Prints the contents of the chest currently targeted by a player.
     */
    private boolean handleDebugChest(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target != null && target.getState() instanceof Chest chest) {
            plugin.getEnhancedChestManager().debugChestContents(chest);
            player.sendMessage(plugin.getLanguageManager().getMessage("debug-chest-success"));
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("debug-chest-target"));
        }
        return true;
    }

    /**
     * Displays optional ItemsAdder detection and data readiness state.
     */
    private boolean handleItemsAdderStatus(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("ia-status", Map.of(
                "enabled", String.valueOf(plugin.isItemsAdderEnabled()),
                "loaded", String.valueOf(plugin.isItemsAdderLoaded())
        )));
        return true;
    }

    /**
     * Prints the active database schema to the console for administrators.
     */
    private boolean handleDatabaseCheck(CommandSender sender) {
        plugin.getDatabaseManager().checkTableStructure();
        sender.sendMessage(plugin.getLanguageManager().getMessage("database-checked"));
        return true;
    }

    /**
     * Runs a player-only handler or sends the standard console rejection message.
     */
    private boolean withPlayer(CommandSender sender, PlayerCommand handler) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }
        return handler.execute(player);
    }

    /**
     * Lists safe stage basenames available for command completion.
     */
    private List<String> getAvailableStageFiles() {
        List<String> stageFiles = new ArrayList<>();
        File blocksDirectory = new File(plugin.getDataFolder(), "blocks");
        File[] files = blocksDirectory.listFiles((directory, name) -> name.matches("[A-Za-z0-9_-]+\\.yml"));
        if (files != null) {
            for (File file : files) {
                stageFiles.add(file.getName().substring(0, file.getName().length() - 4));
            }
        }
        return stageFiles.stream().sorted().toList();
    }

    /**
     * Defines subcommands and their required permissions in display order.
     */
    private static Map<String, String> createSubcommandPermissions() {
        Map<String, String> permissions = new LinkedHashMap<>();
        permissions.put("help", "kaoneblock.help");
        permissions.put("start", "kaoneblock.start");
        permissions.put("stop", "kaoneblock.stop");
        permissions.put("reload", "kaoneblock.reload");
        permissions.put("log", "kaoneblock.log");
        permissions.put("debug", "kaoneblock.debug");
        permissions.put("ia-status", "kaoneblock.debug");
        permissions.put("debugchest", "kaoneblock.debug");
        permissions.put("reset-stage", "kaoneblock.admin");
        permissions.put("set", "kaoneblock.admin");
        permissions.put("checkdb", "kaoneblock.admin");
        return java.util.Collections.unmodifiableMap(permissions);
    }

    /**
     * Represents one player-only command handler.
     */
    @FunctionalInterface
    private interface PlayerCommand {
        boolean execute(Player player);
    }
}
