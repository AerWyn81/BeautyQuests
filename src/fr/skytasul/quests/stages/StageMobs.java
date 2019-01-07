package fr.skytasul.quests.stages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.QuestsConfiguration;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.players.PlayerAccount;
import fr.skytasul.quests.players.PlayersManager;
import fr.skytasul.quests.utils.DebugUtils;
import fr.skytasul.quests.utils.Lang;
import fr.skytasul.quests.utils.Utils;
import fr.skytasul.quests.utils.compatibility.Post1_9;
import fr.skytasul.quests.utils.compatibility.mobs.CompatMobDeathEvent;
import fr.skytasul.quests.utils.types.Mob;

public class StageMobs extends AbstractStage{

	private final List<Mob> mobs;
	private Map<PlayerAccount, PlayerDatas> remaining = new HashMap<>();
	private boolean shoot = false;
	
	private int cachedSize;
	
	public StageMobs(StageManager manager, List<Mob> mobs){
		super(manager);
		if (mobs != null) {
			this.mobs = mobs;
			cachedSize = mobsSize(mobs);
		}else this.mobs = new ArrayList<>();
	}
	
	@EventHandler
	public void onMobKilled(CompatMobDeathEvent e){
		if (shoot && e.getBukkitEntity().getLastDamageCause().getCause() != DamageCause.PROJECTILE) return;
		Player p = e.getKiller();
		PlayerAccount acc = PlayersManager.getPlayerAccount(p);
		if (manager.hasStageLaunched(acc, this)){                                                            
			PlayerDatas player = remaining.get(acc);
			List<Mob> playerMobs = player.remaining;
			for (Mob m : playerMobs){
				/*if (m.hasBukkitMob()){
					
				}else if (m.hasMythicMob()){
					if (((ActiveMob) e.getPluginMob()).getType().equals(m.getMythicMob())){
						m.amount--;
						if (m.amount == 0){
							playerMobs.remove(m);
							break;
						}
					}
				}else if (m.hasEpicBoss()){
					if (e.getPluginMob().equals(m.getBossName())){
						m.amount--;
						if (m.amount == 0){
							playerMobs.remove(m);
							break;
						}
					}
				}*/
				if (m.equalsMob(e.getPluginMob())){
					m.amount--;
					if (m.amount == 0){
						playerMobs.remove(m);
						break;
					}
				}
			}
			finalTest(p, playerMobs, player);
		}
	}
	
	/*@EventHandler
	public void onNPCKilled(NPCDeathEvent e){
		Entity en = e.getNPC().getEntity();
		if (!(en instanceof LivingEntity)) return;
		if (shoot && en.getLastDamageCause().getCause() != DamageCause.PROJECTILE) return;
		Player p = ((LivingEntity) en).getKiller();
		if (p == null) return;
		PlayerAccount acc = PlayersManager.getPlayerAccount(p);
		if (manager.hasStageLaunched(acc, this)){
			if (!remaining.containsKey(acc)) remainingAdd(acc);
			Pair<List<Mob>, Object> pair = remaining.get(acc);
			List<Mob> playerMobs = pair.getKey();
			for (Mob m : playerMobs){
				if (!m.hasNPC()) continue;
				if (e.getNPC().equals(e.getNPC())){
					m.amount--;
					if (m.amount == 0){
						playerMobs.remove(m);
					}
					break;
				}
			}
			finalTest(p, playerMobs, pair.getValue());
		}
	}*/
	
	/*@EventHandler
	public void onEntityKilled(EntityDeathEvent e){
		LivingEntity en = e.getEntity();
		if (en.getKiller() == null) return;
		if (!(en.getKiller() instanceof Player)) return;
		if (shoot && en.getLastDamageCause().getCause() != DamageCause.PROJECTILE) return;
		Player p = en.getKiller();
		PlayerAccount acc = PlayersManager.getPlayerAccount(p);
		if (manager.hasStageLaunched(acc, this)){
			if (!remaining.containsKey(acc)) remainingAdd(acc);
			Pair<List<Mob>, Object> pair = remaining.get(acc);
			List<Mob> playerMobs = pair.getKey();
			for (Mob m : playerMobs){
				if (!m.hasBukkitMob()) continue;
				if (e.getEntityType() == m.getBukkitMob()){
					m.amount--;
					if (m.amount == 0){
						playerMobs.remove(m);
					}
					break;
				}
			}
			finalTest(p, playerMobs, pair.getValue());
		}
	}*/
	
