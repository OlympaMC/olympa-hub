package fr.olympa.hub.minigames.utils;

import java.util.logging.Level;

import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.EnumUtils;

import fr.olympa.api.provider.AccountProvider;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.games.IGame;
import redis.clients.jedis.JedisPubSub;

public class GamesRedisListener extends JedisPubSub {
	
	@Override
	public void onMessage(String channel, String message) {
		OlympaHub.getInstance().getLogger().log(Level.INFO, "§dRecieved from " + channel + " : '" + message + "'");
		
		if (message.length() == 0)
			return;
		
		String[] infos = message.split(":");
		
		if (!EnumUtils.isValidEnum(GameType.class, infos[0]))
			return;
		
		GameType gameType = GameType.valueOf(infos[0]);
		
		if (gameType == null)
			return;
		
		IGame game = MiniGamesManager.getInstance().getGame(gameType);
		
		if (game != null)	
			game.updateScores(AccountProvider.getPlayerInformations(Long.valueOf(infos[1])), Double.valueOf(infos[2]));
	}
}
