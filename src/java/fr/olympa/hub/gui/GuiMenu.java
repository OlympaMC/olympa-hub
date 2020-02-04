package fr.olympa.hub.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.gui.OlympaGUI;
import fr.olympa.api.item.OlympaItemBuild;
import fr.olympa.hub.OlympaHub;

public class GuiMenu extends OlympaGUI {

	private static ItemStack ITEM_ZTA = new OlympaItemBuild(Material.DIAMOND_SWORD, "&7&l■ &6&lZTA &7&l■").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Description.", "", "&7 Status: &aOuvert","§7 Joueurs: §60" , "").build();
	private static ItemStack ITEM_PVFAC = new OlympaItemBuild(Material.DIAMOND_SWORD, "&7&l■ &cPvP-Faction &7&l■").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Description.", "", "&7 Status: &aOuvert","§7 Joueurs: §60" , "").build();
	private static ItemStack ITEM_SPAWN = new OlympaItemBuild(Material.RED_BED, "&eRetour au spawn").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Pour retourner au spawn.", "").build();

	public GuiMenu(Player p) {
		super("&6§lOlympa &8| &7Menu", 5);
		int middle = this.inv.getSize() / 2;

		this.inv.setItem(middle - 1, ITEM_ZTA);
		this.inv.setItem(middle + 1, ITEM_PVFAC);
		this.inv.setItem(40, ITEM_SPAWN);

		/*OlympaPlayer olympaPlayer = AccountProvider.get(p.getUniqueId());
		OlympaItemBuild item = new OlympaItemBuild("&7").lore("", "&6Grade: &e").skullowner(p);
		item.addName(p.getName());
		item.setLore(1, olympaPlayer.getGroupsToHumainString());
		this.inv.setItem(this.inv.getSize() - 1, item.build());*/
	}

	@Override
	public boolean onClick(Player p, ItemStack current, int slot, ClickType click) {
		if (ITEM_ZTA.isSimilar(current)) {
			p.sendMessage("§aTéléportation en cours ...");
			p.closeInventory();
			OlympaHub.getInstance().getMessageChannel().connectToServer(p, "zta1");
			
		} else if (ITEM_PVFAC.isSimilar(current)) {
			p.sendMessage("§aTéléportation en cours ...");
			p.closeInventory();
			OlympaHub.getInstance().getMessageChannel().connectToServer(p, "pvpfac1");
		} else if (ITEM_SPAWN.isSimilar(current)) {
			p.closeInventory();
			p.performCommand("spawn");
		}
		return true;
	}

}
