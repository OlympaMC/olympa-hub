package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.util.Set;

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

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.editor.RegionEditor;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.shapes.Cuboid;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;

public class GameArena extends AQueuedGame{

	//private Region arena;
	
	private Location pos1;
	private Location pos2;
	private Region arena;
	
	//private final int queueCountInvIndex = 7;
	
	boolean isGameStarting = false;
	
	public GameArena(OlympaHub plugin, ConfigurationSection fileConfig) throws ActivateFailedException {
		super(plugin, GameType.ARENA, fileConfig, 2, 10);
		
		pos1 = config.getLocation("player_1_spawn");
		pos2 = config.getLocation("player_2_spawn");
		
		//hotBarContent[queueCountInvIndex] = ItemUtils.item(Material.SUNFLOWER, "");

		allowedTpLocs.add(pos1);
		allowedTpLocs.add(pos2);
		
		arena = (Region) config.get("arena");

		OlympaCore.getInstance().getRegionManager().registerRegion(arena, "fightzone_" + gameType.toString().toLowerCase(), EventPriority.HIGHEST,
			new Flag() {
				@Override
				public ActionResult leaves(Player p, Set<TrackedRegion> to) {
					super.leaves(p, to);
					if (!playingPlayers.contains(p))
						return ActionResult.ALLOW;
					else
						return ActionResult.DENY;
				}
				@Override
				public ActionResult enters(Player p, Set<TrackedRegion> to) {
					super.leaves(p, to);
					if (playingPlayers.contains(p))
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
		
		//p.getPlayer().getInventory().clear();
		
		
		p.getPlayer().setHealth(20d);
		if (score == -1 && playingPlayers.contains(getOtherPlayingPlayer(p.getPlayer())))
			endGame(AccountProvider.get(getOtherPlayingPlayer(p.getPlayer()).getUniqueId()), winnerScore, warpToSpawn);
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
	
		playingPlayers.addAll(playingPlayers);
		
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
			endGame(AccountProvider.get(p.getUniqueId()), 0, true);
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
	
	/*
	private void fireLostFor(Player p) {
		if (!playingPlayers.contains(p) || playingPlayers.size() < 2)
			return;
		
		if (getOtherPlayingPlayer(p) != null)
			endGame(AccountProvider.get(getOtherPlayingPlayer(p).getUniqueId()), 1, true);
		
		endGame(AccountProvider.get(p.getUniqueId()), 0, true);
		
		playingPlayers.clear();
	}*/
	
	private Player getOtherPlayingPlayer(Player p) {
		if (playingPlayers.contains(p))
			for (Player pp : playingPlayers)
				if (!pp.equals(p))
					return pp;
		
		return  null;
	}
	
	/**
	 * MAJ le compteur des joueurs qui joueront avant le joueur concerné
	 */
	/*private void updateGameStartDelay() {
		for (int i = 0 ; i < queuedPlayers.size() ; i++) {
			ItemStack item = queuedPlayers.get(i).getInventory().getItem(queueCountInvIndex);
			item = ItemUtils.name(item, "§7Place dans la file : " + (i + 1));
		}	
	}*/
	
	/*private void tryToInitGame() {
		updateGameStartDelay();
		
		if (isGameStarting || playingPlayers.size() > 0 || queuedPlayers.size() < 2)
			return;
		
		List<Player> list = new ArrayList<Player>();
		
		list.add(queuedPlayers.remove(0));
		list.add(queuedPlayers.remove(0));
		startGame(list, 3);
	}*/
	
	/*private void startGamee(List<Player> startingPlayers, int countdown) {
		//cancel si l'un des joueurs n'est plus dans la partie
		if (startingPlayers.size() != 2 || !getPlayers().contains(startingPlayers.get(0)) || !getPlayers().contains(startingPlayers.get(1))) {
			startingPlayers.forEach(p -> {
				if (getPlayers().contains(p)) {
					p.sendMessage(gameType.getChatPrefix() + "§cVotre adversaire a annulé la partie ! §7En attente d'un nouvau joueur...");
					queuedPlayers.add(0, p);
				}
			});
			
			isGameStarting = false;
			return;
		}
		
		//gestion timer avant début partie
		if (countdown > 0) {
			startingPlayers.forEach(p -> p.sendTitle("§c" + countdown, "§7Début du match dans...", 0, 22, 0));
			startingPlayers.forEach(p -> p.sendMessage(gameType.getChatPrefix() + "§aDébut du match dans " + countdown));
			
			isGameStarting = true;
			
			plugin.getTask().runTaskLater(() -> startGame(startingPlayers, countdown - 1), 20);
		
		//lancement de la partie
		}else {
			
			startingPlayers.forEach(p -> p.getInventory().setItem(queueCountInvIndex, null));
			
			ItemStack potHeal = new ItemStack(Material.SPLASH_POTION);
			PotionMeta meta = (PotionMeta) potHeal.getItemMeta();
			meta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 0), true);
			meta.setDisplayName("§ePotion de soin I");
			potHeal.setItemMeta(meta);
			potHeal.setAmount(2);
			
			startingPlayers.forEach(p -> p.getInventory().addItem(ItemUtils.item(Material.IRON_SWORD, "§7Epée en fer")));
			startingPlayers.forEach(p -> p.getInventory().addItem(potHeal.clone()));
			startingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.HEAD, ItemUtils.item(Material.IRON_HELMET, "§7Casque")));
			startingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.CHEST, ItemUtils.item(Material.IRON_CHESTPLATE, "§7Plastron")));
			startingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.LEGS, ItemUtils.item(Material.IRON_LEGGINGS, "§7Jambières")));
			startingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.FEET, ItemUtils.item(Material.IRON_BOOTS, "§7Bottes")));

			playingPlayers.addAll(startingPlayers);
			
			startingPlayers.get(0).teleport(pos1);
			startingPlayers.get(1).teleport(pos2);
			
			isGameStarting = false;
		}
	}*/

	
	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////
	
	
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
	@Cmd (player = true)
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
	@Cmd (player = true)
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
	@Cmd (player = true)
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










