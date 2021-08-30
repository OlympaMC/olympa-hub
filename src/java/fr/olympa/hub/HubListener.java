package fr.olympa.hub;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.common.player.OlympaPlayer;
import fr.olympa.api.spigot.customevents.OlympaPlayerLoadEvent;
import fr.olympa.api.spigot.customevents.WorldTrackingEvent;
import fr.olympa.api.spigot.item.ItemUtils;
import fr.olympa.api.spigot.region.tracking.flags.DamageFlag;
import fr.olympa.api.spigot.region.tracking.flags.DropFlag;
import fr.olympa.api.spigot.region.tracking.flags.FoodFlag;
import fr.olympa.api.spigot.region.tracking.flags.GameModeFlag;
import fr.olympa.api.spigot.region.tracking.flags.PhysicsFlag;
import fr.olympa.api.spigot.region.tracking.flags.PlayerBlockInteractFlag;
import fr.olympa.api.spigot.region.tracking.flags.PlayerBlocksFlag;
import fr.olympa.hub.gui.MenuGUI;
import fr.olympa.hub.minigames.utils.MiniGamesManager;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import fr.skytasul.music.CommandMusic;

public class HubListener implements Listener {
	//private Map<Integer, Entry<ItemStack, Consumer<Player>>> items = new HashMap<>();
	public static BossBar bossBar = Bukkit.createBossBar("§e§lBon jeu sur §6§lOlympa§e§l !", BarColor.YELLOW, BarStyle.SOLID);

	private Map<ItemStack, Entry<Integer, Consumer<Player>>> menuItems = new HashMap<>();

	public HubListener() {
		bossBar.setProgress(0);

		menuItems.put(ItemUtils.item(Material.CHEST, "§eΩ | Menu §6§lOlympa", "§7Accès rapide :", "§8● §7Serveurs de jeu Olympa", "§8● §7Mini-jeux d'attente", "§8● §7Profil du joueur"),
				new AbstractMap.SimpleEntry<Integer, Consumer<Player>>(4, p -> new MenuGUI(OlympaPlayerHub.get(p)).create(p)));

		menuItems.put(ItemUtils.item(Material.JUKEBOX, "§d♪ | §5§lJukeBox", " §7Profitez de la radio", " §7ou choisissez vos musiques !"),
				new AbstractMap.SimpleEntry<Integer, Consumer<Player>>(7, p -> CommandMusic.open(p)));

		menuItems.put(ItemUtils.item(Material.YELLOW_BED, "§8Ω | §7Téléportation au §lspawn"),
				new AbstractMap.SimpleEntry<Integer, Consumer<Player>>(8, p -> {
					Location location = OlympaHub.getInstance().spawn;
					p.teleport(location);
					p.playSound(location, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.2f, 1);
					p.spawnParticle(Particle.SMOKE_LARGE, location, 7, 0.1, 0.1, 0.1, 0.3);
				}));

		final ItemStack vanishYes = ItemUtils.item(Material.ENDER_EYE, "§aΩ | §2Montrer les joueurs");
		final ItemStack vanishNo = ItemUtils.item(Material.SUNFLOWER, "§aΩ | §2Cacher les joueurs");

		menuItems.put(vanishNo,
				new AbstractMap.SimpleEntry<Integer, Consumer<Player>>(6, p -> {
					if (OlympaHub.getInstance().vanishManager.toogleVanish(p))
						p.getInventory().setItem(6, vanishYes);
				}));
		menuItems.put(vanishYes,
				new AbstractMap.SimpleEntry<Integer, Consumer<Player>>(-1, p -> {
					if (OlympaHub.getInstance().vanishManager.toogleVanish(p))
						p.getInventory().setItem(6, vanishNo);
				}));
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		p.teleport(OlympaHub.getInstance().spawn);
		p.setHealth(20);
		p.setFoodLevel(20);
		p.setRemainingAir(300);
		p.setWalkSpeed(0.22f);
		p.setFlying(false);
		p.setAllowFlight(false);
		p.setCanPickupItems(false);
		menuItems.forEach((key, value) -> {
			if (value.getKey() >= 0)
				p.getInventory().setItem(value.getKey(), key);
		});
		p.getInventory().setHeldItemSlot(4);
		p.sendTitle("§6§lOlympa", "§eBienvenue !", 2, 50, 7);
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4f, 1);
		p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1);
		bossBar.addPlayer(p);
	}

	@EventHandler
	public void onOlympaJoin(OlympaPlayerLoadEvent e) {
		OlympaPlayer player = e.getOlympaPlayer();
		if (player.getGroup().isHighStaff())
			Bukkit.getScheduler().runTask(OlympaHub.getInstance(), () -> OlympaHub.getInstance().lightning.getWorld().strikeLightningEffect(OlympaHub.getInstance().lightning));
		else {
			Sound sound;
			float volume;
			if (HubPermissions.SPECIAL_JOIN_SOUND.hasPermission(player)) {
				sound = Sound.ENTITY_PLAYER_LEVELUP;
				volume = 0.5f;
			}else {
				sound = Sound.ENTITY_ITEM_PICKUP;
				volume = 0.14f;
			}
			Player p = e.getPlayer();
			p.getWorld().playSound(p.getLocation(), sound, volume, 1f);
		}
	}

	@EventHandler
	public void onWorldLoad(WorldTrackingEvent e) {
		e.getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false);
		e.getRegion().registerFlags(new PlayerBlocksFlag(true), new PhysicsFlag(true), new FoodFlag(true), new GameModeFlag(GameMode.ADVENTURE), new DropFlag(true), new PlayerBlockInteractFlag(false, true, true), new DamageFlag(false));
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		if (e.getHand() == EquipmentSlot.HAND)
			if (MiniGamesManager.getInstance().isPlaying(e.getPlayer()) == null && menuItems.containsKey(player.getInventory().getItemInMainHand()))
				menuItems.get(player.getInventory().getItemInMainHand()).getValue().accept(player);
	}

	@EventHandler
	public void onSwapHands(PlayerSwapHandItemsEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();

		if (e.getClickedInventory() == player.getInventory() && MiniGamesManager.getInstance().isPlaying(player) == null) {
			Entry<Integer, Consumer<Player>> entry = menuItems.get(player.getInventory().getItemInMainHand());
			if (entry != null) {
				entry.getValue().accept(player);
				e.setCancelled(true);
			}
		}
	}

}
