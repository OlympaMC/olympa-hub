package fr.olympa.hub.gui;

import java.util.Arrays;

import fr.olympa.hub.OlympaHub;

public enum GuiHub {

	MENU,
	PROFIL;

	public static GuiHub get(String id) {
		return Arrays.stream(GuiHub.values()).filter(gui -> gui.toString().equals(id)).findFirst().orElse(null);
	}

	@Override
	public String toString() {
		return OlympaHub.getInstance().getDescription().getName() + "." + this.name();
	}
}
