package fr.olympa.hub.commonerrors;

import org.bukkit.entity.Player;

public enum Messages {
	SERVER_NOT_FOUND("�c�lERREUR: �cIl semble qu'il y'a une erreur: �f(SERVER_NOT_FOUND)"),
	SPAWN_NOT_FOUND("�c�lERREUR: �cIl semble qu'il y'a une erreur: �f(SPAWN_NOT_FOUND)"),
	ITEM_NOT_AVAIBLE("�c�lERREUR: �cIl semble qu'il y'a une erreur: �f(ITEM_NOT_AVAIBLE)");

	private String erreur;

	Messages(String erreur) {
		this.erreur = erreur;
	}

	public String getErreur() {
		return this.erreur;
	}

	public void send(Player p) {
		p.sendMessage(getErreur());
	}
}
