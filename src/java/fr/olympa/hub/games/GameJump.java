package fr.olympa.hub.games;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import fr.olympa.api.provider.AccountProvider;
import fr.olympa.hub.OlympaHub;

public class GameJump extends IGame{

	private List<Location> checkpoints = new ArrayList<Location>();

	private Map<Player, Long> playersInitTime = new HashMap<Player, Long>();
	private Map<Player, Integer> playersLastCheckPoint = new HashMap<Player, Integer>();
	
	public GameJump(OlympaHub plugin, ConfigurationSection config) {
		super(plugin, GameType.JUMP, config);
		
		config.getStringList("checkpoints").forEach(s -> checkpoints.add(getLoc(s)));
	}

	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);
		
		playersInitTime.put(p.getPlayer(), System.currentTimeMillis());
		playersLastCheckPoint.put(p.getPlayer(), 0);
		
		p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2Objectif : arriver le plus rapidement possible à la fin du jump !");
		p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2" + checkpoints.size() + " checkpoints sont prévus, n'hésitez pas à les utiliser pour obtenir le meilleur temps possible !");
	}
	
	@Override
	protected void restartGame(OlympaPlayerHub p) {
		super.restartGame(p);
		
		playersInitTime.put(p.getPlayer(), System.currentTimeMillis());
		playersLastCheckPoint.put(p.getPlayer(), 0);
		
		p.getPlayer().teleport(startingLoc);
	}
	
	@Override
	protected boolean endGame(OlympaPlayerHub p, double score, boolean teleportToGameSpawn) {
		endGame(p, score, teleportToGameSpawn);
		
		playersInitTime.remove(p.getPlayer());
		playersLastCheckPoint.remove(p.getPlayer());
		return true;
	}
	
	@Override
	protected void onMoveHandler(Player p, Location from, Location to) {
		int check = getCheckpointIndex(to);
		
		if (check == 0)
			return;
		
		if (playersLastCheckPoint.get(p) + 1 == check) {
			if (check == checkpoints.size()) 
				endGame(AccountProvider.get(p.getUniqueId()), (System.currentTimeMillis() - playersInitTime.get(p))/1000, true);
			else
		}
			
	}
	
	private int getCheckpointIndex(Location toCheck) {
		for (Location loc : checkpoints)
			if (loc.getBlockX() == toCheck.getBlockX() && loc.getBlockY() == toCheck.getBlockY() && loc.getBlockZ() == toCheck.getBlockZ())
				return checkpoints.indexOf(loc) + 1;
		
		return 0;
	}

}
