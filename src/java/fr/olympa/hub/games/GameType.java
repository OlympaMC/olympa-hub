package fr.olympa.hub.games;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.sql.OlympaStatement;

public enum GameType {
	ELYTRA("score_elytra", "§6Course d'élytra", "de la ", true, true, null),
	JUMP("score_jump", "§6Jump", "du ", true, true, GameJump::new),
	ARENA("score_arena", "§6Arène 1vs1", "de l'", false, false, GameArena::new),
	LABY("score_laby", "§6Labyrinthe", "du ", false, true, null),
	DAC("score_dac", "§6Dé-à-coudre", "du ", false, false, null),
	
	;
	
	private GameProvider constructor;
	
	private String bddKey;
	private String name;
	private String article;
	private boolean isRestartable;
	private boolean isTimerScore;
	
	private PreparedStatement bddStatement;
	
	GameType(String bddKey, String name, String art, boolean isRestartable, boolean isTimerScore, GameProvider constructor){
		this.constructor = constructor;
		
		this.bddKey = bddKey;
		this.name = name;
		this.article = art;
		this.isRestartable = isRestartable;
		this.isTimerScore = isTimerScore;
		
		String sort;
		if (isTimerScore)
			sort = "ASC";
		else
			sort = "DESC";
		
		try {
			bddStatement = new OlympaStatement("SELECT player_id, " + bddKey + " FROM " + AccountProvider.getPlayerProviderTableName() + 
			 " WHERE " + bddKey + " != 0 ORDER BY " + bddKey + " " + sort + " LIMIT " + IGame.maxTopScoresStored + ";").getStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public GameProvider getGameProvider(){
		return constructor;
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