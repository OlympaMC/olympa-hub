package fr.olympa.hub.gui;

import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import fr.olympa.api.item.OlympaItemBuild;

public class ItemHotbar {
	public static OlympaItemBuild ITEM_COMPASS = new OlympaItemBuild(Material.CHEST, "&6Menu &7(Clique droit)");
	@SuppressWarnings ("deprecation")
	public static ItemStack ITEM_PROFILE = new ItemStack(Material.LEGACY_SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
	public static OlympaItemBuild ITEM_AMIS = new OlympaItemBuild(Material.PAPER, "&dAmis &7(Clique droit)");
	public static OlympaItemBuild ITEM_COSMETICS = new OlympaItemBuild(Material.ENDER_CHEST, "&5Cosm�tiques &7(Clique droit)");

	public static void set(Player player) {
		OlympaItemBuild item1 = ITEM_COMPASS;
		OlympaItemBuild item3 = ITEM_AMIS;
		OlympaItemBuild item4 = ITEM_COSMETICS;
		SkullMeta sm = (SkullMeta) ITEM_PROFILE.getItemMeta();
		sm.setDisplayName("�aProfile �7(Clique droit)");
		sm.setOwner(player.getName());
		ITEM_PROFILE.setItemMeta(sm);
		//item2.skullowner(player);
		player.getInventory().setItem(1, item1.build());
		player.getInventory().setItem(2, ITEM_PROFILE);
		player.getInventory().setItem(6, item3.build());
		player.getInventory().setItem(7, item4.build());
	}
}
