package fr.olympa.hub.games;

import org.bukkit.configuration.ConfigurationSection;

import fr.olympa.hub.OlympaHub;

@FunctionalInterface
public interface GameProvider {

	IGame getGame(OlympaHub plugin, ConfigurationSection config);
}
