package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.editor.RegionEditor;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.shapes.Cuboid;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;

public class GameDac extends IGame {

	private final int minPlayers = 2;
	//private boolean isGameInProgress = false;
	
	private final int countdownDelay = 3;
	private final int playDelay = 8;
	
	private int currentTurn = -1;

	private List<Player> waitingPlayers = new ArrayList<Player>();
	private List<Player> playingPlayers = new ArrayList<Player>();
	
	private Cuboid jumpRegion;
	private Location tpLoc;
	
	private Player playingPlayer = null;
	private boolean hasJumped = false;
	
	public GameDac(OlympaHub plugin, ConfigurationSection configFromFile) throws ActivateFailedException {
		super(plugin, GameType.DAC, configFromFile);

		jumpRegion = (Cuboid) config.get("jump_region");
		
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
		
		if (playingPlayers.contains(p.getPlayer())) {
			playingPlayers.remove(p.getPlayer());
			
			if (p.getPlayer().equals(playingPlayer))
				playGameTurn();
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
			playingPlayers.addAll(waitingPlayers);
			waitingPlayers.clear();
			
			playingPlayers.forEach(p -> p.teleport(tpLoc));

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
				endGame(AccountProvider.get(playingPlayers.get(0).getUniqueId()), 1, true);
			
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

		hasJumped = false;
		
		playingPlayer.sendMessage(gameType.getChatPrefix() + "§aTour " + currentTurn + " : c'est à vous ! §7Sautez du pont, et tentez d'atterir dans l'eau ! Vous avez " + playDelay + " secondes.");

		//si le joueur a mis trop de temps à sauter, expulsion
		final int currentTurnBis = currentTurn;
		
		plugin.getTask().runTaskLater(() -> {
			if (currentTurn == currentTurnBis)
				if (playingPlayer != null && playingPlayer.isOnline())
					endGame(AccountProvider.get(playingPlayer.getUniqueId()), 0, true);
			
		}, playDelay, TimeUnit.SECONDS);
	}
	
	/**
	 * Executes actions when a player lands (in water or on ground)
	 * @param p
	 */
	@Override
	protected void onMoveHandler(Player p, Location from, Location to) {
		//si un joueur a sauté
		if (p.getLocation().add(0, -1, 0).getBlock().getType() == Material.AIR && 
				p.getLocation().clone().add(0, -2, 0).getBlock().getType() == Material.AIR &&
				p.getLocation().clone().add(0, -3, 0).getBlock().getType() == Material.AIR &&
				p.getLocation().clone().add(0, -4, 0).getBlock().getType() == Material.AIR) {
			
			//détection de si c'est le joueur qui devait jouer qui a sauté
			if (p.equals(playingPlayer))
				hasJumped = true;
			else {
				p.teleport(tpLoc);
				p.sendMessage(gameType.getChatPrefix() + "§7Ce n'est pas votre tour de sauter !");
			}
			
			return;
		}
		
		//si ce n'est pas le bon joueur ou s'il n'a pas sauté
		if (!p.equals(playingPlayer) || !hasJumped)
			return;
		
		Block block = to.clone().add(0, -1, 0).getBlock();

		if (block.getType() == Material.WATER && jumpRegion.isIn(block.getLocation())) {
			playingPlayers.add(playingPlayers.remove(0));
			playingPlayer = null;
			
			p.teleport(tpLoc);
			p.sendMessage(gameType.getChatPrefix() + "§aBien visé !");
			
			block.setType(Material.BEDROCK);
			
			plugin.getTask().runTaskLater(() -> playGameTurn(), 500, TimeUnit.MILLISECONDS);
			
		}else if (block.getType() != Material.AIR) {
			playingPlayers.remove(0);
			playingPlayer = null;
			
			endGame(AccountProvider.get(p.getUniqueId()), 0, true);
			plugin.getTask().runTaskLater(() -> playGameTurn(), 500, TimeUnit.MILLISECONDS);
		}
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

}











