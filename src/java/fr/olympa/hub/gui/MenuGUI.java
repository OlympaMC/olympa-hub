package fr.olympa.hub.gui;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import fr.olympa.api.gui.OlympaGUI;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.player.OlympaPlayer;
import fr.olympa.api.utils.Prefix;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.servers.ServerInfo;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class MenuGUI extends OlympaGUI {

	private static ItemStack[] basicContents = new ItemStack[54];
	private static TextComponent twitter, discord, website, yt;
	static {
		ItemStack orangeSeparator = ItemUtils.itemSeparator(DyeColor.ORANGE);
		ItemStack yellowSeparator = ItemUtils.itemSeparator(DyeColor.YELLOW);
		for (int slot = 0; slot < 54; slot++) { // fill du orange partout
			basicContents[slot] = orangeSeparator;
		}
		for (int slot : new int[] { 2, 11, 10, 9, 18, 27, 36, 45, 46, 47, 48, 49, 50, 51, 52, 53, 44, 35, 26, 17, 16, 15, 6 }) { // fait le contour jaune
			basicContents[slot] = yellowSeparator;
		}
		basicContents[0] = ItemUtils.skullCustom("§bTwitter", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2M3NDVhMDZmNTM3YWVhODA1MDU1NTkxNDllYTE2YmQ0YTg0ZDQ0OTFmMTIyMjY4MThjMzg4MWMwOGU4NjBmYyJ9fX0=");
		basicContents[1] = ItemUtils.skullCustom("§5Discord", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTNiMTgzYjE0OGI5YjRlMmIxNTgzMzRhZmYzYjViYjZjMmMyZGJiYzRkNjdmNzZhN2JlODU2Njg3YTJiNjIzIn19fQ==");
		basicContents[7] = ItemUtils.skullCustom("§l§ewww.§6§lolympa§e§l.fr", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjM0N2EzOTQ5OWRlNDllMjRjODkyYjA5MjU2OTQzMjkyN2RlY2JiNzM5OWUxMTg0N2YzMTA0ZmRiMTY1YjZkYyJ9fX0=");
		basicContents[8] = ItemUtils.skullCustom("§cYouTube", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTY3OWQ2MzBmODUxYzU4OTdkYTgzYTY0MjUxNzQzM2Y2NWRjZmIzMmIxYmFiYjFmZWMzMmRhNzEyNmE5ZjYifX19");

		twitter = new TextComponent("Rejoins notre Twitter : ");
		twitter.setColor(ChatColor.AQUA);
		twitter.addExtra(URLComponent("@Olympa_fr", "https://twitter.com/olympa_fr", ChatColor.DARK_AQUA));

		discord = new TextComponent("Rejoins notre serveur Discord ");
		discord.setColor(ChatColor.LIGHT_PURPLE);
		discord.addExtra(URLComponent("ici !", "https://discord.gg/guF78Zb", ChatColor.DARK_PURPLE));

		website = new TextComponent("Visite notre site à l'adresse ");
		website.setColor(ChatColor.YELLOW);
		website.addExtra(URLComponent("www.olympa.fr", "https://olympa.fr", ChatColor.GOLD));

		yt = new TextComponent("A venir...");
		yt.setColor(ChatColor.RED);
		yt.setItalic(true);
	}

	private static TextComponent URLComponent(String message, String url, ChatColor color) {
		TextComponent component = new TextComponent(message);
		component.setColor(color);
		component.setBold(true);
		component.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url));
		component.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eClique pour ouvrir le lien !")));
		return component;
	}

	private OlympaPlayer player;

	public MenuGUI(OlympaPlayer player) {
		super("Menu Olympa", 6);
		this.player = player;
		inv.setContents(basicContents);

		ItemUtils.skull(x -> inv.setItem(13, x), "§eMon profil", player.getName(),
				"§8> §7" + player.getName(),
				"§8> §7" + player.getGroupName(),
				"",
				"§8> §7Membre depuis le " + DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.FRANCE).format(new Date(player.getFirstConnection())),
				"",
				"§8> §7" + (player.getEmail() == null ? "§oMail non spécifié" : player.getEmail()),
				"§8> §7Compte Discord " + (player.getDiscordId() == 0 ? "lié !" : "non relié"));

		int slot = 29;
		for (ServerInfo server : OlympaHub.getInstance().serversInfos.servers) {
			inv.setItem(slot, server.getMenuItem());
			slot += 2;
		}
	}

	@Override
	public boolean onClick(Player p, ItemStack current, int slot, ClickType click) {
		TextComponent link = getSlotLink(slot);
		if (link != null) {
			p.spigot().sendMessage(link);
			return true;
		}
		ServerInfo server = OlympaHub.getInstance().serversInfos.servers.get((slot - 29) / 2);
		if (server != null) {
			if (server.getStatus().getPermission().hasPermission(player)) {
				Prefix.DEFAULT_GOOD.sendMessage(p, "Tu vas être transféré au serveur %s sous peu !", server.name);
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF("Connect");
				out.writeUTF(server.name);
				p.sendPluginMessage(OlympaHub.getInstance(), "BungeeCord", out.toByteArray());
			}else {
				Prefix.DEFAULT_BAD.sendMessage(p, "Tu n'as pas la permission de te connecter à ce serveur.");
			}
		}
		return true;
	}

	private TextComponent getSlotLink(int slot) {
		switch (slot) {
		case 0:
			return twitter;
		case 1:
			return discord;
		case 7:
			return website;
		case 8:
			return yt;
		}
		return null;
	}

}
