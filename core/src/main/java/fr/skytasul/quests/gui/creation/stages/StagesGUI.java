package fr.skytasul.quests.gui.creation.stages;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.api.stages.StageCreation;
import fr.skytasul.quests.api.stages.StageType;
import fr.skytasul.quests.gui.CustomInventory;
import fr.skytasul.quests.gui.Inventories;
import fr.skytasul.quests.gui.ItemUtils;
import fr.skytasul.quests.gui.creation.FinishGUI;
import fr.skytasul.quests.stages.*;
import fr.skytasul.quests.structure.Quest;
import fr.skytasul.quests.structure.QuestBranch;
import fr.skytasul.quests.utils.DebugUtils;
import fr.skytasul.quests.utils.Lang;
import fr.skytasul.quests.utils.Utils;
import fr.skytasul.quests.utils.XMaterial;

public class StagesGUI implements CustomInventory {

	private static final int SLOT_FINISH = 52;
	
	private static final ItemStack stageCreate = ItemUtils.item(XMaterial.SLIME_BALL, Lang.stageCreate.toString());
	private static final ItemStack stageRemove = ItemUtils.item(XMaterial.BARRIER, Lang.stageRemove.toString());
	private static final ItemStack notDone = ItemUtils.lore(ItemUtils.itemNotDone.clone(), Lang.cantFinish.toString());
	
	public static final ItemStack ending = ItemUtils.item(XMaterial.BAKED_POTATO, Lang.ending.toString());
	public static final ItemStack descMessage = ItemUtils.item(XMaterial.OAK_SIGN, Lang.descMessage.toString());
	public static final ItemStack startMessage = ItemUtils.item(XMaterial.FEATHER, Lang.startMsg.toString());
	public static final ItemStack validationRequirements = ItemUtils.item(XMaterial.NETHER_STAR, Lang.validationRequirements.toString(), QuestOption.formatDescription(Lang.validationRequirementsLore.toString()));
	
	private List<Line> lines = new ArrayList<>();
	
	private Quest edit;
	private boolean stagesEdited = false;

	private FinishGUI finish = null;
	private StagesGUI previousBranch;

	public Inventory inv;
	int page;
	private boolean stop = false;

	public StagesGUI(StagesGUI previousBranch){
		this.previousBranch = previousBranch;
	}
	
	@Override
	public Inventory open(Player p) {
		if (inv == null){
			inv = Bukkit.createInventory(null, 54, Lang.INVENTORY_STAGES.toString());

			page = 0;
			for (int i = 0; i < 20; i++) lines.add(new Line(i, this));
			setStageCreate(lines.get(0), false);
			setStageCreate(lines.get(15), true);

			inv.setItem(45, ItemUtils.itemLaterPage);
			inv.setItem(50, ItemUtils.itemNextPage);

			inv.setItem(SLOT_FINISH, isEmpty() ? notDone : ItemUtils.itemDone);
			inv.setItem(53, previousBranch == null ? ItemUtils.itemCancel : ItemUtils.item(XMaterial.FILLED_MAP, Lang.previousBranch.toString()));
			refresh();
		}

		if (p != null) inv = p.openInventory(inv).getTopInventory();
		return inv;
	}

	/**
	 * Get the StagesGUI, open it for player if specified, and re implement the player in the inventories system if on true
	 * @param p player to open (can be null)
	 * @param reImplement re implement the player in the inventories system
	 * @return this StagesGUI
	 */
	public StagesGUI reopen(Player p, boolean reImplement){
		if (p != null){
			if (reImplement) Inventories.put(p, this, inv);
			p.openInventory(inv);
		}
		return this;
	}

	private void setStageCreate(Line line, boolean branches){
		line.removeItems();
		line.setItem(0, stageCreate.clone(), (p, item) -> {
			line.setItem(0, null, null, true, false);
			int i = 0;
			for (StageType<?> creator : QuestsAPI.stages) {
				if (creator.isValid()) {
					line.setItem(++i, creator.item, (p1, item1) -> {
						runClick(line, creator, branches).start(p1);
					}, true, false);
				}
			}
			line.setItems(0);
		});
		line.setItems(0);
	}

	private StageCreation<?> runClick(Line line, StageType<?> creator, boolean branches) {
		line.removeItems();
		StageCreation<?> creation = creator.creationSupplier.supply(line, branches);
		line.creation = creation;
		
		inv.setItem(SLOT_FINISH, ItemUtils.itemDone);

		int maxStages = branches ? 20 : 15;
		line.setItem(0, ItemUtils.lore(stageRemove.clone(), QuestOption.formatDescription(creator.name)), (p, item) -> {
			line.creation = null;
			line.removeItems();
			if (line.getLine() != maxStages - 1) {
				for (int i1 = line.getLine() + 1; i1 < maxStages; i1++) {
					getLine(i1).exchangeLines(getLine(i1 - 1));
				}
			}
			for (int i2 = 0; i2 < maxStages; i2++) {
				Line l = getLine(i2);
				if (!isActiveLine(l)) {
					setStageCreate(l, i2 > maxStages - 1);
					break;
				}
			}
			if (isEmpty()) inv.setItem(SLOT_FINISH, notDone);
		});

		if (line.getLine() != maxStages-1){
			Line next = getLine(line.getLine() + 1);
			if (!isActiveLine(next)) setStageCreate(next, branches);
		}
		
		if (branches){
			if (creation.getLeadingBranch() == null) creation.setLeadingBranch(new StagesGUI(this));
			line.setItem(14, ItemUtils.item(XMaterial.FILLED_MAP, Lang.newBranch.toString()), (p, item) -> Inventories.create(p, creation.getLeadingBranch()));
		}
		
		return creation;
	}

