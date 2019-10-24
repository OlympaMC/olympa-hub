package fr.olympa.hub.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class GuiHubListener implements Listener {

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if (item == null) {
			return;
		}

		if (item.equals(ItemHotbar.ITEM_COMPASS.build())) {
			new GuiMenu().open(player);
		}
	}
}
