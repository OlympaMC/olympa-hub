package fr.olympa.hub.minigames.utils;


import org.bukkit.configuration.ConfigurationSection;

import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.games.AGame;

@FunctionalInterface
public interface GameProvider {

	AGame getGame(OlympaHub plugin, ConfigurationSection config) throws UnsupportedOperationException;
}
