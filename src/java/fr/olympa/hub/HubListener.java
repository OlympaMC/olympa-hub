package fr.olympa.hub;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.customevents.OlympaPlayerLoadEvent;
import fr.olympa.api.customevents.WorldTrackingEvent;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.tracking.flags.DamageFlag;
import fr.olympa.api.region.tracking.flags.DropFlag;
import fr.olympa.api.region.tracking.flags.FoodFlag;
import fr.olympa.api.region.tracking.flags.GameModeFlag;
import fr.olympa.api.region.tracking.flags.PhysicsFlag;
import fr.olympa.api.region.tracking.flags.PlayerBlockInteractFlag;
import fr.olympa.api.region.tracking.flags.PlayerBlocksFlag;
import fr.olympa.hub.gui.MenuGUI;
import fr.olympa.hub.minigames.utils.MiniGamesManager;

public class HubListener implements Listener {
	private ItemStack[] inventoryContents = new ItemStack[] { null, null, null, null, ItemUtils.item(Material.CHEST, "§eΩ | Menu §6§lOlympa") };
	private BossBar bossBar = Bukkit.createBossBar("§e§lBon jeu sur §6§lOlympa§e§l !", BarColor.YELLOW, BarStyle.SOLID);
	
	public HubListener() {
		bossBar.setProgress(0);
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
		p.getInventory().setContents(inventoryContents);
		p.getInventory().setHeldItemSlot(4);
		p.sendTitle("§6§lOlympa", "§eBienvenue !", 2, 50, 7);
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4f, 1);
		p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1);
		bossBar.addPlayer(p);
	}
	
	@EventHandler
	public void onOlympaJoin(OlympaPlayerLoadEvent e) {
		if (e.getOlympaPlayer().getGroup().isHighStaff()) {
			Bukkit.getScheduler().runTask(OlympaHub.getInstance(), () -> OlympaHub.getInstance().lightning.getWorld().strikeLightningEffect(OlympaHub.getInstance().lightning));
		}else {
			Player p = e.getPlayer();
			p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.1f, 1f);
		}
	}

	
	@EventHandler
	public void onWorldLoad(WorldTrackingEvent e) {
		e.getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false);
		e.getRegion().registerFlags(new PlayerBlocksFlag(true), new PhysicsFlag(true), new FoodFlag(true, true), new GameModeFlag(GameMode.ADVENTURE), new DropFlag(true), new PlayerBlockInteractFlag(false, true, true), new DamageFlag(false) {
			@Override
			public void damageEvent(EntityDamageEvent event) {
				if (event.getCause() == DamageCause.VOID) {
					event.getEntity().teleport(OlympaHub.getInstance().spawn);
				}
				super.damageEvent(event);
			}
		});
	}
	
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		if (e.getHand() == EquipmentSlot.HAND) {
			if (player.getInventory().getHeldItemSlot() == 4 && MiniGamesManager.getInstance().isPlaying(e.getPlayer()) == null) {
				new MenuGUI(AccountProvider.get(player.getUniqueId())).create(player);
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		if (e.getClickedInventory() == player.getInventory() && e.getSlot() == 4 && MiniGamesManager.getInstance().isPlaying(player) == null) {
			new MenuGUI(AccountProvider.get(player.getUniqueId())).create(player);
			e.setCancelled(true);
		}
	}

}
