package fr.olympa.hub.games;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import fr.olympa.hub.OlympaHub;

public class MiniGamesManager {

	private static MiniGamesManager instance;
	
	private Map<GameType, IGame> games = new HashMap<GameType, IGame>(); 
	
	public MiniGamesManager(OlympaHub plugin) {
		instance = this;
		
        File gamesConfigFile = new File(plugin.getDataFolder(), "games.yml");
        
        if (!gamesConfigFile.exists()) {
        	gamesConfigFile.getParentFile().mkdirs();
            plugin.saveResource("games.yml", false);
         }

        YamlConfiguration gamesConfig = new YamlConfiguration();
        try {
        	gamesConfig.load(gamesConfigFile);
            plugin.getLogger().log(Level.INFO, "§aSuccessfully loaded games.yml.");
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            plugin.getLogger().log(Level.SEVERE, "§cUnable to load minigames config, please check the file or delete it to generate a new one.");
            return;
        }
        
        for (GameType game : GameType.values())
        	if (game.getGameProvider() != null)
        		games.put(game, game.getGameProvider().getGame(plugin, gamesConfig.getConfigurationSection("game_" + game.toString().toLowerCase())));
        
        Bukkit.getLogger().log(Level.INFO, "§aLoaded minigames : " + games.keySet());
	}
	
	public static MiniGamesManager getInstance() {
		return instance;
	}
	
	/**
	 * Return game the player is player
	 * @param p player to test
	 * @return GameType of the game, or null if player isn't playing any game
	 */
	public GameType isPlaying(Player p) {
		for (IGame game : games.values())
			if (game.getPlayers().contains(p.getUniqueId()))
				return game.getType();
		
		return null;
	}
	
	public IGame getGame(GameType game) {
		return games.get(game);
	}

	
	///////////////////////////////////////////////////////////
	//                  GAMETYPE SUB CLASS                   //
	///////////////////////////////////////////////////////////
	

}
