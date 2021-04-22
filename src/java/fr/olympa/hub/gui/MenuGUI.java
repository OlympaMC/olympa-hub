package fr.olympa.hub.gui;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.gui.OlympaGUI;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.player.OlympaPlayer;
import fr.olympa.api.server.MonitorInfo;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.servers.ServerInfo;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class MenuGUI extends OlympaGUI {

	private static ItemStack[] basicContents = new ItemStack[54];
	private static TextComponent twitter, discord, website, yt;
	static {

		ItemStack orangeSeparator = ItemUtils.itemSeparator(DyeColor.ORANGE);
		ItemStack yellowSeparator = ItemUtils.itemSeparator(DyeColor.YELLOW);

		for (int slot = 9; slot < 18; slot++)
			basicContents[slot] = yellowSeparator;

		basicContents[0] = ItemUtils.skullCustom("§bTwitter",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2M3NDVhMDZmNTM3YWVhODA1MDU1NTkxNDllYTE2YmQ0YTg0ZDQ0OTFmMTIyMjY4MThjMzg4MWMwOGU4NjBmYyJ9fX0=");
		basicContents[1] = ItemUtils.skullCustom("§5Discord",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTNiMTgzYjE0OGI5YjRlMmIxNTgzMzRhZmYzYjViYjZjMmMyZGJiYzRkNjdmNzZhN2JlODU2Njg3YTJiNjIzIn19fQ==");
		basicContents[7] = ItemUtils.skullCustom("§ewww.§6§lolympa§e.fr",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjM0N2EzOTQ5OWRlNDllMjRjODkyYjA5MjU2OTQzMjkyN2RlY2JiNzM5OWUxMTg0N2YzMTA0ZmRiMTY1YjZkYyJ9fX0=");
		basicContents[8] = ItemUtils.skullCustom("§cYouTube",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTY3OWQ2MzBmODUxYzU4OTdkYTgzYTY0MjUxNzQzM2Y2NWRjZmIzMmIxYmFiYjFmZWMzMmRhNzEyNmE5ZjYifX19");

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
		component.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText("§eClique pour ouvrir le lien !"))));
		return component;
	}

	private OlympaPlayer player;

	private Map<Integer, GameType> minigames = new HashMap<>();

	public MenuGUI(OlympaPlayer player) {
		super("Ω | Menu Olympa", 6);
		this.player = player;
		inv.setContents(basicContents);

		ItemUtils.skull(x -> inv.setItem(4, x), "§eMon profil", player.getName(),
				"§8> §7" + player.getName(),
				"§8> §7" + player.getGroupsToHumainString(),
				"",
				"§8> §7Membre depuis le " + DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.FRANCE).format(new Date(player.getFirstConnection() * 1000)),
				"",
				"§8> §7" + (player.getEmail() == null ? "§oMail non spécifié" : player.getEmail()),
				"§8> §7Compte Teamspeak " + (player.getTeamspeakId() == 0 ? "lié !" : "non relié"),
				"§8> §7Version " + (player.getPremiumUniqueId() != null ? "Premium" : "Crack"));
		//				"§8> §7Compte Discord " + (player.getDiscordId() == 0 ? "lié !" : "non relié"));

		OlympaCore.getInstance().retreiveMonitorInfos(serverInfo -> {
			for (MonitorInfo mi : serverInfo) {
				ServerInfo server = OlympaHub.getInstance().serversInfos.getServer(mi);
				if (!server.getServer().canConnect(player))
					continue;
				setServerItem(server);
				server.observe("gui_" + hashCode(), () -> setServerItem(server));
			}
		}, false);

		ConfigurationSection minigamesConfig = OlympaHub.getInstance().getConfig().getConfigurationSection("minigames");

		for (String minigame : minigamesConfig.getKeys(false)) {
			int slot = minigamesConfig.getInt(minigame + ".slot");
			GameType game = GameType.valueOf(minigame);

			minigames.put(slot, game);

			inv.setItem(slot, ItemUtils.item(Material.getMaterial(minigamesConfig.getString(minigame + ".item")),
					"§aDébut " + game.getNameWithArticle(), minigamesConfig.getString(minigame + ".description")));

			//System.out.println(minigame + " - " + minigamesConfig.getString(minigame+".item") + " - " + minigamesConfig.getString(minigame+".description"));
		}
	}

	private void setServerItem(ServerInfo server) {
		inv.setItem(server.slot, server.getMenuItem());
	}

	@Override
	public boolean onClick(Player p, ItemStack current, int slot, ClickType click) {
		TextComponent link = getSlotLink(slot);
		if (link != null) {
			p.spigot().sendMessage(link);
			return true;
		}
		try {
			Optional<Entry<String, ServerInfo>> server = OlympaHub.getInstance().serversInfos.servers.entrySet().stream().filter(e -> e.getValue().slot == slot && e.getValue().getServer().canConnect(player)).findFirst();
			if (server.isPresent())
				if (server.get().getValue().connect(p))
					p.closeInventory();
		} catch (IndexOutOfBoundsException ex) {}

		if (minigames.keySet().contains(slot))
			if (minigames.get(slot) == GameType.LABY) {
				if (!OlympaHub.getInstance().getConfig().getKeys(false).contains("laby_tp_loc")) {
					OlympaHub.getInstance().getConfig().set("laby_tp_loc", new Location(OlympaHub.getInstance().spawn.getWorld(), 0, 0, 0));
					OlympaHub.getInstance().saveConfig();
				}
				p.teleport(OlympaHub.getInstance().getConfig().getLocation("laby_tp_loc"));
			} else if (OlympaHub.getInstance().games.getGame(minigames.get(slot)) != null)
				OlympaHub.getInstance().games.getGame(minigames.get(slot)).beginGame(p);
			else
				p.sendMessage(OlympaHub.getInstance().getPrefixConsole() + "§cUne erreur est survenue, veuillez contacter un membre du staff.");
		return true;
	}

	@Override
	public boolean onClose(Player p) {
		for (ServerInfo server : OlympaHub.getInstance().serversInfos.getServers())
			server.unobserve("gui_" + hashCode());
		return super.onClose(p);
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
