package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

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
     * 记录日志
     *
     * @param player   玩家名称
     * @param world    世界名称
     * @param location 位置
     * @param block    方块类型
     */
    public void logBlockGeneration(String player, String world, Location location, Material block) {
        if (!enabled) return;

        // 获取当前日期和时间
        Date now = new Date();
        String dateStr = dateFormat.format(now);
        String timeStr = timeFormat.format(now);

        // 创建日志文件路径
        File logFile = new File(logDirectory, dateStr + ".log");

        // 格式化位置信息
        String locationStr = String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());

        // 格式化方块名称
        String blockName = block.name().toLowerCase().replace("_", " ");

        // 创建日志消息
        String logMessage = String.format("[%s] %s 在世界 %s 的位置 %s 生成方块为 %s", timeStr, player, world, locationStr, blockName);

        // 写入日志文件
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to write to log file", e);
        }
    }

    /**
     * 记录方块替换日志
     *
     * @param player   玩家名称
     * @param world    世界名称
     * @param location 位置
     * @param block    方块类型
     */
    public void logBlockReplacement(String player, String world, Location location, String blockType) {
        if (!enabled) return;

        // 获取当前日期和时间
        Date now = new Date();
        String dateStr = dateFormat.format(now);
        String timeStr = timeFormat.format(now);

        // 创建日志文件路径
        File logFile = new File(logDirectory, dateStr + ".log");

        // 格式化位置信息
        String locationStr = String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());

        // 格式化方块名称
        String blockName = formatBlockName(blockType);

        // 创建日志消息
        String logMessage = String.format("[%s] %s 在世界 %s 的位置 %s 替换方块为 %s", timeStr, player, world, locationStr, blockName);

        // 写入日志文件
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to write to log file", e);
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
     *
     * @param enabled 是否启用日志
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}