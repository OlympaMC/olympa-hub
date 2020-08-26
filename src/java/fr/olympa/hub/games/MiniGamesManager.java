package fr.olympa.hub.games;

import java.util.HashMap;
import java.util.Map;

import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.games.IGame.GameType;

public class MiniGamesManager {

	private static MiniGamesManager instance;
	
	private Map<GameType, IGame> games = new HashMap<GameType, IGame>(); 
	
	public MiniGamesManager(OlympaHub olympaHub) {
		instance = this;
	}
	
	public static MiniGamesManager getInstance() {
		return instance;
	}
	
	public IGame getGame(GameType game) {
		return games.get(game);
	}
}
