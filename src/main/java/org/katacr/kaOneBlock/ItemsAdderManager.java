package org.katacr.kaOneBlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * ItemsAdder 管理器
 * 处理 ItemsAdder 相关功能
 * 
 * 这个类使用反射来检测和使用 ItemsAdder，避免在未安装 ItemsAdder 时出现 ClassNotFoundException
 */
public class ItemsAdderManager {
    private final JavaPlugin plugin;
    private boolean enabled = false;
    private boolean loaded = false;
    
    // 反射类
    private Class<?> customStackClass;
    private Class<?> customBlockClass;

    public ItemsAdderManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initialize();
    }

    /**
     * 获取任意ItemsAdder物品
     *
     * @param itemId 完整的物品ID（格式：namespace:item_id）
     * @return 物品堆，如果获取失败则返回null
     */
    public ItemStack getCustomItem(String itemId) {
        if (!enabled || !loaded) {
            plugin.getLogger().warning("ItemsAdder is not available, cannot get custom item: " + itemId);
            return null;
        }
        
        if (customStackClass == null) {
            return null;
        }
        
        try {
            // 使用反射调用 CustomStack.getInstance
            Object customStack = customStackClass.getMethod("getInstance", String.class).invoke(null, itemId);
            if (customStack != null) {
                return (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get custom item: " + itemId, e);
        }
        return null;
    }

    /**
     * 初始化 ItemsAdder 管理器
     */
    public void initialize() {
        // 检查 ItemsAdder 插件是否安装并启用
        Plugin itemsAdderPlugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdderPlugin != null && itemsAdderPlugin.isEnabled()) {
            enabled = true;
            plugin.getLogger().info("ItemsAdder detected, enabling integration");
            
            // 尝试加载 ItemsAdder 类
            try {
                ClassLoader classLoader = itemsAdderPlugin.getClass().getClassLoader();
                
                // 加载必要的类
                customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack", false, classLoader);
                customBlockClass = Class.forName("dev.lone.itemsadder.api.CustomBlock", false, classLoader);
                
                // 检查 ItemsAdder 数据是否已加载
                if (isItemsAdderDataLoaded()) {
                    loaded = true;
                    plugin.getLogger().info("ItemsAdder data loaded");
                } else {
                    plugin.getLogger().info("Waiting for ItemsAdder data to load...");
                    // 注册事件监听器等待数据加载
                    Bukkit.getPluginManager().registerEvents(new ItemsAdderListener(), plugin);
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("Failed to load ItemsAdder classes: " + e.getMessage());
                enabled = false;
            }
        } else {
            plugin.getLogger().info("ItemsAdder not found, custom blocks will not work");
        }
    }

    /**
     * 检查 ItemsAdder 数据是否已加载
     *
     * @return 是否已加载
     */
    private boolean isItemsAdderDataLoaded() {
        if (customBlockClass == null) {
            return false;
        }
        
        try {
            // 尝试获取一个自定义方块实例
            Object customBlock = customBlockClass.getMethod("getInstance", String.class).invoke(null, "dirt");
            return customBlock != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 放置 ItemsAdder 方块
     *
     * @param location 位置
     * @param blockId  方块ID
     * @return 是否成功放置
     */
    public boolean placeBlock(Location location, String blockId) {
        if (!enabled || !loaded) {
            plugin.getLogger().warning("Cannot place ItemsAdder block: integration not ready");
            return false;
        }

        if (customBlockClass == null) {
            return false;
        }

        try {
            // 尝试获取自定义方块实例
            Object customBlock = customBlockClass.getMethod("getInstance", String.class).invoke(null, blockId);
            if (customBlock != null) {
                // 放置方块
                customBlockClass.getMethod("place", Location.class).invoke(customBlock, location);

                return true;
            } else {
                plugin.getLogger().warning("Custom block not found: " + blockId);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to place custom block: " + blockId, e);
            return false;
        }
    }

    // 添加新方法：静默获取物品（不记录警告）
    public ItemStack getCustomItemSilently(String itemId) {
        if (!enabled || !loaded || customStackClass == null) return null;
        try {
            Object customStack = customStackClass.getMethod("getInstance", String.class).invoke(null, itemId);
            return (customStack != null) ? (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查 ItemsAdder 是否可用
     *
     * @return 是否可用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 检查 ItemsAdder 数据是否已加载
     *
     * @return 是否已加载
     */
    public boolean isLoaded() {
        return loaded;
    }


    /**
     * ItemsAdder 事件监听器
     */
    private class ItemsAdderListener implements Listener {
        // 使用反射处理事件，避免直接依赖 ItemsAdder 类
        @EventHandler
        public void onItemsAdderLoad(Object event) {
            loaded = true;
            plugin.getLogger().info("ItemsAdder data loaded, API ready");

            // 取消注册此监听器
            HandlerList.unregisterAll(this);
        }
    }
}