	private boolean isActiveLine(Line line) {
		return line.creation != null;
	}

	public Line getLine(int id){
		for (Line l : lines){
			if (l.getLine() == id) return l;
		}
		return null;
	}
	
	public boolean isEmpty(){
		if (lines.isEmpty()) return true; // if this StagesGUI has never been opened
		return !isActiveLine(getLine(0)) && !isActiveLine(getLine(15));
	}
	
	public void deleteStageLine(Line line) {
		if (isActiveLine(line)) line.execute(0, null, null); // item and player not used for deletion item
	}

	@Override
	public boolean onClick(Player p, Inventory inv, ItemStack current, int slot, ClickType click) {
		if (slot > 44) {
			if (slot == 45) {
				if (page > 0) {
					page--;
					refresh();
				}
			}else if (slot > 45 && slot < 50){
				page = slot - 46;
				refresh();
			}else if (slot == 50) {
				if (page < 3) {
					page++;
					refresh();
				}
			}else if (slot == 52) {
				if (previousBranch == null){ // main inventory = directly finish if not empty
					if (isEmpty()) {
						Utils.playPluginSound(p.getLocation(), "ENTITY_VILLAGER_NO", 0.6f);
					}else {
						finish(p);
					}
				}else { // branch inventory = get the main inventory to finish
					StagesGUI branch = previousBranch;
					while (branch.previousBranch != null) branch = branch.previousBranch; // get the very first branch
					branch.finish(p);
				}
			}else if (slot == 53) {
				if (previousBranch == null){ // main inventory = cancel button
					stop = true;
					p.closeInventory();
					if (!isEmpty()) {
						if (edit == null) {
							Lang.QUEST_CANCEL.send(p);
						}else Lang.QUEST_EDIT_CANCEL.send(p);
					}
				}else { // branch inventory = previous branch button
					Inventories.create(p, previousBranch);
				}
			}
		}else {
			StagesGUI branch = this;
			while (branch.previousBranch != null) branch = branch.previousBranch; // get the very first branch
			branch.stagesEdited = true;
			Line line = getLine((slot - slot % 9)/9 +5*page);
			line.click(slot - (line.getLine() - page * 5) * 9, p, current);
		}
		return true;
	}

	@Override
	public CloseBehavior onClose(Player p, Inventory inv){
		if (isEmpty() || stop) return CloseBehavior.REMOVE;
		return CloseBehavior.REOPEN;
	}

	private void refresh() {
		for (int i = 0; i < 3; i++) inv.setItem(i + 46, ItemUtils.item(i == page ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.WHITE_STAINED_GLASS_PANE, Lang.regularPage.toString()));
		inv.setItem(49, ItemUtils.item(page == 3 ? XMaterial.MAGENTA_STAINED_GLASS_PANE : XMaterial.PURPLE_STAINED_GLASS_PANE, Lang.branchesPage.toString()));
		
		for (Line line : lines) {
			line.setItems(line.getActivePage());
		}
	}

	private void finish(Player p){
		if (finish == null){
			finish = Inventories.create(p, edit != null ? new FinishGUI(this, edit, stagesEdited) : new FinishGUI(this));
		}else {
			Inventories.create(p, finish);
			if (edit != null && stagesEdited) finish.setStagesEdited(false);
		}
	}

	public List<StageCreation> getStageCreations() {
		List<StageCreation> stages = new LinkedList<>();
		for (int i = 0; i < 20; i++) {
			Line line = getLine(i);
			if (isActiveLine(line)) stages.add(line.creation);
		}
		return stages;
	}

	public void edit(Quest quest){
		edit = quest;
		editBranch(quest.getBranchesManager().getBranch(0));
		inv.setItem(SLOT_FINISH, ItemUtils.itemDone);
	}
	
	private void editBranch(QuestBranch branch){
		for (AbstractStage stage : branch.getRegularStages()){
			Line line = getLine(stage.getID());
			@SuppressWarnings ("rawtypes")
			StageCreation creation = runClick(line, stage.getType(), false);
			creation.edit(stage);
			line.setItems(0);
		}
		
		int i = 15;
		for (Entry<AbstractStage, QuestBranch> en : branch.getEndingStages().entrySet()){
			Line line = getLine(i);
			@SuppressWarnings ("rawtypes")
			StageCreation creation = runClick(line, en.getKey().getType(), true);
			StagesGUI gui = new StagesGUI(this);
			gui.open(null); // init other GUI
			creation.setLeadingBranch(gui);
			if (en.getValue() != null) gui.editBranch(en.getValue());
			creation.edit(en.getKey());
			line.setItems(0);
			i++;
		}
	}



