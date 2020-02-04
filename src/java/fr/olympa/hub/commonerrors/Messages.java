package fr.olympa.hub.commonerrors;

import fr.olympa.api.utils.SpigotUtils;

public enum Messages {

	SERVER_NOT_FOUND("&c&lERREUR: &cIl semble qu'il y'a une erreur: &f(SERVER_NOT_FOUND)"),
	SPAWN_NOT_FOUND("&c&lERREUR: &cIl semble qu'il y'a une erreur: &f(SPAWN_NOT_FOUND)");

	private String erreur;

	Messages(String erreur) {
		this.erreur = erreur;
	}

	public String getErreur() {
		return SpigotUtils.color(this.erreur);
	}
}
