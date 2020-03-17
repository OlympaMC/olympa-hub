package fr.olympa.hub.gui;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import fr.olympa.api.gui.OlympaGUI;
import fr.olympa.api.item.OlympaItemBuild;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.utils.utility.Heads;

public class GuiMenu extends OlympaGUI {
	private static ItemStack ITEM_ZTA = (new OlympaItemBuild(Material.IRON_CHESTPLATE, "&7&l▪ &6&lZTA &7&l▪")).flag(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES }).lore(new String[] { "", "&7Description.", "", "&7 Status: &aOuvert", "&7 Joueurs: &60", "" }).build();
	private static ItemStack ITEM_PVPFAC = (new OlympaItemBuild(Material.DIAMOND_SWORD, "&7&l▪ &c&lPvP-Faction &7&l▪")).flag(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES }).lore(new String[] { "", "&7Description.", "", "&7 Status: &aOuvert", "&7 Joueurs: &60", "" }).build();
	private static ItemStack ITEM_CREATIF = (new OlympaItemBuild(Material.GRASS_BLOCK, "&7&l▪ &a&lCREATIF &7&l▪")).flag(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES }).lore(new String[] { "", "&7Description.", "", "&7 Status: &aOuvert", "&7 Joueurs: &60", "" }).build();
	private static ItemStack ITEM_DRAW = (new OlympaItemBuild(Material.PAPER, "&7&l▪ &d&lDRAW BATTLE &7&l▪")).flag(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES }).lore(new String[] { "", "&7Description.", "", "&7 Status: &aOuvert", "&7 Joueurs: &60", "" }).build();
	private static ItemStack ITEM_SPAWN = (new OlympaItemBuild(Material.RED_BED, "&eRetour au spawn")).flag(new ItemFlag[] { ItemFlag.HIDE_ATTRIBUTES }).lore(new String[] { "", "&7Pour retourner au spawn.", "" }).build();
	private static ItemStack ITEM_AMIS;

	@SuppressWarnings ("deprecation")
	public GuiMenu(Player p) {
		super("&6&lOlympa &8| &7Menu", 5);
		int middle = this.inv.getSize() / 2;

		ItemStack vitre1 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre2 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre3 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre4 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre5 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre6 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre7 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre8 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre9 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre10 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre11 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre12 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre13 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre14 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre15 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre16 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre17 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre18 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre19 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre20 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre21 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre22 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);

		ItemStack fleche1 = new ItemStack(Material.LEGACY_SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
		;
		SkullMeta sm = (SkullMeta) fleche1.getItemMeta();
		Heads.setHead(sm, "http://textures.minecraft.net/texture/51d383401f77beffcb998c2cf79b7afee23f18c41d8a56affed79bb56e2267a3");
		sm.setDisplayName("§7§l▪ §dAmis §7§l▪");
		sm.setLore(Arrays.asList("", "§ePour ouvrir le menu de tes amis", ""));
		fleche1.setItemMeta(sm);
		ITEM_AMIS = fleche1;
		this.inv.setItem(middle - 1, ITEM_ZTA);
		this.inv.setItem(middle + 1, ITEM_PVPFAC);
		this.inv.setItem(10, ITEM_CREATIF);
		this.inv.setItem(16, ITEM_DRAW);
		this.inv.setItem(40, ITEM_SPAWN);
		this.inv.setItem(38, ITEM_AMIS);

		this.inv.setItem(0, vitre1);
		this.inv.setItem(1, vitre2);
		this.inv.setItem(2, vitre3);
		this.inv.setItem(3, vitre4);
		this.inv.setItem(4, vitre5);
		this.inv.setItem(5, vitre6);
		this.inv.setItem(6, vitre7);
		this.inv.setItem(7, vitre8);
		this.inv.setItem(8, vitre9);
		this.inv.setItem(9, vitre10);
		this.inv.setItem(17, vitre11);
		this.inv.setItem(18, vitre12);
		this.inv.setItem(26, vitre13);
		this.inv.setItem(27, vitre14);
		this.inv.setItem(28, vitre15);
		this.inv.setItem(29, vitre16);
		this.inv.setItem(30, vitre17);
		this.inv.setItem(31, vitre18);
		this.inv.setItem(32, vitre19);
		this.inv.setItem(33, vitre20);
		this.inv.setItem(34, vitre21);
		this.inv.setItem(35, vitre22);

	}

	public boolean onClick(Player p, ItemStack current, int slot, ClickType click) {
		if (ITEM_ZTA.isSimilar(current)) {
			p.sendMessage("§aTéléportation en cours ...");
			p.closeInventory();
			OlympaHub.getInstance().getMessageChannel().connectToServer(p, "zta1");
		}else if (ITEM_PVPFAC.isSimilar(current)) {
			p.sendMessage("§aTéléportation en cours ...");
			p.closeInventory();
			OlympaHub.getInstance().getMessageChannel().connectToServer(p, "pvpfac1");
		}else if (ITEM_SPAWN.isSimilar(current)) {
			p.closeInventory();
			p.performCommand("spawn");
		}else if (current.getItemMeta().getDisplayName().contains("§7§l▪ §dAmis §7§l▪")) {
			new GuiAmis(p).create(p);
		}else if (ITEM_CREATIF.isSimilar(current)) {
			p.sendMessage("§aTéléportation en cours ...");
			p.closeInventory();
			OlympaHub.getInstance().getMessageChannel().connectToServer(p, "creatif1");
		}else if (ITEM_DRAW.isSimilar(current)) {
			p.sendMessage("§aTéléportation en cours ...");
			p.closeInventory();
			OlympaHub.getInstance().getMessageChannel().connectToServer(p, "drawbattle1");
		}
		return true;
	}
}