	private static final ItemStack stageNPC = ItemUtils.item(XMaterial.OAK_SIGN, Lang.stageNPC.toString());
	private static final ItemStack stageItems = ItemUtils.item(XMaterial.CHEST, Lang.stageBring.toString());
	private static final ItemStack stageArea = ItemUtils.item(XMaterial.WOODEN_AXE, Lang.stageGoTo.toString());
	private static final ItemStack stageMobs = ItemUtils.item(XMaterial.WOODEN_SWORD, Lang.stageMobs.toString());
	private static final ItemStack stageMine = ItemUtils.item(XMaterial.WOODEN_PICKAXE, Lang.stageMine.toString());
	private static final ItemStack stagePlace = ItemUtils.item(XMaterial.OAK_STAIRS, Lang.stagePlace.toString());
	private static final ItemStack stageChat = ItemUtils.item(XMaterial.PLAYER_HEAD, Lang.stageChat.toString());
	private static final ItemStack stageInteract = ItemUtils.item(XMaterial.STICK, Lang.stageInteract.toString());
	private static final ItemStack stageFish = ItemUtils.item(XMaterial.COD, Lang.stageFish.toString());
	private static final ItemStack stageCraft = ItemUtils.item(XMaterial.CRAFTING_TABLE, Lang.stageCraft.toString());
	private static final ItemStack stageBucket = ItemUtils.item(XMaterial.BUCKET, Lang.stageBucket.toString());
	private static final ItemStack stageLocation = ItemUtils.item(XMaterial.MINECART, Lang.stageLocation.toString());
	private static final ItemStack stagePlayTime = ItemUtils.item(XMaterial.CLOCK, Lang.stagePlayTime.toString());
	private static final ItemStack stageBreed = ItemUtils.item(XMaterial.WHEAT, Lang.stageBreedAnimals.toString());
	private static final ItemStack stageTame = ItemUtils.item(XMaterial.CARROT, Lang.stageTameAnimals.toString());

	public static void initialize(){
		DebugUtils.logMessage("Initlializing default stage types.");

		QuestsAPI.registerStage(new StageType<StageArea>("REGION", StageArea.class, Lang.Find.name(), StageArea::deserialize, stageArea, StageArea.Creator::new, "WorldGuard"));
		QuestsAPI.registerStage(new StageType<>("NPC", StageNPC.class, Lang.Talk.name(), StageNPC::deserialize, stageNPC, StageNPC.Creator::new));
		QuestsAPI.registerStage(new StageType<>("ITEMS", StageBringBack.class, Lang.Items.name(), StageBringBack::deserialize, stageItems, StageBringBack.Creator::new));
		QuestsAPI.registerStage(new StageType<>("MOBS", StageMobs.class, Lang.Mobs.name(), StageMobs::deserialize, stageMobs, StageMobs.Creator::new));
		QuestsAPI.registerStage(new StageType<>("MINE", StageMine.class, Lang.Mine.name(), StageMine::deserialize, stageMine, StageMine.Creator::new));
		QuestsAPI.registerStage(new StageType<>("PLACE_BLOCKS", StagePlaceBlocks.class, Lang.Place.name(), StagePlaceBlocks::deserialize, stagePlace, StagePlaceBlocks.Creator::new));
		QuestsAPI.registerStage(new StageType<>("CHAT", StageChat.class, Lang.Chat.name(), StageChat::deserialize, stageChat, StageChat.Creator::new));
		QuestsAPI.registerStage(new StageType<>("INTERACT", StageInteract.class, Lang.Interact.name(), StageInteract::deserialize, stageInteract, StageInteract.Creator::new));
		QuestsAPI.registerStage(new StageType<>("FISH", StageFish.class, Lang.Fish.name(), StageFish::deserialize, stageFish, StageFish.Creator::new));
		QuestsAPI.registerStage(new StageType<>("CRAFT", StageCraft.class, Lang.Craft.name(), StageCraft::deserialize, stageCraft, StageCraft.Creator::new));
		QuestsAPI.registerStage(new StageType<>("BUCKET", StageBucket.class, Lang.Bucket.name(), StageBucket::deserialize, stageBucket, StageBucket.Creator::new));
		QuestsAPI.registerStage(new StageType<>("LOCATION", StageLocation.class, Lang.Location.name(), StageLocation::deserialize, stageLocation, StageLocation.Creator::new));
		QuestsAPI.registerStage(new StageType<>("PLAY_TIME", StagePlayTime.class, Lang.PlayTime.name(), StagePlayTime::deserialize, stagePlayTime, StagePlayTime.Creator::new));
		QuestsAPI.registerStage(new StageType<>("BREED", StageBreed.class, Lang.Breed.name(), StageBreed::deserialize, stageBreed, StageBreed.Creator::new));
		QuestsAPI.registerStage(new StageType<>("TAME", StageTame.class, Lang.Tame.name(), StageTame::deserialize, stageTame, StageTame.Creator::new));
	}
}