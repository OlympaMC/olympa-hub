package fr.olympa.hub.minigames.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.games.IGame;
import redis.clients.jedis.Jedis;

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
        	if (game.getGameProvider() != null) {
        		IGame iGame = game.getGameProvider().getGame(plugin, gamesConfig.getConfigurationSection("game_" + game.toString().toLowerCase()));
        		plugin.getLogger().log(Level.INFO, "§aGame " + game + " successfully loaded.");
        		games.put(game, iGame);	
        	}else
        		plugin.getLogger().log(Level.WARNING, "§cGame " + game + " wasn't loaded successfully.");	
        
		
		//register redis
        
        /*plugin.getTask().runTaskLater(() -> {
        	OlympaCore.getInstance().registerRedisSub(RedisAccess.INSTANCE.connect(), new GamesRedisListener(), RedisChannel.SPIGOT_LOBBY_MINIGAME_SCORE.name());
            plugin.getLogger().log(Level.INFO, "§aLoaded minigames redis chanel " + RedisChannel.SPIGOT_LOBBY_MINIGAME_SCORE.name().toLowerCase());
        }, 100);*/
		
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
