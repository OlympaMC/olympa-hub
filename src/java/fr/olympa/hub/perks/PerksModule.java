package fr.olympa.hub.perks;

import java.util.Arrays;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.olympa.api.common.module.OlympaModule.ModuleApi;
import fr.olympa.api.common.module.SpigotModule;
import fr.olympa.api.spigot.command.OlympaCommand;
import fr.olympa.api.spigot.customevents.AsyncOlympaPlayerChangeGroupEvent;
import fr.olympa.api.spigot.customevents.OlympaPlayerLoadEvent;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import fr.olympa.hub.perks.particles.PerkFootCloud;
import fr.olympa.hub.perks.particles.PerkSurroundings;

public class PerksModule implements ModuleApi<OlympaHub>, Listener {
	
	private OlympaHub plugin;
	
	private List<AbstractPerk> perks = Arrays.asList(new PerkFootCloud(), new PerkSurroundings());
	
	public PerksModule(OlympaHub plugin) throws Exception {
		SpigotModule<PerksModule, PerksModule, OlympaHub, OlympaCommand> module = new SpigotModule<>(plugin, "parachute", x -> this);
		module.listener(getClass());
		module.enableModule();
		module.registerModule();
	}
	
	@Override
	public boolean disable(OlympaHub plugin) {
		if (this.plugin != null) {
			this.plugin = null;
			perks.forEach(AbstractPerk::stop);
		}
		return true;
	}
	
	@Override
	public boolean enable(OlympaHub plugin) {
		if (this.plugin == null) {
			this.plugin = plugin;
			perks.forEach(AbstractPerk::start);
		}
		return true;
	}
	
	@Override
	public boolean setToPlugin(OlympaHub plugin) {
		return true;
	}
	
	@Override
	public boolean isEnabled() {
		return plugin != null;
	}
	
	@EventHandler
	public void onPlayerLoad(OlympaPlayerLoadEvent e) {
		OlympaPlayerHub player = e.getOlympaPlayer();
		for (AbstractPerk perk : perks) {
			if (perk.getPermission().hasPermission(player)) {
				perk.enable(player);
			}
		}
	}
	
	@EventHandler
	public void onGroupChange(AsyncOlympaPlayerChangeGroupEvent e) {
		OlympaPlayerHub player = e.getOlympaPlayer();
		for (AbstractPerk perk : perks) {
			boolean enabled = perk.isEnabled(player);
			if (enabled != perk.getPermission().hasPermission(player)) {
				if (enabled)
					perk.disable(player);
				else
					perk.enable(player);
			}
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		for (AbstractPerk perk : perks) {
			perk.disable(OlympaPlayerHub.get(e.getPlayer()));
		}
	}
	
}
