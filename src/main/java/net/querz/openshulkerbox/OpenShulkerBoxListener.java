package net.querz.openshulkerbox;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import java.util.*;

public class OpenShulkerBoxListener implements Listener {
	private static final String PERMISSION_OPEN = "openshulkerbox.open";
	private static final String DEFAULT_SHULKER_BOX_NAME = "Shulker Box";

	//Player UUID - RAW SLOT
	private Map<UUID, Integer> openShulkerInventories = new HashMap<>();
	private Set<UUID> openShulkerBoxOnCursor = new HashSet<>();

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack itemInMainHand;
		if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
				&& (itemInMainHand = event.getPlayer().getInventory().getItemInMainHand()) != null
				&& isShulkerBox(itemInMainHand.getType())
				&& event.getPlayer().hasPermission(PERMISSION_OPEN)
				&& event.getPlayer().isSneaking() == OpenShulkerBoxPlugin.getInstance().openWhileSneaking()) {
			ShulkerBox shulkerBox = (ShulkerBox) ((BlockStateMeta) itemInMainHand.getItemMeta()).getBlockState();
			Inventory inv = Bukkit.createInventory(
					null,
					27,
					itemInMainHand.getItemMeta().getDisplayName() == null ? DEFAULT_SHULKER_BOX_NAME : itemInMainHand.getItemMeta().getDisplayName()
			);
			inv.setContents(shulkerBox.getInventory().getContents());
			event.getPlayer().openInventory(inv);
			openShulkerInventories.put(event.getPlayer().getUniqueId(), toRawSlot(event.getPlayer().getInventory().getHeldItemSlot()));
			event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, .1F, 1.0F);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (openShulkerInventories.containsKey(event.getPlayer().getUniqueId())) {
			ItemStack[] items = event.getInventory().getContents();
			saveShulkerBox(event.getPlayer(), items);
			openShulkerInventories.remove(event.getPlayer().getUniqueId());
			event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, .1F, 1.0F);
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (openShulkerInventories.containsKey(event.getWhoClicked().getUniqueId())) {

			//prevent shulker box from dropping into shulker box
			if (event.getCursor() != null
					&& isShulkerBox(event.getCursor().getType())
					&& isInShulkerBox(event.getRawSlot())) {
				event.setCancelled(true);
				return;
			}

			ItemStack[] items = event.getInventory().getContents();
			saveShulkerBox(event.getWhoClicked(), items);

			//close inventory if opened shulker box is dropped
			if (openShulkerInventories.get(event.getWhoClicked().getUniqueId()).equals(event.getRawSlot())) {
				if (isPickupAction(event.getAction())) {
					openShulkerBoxOnCursor.add(event.getWhoClicked().getUniqueId());
					return;
				} else if (event.getAction() == InventoryAction.DROP_ALL_SLOT
						|| event.getAction() == InventoryAction.DROP_ONE_SLOT) {

					//simulate item drop, this is buggy
					dropItem(event.getCurrentItem(), event.getWhoClicked());
					event.setCurrentItem(null);
					event.getWhoClicked().closeInventory();
					return;
				}
			}

			Integer newItemSlot = null;

			//close inventory if opened shulker box is dropped from cursor
			if (openShulkerBoxOnCursor.contains(event.getWhoClicked().getUniqueId())) {
				if (event.getAction() == InventoryAction.DROP_ALL_CURSOR
						|| event.getAction() == InventoryAction.DROP_ONE_CURSOR) {
					event.getWhoClicked().closeInventory();
					return;
				} else if (isPlaceAction(event.getAction())) {
					newItemSlot = toSlot(event.getRawSlot());
					openShulkerBoxOnCursor.remove(event.getWhoClicked().getUniqueId());
				}
			}

			//use number keys to swap shulker box with hotbar
			if (event.getClick() == ClickType.NUMBER_KEY
					&& (event.getAction() == InventoryAction.HOTBAR_SWAP
					|| event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD)) {

				//cancel if shulker box is moved to slot inside shulker box
				if (isInShulkerBox(event.getRawSlot())
						&& event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) != null
						&& isShulkerBox(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()).getType())) {
					event.setCancelled(true);
					return;
				}

				//mouse on shulker box
				if (openShulkerInventories.get(event.getWhoClicked().getUniqueId()).equals(event.getRawSlot())) {
					newItemSlot = event.getHotbarButton();
				} else if (openShulkerInventories.get(event.getWhoClicked().getUniqueId()).equals(toRawSlot(event.getHotbarButton()))) {
					newItemSlot = toSlot(event.getRawSlot());
				}
			}

			//shift+click shulker box from hotbar to inventory
			if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
					&& event.getCurrentItem() != null
					&& isShulkerBox(event.getCurrentItem().getType())) {

				//check if shulker box will be moved from hotbar to inventory
				if (event.getRawSlot() > 53 && event.getRawSlot() < 63) {

					//move shulker box to next free inventory slot
					newItemSlot = moveItemToSlotRange(9, 36, event);

				//check if shulker box will be moved from inventory to hotbar
				} else if (event.getRawSlot() > 26 && event.getRawSlot() < 54) {
					newItemSlot = moveItemToSlotRange(0, 9, event);
				}

				if (newItemSlot != null
						&& !openShulkerInventories.get(event.getWhoClicked().getUniqueId()).equals(event.getRawSlot())) {
					newItemSlot = null;
				}

				event.setCancelled(true);
			}

			if (newItemSlot != null) {
				openShulkerInventories.put(event.getWhoClicked().getUniqueId(), toRawSlot(newItemSlot));
			}
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		if (openShulkerInventories.containsKey(event.getWhoClicked().getUniqueId())
				&& isShulkerBox(event.getOldCursor().getType())) {
			if (event.getRawSlots().stream().anyMatch(a -> a < 27)
					|| event.getRawSlots().size() > 1) {
				event.setCancelled(true);
				return;
			}

			if (openShulkerBoxOnCursor.contains(event.getWhoClicked().getUniqueId())) {
				openShulkerInventories.put(event.getWhoClicked().getUniqueId(), toRawSlot((int) event.getInventorySlots().toArray()[0]));
				openShulkerBoxOnCursor.remove(event.getWhoClicked().getUniqueId());
			}
		}
	}

	private void saveShulkerBox(HumanEntity player, ItemStack[] items) {
		ItemStack shulkerbox = player.getInventory().getItem(toSlot(openShulkerInventories.get(player.getUniqueId())));
		if (shulkerbox == null || !isShulkerBox(shulkerbox.getType())) {
			return;
		}
		BlockStateMeta bsm  = (BlockStateMeta) shulkerbox.getItemMeta();
		ShulkerBox box = (ShulkerBox) bsm.getBlockState();
		box.getInventory().setContents(items);
		bsm.setBlockState(box);
		shulkerbox.setItemMeta(bsm);
	}

	private boolean isShulkerBox(Material m) {
		switch (m) {
			case SILVER_SHULKER_BOX:
			case BLACK_SHULKER_BOX:
			case BLUE_SHULKER_BOX:
			case BROWN_SHULKER_BOX:
			case CYAN_SHULKER_BOX:
			case GRAY_SHULKER_BOX:
			case GREEN_SHULKER_BOX:
			case LIGHT_BLUE_SHULKER_BOX:
			case LIME_SHULKER_BOX:
			case MAGENTA_SHULKER_BOX:
			case ORANGE_SHULKER_BOX:
			case PINK_SHULKER_BOX:
			case PURPLE_SHULKER_BOX:
			case RED_SHULKER_BOX:
			case WHITE_SHULKER_BOX:
			case YELLOW_SHULKER_BOX:
				return true;
			default:
				return false;
		}
	}

	private boolean isPlaceAction(InventoryAction action) {
		return action == InventoryAction.PLACE_ALL
				|| action == InventoryAction.PLACE_ONE
				|| action == InventoryAction.PLACE_SOME
				|| action == InventoryAction.SWAP_WITH_CURSOR;
	}

	private boolean isPickupAction(InventoryAction action) {
		return action == InventoryAction.PICKUP_ALL
				|| action == InventoryAction.PICKUP_HALF
				|| action == InventoryAction.PICKUP_ONE
				|| action == InventoryAction.PICKUP_SOME
				|| action == InventoryAction.SWAP_WITH_CURSOR;
	}

	private boolean isInShulkerBox(int rawSlot) {
		return rawSlot >= 0 && rawSlot < 27;
	}

	private int toRawSlot(int slot) {
		return slot >= 0 && slot < 9 ? slot + 54 : slot + 18;
	}

	private int toSlot(int rawSlot) {
		return rawSlot >= 54 ? rawSlot - 54 : rawSlot - 18;
	}

	private Integer moveItemToSlotRange(int rangeMin, int rangeMax, InventoryClickEvent event) {
		for (int i = rangeMin; i < rangeMax; i++) {
			if (event.getClickedInventory().getItem(i) == null
					|| event.getClickedInventory().getItem(i).getType() == Material.AIR) {
				event.getClickedInventory().setItem(i, event.getCurrentItem());
				event.setCurrentItem(null);
				return i;
			}
		}
		return null;
	}

	private void dropItem(ItemStack itemStack, HumanEntity player) {
		Item item = player.getWorld().dropItem(player.getEyeLocation(), itemStack);
		item.setVelocity(player.getLocation().getDirection().multiply(0.33));
		item.setPickupDelay(40);
	}
}