	public void finalTest(Player p, List<Mob> playerMobs, PlayerDatas datas){
		if (playerMobs.isEmpty()){
			finishStage(p);
		}else {
			int i = 0;
			for (Mob m : playerMobs){
				if (m.isEmpty()){ // check problem
					p.sendMessage("§cMob instance is empty. Please notice an administrator !");
					BeautyQuests.logger.warning("Mob instance for stage " + getID() + " of quest " + manager.getQuest().getID() + " is empty.");
					playerMobs.remove(m);
					finalTest(p, playerMobs, datas);
					return;
				}
				i = i + m.amount;
			}
			updateAmount(datas, i);
			//Utils.sendMessage(p, Lang.STAGE_MOBSREM.toString(), i);
		}
	}
	
	public List<Mob> getMobs(){
		return mobs;
	}

	public boolean isShoot(){
		return shoot;
	}

	public void setShoot(boolean shoot){
		this.shoot = shoot;
	}

	public String descriptionLine(PlayerAccount acc){
		List<Mob> list = remaining.get(acc).remaining;
		String[] str = new String[list.size()];
		for (int i = 0; i < list.size(); i++){
			Mob m = list.get(i);
			str[i] = QuestsConfiguration.getItemNameColor() + Utils.getStringFromNameAndAmount(m.getName(), QuestsConfiguration.getItemAmountColor(), m.amount);
		}
		return Utils.format(Lang.SCOREBOARD_MOBS.toString(), (str.length != 0) ? Utils.itemsToFormattedString(str) : Lang.Unknown.toString());
	}
	
	protected String descriptionMenu(PlayerAccount acc){
		List<Mob> list = remaining.get(acc).remaining;
		String str = "";
		for (int i = 0; i < list.size(); i++){
			Mob m = list.get(i);
			str = str + "\\n" + QuestsConfiguration.getItemNameColor() + Utils.getStringFromNameAndAmount(m.getName(), QuestsConfiguration.getItemAmountColor(), m.amount);
		}
		return Utils.format(Lang.SCOREBOARD_MOBS.toString(), list.isEmpty() ? Lang.Unknown.toString() : "") + str;
	}
	
	private boolean remainingAdd(PlayerAccount acc){
		Player p = acc.getPlayer();
		if (mobs.isEmpty()){
			Utils.sendMessage(p, Lang.STAGE_NOMOBS.toString());
			finishStage(p);
			return false;
		}
		List<Mob> tmp = new ArrayList<>();
		for (Mob m : mobs){
			tmp.add(m.clone());
		}
		PlayerDatas datas = new PlayerDatas(acc, tmp, null);
		remaining.put(acc, datas);
		createBar(datas);
		updateAmount(datas, cachedSize);
		return true;
	}
	
	private Object createBar(PlayerDatas datas) {
		if (QuestsConfiguration.showMobsProgressBar() && cachedSize != 1) {
			Object bar = Post1_9.createMobsBar(this.manager.getQuest().getName(), cachedSize);
			datas.bar = bar;
			if (datas.acc.isCurrent()) Post1_9.showBar(bar, datas.acc.getPlayer());
			return bar;
		}
		return null;
	}
	
	private void updateAmount(PlayerDatas datas, int newAmount) {
		if (QuestsConfiguration.showMobsProgressBar()){
			Post1_9.setBarProgress(this.manager.getQuest().getName(), datas.bar, newAmount, cachedSize);
			if (datas.acc.isCurrent()) Post1_9.showBar(datas.bar, datas.acc.getPlayer());
			timerBar(datas);
		}
	}
	
	private void timerBar(PlayerDatas datas){
		if (QuestsConfiguration.getProgressBarTimeout() > 0)
		if (datas.task != null) datas.task.cancel();
		datas.task = new BukkitRunnable() {
			public void run(){
				if (datas.acc.isCurrent()) Post1_9.hideBar(datas.bar, datas.acc.getPlayer());
				datas.task = null;
			}
		}.runTaskLater(BeautyQuests.getInstance(), QuestsConfiguration.getProgressBarTimeout() * 20L);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (!QuestsConfiguration.showMobsProgressBar()) return;
		PlayerAccount acc = PlayersManager.getPlayerAccount(e.getPlayer());
		if (!remaining.containsKey(acc)) return;
		PlayerDatas datas = remaining.get(acc);
		Post1_9.showBar(datas.bar, e.getPlayer());
		timerBar(datas);
	}
	
