package fr.olympa.hub.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import fr.olympa.hub.commonerrors.Messages;

public class GuiHubListener
		implements Listener {
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		if (item == null) {
			return;
		}

		if (item.equals(ItemHotbar.ITEM_COMPASS.build()))
			new GuiMenu(player).create(player);
		else if (item.getItemMeta().getDisplayName().contains(ItemHotbar.ITEM_PROFILE.getItemMeta().getDisplayName()))
			new GuiProfile(player).create(player);
		else if (item.equals(ItemHotbar.ITEM_AMIS.build()))
			new GuiAmis(player).create(player);
		else if (item.equals(ItemHotbar.ITEM_COSMETICS.build())) Messages.ITEM_NOT_AVAIBLE.send(player);
	}
}