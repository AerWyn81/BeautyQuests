package fr.skytasul.quests.scoreboards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.QuestsConfiguration;
import fr.skytasul.quests.utils.DebugUtils;

public class ScoreboardManager{

	// Config parameters
	private List<ScoreboardLine> lines = new ArrayList<>();
	private int changeTime;
	private boolean hide;
	private boolean refreshLines;
	
	private List<String> worldsFilter;
	private boolean isWorldAllowList;
	
	private Map<Player, Scoreboard> scoreboards = new HashMap<>();
	
	public ScoreboardManager(YamlConfiguration config){
		if (!QuestsConfiguration.showScoreboards()) return;
		
		changeTime = config.getInt("quests.changeTime", 11);
		hide = config.getBoolean("quests.hideIfEmpty", true);
		refreshLines = config.getBoolean("quests.refreshLines", true);
		
		worldsFilter = config.getStringList("worlds.filterList");
		isWorldAllowList = config.getBoolean("worlds.isAllowList");
		
		for (Map<?, ?> map : config.getMapList("lines")){
			if (lines.size() == 15){
				BeautyQuests.logger.warning("Limit of 15 scoreboard lines reached - please delete some in scoreboard.yml");
				break;
			}
			try{
				lines.add(ScoreboardLine.deserialize((Map<String, Object>) map));
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
		DebugUtils.logMessage("Registered " + lines.size() + " lines in scoreboard");
	}
	
	public List<ScoreboardLine> getScoreboardLines(){
		return lines;
	}
	
	public int getQuestChangeTime(){
		return changeTime;
	}
	
	public boolean hideEmtptyScoreboard(){
		return hide;
	}
	
	public boolean refreshLines(){
		return refreshLines;
	}
	
	public List<String> getWorldsFilter() {
		return worldsFilter;
	}
	
	public boolean isWorldAllowList() {
		return isWorldAllowList;
	}
	
	public boolean isWorldAllowed(String worldName) {
		return isWorldAllowList() ? getWorldsFilter().contains(worldName) : !getWorldsFilter().contains(worldName);
	}
	
	public Scoreboard getPlayerScoreboard(Player p){
		return scoreboards.get(p);
	}
	
	public void removePlayerScoreboard(Player p){
		if (scoreboards.containsKey(p)) scoreboards.remove(p).cancel();
	}
	
	public void create(Player p){
		if (!QuestsConfiguration.showScoreboards()) return;
		removePlayerScoreboard(p);
		scoreboards.put(p, new Scoreboard(p, this));
	}
	
	public void unload(){
		for (Scoreboard s : scoreboards.values()) s.cancel();
		if (!scoreboards.isEmpty()) BeautyQuests.getInstance().getLogger().info(scoreboards.size() + " scoreboards deleted.");
		scoreboards.clear();
	}
	
}