	public void start(PlayerAccount account){
		if (!remaining.containsKey(account)){
			if (account.abstractAcc.isCurrent()) remainingAdd(account);
		}
	}
	
	public void launch(Player p){
		if (remainingAdd(PlayersManager.getPlayerAccount(p))){
			super.launch(p);
			String[] str = new String[mobs.size()];
			for (int i = 0; i < mobs.size(); i++){
				Mob m = mobs.get(i);
				str[i] = Utils.getStringFromNameAndAmount(m.getName(), ChatColor.YELLOW.toString(), m.amount) + "§a";
			}
			if (sendStartMessage()) {
				Utils.sendMessage(p, Lang.STAGE_MOBSLIST.toString(), Utils.itemsToFormattedString(str));
			}
		}
	}
	
	public void unload(){
		super.unload();
		if (QuestsConfiguration.showMobsProgressBar()) {
			for (PlayerDatas value : remaining.values()) {
				Post1_9.removeBar(value.bar);
			}
		}
	}
	
	public void end(PlayerAccount account){
		if (QuestsConfiguration.showMobsProgressBar()){
			PlayerDatas datas = remaining.remove(account);
			Post1_9.removeBar(datas.bar);
			datas.bar = null;
			if (datas.task != null) datas.task.cancel();
		}
	}

	protected Map<String, Object> serialize(Map<String, Object> map){
		map.put("mobs", fromMobs(mobs));
		
		Map<String, List<Map<String, Object>>> re = new HashMap<>();
		for (Entry<PlayerAccount, PlayerDatas> m : remaining.entrySet()){
			re.put(m.getKey().getIndex(), fromMobs(m.getValue().remaining));
		}
		map.put("remaining", re);
		if (shoot) map.put("shoot", true);
		
		return map;
	}
	
	public static AbstractStage deserialize(Map<String, Object> map, StageManager manager){
		StageMobs st = new StageMobs(manager, fromMapList((List<Map<String, Object>>) map.get("mobs")));
		
		Map<String, List<Map<String, Object>>> re = (Map<String, List<Map<String, Object>>>) map.get("remaining");
		if (re != null){
			for (Entry<String, List<Map<String, Object>>> en : re.entrySet()){
				PlayerAccount acc = PlayersManager.getByIndex(en.getKey());
				if (acc == null) continue;
				List<Mob> list = fromMapList(en.getValue());
				if (list.isEmpty()){
					DebugUtils.debugMessage(null, "Player " + en.getKey() + " unused for StageMobs");
				}else{
					PlayerDatas datas = new PlayerDatas(acc, list, null);
					st.remaining.put(acc, datas);
					st.createBar(datas);
					st.updateAmount(datas, mobsSize(list));
				}
			}
		}
		if (map.containsKey("shoot")) st.shoot = (boolean) map.get("shoot");
		
		return st;
	}
	
	private static int mobsSize(List<Mob> mobs) {
		int size = 0;
		for(Mob mob : mobs) {
			size += mob.amount;
		}
		return size;
	}
	
	private static List<Map<String, Object>> fromMobs(List<Mob> mobs){
		List<Map<String, Object>> smobs = new ArrayList<>();
		for (Mob m : mobs){
			smobs.add(m.serialize());
		}
		return smobs;
	}
	
	private static List<Mob> fromMapList(List<Map<String, Object>> ls){
		List<Mob> t = new ArrayList<>();
		for (Map<String, Object> m : ls){
			Mob mob = Mob.deserialize(m);
			if (mob != null) t.add(mob);
		}
		return t;
	}
	
	static class PlayerDatas{
		List<Mob> remaining = new ArrayList<>();
		Object bar = null;
		
		PlayerAccount acc;
		BukkitTask task;
		
		PlayerDatas(PlayerAccount acc, List<Mob> remaining, Object bar){
			this.acc = acc;
			this.remaining = remaining;
			this.bar = bar;
		}
	}

}
