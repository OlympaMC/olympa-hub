package fr.olympa.hub;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.customevents.WorldTrackingEvent;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.plugin.OlympaAPIPlugin;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.tracking.flags.DamageFlag;
import fr.olympa.api.region.tracking.flags.DropFlag;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.api.region.tracking.flags.FoodFlag;
import fr.olympa.api.region.tracking.flags.GameModeFlag;
import fr.olympa.api.region.tracking.flags.PhysicsFlag;
import fr.olympa.api.region.tracking.flags.PlayerBlocksFlag;
import fr.olympa.api.region.tracking.flags.PlayerInteractFlag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.gui.MenuGUI;
import fr.olympa.hub.servers.ServerInfosListener;

public class OlympaHub extends OlympaAPIPlugin implements Listener {

	private static OlympaHub instance;

	public static OlympaHub getInstance() {
		return instance;
	}

	private ItemStack[] inventoryContents = new ItemStack[] { null, null, null, null, ItemUtils.item(Material.CHEST, "§eMenu §6§lOlympa ☁") };

	public ServerInfosListener serversInfos;

	@Override
	public void onEnable() {
		instance = this;
		super.onEnable();

		getServer().getPluginManager().registerEvents(this, this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		new SpawnCommand(this).register();

		OlympaCore.getInstance().registerRedisSub(serversInfos = new ServerInfosListener(getConfig().getConfigurationSection("servers")), "sendServersInfos");
		
		OlympaCore.getInstance().getRegionManager().registerRegion(getConfig().getSerializable("zone", Region.class), "zone", EventPriority.HIGH, new Flag().setEntryExitDenied(false, true));
	}

	@EventHandler
	public void onWorldLoad(WorldTrackingEvent e) {
		e.getRegion().registerFlags(new PlayerBlocksFlag(true), new PhysicsFlag(true), new FoodFlag(true), new GameModeFlag(GameMode.ADVENTURE), new DropFlag(true), new PlayerInteractFlag(false, true, true) {
			@Override
			public void interactEvent(PlayerInteractEvent event) {
				Player player = event.getPlayer();
				if (player.getInventory().getHeldItemSlot() == 4) {
					new MenuGUI(AccountProvider.get(player.getUniqueId())).create(player);
				}
				super.interactEvent(event);
			}
		}, new DamageFlag(false) {
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
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		p.teleport(p.getWorld().getSpawnLocation());
		p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(2);
		p.setFoodLevel(20);
		p.setFlying(false);
		p.setAllowFlight(false);
		p.setCanPickupItems(false);
		p.getInventory().setContents(inventoryContents);
	}

}
