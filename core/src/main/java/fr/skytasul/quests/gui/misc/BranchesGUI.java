package fr.skytasul.quests.gui.misc;

import java.util.LinkedList;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.skytasul.quests.gui.CustomInventory;
import fr.skytasul.quests.gui.ItemUtils;
import fr.skytasul.quests.utils.Lang;
import fr.skytasul.quests.utils.XMaterial;

public class BranchesGUI implements CustomInventory { // WIP
	
	private Branch main = new Branch(null);
	
	private Inventory inv;
	
	private Branch shown;
	private int xOffset = 0;
	
	@Override
	public Inventory open(Player p) {
		inv = Bukkit.createInventory(null, 54, "Branches");
		
		inv.setItem(49, ItemUtils.item(XMaterial.DIAMOND_BLOCK, "§bBack one branch"));
		
		inv.setItem(52, ItemUtils.item(XMaterial.ARROW, "§aScroll left"));
		inv.setItem(53, ItemUtils.item(XMaterial.ARROW, "§aScroll right"));
		
		main.things.add(new Thing());
		main.things.add(new Thing());
		main.things.add(new Thing());
		Branch branch = new Branch(main);
		branch.things.add(new Thing());
		branch.things.add(new Thing());
		branch.things.add(new Thing());
		main.choices.add(branch);
		branch = new Branch(main);
		branch.things.add(new Thing());
		branch.things.add(new Thing());
		main.choices.add(branch);
		
		setBranch(main);
		
		return p.openInventory(inv).getTopInventory();
	}
	
	public void refresh() {
		setBranch(shown);
	}
	
	private void setBranch(Branch branch) {
		this.shown = branch;
		for (int i = 0; i < 45; i++) {
			inv.clear(i);
		}
		
		int y = 2;
		int start = y * 9;
		displayBranch(shown, start, xOffset, true);
	}
	
	private void displayBranch(Branch branch, int start, int xOffset, boolean showBranches) {
		int to;
		if (branch.things.size() >= xOffset + 9) {
			to = xOffset + 9;
			showBranches = false; // no space to continue
		}else {
			to = branch.things.size();
		}
		for (int i = xOffset; i < to; i++) {
			IThing thing = branch.things.get(i);
			inv.setItem(start + i - xOffset, thing.getItem(i == to - 1 ? branch.choices.isEmpty() ? ThingType.END : ThingType.BRANCHING : ThingType.NORMAL));
		}
		if (!showBranches) return;
		if (branch.choices.isEmpty()) { // no branch at the end
			inv.setItem(start + to - xOffset, ItemUtils.item(XMaterial.SLIME_BALL, "§eCreate next thing", "§8> LEFT CLICK : §7Create normal thing", "§8> RIGHT CLICK : §7Create choices"));
		}else {
			int i = 0;
			for (Branch endBranch : branch.choices) {
				displayBranch(endBranch, i * 9 + to - xOffset, 0, false);
				i++;
			}
			for (; i < 5; i++) {
				inv.setItem(i * 9 + to - xOffset, ItemUtils.item(XMaterial.SLIME_BALL, "§6Create choice"));
			}
		}
	}
	
	public void create(Consumer<IThing> thing) {
		thing.accept(new Thing());
	}
	
	@Override
	public boolean onClick(Player p, Inventory inv, ItemStack current, int slot, ClickType click) {
		if (slot == 52) {
			if (xOffset > 0) {
				xOffset--;
				refresh();
			}
		}else if (slot == 53) {
			if (xOffset < shown.things.size()) {
				xOffset++;
				refresh();
			}
		}else if (slot == 49) {
			if (shown.parent != null) setBranch(shown.parent);
		}else {
			int y = (int) (slot / 9D);
			int x = slot - (y * 9);
			int xThing = xOffset + x;
			if (xThing < shown.things.size()) {
				p.sendMessage("You clicked on thing " + shown.things.get(xThing).getID());
			}else {
				if (shown.choices.isEmpty()) {
					p.sendMessage("You want to create thing at place " + xThing);
					if (click.isLeftClick()) {
						create(thing -> {
							shown.things.add(thing);
							refresh();
						});
					}else if (click.isRightClick()) {
						p.sendMessage("You want to create branch at place " + xThing);
						create(thing -> {
							Branch branch = new Branch(shown);
							branch.things.add(thing);
							shown.choices.add(branch);
							refresh();
						});
					}
				}else if (y < shown.choices.size()) {
					Branch branch = shown.choices.get(y);
					xThing -= shown.things.size();
					p.sendMessage("You clicked on thing " + branch.things.get(xThing).getID());
					if (branch != shown) setBranch(branch);
				}else {
					p.sendMessage("You want to create a branch at place " + y);
					create(thing -> {
						Branch branch = new Branch(shown);
						branch.things.add(thing);
						shown.choices.add(branch);
						refresh();
					});
				}
			}
		}
		return true;
	}
	
	@Override
	public CloseBehavior onClose(Player p, Inventory inv) {
		return CloseBehavior.REMOVE;
	}
	
	static class Branch {
		LinkedList<IThing> things = new LinkedList<>();
		LinkedList<Branch> choices = new LinkedList<>();
		
		Branch parent;
		
		Branch(Branch parent) {
			this.parent = parent;
		}
	}
	
	static enum ThingType {
		NORMAL(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE, "§bBasic step"), BRANCHING(XMaterial.ORANGE_STAINED_GLASS_PANE, "§6Branching step"), END(XMaterial.RED_STAINED_GLASS_PANE, "§cEnding step");
		
		private XMaterial material;
		private String name;
		
		private ThingType(XMaterial material, String name) {
			this.material = material;
			this.name = name;
		}
	}
	
	static interface IThing {
		int getID();
		
		ItemStack getItem(ThingType type);
	}
	
	static class Thing implements IThing {
		private static int counter = 0;
		
		private int id = counter++;
		
		@Override
		public int getID() {
			return id;
		}
		
		@Override
		public ItemStack getItem(ThingType type) {
			return ItemUtils.item(type.material, "§4Thing §b§l" + id, Lang.RemoveMid.toString(), type.name);
		}
	}
	
}
