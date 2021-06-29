package fr.olympa.hub.minigames.games;

import java.lang.reflect.Field;
import java.rmi.activation.ActivateFailedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_16_R3.CraftParticle;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import fr.olympa.api.common.command.complex.Cmd;
import fr.olympa.api.common.command.complex.CommandContext;
import fr.olympa.api.common.provider.AccountProviderAPI;
import fr.olympa.api.spigot.editor.RegionEditor;
import fr.olympa.api.spigot.item.ItemUtils;
import fr.olympa.api.spigot.region.Region;
import fr.olympa.api.spigot.region.shapes.Cuboid;
import fr.olympa.api.spigot.region.tracking.ActionResult;
import fr.olympa.api.spigot.region.tracking.RegionEvent.ExitEvent;
import fr.olympa.api.spigot.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.MiniGamesManager;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import net.minecraft.server.v1_16_R3.EntityCow;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_16_R3.PacketPlayOutWorldParticles;
import net.minecraft.server.v1_16_R3.WorldServer;

public class GameTrident extends AQueuedGame {

	private Region exteriorRegion;
	private Region interiorRegion;

	private Map<Player, Entity> playersTridents = new HashMap<>();

	public GameTrident(OlympaHub plugin, ConfigurationSection gameConfig) throws ActivateFailedException {
		super(plugin, GameType.TRIDENT, gameConfig, 2, 15);

		allowFly = true;

		exteriorRegion = (Region) config.get("fly_region");
		interiorRegion = new Cuboid(exteriorRegion.getMin().clone().add(4, 4, 4), exteriorRegion.getMax().clone().subtract(4, 4, 4));

		//cancel going out of game area
		OlympaCore.getInstance().getRegionManager().registerRegion(exteriorRegion, "trident_exterior", EventPriority.NORMAL, new Flag() {
			@Override
			public ActionResult leaves(ExitEvent event) {
				super.leaves(event);

				if (playingPlayers.contains(event.getPlayer()))
					return ActionResult.DENY;
				else
					return ActionResult.ALLOW;
			}
		});

		//show border barrier particles
		OlympaCore.getInstance().getRegionManager().registerRegion(interiorRegion, "trident_interior", EventPriority.NORMAL, new Flag() {
			@Override
			public ActionResult leaves(ExitEvent event) {
				if (playingPlayers.contains(event.getPlayer()))
					//showBarrier(p);
					sendMessage(event.getPlayer(), "§7Attention, vous vous approchez de la bordure de la carte !");
				return super.leaves(event);
			}
		});

		//send barrier particles each 10 ticks
		GameTrident game = this;
		new BukkitRunnable() {
			@Override
			public void run() {
				playingPlayers.forEach(game::showBarriers);
			}
		}.runTaskTimer(plugin, 10, 10);

		//cancel entity packet if this packet contains a trident which isn't the player's one
		MiniGamesManager.getInstance().getPacketsListener().onPacketSend((p, pckt) -> {
			if (!playingPlayers.contains(p) || !(pckt instanceof PacketPlayOutSpawnEntity))
				return true;

			PacketPlayOutSpawnEntity packet = (PacketPlayOutSpawnEntity) pckt;

			try {
				Field entityId = PacketPlayOutSpawnEntity.class.getDeclaredField("b");
				entityId.setAccessible(true);

				//TODO vérifier que ça fonctionne bien sans le cast des en int
				if (!entityId.get(packet).equals(playersTridents.get(p)))
					return false;

			} catch (Exception e) {
				plugin.getLogger().warning("§cError while trying to intercept entity spawn packet for Trident minigame");
				e.printStackTrace();
			}

			return true;
		});
	}

