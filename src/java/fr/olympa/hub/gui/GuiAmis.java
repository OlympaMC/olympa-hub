package fr.olympa.hub.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import fr.olympa.api.gui.OlympaGUI;
import fr.olympa.api.objects.OlympaPlayer;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.utils.utility.Heads;

public class GuiAmis extends OlympaGUI {

	Map<String, OlympaPlayer> amis = new HashMap<String, OlympaPlayer>();

	@SuppressWarnings ("deprecation")
	public GuiAmis(Player player) {
		super("&6Olympa &8| &7Amis", 6);

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
		ItemStack vitre23 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack vitre24 = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);

		ItemStack fleche1 = new ItemStack(Material.LEGACY_SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
		SkullMeta sm = (SkullMeta) fleche1.getItemMeta();
		sm.setDisplayName("�e�lCLIQUE");
		sm.setLore(Arrays.asList("", "�7Clique ici pour page pr�c�dente", ""));
		Heads.setHead(sm, "http://textures.minecraft.net/texture/eed78822576317b048eea92227cd85f7afcc44148dcb832733baccb8eb56fa1");
		fleche1.setItemMeta(sm);

		ItemStack fleche2 = new ItemStack(Material.LEGACY_SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
		SkullMeta sm2 = (SkullMeta) fleche2.getItemMeta();
		sm2.setDisplayName("�e�lCLIQUE");
		sm2.setLore(Arrays.asList("", "�7Clique ici pour la page suivante", ""));
		Heads.setHead(sm2, "http://textures.minecraft.net/texture/715445da16fab67fcd827f71bae9c1d2f90c73eb2c1bd1ef8d8396cd8e8");
		fleche2.setItemMeta(sm2);

		//Syst�me page � rajouter et fix ce syst�me
		for (Player players : Bukkit.getOnlinePlayers()) {

			String color = "";

			String serverPlayer = OlympaCore.getInstance().getServer().getName();//incorrect
			String serverTarget = OlympaCore.getInstance().getServer().getName();
			OlympaPlayer amisPlayers = AccountProvider.get(players.getUniqueId());
			amis.put(serverTarget, amisPlayers);

			for (OlympaPlayer olympaPlayers : amis.values()) {
				//Player playerss = olympaPlayers.getPlayer();
				Player playerss = player;
				if (playerss.isOnline()) {
					if (serverPlayer == serverTarget)
						color = "�a";
					else color = "�6";
				}else {
					color = "�c";
				}
			}

			ItemStack amisplayers = new ItemStack(Material.PAPER);
			ItemMeta im = (ItemMeta) amisplayers.getItemMeta();
			im.setDisplayName(color + players.getName());
			im.setLore(Arrays.asList("", "�7Serveur: �a" + serverTarget, "", "�7Grade: ", "�7Premier connexion: �e", "�7Derni�re connection: �e", ""));
			amisplayers.setItemMeta(im);
			this.inv.addItem(amisplayers);
		}

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
		this.inv.setItem(35, vitre15);
		this.inv.setItem(36, vitre16);
		this.inv.setItem(37, vitre17);
		this.inv.setItem(38, vitre18);
		this.inv.setItem(39, vitre19);
		this.inv.setItem(40, vitre20);
		this.inv.setItem(41, vitre21);
		this.inv.setItem(42, vitre22);
		this.inv.setItem(43, vitre23);
		this.inv.setItem(44, vitre24);

		this.inv.setItem(50, fleche2);
		this.inv.setItem(48, fleche1);
	}

	@Override
	public boolean onClick(Player player, ItemStack current, int slot, ClickType clicks) {

		return true;
	}

}