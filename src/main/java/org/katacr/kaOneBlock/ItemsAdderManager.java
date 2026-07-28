package org.katacr.kaOneBlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

/**
 * Integrates with ItemsAdder through cached reflection without linking optional API classes at startup.
 */
public class ItemsAdderManager {
    private final KaOneBlock plugin;
    private boolean enabled;
    private boolean loaded;
    private Class<?> customStackClass;
    private Class<?> customBlockClass;
    private Method getCustomStack;
    private Method getItemStack;
    private Method getCustomBlock;
    private Method placeCustomBlock;

    public ItemsAdderManager(KaOneBlock plugin) {
        this.plugin = plugin;
        initialize();
    }

    /**
     * Detects ItemsAdder, caches its reflective API and observes its real data-load event.
     */
    public final void initialize() {
        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null || !itemsAdder.isEnabled()) {
            enabled = false;
            loaded = false;
            return;
        }

        try {
            ClassLoader classLoader = itemsAdder.getClass().getClassLoader();
            customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack", false, classLoader);
            customBlockClass = Class.forName("dev.lone.itemsadder.api.CustomBlock", false, classLoader);
            getCustomStack = customStackClass.getMethod("getInstance", String.class);
            getItemStack = customStackClass.getMethod("getItemStack");
            getCustomBlock = customBlockClass.getMethod("getInstance", String.class);
            placeCustomBlock = customBlockClass.getMethod("place", Location.class);
            enabled = true;

            registerLoadEvent(classLoader);
            loaded = queryLoadedState(classLoader);
            plugin.getLogger().info("ItemsAdder integration enabled; data loaded=" + loaded);
        } catch (ReflectiveOperationException exception) {
            enabled = false;
            loaded = false;
            plugin.getLogger().log(Level.WARNING, "ItemsAdder API is incompatible", exception);
        }
    }

    /**
     * Returns a custom item and logs a useful warning when the integration is unavailable.
     */
    public ItemStack getCustomItem(String itemId) {
        ItemStack item = getCustomItemSilently(itemId);
        if (item == null) {
            plugin.getLogger().warning("Cannot resolve ItemsAdder item: " + itemId);
        }
        return item;
    }

    /**
     * Returns a cloned custom item without producing warnings during optional loot resolution.
     */
    public ItemStack getCustomItemSilently(String itemId) {
        if (!enabled || !loaded || getCustomStack == null || getItemStack == null) {
            return null;
        }
        try {
            Object customStack = getCustomStack.invoke(null, itemId);
            if (customStack == null) {
                return null;
            }
            ItemStack item = (ItemStack) getItemStack.invoke(customStack);
            return item == null ? null : item.clone();
        } catch (ReflectiveOperationException | ClassCastException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to resolve ItemsAdder item: " + itemId, exception);
            return null;
        }
    }

    /**
     * Places a custom block through the cached ItemsAdder API.
     */
    public boolean placeBlock(Location location, String blockId) {
        if (!enabled || !loaded || getCustomBlock == null || placeCustomBlock == null) {
            return false;
        }
        try {
            Object customBlock = getCustomBlock.invoke(null, blockId);
            if (customBlock == null) {
                return false;
            }
            placeCustomBlock.invoke(customBlock, location);
            return true;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to place ItemsAdder block: " + blockId, exception);
            return false;
        }
    }

    /**
     * Reports whether the optional plugin and compatible API classes are available.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Reports whether ItemsAdder has completed loading its content data.
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Queries supported ItemsAdder readiness APIs without probing an arbitrary custom block ID.
     */
    private boolean queryLoadedState(ClassLoader classLoader) {
        try {
            Class<?> itemsAdderApi = Class.forName("dev.lone.itemsadder.api.ItemsAdder", false, classLoader);
            Object result = itemsAdderApi.getMethod("isLoaded").invoke(null);
            if (result instanceof Boolean state) {
                return state;
            }
        } catch (ReflectiveOperationException ignored) {
            // Older API versions do not expose a direct readiness method.
        }

        for (Class<?> apiClass : List.of(customStackClass, customBlockClass)) {
            try {
                Object ids = apiClass.getMethod("getNamespacedIds").invoke(null);
                if (ids != null) {
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
                // Continue to the other supported readiness probe.
            }
        }
        return false;
    }

    /**
     * Registers the optional ItemsAdder load event through Bukkit's dynamic event API.
     */
    @SuppressWarnings("unchecked")
    private void registerLoadEvent(ClassLoader classLoader) {
        for (String className : List.of(
                "dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent",
                "dev.lone.itemsadder.api.events.ItemsAdderLoadDataEvent"
        )) {
            try {
                Class<?> rawEventClass = Class.forName(className, false, classLoader);
                if (!Event.class.isAssignableFrom(rawEventClass)) {
                    continue;
                }
                Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
                Listener listener = new Listener() {
                };
                Bukkit.getPluginManager().registerEvent(
                        eventClass,
                        listener,
                        EventPriority.MONITOR,
                        (registered, event) -> {
                            loaded = true;
                            HandlerList.unregisterAll(registered);
                            if (plugin.getEnhancedChestManager() != null) {
                                plugin.getEnhancedChestManager().loadChestConfigs();
                            }
                            plugin.getLogger().info("ItemsAdder data loaded; integration is ready");
                        },
                        plugin
                );
                return;
            } catch (ClassNotFoundException ignored) {
                // Try the package name used by another supported ItemsAdder API generation.
            }
        }
        plugin.getLogger().warning("ItemsAdder load event was not found; readiness will use the immediate API probe");
    }
}
