package fr.olympa.hub.minigames.utils;

import java.sql.Types;

import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.sql.SQLColumn;
import fr.olympa.api.sql.statement.OlympaStatement;
import fr.olympa.hub.minigames.games.GameArena;
import fr.olympa.hub.minigames.games.GameDac;
import fr.olympa.hub.minigames.games.GameElytra;
import fr.olympa.hub.minigames.games.GameJump;
import fr.olympa.hub.minigames.games.GameTrident;
import fr.olympa.hub.minigames.games.AGame;

public enum GameType {
	ELYTRA("score_elytra", "§6Course d'élytra", "§6de la ", true, true, GameElytra::new),
	JUMP("score_jump", "§6Jump", "§6du ", true, true, GameJump::new),
	ARENA("score_arena", "§6Arène 1vs1", "§6de l'", false, false, GameArena::new),
	LABY("score_laby", "§6Labyrinthe", "§6du ", false, true, null),
	DAC("score_dac", "§6Dé à coudre", "§6du ", false, false, GameDac::new), 
	TRIDENT("score_trident", "§6Trident Run", "§6du ", false, false, GameTrident::new),
	;
	
	private GameProvider constructor;
	
	private String bddKey;
	private String name;
	private String article;
	private boolean isRestartable;
	private boolean isTimerScore;
	
	private final SQLColumn<OlympaPlayerHub> scoreColumn;
	
	GameType(String bddKey, String name, String art, boolean isRestartable, boolean isTimerScore, GameProvider constructor){
		this.constructor = constructor;
		
		this.bddKey = bddKey;
		this.name = name;
		this.article = art;
		this.isRestartable = isRestartable;
		this.isTimerScore = isTimerScore;
		
		scoreColumn = new SQLColumn<OlympaPlayerHub>(bddKey, "DOUBLE NOT NULL DEFAULT 0", Types.DOUBLE).setUpdatable();
	}
	
	public static GameType getGameTypeOfBddKey(String key) {
		for (GameType game : GameType.values()) if (game.getBddKey().equals(key))
			return game;
		
		return null;
	}
	
	public GameProvider getGameProvider(){
		return constructor;
	}
	
	public SQLColumn<OlympaPlayerHub> getScoreColumn() {
		return scoreColumn;
	}
	
	public String getBddKey() {
		return bddKey;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isRestartable() {
		return isRestartable;
	}
	
	public boolean isTimerScore() {
		return isTimerScore;
	}
	
	public OlympaStatement getStatement() {
		return new OlympaStatement("SELECT player_id, " + bddKey + " FROM " + AccountProvider.getPluginPlayerTable().getName() + 
				" WHERE " + bddKey + " != 0 ORDER BY " + bddKey + (isTimerScore ? " ASC" : " DESC") + " LIMIT " + AGame.maxTopScoresStored + ";");
	}
	
	public String getNameWithArticle() {
		return article + name.toLowerCase();
	}
	
	public String getChatPrefix() {
		return name + " > ";
	}
}	