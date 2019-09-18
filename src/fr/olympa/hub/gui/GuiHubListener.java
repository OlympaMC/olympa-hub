package fr.olympa.hub.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import fr.tristiisch.olympa.api.customevents.GuiClickEvent;
import fr.tristiisch.olympa.api.gui.GuiHandler;
import fr.tristiisch.olympa.api.gui.OlympaGuiBuild;

public class GuiHubListener implements Listener {

	@EventHandler
	public void onGuiClick(GuiClickEvent event) {
		// Player player = event.getPlayer();
		OlympaGuiBuild gui = event.getGui();
		InventoryClickEvent clickEvent = event.getInventoryClickEvent();
		int slot = clickEvent.getSlot();

		GuiHub guiHub = GuiHub.get(gui.getId());
		switch (guiHub) {

		case MENU:
			if (slot == gui.getMiddleSlot()) {
				GuiHandler.cancelInDev(clickEvent);
			}
			break;
		default:
			break;

		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if (item == null) {
			return;
		}

		if (item.equals(GuisHub.COMPASS.build())) {
			GuisHub.mainGui(player);
		}
	}
}
