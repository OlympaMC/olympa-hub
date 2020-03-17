package fr.olympa.hub.gui;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import fr.olympa.api.gui.OlympaGUI;

public class GuiProfile extends OlympaGUI {

	@SuppressWarnings ("deprecation")
	public GuiProfile(Player p) {
		super("§6Olympa §8| §7Profile", 3);

		//OlympaPlayer olympaPlayer = AccountProvider.get(p.getUniqueId());
		ItemStack item = new ItemStack(Material.LEGACY_SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
		//ItemStack item = new OlympaItemBuild(Material.LEGACY_SKULL_ITEM, "§8▪ §a" + p.getName() + "§8 ▪").lore("", "§8»§7 Money: §a0", "§8»§7 Grade: §cgrade", "", "§8»§7 Première connexion: §edate", "§8»§7 Dernière connexion: §6co", "§8»§7 Email: §bemail", "", "").build();
		SkullMeta sm = (SkullMeta) item.getItemMeta();
		sm.setOwner(p.getName());
		sm.setDisplayName("§8▪ §a" + p.getName() + "§8 ▪");
		sm.setLore(Arrays.asList("", "§8»§7 Money: §a0", "§8»§7 Grade: §cgrade", "", "§8»§7 Première connexion: §edate", "§8»§7 Dernière connexion: §6co", "§8»§7 Email: §bemail", ""));
		item.setItemMeta(sm);
		this.inv.setItem(4, item);

	}

	@Override
	public boolean onClick(Player p, ItemStack current, int slot, ClickType click) {
		return true;
	}

}
