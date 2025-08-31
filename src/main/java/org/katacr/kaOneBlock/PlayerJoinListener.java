package org.katacr.kaOneBlock;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final KaOneBlock plugin;

    public PlayerJoinListener(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getStageManager().onPlayerJoin(event.getPlayer());
    }
}