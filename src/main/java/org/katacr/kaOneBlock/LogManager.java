package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.World;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Writes bounded, ordered activity logs away from the Minecraft server thread.
 */
public class LogManager {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final KaOneBlock plugin;
    private final File logDirectory;
    private final ThreadPoolExecutor writerExecutor;
    private final AtomicBoolean queueWarningLogged = new AtomicBoolean();
    private volatile boolean enabled;
    private volatile boolean closed;
    private BufferedWriter currentWriter;
    private LocalDate currentWriterDate;

    public LogManager(KaOneBlock plugin) {
        this.plugin = plugin;
        enabled = plugin.getConfig().getBoolean("log", true);
        logDirectory = new File(plugin.getDataFolder(), "logs");
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            plugin.getLogger().warning("Failed to create log directory: " + logDirectory.getAbsolutePath());
        }
        writerExecutor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(4096),
                runnable -> {
                    Thread thread = new Thread(runnable, "KaOneBlock-Log");
                    thread.setDaemon(true);
                    return thread;
                },
                (task, executor) -> {
                    if (queueWarningLogged.compareAndSet(false, true)) {
                        plugin.getLogger().warning("KaOneBlock log queue is full; dropping activity log entries");
                    }
                }
        );
    }

    /**
     * Queues a generated chest activity entry.
     */
    public void logChestGeneration(String player, Location location, String chestConfig) {
        enqueue(player + " 在 " + formatLocation(location) + " 生成宝箱 (配置: " + chestConfig + ")");
    }

    /**
     * Queues an initial generated block activity entry.
     */
    public void logBlockGeneration(String player, Location location, String blockType) {
        enqueue(player + " 在 " + formatLocation(location) + " 生成方块 " + formatBlockName(blockType));
    }

    /**
     * Queues a replacement block activity entry.
     */
    public void logBlockReplacement(String player, Location location, String blockType) {
        enqueue(player + " 在 " + formatLocation(location) + " 替换方块为 " + formatBlockName(blockType));
    }

    /**
     * Queues a generated entity activity entry.
     */
    public void logEntityGeneration(String player, Location location, String entityName, String entityPack) {
        enqueue(player + " 在 " + formatLocation(location) + " 生成实体 " + entityName + " (实体包: " + entityPack + ")");
    }

    /**
     * Changes whether new activity log entries are accepted.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Flushes queued activity entries and closes the rolling file writer.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        writerExecutor.shutdown();
        try {
            if (!writerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out while flushing KaOneBlock activity logs");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        closeCurrentWriter();
    }

    /**
     * Submits one timestamped message to the bounded writer queue.
     */
    private void enqueue(String message) {
        if (!enabled || closed) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String line = "[" + TIME_FORMAT.format(now) + "] " + message;
        writerExecutor.execute(() -> writeLine(now.toLocalDate(), line));
    }

    /**
     * Writes one line to the active daily log file on the dedicated writer thread.
     */
    private void writeLine(LocalDate date, String line) {
        try {
            if (!date.equals(currentWriterDate)) {
                closeCurrentWriter();
                currentWriterDate = date;
                currentWriter = Files.newBufferedWriter(
                        new File(logDirectory, date + ".log").toPath(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }
            currentWriter.write(line);
            currentWriter.newLine();
            currentWriter.flush();
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write KaOneBlock activity log", exception);
        }
    }

    /**
     * Closes the currently active daily writer and clears its state.
     */
    private void closeCurrentWriter() {
        if (currentWriter == null) {
            return;
        }
        try {
            currentWriter.close();
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to close KaOneBlock activity log", exception);
        } finally {
            currentWriter = null;
            currentWriterDate = null;
        }
    }

    /**
     * Formats a location with a world name so logs remain unambiguous.
     */
    private String formatLocation(Location location) {
        World world = location.getWorld();
        String worldName = world == null ? "unknown" : world.getName();
        return worldName + " (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    /**
     * Formats a vanilla or ItemsAdder identifier for logs.
     */
    private String formatBlockName(String blockType) {
        String normalized = blockType.regionMatches(true, 0, "ia:", 0, 3) ? blockType.substring(3) : blockType;
        return normalized.toLowerCase().replace('_', ' ');
    }
}
