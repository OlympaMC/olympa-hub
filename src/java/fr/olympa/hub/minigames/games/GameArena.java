package fr.olympa.hub.minigames.games;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import fr.olympa.api.common.command.complex.Cmd;
import fr.olympa.api.common.command.complex.CommandContext;
import fr.olympa.api.common.provider.AccountProviderAPI;
import fr.olympa.api.spigot.editor.RegionEditor;
import fr.olympa.api.spigot.item.ItemUtils;
import fr.olympa.api.spigot.region.Region;
import fr.olympa.api.spigot.region.shapes.Cuboid;
import fr.olympa.api.spigot.region.tracking.ActionResult;
import fr.olympa.api.spigot.region.tracking.RegionEvent.EntryEvent;
import fr.olympa.api.spigot.region.tracking.RegionEvent.ExitEvent;
import fr.olympa.api.spigot.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;

public class GameArena extends AQueuedGame {

	//private Region arena;

	private Location pos1;
	private Location pos2;
	private Region arena;

	//private final int queueCountInvIndex = 7;

	boolean isGameStarting = false;

	public GameArena(OlympaHub plugin, ConfigurationSection fileConfig) throws UnsupportedOperationException {
		super(plugin, GameType.ARENA, fileConfig, 2, 2);

		//winnerScore = 1;

		pos1 = config.getLocation("player_1_spawn");
		pos2 = config.getLocation("player_2_spawn");

		allowedTpLocs.add(pos1);
		allowedTpLocs.add(pos2);

		arena = (Region) config.get("arena");

		OlympaCore.getInstance().getRegionManager().registerRegion(arena, "fightzone_" + gameType.toString().toLowerCase(), EventPriority.HIGHEST, new Flag() {
			@Override
			public ActionResult leaves(ExitEvent event) {
				super.leaves(event);
				if (!playingPlayers.contains(event.getPlayer()))
					return ActionResult.ALLOW;
				else
					return ActionResult.DENY;
			}

			@Override
			public ActionResult enters(EntryEvent event) {
				super.enters(event);
				if (playingPlayers.contains(event.getPlayer()))
					return ActionResult.ALLOW;
				else
					return ActionResult.DENY;
			}
		});
	}

	@Override
	protected boolean startGame(OlympaPlayerHub p) {
		return super.startGame(p);
	}

	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);
		Player player = (Player) p.getPlayer();

		player.setHealth(20d);
		/*if (score == -1 && playingPlayers.contains(getOtherPlayingPlayer(player)))
			try {
				endGame(AccountProviderAPI.getter().get(getOtherPlayingPlayer(player).getUniqueId()), winnerScore, warpToSpawn);
			} catch (NullPointerException e) {
				e.printStackTrace();
				// getOtherPlayingPlayer(player) can be null, we need to fix it and remove this try catch.
			}*/
	}

	@Override
	protected void startGame() {
		ItemStack potHeal = new ItemStack(Material.SPLASH_POTION);
		PotionMeta meta = (PotionMeta) potHeal.getItemMeta();
		meta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 0), true);
		meta.setDisplayName("§ePotion de soin I");
		potHeal.setItemMeta(meta);
		potHeal.setAmount(2);

		playingPlayers.forEach(p -> p.getInventory().addItem(ItemUtils.item(Material.IRON_SWORD, "§7Epée en fer")));
		playingPlayers.forEach(p -> p.getInventory().addItem(potHeal.clone()));
		playingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.HEAD, ItemUtils.item(Material.IRON_HELMET, "§7Casque")));
		playingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.CHEST, ItemUtils.item(Material.IRON_CHESTPLATE, "§7Plastron")));
		playingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.LEGS, ItemUtils.item(Material.IRON_LEGGINGS, "§7Jambières")));
		playingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.FEET, ItemUtils.item(Material.IRON_BOOTS, "§7Bottes")));

		//playingPlayers.addAll(playingPlayers);

		playingPlayers.get(0).teleport(pos1);
		playingPlayers.get(1).teleport(pos2);
	}

	@Override
	protected void endGame() {

	}

	@Override
	protected void onInterractHandler(PlayerInteractEvent e) {
		if (playingPlayers.contains(e.getPlayer()))
			e.setCancelled(false);
	}

	@Override
	protected void onDamageHandler(EntityDamageEvent e) {
		if (!playingPlayers.contains(e.getEntity()))
			return;

		Player p = (Player) e.getEntity();

		if (p.getHealth() <= e.getFinalDamage())
			endGame(OlympaPlayerHub.get(p), 0, true);
		else
			e.setCancelled(false);
	}

	@EventHandler
	public void onHeal(EntityRegainHealthEvent e) {
		if (!playingPlayers.contains(e.getEntity()))
			return;

		if (e.getRegainReason() == RegainReason.MAGIC || e.getRegainReason() == RegainReason.MAGIC_REGEN)
			return;

		e.setCancelled(true);
	}

	private Player getOtherPlayingPlayer(Player p) {
		if (playingPlayers.contains(p))
			for (Player pp : playingPlayers)
				if (!pp.equals(p))
					return pp;

		return null;
	}

	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////

	@Override
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		config = super.initConfig(config);

		if (!config.contains("player_1_spawn"))
			config.set("player_1_spawn", new Location(world, 0, 0, 0));

		if (!config.contains("player_2_spawn"))
			config.set("player_2_spawn", new Location(world, 0, 0, 0));

		if (!config.contains("arena"))
			config.set("arena", new Cuboid(world, 0, 0, 0, 1, 1, 1));

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
	public void setPlayerOneSpawn(CommandContext cmd) {
		pos1 = getPlayer().getLocation();
		config.set("player_1_spawn", pos1);

		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLa position de téléportation du joueur 1 a été définie en " +
				pos1.getBlockX() + ", " + pos1.getBlockY() + ", " + pos1.getBlockZ());
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd(player = true)
	public void setPlayerTwoSpawn(CommandContext cmd) {
		pos2 = getPlayer().getLocation();
		config.set("player_2_spawn", pos2);

		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLa position de téléportation du joueur 2 a été définie en " +
				pos2.getBlockX() + ", " + pos2.getBlockY() + ", " + pos2.getBlockZ());
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd(player = true)
	public void setArena(CommandContext cmd) {
		Player p = getPlayer();

		p.sendMessage(gameType.getChatPrefix() + "§aSélectionnez la région de l'arène.");

		new RegionEditor(p, region -> {
			if (region == null)
				return;

			arena = region;
			config.set("arena", region);
			p.sendMessage(gameType.getChatPrefix() + "§aRégion arena mise à jour avec succès.");

		}).enterOrLeave();
	}
}
