package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class LogManager {
    private final KaOneBlock plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final File logDirectory;
    private boolean enabled;

    public LogManager(KaOneBlock plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("log", true);
        this.logDirectory = new File(plugin.getDataFolder(), "logs");

        // 创建日志目录
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            plugin.getLogger().warning("Failed to create log directory: " + logDirectory.getAbsolutePath());
        }
    }

    /**
     * 记录宝箱生成日志
     */
    public void logChestGeneration(String player, Location location, String chestConfig) {
        if (!enabled) return;

        Date now = new Date();
        String dateStr = dateFormat.format(now);
        String timeStr = timeFormat.format(now);

        File logFile = new File(logDirectory, dateStr + ".log");

        // 从位置获取世界名称
        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "未知世界";

        String locationStr = String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());

        // 使用字符串拼接代替格式化
        String logMessage = "[" + timeStr + "] " + player + " 在世界 " + worldName + " 的位置 " + locationStr + " 生成了宝箱 (配置: " + chestConfig + ")";

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write to log file", e);
        }
    }

    /**
     * 记录方块生成日志 (Material 版本)
     */
    public void logBlockGeneration(String player, Location location, Material block) {
        logBlockGeneration(player, location, block.name());
    }

    /**
     * 记录方块生成日志 (字符串版本)
     */
    public void logBlockGeneration(String player, Location location, String blockType) {
        if (!enabled) return;

        Date now = new Date();
        String dateStr = dateFormat.format(now);
        String timeStr = timeFormat.format(now);

        File logFile = new File(logDirectory, dateStr + ".log");

        // 从位置获取世界名称
        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "未知世界";

        String locationStr = String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());

        String blockName = formatBlockName(blockType);

        String logMessage = "[" + timeStr + "] " + player + " 在世界 " + worldName + " 的位置 " + locationStr + " 生成方块为 " + blockName;

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write to log file", e);
        }
    }

    /**
     * 记录方块替换日志
     */
    public void logBlockReplacement(String player, Location location, String blockType) {
        if (!enabled) return;

        Date now = new Date();
        String dateStr = dateFormat.format(now);
        String timeStr = timeFormat.format(now);

        File logFile = new File(logDirectory, dateStr + ".log");

        // 从位置获取世界名称
        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "未知世界";

        String locationStr = String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());

        String blockName = formatBlockName(blockType);

        String logMessage = "[" + timeStr + "] " + player + " 在世界 " + worldName + " 的位置 " + locationStr + " 替换方块为 " + blockName;

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write to log file", e);
        }
    }

    private String formatBlockName(String blockType) {
        if (blockType.startsWith("IA:")) {
            return blockType.substring(3).replace("_", " ");
        }
        return blockType.toLowerCase().replace("_", " ");
    }

    /**
     * 更新日志状态
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}