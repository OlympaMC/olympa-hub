package fr.olympa.hub.minigames.utils;

import java.io.File;
import java.io.IOException;
import java.rmi.activation.ActivateFailedException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import fr.olympa.api.command.complex.ComplexCommand;
import fr.olympa.api.redis.RedisAccess;
import fr.olympa.api.redis.RedisChannel;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.HubPermissions;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.games.IGame;
import redis.clients.jedis.Jedis;

public class MiniGamesManager {

	private static MiniGamesManager instance;
	
	private Map<GameType, IGame> games = new HashMap<GameType, IGame>(); 
	
	private YamlConfiguration config;
	private File configFile;
	
	public MiniGamesManager(OlympaHub plugin) {
		instance = this;
		
        configFile = new File(plugin.getDataFolder(), "games.yml");
        
        if (!configFile.exists()) {
        	try {
				if (configFile.getParentFile().mkdirs() || configFile.createNewFile())
		            plugin.getLogger().log(Level.WARNING, "Games config games.yml wasn't found, creating empty file.");
					
			} catch (IOException e) {
	            plugin.getLogger().log(Level.SEVERE, "§cUnable to create minigames config, please check the files permissions.");
				e.printStackTrace();
			}
            //plugin.saveResource("games.yml", false);
         }

        config = new YamlConfiguration();

        try {
        	config.load(configFile);
            plugin.getLogger().log(Level.INFO, "§aSuccessfully loaded games.yml.");
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            plugin.getLogger().log(Level.SEVERE, "§cUnable to load minigames config, please check the file or delete it to generate a new one.");
            return;
        }
        
        for (GameType game : GameType.values())
        	if (game.getGameProvider() != null) {
        		try {
        			IGame iGame = game.getGameProvider().getGame(plugin, config.getConfigurationSection(game.toString().toLowerCase()));
        					
            		iGame.register(); //register game commands
            		games.put(game, iGame);	
            		
            		plugin.getLogger().log(Level.INFO, "§aGame " + game + " successfully loaded.");
        		}catch (ActivateFailedException e) {
        			plugin.getLogger().log(Level.WARNING, "§eGame " + game + "§e wasn't loaded because isEnabled property in games.yml was set to false.");
        		}
        		
        	}else
        		plugin.getLogger().log(Level.WARNING, "§cGame " + game + " wasn't loaded successfully.");	
        
        saveConfig(getConfig());
		
		//register redis
        OlympaCore.getInstance().registerRedisSub(RedisAccess.INSTANCE.connect(), new GamesRedisListener(), RedisChannel.SPIGOT_LOBBY_MINIGAME_SCORE.name());
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
	
	/**
	 * Get games config
	 * @return
	 */
	public YamlConfiguration getConfig() {
		return config;
	}
	
	/**
	 * Save config to games.yml file
	 * @param config
	 */
	public void saveConfig(YamlConfiguration config) {
		try {
			config.save(configFile);
		} catch (IOException e) {
			OlympaHub.getInstance().getLogger().log(Level.SEVERE, "Failed to save games.yml, please check authorizations.");
			e.printStackTrace();
		}
	}
}
