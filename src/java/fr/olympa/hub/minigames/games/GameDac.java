package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitTask;

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.editor.RegionEditor;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.shapes.Cuboid;
import fr.olympa.hub.HubListener;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;

public class GameDac extends IGame {

	private final List<Material> wools = Arrays.asList(Material.RED_WOOL, Material.YELLOW_WOOL, Material.BLUE_WOOL, Material.MAGENTA_WOOL, Material.LIME_WOOL, Material.ORANGE_WOOL, Material.WHITE_WOOL, Material.BLACK_WOOL);
	private final int minPlayers = 2;
	//private boolean isGameInProgress = false;
	
	private final int countdownDelay = 3;
	private final int playDelay = 10;
	
	private int currentTurn = -1;

	private List<Player> waitingPlayers = new ArrayList<>();
	private List<DacPlayer> playingPlayers = new ArrayList<>();
	
	private Cuboid jumpRegion;
	private Location tpLoc;
	private int minJumpY;
	
	private DacPlayer playingPlayer = null;
	private boolean hasJumped = false;
	
	private int remainingTime;
	private BukkitTask timeTask = null;
	
	private BossBar bar = Bukkit.createBossBar("dac", BarColor.PURPLE, BarStyle.SEGMENTED_10);
	
	public GameDac(OlympaHub plugin, ConfigurationSection configFromFile) throws ActivateFailedException {
		super(plugin, GameType.DAC, configFromFile);

		jumpRegion = (Cuboid) config.get("jump_region");
		minJumpY = config.getInt("min_jump_y");
		
		allowedTpLocs.add(tpLoc = config.getLocation("tp_loc"));

		resetLandingArea();

		//cancel region entering if player isn't the one which should jump
		//and set the jump marker to true
		/*
		OlympaCore.getInstance().getRegionManager().registerRegion(barrierRegion, "dac_barrier_area",
				EventPriority.HIGH, new Flag() {
			@Override
			public ActionResult enters(Player p, Set<TrackedRegion> to) {
				super.enters(p, to);
				if (p.equals(playingPlayer) && !hasJumped) {
					hasJumped = true;
					return ActionResult.ALLOW;
				}else
					return ActionResult.DENY;
			}
		});*/
	}

	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);

		waitingPlayers.add(p.getPlayer());
		
		if (playingPlayers.size() > 0)
			p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Une partie est déjà en cours...");
		else
			tryToInitGame();
	}
	 
	
	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);

		waitingPlayers.remove(p.getPlayer());
		
		/*playingPlayers.remove(new DacPlayer(p.getPlayer(), null));
		if (playingPlayer.equals(new DacPlayer(p.getPlayer(), null)))
			playingPlayer = null;*/
		bar.removePlayer(p.getPlayer());
		HubListener.bossBar.addPlayer(p.getPlayer());
		
		for (Iterator<DacPlayer> iterator = playingPlayers.iterator(); iterator.hasNext();) {
			DacPlayer dacPlayer = iterator.next();
			
			if (dacPlayer.p.equals(p.getPlayer())) {
				iterator.remove();
				if (dacPlayer == playingPlayer) playGameTurn();
			}
		}
	} 
	 
	private void tryToInitGame() {
		if (playingPlayers.size() > 0)
			waitingPlayers.forEach(p -> p.sendMessage(gameType.getChatPrefix() + "§aUne partie est déjà en cours. Nombres de joueurs prêts pour la prochaine partie : " + waitingPlayers.size() + "/" + minPlayers));
		else if (waitingPlayers.size() < minPlayers)
			waitingPlayers.forEach(p -> p.sendMessage(gameType.getChatPrefix() + "§aNombre de joueurs prêts : " + waitingPlayers.size() + "/" + minPlayers));
		else
			startGame(countdownDelay);
	}
	
	private void startGame(int countdown) {
		if (playingPlayers.size() > 0)
			return;
		
		//s'il n'y a plus assez de joueurs pour commencer la partie, cancel de cette dernière
		if (waitingPlayers.size() < minPlayers) 
			waitingPlayers.forEach(p -> p.sendMessage(gameType.getChatPrefix() + "§7Plus assez de joueur pour commencer la partie... (" + waitingPlayers.size() + "/" + minPlayers + ")"));
		
		//actions avant début de partie
		if (countdown > 0) {
			waitingPlayers.forEach(p -> p.sendTitle("§c" + countdown, "§7Début du match dans...", 0, 21, 0));
			waitingPlayers.forEach(p -> p.sendMessage(gameType.getChatPrefix() + "§aDébut du match dans " + countdown));
			
			plugin.getTask().runTaskLater(() -> startGame(countdown - 1), 1, TimeUnit.SECONDS);
			
		//actions de début de partie
	}else {
		bar.setTitle(gameType.getName());
			for (int i = 0; i < waitingPlayers.size(); i++) {
				DacPlayer dacPlayer = new DacPlayer(waitingPlayers.get(i), wools.get(i));
				playingPlayers.add(dacPlayer);
				dacPlayer.p.teleport(tpLoc);
				dacPlayer.sendDacMessage("§eLe match de dé à coudre commence ! Sélection du tour...");
				HubListener.bossBar.removePlayer(dacPlayer.p);
				bar.addPlayer(dacPlayer.p);
			}
			waitingPlayers.clear();

			currentTurn = 0;
			
			plugin.getTask().runTaskLater(() -> playGameTurn(), 2, TimeUnit.SECONDS);
		}
	}
	
	private void playGameTurn() {
		if (playingPlayers.size() == 0)
			return;
		
		//si plus qu'un seul joueur en lice, fin de jeu (ou reset du jeu si 0 joueurs restants)
		if (playingPlayers.size() <= 1) {
			if (playingPlayers.size() == 1)
				endGame(AccountProvider.get(playingPlayers.get(0).p.getUniqueId()), 1, true);
			
			//réinitialisation du jeu
			resetLandingArea();
			playingPlayers.clear();
			playingPlayer = null;
			
			//tentative de relance du jeu (si assez de joueurs sont en file d'atente)
			tryToInitGame();
			return;
		}
		
		playingPlayer = playingPlayers.get(0);
		currentTurn++;

		bar.setTitle("§5Dé à coudre : §d§l" + playingPlayer.p.getName());
		bar.setProgress(1);
		
		hasJumped = false;
		
		playingPlayer.sendDacMessage("§a§lTour " + currentTurn + " : c'est à vous ! §7Sautez du pont, et tentez d'atterir dans l'eau ! Vous avez " + playDelay + " secondes.");
		playingPlayers.stream().filter(x -> x != playingPlayer).forEach(player -> player.sendDacMessage("§aTour " + currentTurn + " : c'est à " + playingPlayer.p.getName() + " de jouer !"));
		
		//si le joueur a mis trop de temps à sauter, expulsion
		final int currentTurnBis = currentTurn;
		
		remainingTime = playDelay;
		timeTask = Bukkit.getScheduler().runTaskTimer(OlympaHub.getInstance(), () -> {
			if (remainingTime == 0) {
				if (currentTurn == currentTurnBis)
					if (playingPlayer != null && playingPlayer.p.isOnline()) {
						playingPlayer.sendDacMessage("§cVous avez attendu trop longtemps avant de sauter !");
						endGame(AccountProvider.get(playingPlayer.p.getUniqueId()), 0, true);
					}
				timeTask.cancel();
			}else {
				remainingTime--;
				bar.setProgress((double) remainingTime / (double) playDelay);
			}
		}, 20, 20);
	}
	
	/**
	 * Executes actions when a player lands (in water or on ground)
	 * @param p
	 */
	@Override
	protected void onMoveHandler(Player p, Location from, Location to) {
		if (playingPlayers.isEmpty()) return; // le jeu n'a pas commencé

		//Bukkit.broadcastMessage("TO Y : " + to.getBlockY() + " - minJumpY : " + minJumpY);
		
		//si un joueur a sauté
		World world = from.getWorld();
		int x = from.getBlockX();
		int y = from.getBlockY();
		int z = from.getBlockZ();
		if (to.getBlockY() < minJumpY &&
				world.getBlockAt(x, y - 1, z).getType() == Material.AIR && 
				world.getBlockAt(x, y - 2, z).getType() == Material.AIR &&
				world.getBlockAt(x, y - 3, z).getType() == Material.AIR &&
				world.getBlockAt(x, y - 4, z).getType() == Material.AIR) {
			
			//Bukkit.broadcastMessage("Detected jump : " + p.getName());
			
			//détection de si c'est le joueur qui devait jouer qui a sauté
			if (playingPlayer != null && p.equals(playingPlayer.p)) {
				//if (!hasJumped) Bukkit.broadcastMessage("Detected jump : " + p.getName());
				hasJumped = true;
				
			}else {
				p.teleport(tpLoc);
				p.sendMessage(gameType.getChatPrefix() + "§7Ce n'est pas votre tour de sauter !");
			}
		}
		
		//Bukkit.broadcastMessage("§8Detected move of the player : " + p.getName());
		
		//si ce n'est pas le bon joueur ou s'il n'a pas sauté
		if (!hasJumped || playingPlayer == null || !p.equals(playingPlayer.p))
			return;
		
		//Bukkit.broadcastMessage("§7Detected move of playing player : " + p.getName());
		
		Block block = to.getBlock();
		if (block.getType() == Material.WATER/* && jumpRegion.isIn(to)*/) {
			p.teleport(tpLoc);
			p.sendMessage(gameType.getChatPrefix() + "§aBien visé !");
			
			//détermination du bloc le plus en haut de la zone
			Location loc = block.getLocation().clone();
			while(loc.clone().add(0, 1, 0).getBlock().getType() == Material.WATER)
				loc = loc.add(0, 1, 0);
			
			loc.getBlock().setType(playingPlayer.wool);
			
			playingPlayers.add(playingPlayers.remove(0));
			playingPlayer = null;
			
			timeTask.cancel();
			
			plugin.getTask().runTaskLater(() -> playGameTurn(), 500, TimeUnit.MILLISECONDS);
			
		}//else if (block.getType() != Material.AIR) {}
		
	}
	
	
	//Si le joueur prend des dégâts de chute, c'est qu'il a fail son saut 
	protected void onDamageHandler(EntityDamageEvent e) {
		//return si ce n'est pas le joueur en train de sauter qui a pris des dégâts de chute, ou s'il a fini son saut et s'est fait retp en haut
		if (e.getCause() != DamageCause.FALL || 
				playingPlayer == null || !e.getEntity().equals(playingPlayer.p) || 
				e.getEntity().getLocation().getBlockY() >= minJumpY)
			return;
		
		playingPlayer.sendDacMessage("§cLoupé ! Vous n'avez pas atteri dans l'eau...");
		
		playingPlayers.remove(0);
		playingPlayer = null;
		
		timeTask.cancel();
		
		endGame(AccountProvider.get(e.getEntity().getUniqueId()), 0, true);
		plugin.getTask().runTaskLater(() -> playGameTurn(), 500, TimeUnit.MILLISECONDS);
	}
	
	
	
	private void resetLandingArea() {
		for (int x = jumpRegion.getMin().getBlockX() ; x <= jumpRegion.getMax().getBlockX() ; x++)
			for (int z = jumpRegion.getMin().getBlockZ() ; z <= jumpRegion.getMax().getBlockZ() ; z++)
				world.getBlockAt(x, jumpRegion.getMax().getBlockY(), z).setType(Material.WATER);
	}

	
	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////
	
	
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		config = super.initConfig(config);

		if (!config.contains("jump_region"))
			config.set("jump_region", new Cuboid(world, 0, 0, 0, 1, 1, 1));	
		
		if (!config.contains("tp_loc"))
			config.set("tp_loc", new Location(world, 0, 0, 0));	
		
		if (!config.contains("min_jump_y"))
			config.set("min_jump_y", 0);
		
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
	public void jumpArea(CommandContext cmd) {
		Player p = getPlayer();
		
		new RegionEditor(p, region -> {
			if (region == null || !(region instanceof Cuboid) || region.getMax().getBlockY() != region.getMin().getBlockY()) {
				p.sendMessage(gameType.getChatPrefix() + "§cLa sélection n'est pas valide. Ce doit être un cuboïde de 1 bloc d'épaisseur.");
				return;
			}
			
			jumpRegion = (Cuboid) region;
			config.set("jump_region", region);
			
			p.sendMessage(gameType.getChatPrefix() + "§aLa nouvelle zone de saut a bien été définie. §7Un redémarage est nécessaire.");
		}).enterOrLeave();
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void tpLoc(CommandContext cmd) {
		Location loc = getPlayer().getLocation();
		
		tpLoc = loc;
		config.set("tp_loc", tpLoc);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLe point de téléportation a été défini en " +
				loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void minYloc(CommandContext cmd) {
		Location loc = getPlayer().getLocation();
		
		minJumpY = loc.getBlockY();
		config.set("min_jump_y", minJumpY);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLe niveau Y minimal pour valider le saut est maintenant en y = " +
				loc.getBlockY());
	}

	class DacPlayer {
		private Player p;
		private Material wool;
		
		public DacPlayer(Player p, Material wool) {
			this.p = p;
			this.wool = wool;
		}
		
		public void sendDacMessage(String msg) {
			p.sendMessage(gameType.getChatPrefix() + msg);
		}
		
		@Override
		public boolean equals(Object o) {
			return o instanceof DacPlayer && p.equals(((DacPlayer)o).p);
		}
	}
	
}