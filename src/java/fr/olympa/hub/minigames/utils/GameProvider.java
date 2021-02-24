package fr.olympa.hub.minigames.utils;

import java.rmi.activation.ActivateFailedException;

import org.bukkit.configuration.ConfigurationSection;

import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.games.IGame;

@FunctionalInterface
public interface GameProvider {

	IGame getGame(OlympaHub plugin, ConfigurationSection config) throws ActivateFailedException;
}
