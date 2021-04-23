package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import fr.olympa.api.provider.AccountProvider;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;

public abstract class AQueuedGame extends AGame {

	private List<Player> waitingPlayers = new ArrayList<>();
	protected List<Player> playingPlayers = new ArrayList<>();

	private int minPlayers;
	private int maxPlayers;
	
	private BukkitTask countdownTask = null;

	private int countdown;
	private final int countdownDelay = 3;

	protected int winnerScore = 0;
	
	public AQueuedGame(OlympaHub plugin, GameType game, ConfigurationSection configFromFile, int minPlayers, int maxPlayers) throws ActivateFailedException {
		super(plugin, game, configFromFile);
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
	}
	
	@Override
	protected boolean startGame(OlympaPlayerHub p) {
		if (!super.startGame(p))
			return false;

		waitingPlayers.forEach(wp -> sendMessage(wp, "§a§l" + p.getName() + " §aa rejoint la file d'attente !"));
		waitingPlayers.add(p.getPlayer());	
		
		if (playingPlayers.size() > 0) 
			sendMessage(p.getPlayer(), "§7Une partie est déjà en cours...");
		else 
			tryToInitGame();	
		
		return true;
	}

	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);

		waitingPlayers.remove(p.getPlayer());
		
		if (playingPlayers.remove(p.getPlayer()) && playingPlayers.size() <= 1) {
			if (playingPlayers.size() == 1)
				endGame(AccountProvider.get(playingPlayers.get(0).getUniqueId()), winnerScore, true);
			endGame();
			tryToInitGame();
		}
	}
	 
	private void tryToInitGame() {
		if (playingPlayers.size() > 0)
			return;
		
		if (playingPlayers.size() > 0) {
			waitingPlayers.forEach(p -> sendMessage(p, "§aUne partie est déjà en cours. Nombres de joueurs prêts pour la prochaine partie : " + waitingPlayers.size() + "/" + minPlayers));
			
		}else {
			//démarre le compte à rebours avant lancement de la partie
			if (waitingPlayers.size() >= minPlayers && countdownTask == null) {
				
				countdown = countdownDelay;
				countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
					
					//arrête le timer si plus assez de joueurs ne sont en ligne
					if (waitingPlayers.size() < minPlayers) {
						waitingPlayers.forEach(p -> sendMessage(p, "§7Plus assez de joueur pour commencer la partie... (" + waitingPlayers.size() + "/" + minPlayers + ")"));
						countdownTask.cancel();
						countdownTask = null;
						return;
					}
					
					if (countdown > 0) {
						waitingPlayers.forEach(p -> {
							sendMessage(p, "§aDébut du match dans §l" + countdown);
							p.sendTitle("§c" + countdown, "§7Début du match dans...", 0, 21, 0);
						});
						
						countdown--;
					}else {
						countdownTask.cancel();
						countdownTask = null;
						
						for (Iterator<Player> i = waitingPlayers.iterator(); i.hasNext();) {
							Player p = i.next();
							
							if (playingPlayers.size() < maxPlayers) {
								playingPlayers.add(p);
								i.remove();
								sendMessage(p, "§aVous rejoignez la partie !");
							}else
								sendMessage(p, "§7Votre place dans la file ne vous a pas permis de rejoindre la partie, veuillez patienter...");	
						}

						Collections.shuffle(playingPlayers);

						winnerScore = playingPlayers.size() - 1;

						//lance le jeu
						startGame();
					}
				}, 0, 20);
				
			}else
				waitingPlayers.forEach(p -> sendMessage(p, "§aNombre de joueurs prêts : " + waitingPlayers.size() + "/" + minPlayers));
		}
	}
	
	/**
	 * This will be executed when the minimum count of player will be reached and the countdown timer ended
	 */
	protected abstract void startGame();
	
	/**
	 * This will be executed when the game ends (playingPlayers size = 0)
	 */
	protected abstract void endGame();
}
