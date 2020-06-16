package fr.olympa.hub;

import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Material;
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

public class HubListener implements Listener {

	private ItemStack[] inventoryContents = new ItemStack[] { null, null, null, null, ItemUtils.item(Material.CHEST, "§eMenu §6§lOlympa ☁") };

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		p.teleport(OlympaHub.getInstance().spawn);
		p.setHealth(20);
		p.setFoodLevel(20);
		p.setFlying(false);
		p.setAllowFlight(false);
		p.setCanPickupItems(false);
		p.getInventory().setContents(inventoryContents);
	}

	@EventHandler
	public void onWorldLoad(WorldTrackingEvent e) {
		e.getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false);
		e.getRegion().registerFlags(new PlayerBlocksFlag(true), new PhysicsFlag(true), new FoodFlag(true), new GameModeFlag(GameMode.ADVENTURE), new DropFlag(true), new PlayerBlockInteractFlag(false, true, true), new DamageFlag(false) {
			@Override
			public void damageEvent(EntityDamageEvent event) {
				if (event.getCause() == DamageCause.VOID) {
					event.getEntity().teleport(event.getEntity().getWorld().getSpawnLocation());
				}
				super.damageEvent(event);
			}
		});
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND) {
			Player player = e.getPlayer();
			if (player.getInventory().getHeldItemSlot() == 4) {
				new MenuGUI(AccountProvider.get(player.getUniqueId())).create(player);
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getSlot() == 4) {
			Player player = (Player) e.getWhoClicked();
			new MenuGUI(AccountProvider.get(player.getUniqueId())).create(player);
			e.setCancelled(true);
		}
	}

}