	@Override
	protected boolean startGame(OlympaPlayerHub p) {
		return super.startGame(p);
	}

	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);
		Entity trident = playersTridents.remove(p.getPlayer());
		if (trident != null)
			trident.remove();
		Player player = (Player) p.getPlayer();
		//world.getEntities().stream().filter(ent -> ent.getUniqueId().equals(id)).findFirst().ifPresent(ent -> ent.remove());
		player.setAllowFlight(false);
		player.setHealth(20d);
		player.removePotionEffect(PotionEffectType.REGENERATION);
		player.getInventory().remove(Material.TRIDENT);
	}

	@Override
	protected void startGame() {
		for (Player p : playingPlayers) {
			p.setAllowFlight(true);
			p.setFlying(true);

			//définit la localisation où le joueur sera tp
			Location playerTpLoc = null;
			do
				playerTpLoc = interiorRegion.getRandomLocation();
			while (playerTpLoc.getBlock().getType() != Material.AIR || playerTpLoc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR);

			//donne l'item de bataille au joueur
			ItemStack hoe = ItemUtils.item(Material.DIAMOND_HOE, "§5Excalibur");
			hoe = ItemUtils.addEnchant(hoe, Enchantment.KNOCKBACK, 1);

			p.getInventory().addItem(hoe);
			p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1000000, 0, false, false, false));

			//crée le trident qui sera spawn
			ItemStack it = new ItemStack(Material.TRIDENT);
			it = ItemUtils.addEnchant(it, Enchantment.LOYALTY, 1);

			//crée le trident à spawn
			WorldServer worldServer = ((CraftWorld) world).getHandle();

			//EntityThrownTrident trident = new EntityThrownTrident(worldServer, ((CraftPlayer)p).getHandle(), CraftItemStack.asNMSCopy(it));
			EntityCow trident = new EntityCow(EntityTypes.COW, worldServer);
			playersTridents.put(p, trident.getBukkitEntity());

			ThreadLocalRandom random = ThreadLocalRandom.current();

			/*trident.setLocation(
					p.getLocation().getX() + (random.nextBoolean() ? random.nextDouble(15, 30) : -random.nextDouble(15, 30)),
					p.getLocation().getY() + (random.nextBoolean() ? random.nextDouble(10, 15) : -random.nextDouble(10, 15)),
					p.getLocation().getZ() + (random.nextBoolean() ? random.nextDouble(15, 30) : -random.nextDouble(15, 30)),
					0, 0);*/
			Location tridentLoc = p.getLocation().clone().subtract(
					p.getLocation().getX() + (random.nextBoolean() ? random.nextDouble(5, 10) : -random.nextDouble(5, 10)),
					p.getLocation().getY(),
					p.getLocation().getZ() + (random.nextBoolean() ? random.nextDouble(5, 10) : -random.nextDouble(5, 10)));

			//définit la position du trident puis le fais spawn
			worldServer.addEntity(trident, CreatureSpawnEvent.SpawnReason.CUSTOM);
			trident.setLocation(tridentLoc.getX(), tridentLoc.getY(), tridentLoc.getZ(), tridentLoc.getYaw(), tridentLoc.getPitch());

			//teleport the player in the fly area and make him face to his trident
			Vector dirBetweenLocations = tridentLoc.toVector().subtract(playerTpLoc.clone().toVector()).multiply(-1);
			playerTpLoc.setDirection(dirBetweenLocations);
			teleport(p, playerTpLoc);
		}
	}

	@Override
	protected void endGame() {
		//remove all tridents
		//playersTridents.values().forEach(uuid -> world.getEntities().stream().filter(ent -> ent.getUniqueId().equals(uuid)).findFirst().get().remove());
		playersTridents.clear();
		world.getEntities().stream().filter(e -> e.getType() == EntityType.TRIDENT).forEach(e -> e.remove());
	}

	//Spawns barrier particles if player is too close from the exterior region border
	private void showBarriers(Player p) {
		Set<Float> xLimits = new HashSet<>();
		Set<Float> yLimits = new HashSet<>();
		Set<Float> zLimits = new HashSet<>();

		xLimits.add((float) exteriorRegion.getMin().getBlockX());
		xLimits.add((float) exteriorRegion.getMax().getBlockX());
		yLimits.add((float) exteriorRegion.getMin().getBlockY());
		yLimits.add((float) exteriorRegion.getMax().getBlockY());
		zLimits.add((float) exteriorRegion.getMin().getBlockZ());
		zLimits.add((float) exteriorRegion.getMax().getBlockZ());

		for (float x = p.getLocation().getBlockX() - 4; x <= p.getLocation().getBlockX() + 4; x++)
			for (float y = p.getLocation().getBlockY() - 4; y <= p.getLocation().getBlockY() + 4; y++)
				for (float z = p.getLocation().getBlockZ() - 4; z <= p.getLocation().getBlockZ() + 4; z++)
					if ((xLimits.contains(x) || yLimits.contains(y) || zLimits.contains(z)) && exteriorRegion.isIn(world, (int) x, (int) y, (int) z)) {
						PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(CraftParticle.toNMS(Particle.BARRIER), true, x + 0.5, y, z + 0.5, 0f, 0f, 0f, 1f, 1);
						((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
					}
	}

	@EventHandler //détecte si l'entité supprimée est un trident appartenant à un joueur en jeu
	public void onEntityRemove(EntityRemoveFromWorldEvent e) {

		if (playersTridents.containsValue(e.getEntity()))
			for (Entry<Player, Entity> entry : playersTridents.entrySet())

				if (entry.getValue().equals(e.getEntity())) {
					if (playingPlayers.size() > 1) {
						sendMessage(entry.getKey(), "§7Dommage, vous avez été rattrapé par votre trident !");
						endGame(AccountProviderAPI.getter().get(entry.getKey().getUniqueId()), 0, false);
					}
					return;
				}
	}

	@Override //fire player end game if he dies
	protected void onDamageHandler(EntityDamageEvent e) {
		if (!playingPlayers.contains(e.getEntity()))
			return;

		if (playingPlayers.contains(e.getEntity()))
			if (((Player) e.getEntity()).getHealth() <= e.getFinalDamage())
				endGame(AccountProviderAPI.getter().get(e.getEntity().getUniqueId()), 0, true);
			else
				e.setCancelled(false);
	}

	@EventHandler //cancel all natural health regen sources
	public void onHeal(EntityRegainHealthEvent e) {
		if (!playingPlayers.contains(e.getEntity()))
			return;

		if (e.getRegainReason() == RegainReason.MAGIC || e.getRegainReason() == RegainReason.MAGIC_REGEN)
			return;

		e.setCancelled(true);
	}

	@EventHandler //security check: kill all tridents on chunk loading
	public void onChunkLoad(ChunkLoadEvent e) {
		for (Entity ent : e.getChunk().getEntities())
			if (ent.getType() == EntityType.TRIDENT)
				ent.remove();
	}

	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////

	@Override
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		config = super.initConfig(config);

		if (!config.contains("fly_region"))
			config.set("fly_region", new Cuboid(world, 0, 0, 0, 1, 1, 1));

		/*if (!config.contains("tp_loc"))
			config.set("tp_loc", new Location(world, 0, 0, 0));*/

		return config;
	}

	///////////////////////////////////////////////////////////
	//                       COMMANDS                        //
	///////////////////////////////////////////////////////////

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd(player = true)
	public void flyArea(CommandContext cmd) {
		Player p = getPlayer();

		new RegionEditor(p, region -> {
			if (region == null || !(region instanceof Cuboid)) {
				sendMessage(p, "§cLa sélection n'est pas valide, veuillez réessayer.");
				return;
			}

			exteriorRegion = region;
			config.set("fly_region", region);

			sendMessage(p, "§aLa nouvelle zone de vol a bien été définie. §7Un redémarage est nécessaire.");
		}).enterOrLeave();
	}
}
