package fr.olympa.hub.minigames.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.sql.SQLColumn;
import fr.olympa.api.sql.statement.OlympaStatement;
import fr.olympa.hub.minigames.games.GameArena;
import fr.olympa.hub.minigames.games.GameDac;
import fr.olympa.hub.minigames.games.GameElytra;
import fr.olympa.hub.minigames.games.GameJump;
import fr.olympa.hub.minigames.games.IGame;

public enum GameType {
	ELYTRA("score_elytra", "§6Course d'élytra", "§6de la ", true, true, GameElytra::new),
	JUMP("score_jump", "§6Jump", "§6du ", true, true, GameJump::new),
	ARENA("score_arena", "§6Arène 1vs1", "§6de l'", false, false, GameArena::new),
	LABY("score_laby", "§6Labyrinthe", "du ", false, true, null),
	DAC("score_dac", "§6Dé à coudre", "§6du ", false, false, GameDac::new),
	
	;
	
	private GameProvider constructor;
	
	private String bddKey;
	private String name;
	private String article;
	private boolean isRestartable;
	private boolean isTimerScore;
	
	private final SQLColumn<OlympaPlayerHub> scoreColumn;
	private PreparedStatement bddStatement;
	
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
	
	public void initBddStatement() {
		String sort;
		if (isTimerScore)
			sort = "ASC";
		else
			sort = "DESC";
		try {
			bddStatement = new OlympaStatement("SELECT player_id, " + bddKey + " FROM " + AccountProvider.getPluginPlayerTable().getName() + 
			 " WHERE " + bddKey + " != 0 ORDER BY " + bddKey + " " + sort + " LIMIT " + IGame.maxTopScoresStored + ";").getStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
	
	public PreparedStatement getStatement() {
		return bddStatement;
	}
	
	public String getNameWithArticle() {
		return article + name.toLowerCase();
	}
	
	public String getChatPrefix() {
		return name + " > ";
	}
}	