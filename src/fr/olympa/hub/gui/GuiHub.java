package fr.olympa.hub.gui;

import java.util.Arrays;

import fr.tristiisch.olympa.api.plugin.OlympaPlugin;

public enum GuiHub {

	MENU,
	PROFIL;

	public static GuiHub get(String id) {
		return Arrays.stream(GuiHub.values()).filter(gui -> gui.toString().equals(id)).findFirst().orElse(null);
	}

	@Override
	public String toString() {
		return OlympaPlugin.getInstance().getDescription().getName() + "." + this.name();
	}
}
