package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;

public class GameArena extends IGame{
	
	private List<Player> queuedPlayers = new ArrayList<Player>();

	private List<Player> playingPlayers = new ArrayList<Player>();

	//private Region arena;
	
	private Location pos1;
	private Location pos2;
	
	private final int queueCountInvIndex = 7;
	
	boolean isGameStarting = false;
	
	public GameArena(OlympaHub plugin, ConfigurationSection fileConfig) throws ActivateFailedException {
		super(plugin, GameType.ARENA, fileConfig);

		region.getRegion().getWorld().setPVP(true);
		
		pos1 = config.getLocation("player_1_spawn");
		pos2 = config.getLocation("player_2_spawn");
		
		hotBarContent[queueCountInvIndex] = ItemUtils.item(Material.SUNFLOWER, "");

		allowedTpLocs.add(pos1);
		allowedTpLocs.add(pos2);
	}
	
	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);
		
		queuedPlayers.add(p.getPlayer());
		updateGameStartDelay();
		
		tryToInitGame();
	}
	
	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		if (score == -1 && playingPlayers.contains(p.getPlayer()))
			fireLostFor(p.getPlayer());
		else {
			super.endGame(p, score, warpToSpawn);
			
			queuedPlayers.remove(p.getPlayer());
			//startingPlayers.remove(p.getPlayer());
			playingPlayers.remove(p.getPlayer());
			
			updateGameStartDelay();	
		}
	}
	
	@Override
	protected void onInterractHandler(PlayerInteractEvent e) {
		if (!playingPlayers.contains(e.getPlayer()))
			return;

		e.setCancelled(false);
	}

	protected void onDamageHandler(EntityDamageEvent e) {
		if (!playingPlayers.contains(e.getEntity()))
			return;
		
		Player p = (Player) e.getEntity();
		
		if (p.getHealth() <= e.getFinalDamage())
			fireLostFor(p);
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
	
	private void fireLostFor(Player p) {
		if (!playingPlayers.contains(p))
			return;
		
		playingPlayers.forEach(pp -> pp.setHealth(20d));

		endGame(AccountProvider.get(getOtherPlayingPlayer(p).getUniqueId()), 1, true);
		endGame(AccountProvider.get(p.getUniqueId()), 0, true);
		
		playingPlayers.clear();
		tryToInitGame();
	}
	
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
	private void updateGameStartDelay() {
		for (int i = 0 ; i < queuedPlayers.size() ; i++) {
			ItemStack item = queuedPlayers.get(i).getInventory().getItem(queueCountInvIndex);
			item = ItemUtils.name(item, "§7Place dans la file : " + (i + 1));
		}	
	}
	
	private void tryToInitGame() {
		updateGameStartDelay();
		
		if (isGameStarting || playingPlayers.size() > 0 || queuedPlayers.size() < 2)
			return;
		
		List<Player> list = new ArrayList<Player>();
		
		list.add(queuedPlayers.remove(0));
		list.add(queuedPlayers.remove(0));
		startGame(list, 3);
	}
	
	private void startGame(List<Player> startingPlayers, int countdown) {
		//cancel si l'un des joueurs n'est plus dans la partie
		if (startingPlayers.size() != 2 || !getPlayers().contains(startingPlayers.get(0).getUniqueId()) || !getPlayers().contains(startingPlayers.get(1).getUniqueId())) {
			startingPlayers.forEach(p -> {
				if (getPlayers().contains(p.getUniqueId())) {
					p.sendMessage(gameType.getChatPrefix() + "§cVotre adversaire a annulé la partie ! §7En attente d'un nouvau joueur...");
					queuedPlayers.add(0, p);
				}
			});
			
			isGameStarting = false;
			tryToInitGame();
			
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
	}

	
	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////
	
	
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		config = super.initConfig(config);
		
		if (!config.contains("player_1_spawn"))
			config.set("player_1_spawn", new Location(world, 0, 0, 0));
		
		if (!config.contains("player_2_spawn"))
			config.set("player_2_spawn", new Location(world, 0, 0, 0));
		
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
}










