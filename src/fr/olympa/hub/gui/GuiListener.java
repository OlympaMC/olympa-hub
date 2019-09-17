package fr.olympa.hub.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if (item == null) {
			return;
		}

		if (item.equals(GuiHub.COMPASS.build())) {
			GuiHub.mainGui(player);
		}
	}
}
