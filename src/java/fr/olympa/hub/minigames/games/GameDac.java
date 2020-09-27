package fr.olympa.hub.minigames.games;

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

	private final int minPlayers = 4;
	private boolean isGameInProgress = false;
	
	private final int countdownDelay = 3;
	private final int playDelay = 8;
	
	private int currentTurn = -1;

	private List<Player> waitingPlayers = new ArrayList<Player>();
	private List<Player> playingPlayers = new ArrayList<Player>();
	
	private Cuboid jumpRegion;
	private Location tpLoc;
	
	private Cuboid barrierRegion;
	
	private Player playingPlayer = null;
	private boolean hasJumped = false;
	
	public GameDac(OlympaHub plugin, ConfigurationSection configFromFile) {
		super(plugin, GameType.DAC, configFromFile);

		jumpRegion = (Cuboid) config.get("jump_region");		
		barrierRegion = (Cuboid) config.get("barrier_region");
		
		allowedTpLocs.add(tpLoc = config.getLocation("tp_loc"));

		resetLandingArea();

		//cancel region entering if player isn't the one which should jump
		//and set the jump marker to true
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
		});
	}

	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);

		waitingPlayers.add(p.getPlayer());
		
		if (isGameInProgress)
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
			playGameTurn(false);
		}
	} 
	 
	private void tryToInitGame() {
		if (isGameInProgress || waitingPlayers.size() < minPlayers)
			getPlayers().forEach(id -> Bukkit.getPlayer(id).sendMessage(gameType.getChatPrefix() + "§aNombres de joueurs : " + getPlayers().size() + "/" + minPlayers));
		else
			startGame(countdownDelay);
	}
	
	private void startGame(int countdown) {
		if (isGameInProgress)
			return;
		
		//s'il n'y a plus assez de joueurs pour commencer la partie, cancel de cette der
		if (waitingPlayers.size() <= 1) 
			waitingPlayers.forEach(p -> p.sendMessage(gameType.getChatPrefix() + "§7Plus assez de joueur pour commencer la partie..."));
		
		//actions avant début de partie
		if (countdown > 0) {
			waitingPlayers.forEach(p -> p.sendTitle("§c" + countdown, "§7Début du match dans...", 0, 21, 0));
			waitingPlayers.forEach(p -> p.sendMessage(gameType.getChatPrefix() + "§aDébut du match dans " + countdown));
			
			plugin.getTask().runTaskLater(() -> startGame(countdown - 1), 1, TimeUnit.SECONDS);
			
		//actions de début de partie
		}else {
			isGameInProgress = true;
			
			playingPlayers.addAll(waitingPlayers);
			waitingPlayers.clear();
			
			playingPlayers.forEach(p -> p.teleport(tpLoc));
			
			playGameTurn(true);	
		}
	}
	
	private void playGameTurn(boolean isInit) {
		if (!isGameInProgress)
			return;
		
		//si plus qu'un seul joueur en lice, fin de jeu (ou reset du jeu si 0 joueurs restants)
		if (playingPlayers.size() <= 1) {
			if (playingPlayers.size() == 1)
				endGame(AccountProvider.get(playingPlayers.get(0).getUniqueId()), 1, true);
			
			//réinitialisation du jeu
			isGameInProgress = false;
			resetLandingArea();
			playingPlayers.clear();
			playingPlayer = null;
			
			//tentative de relance du jeu (si assez de joueurs sont en file d'atente)
			tryToInitGame();
			return;
		}
		
		if (isInit) {
			currentTurn = 0;
			plugin.getTask().runTaskLater(() -> playGameTurn(false), 1, TimeUnit.SECONDS);
			return;
			
		}else {
			playingPlayer = playingPlayers.get(0);
			currentTurn++;
		}

		hasJumped = false;
		
		playingPlayer.sendMessage(gameType.getChatPrefix() + "§aTour " + currentTurn + " : c'est à vous ! §7Sautez du pont, et tentez d'atterir dans l'eau !");

		//si le joueur a mis trop de temps à sauter, forfait
		final int currentTurnBis = currentTurn;
		
		plugin.getTask().runTaskLater(() -> {
			if (currentTurn == currentTurnBis)
				if (playingPlayer != null)
					endGame(AccountProvider.get(playingPlayer.getUniqueId()), 0, true);
			
		}, playDelay, TimeUnit.SECONDS);
	}
	
	/**
	 * Executes actions when a player lands (in water or on ground)
	 * @param p
	 */
	@Override
	protected void onMoveHandler(Player p, Location from, Location to) {
		if (!p.equals(playingPlayer) || !hasJumped)
			return;
		
		Block block = to.clone().add(0, -1, 0).getBlock();
		/*
		if (!hasJumped && block.getType() == Material.AIR) {
			hasJumped = true;
			return;
		}*/
		
		if (hasJumped)
			if (block.getType() == Material.WATER && jumpRegion.isIn(block.getLocation())) {
				playingPlayers.add(playingPlayers.remove(0));
				playingPlayer = null;
				
				p.teleport(tpLoc);
				p.sendMessage(gameType.getChatPrefix() + "§aBien visé !");
				
				block.setType(Material.BEDROCK);
				
				plugin.getTask().runTaskLater(() -> playGameTurn(false), 500, TimeUnit.MILLISECONDS);
				
			}else if (block.getType() != Material.AIR) {
				playingPlayers.remove(0);
				playingPlayer = null;
				
				endGame(AccountProvider.get(p.getUniqueId()), 0, true);
				plugin.getTask().runTaskLater(() -> playGameTurn(false), 500, TimeUnit.MILLISECONDS);
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
		if (!config.contains("barrier_region"))
			config.set("barrier_region", new Cuboid(world, 0, 0, 0, 1, 1, 1));	
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
	public void barrierArea(CommandContext cmd) {
		Player p = getPlayer();

		new RegionEditor(p, region -> {
			if (region == null || !(region instanceof Cuboid)) {
				p.sendMessage(gameType.getChatPrefix() + "§cLa sélection n'est pas valide. Ce doit être un cuboïde.");
				return;
			}
			
			barrierRegion = (Cuboid) region;
			config.set("barrier_region", region);
			
			p.sendMessage(gameType.getChatPrefix() + "§aLa nouvelle région barrière a bien été définie. §7Un redémarage est nécessaire.");
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











