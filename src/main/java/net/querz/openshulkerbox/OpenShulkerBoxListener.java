package net.querz.openshulkerbox;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class OpenShulkerBoxListener implements Listener {
	private static final String PERMISSION_OPEN = "openshulkerbox.open";
	private static final String DEFAULT_SHULKER_BOX_NAME = "Shulker Box";

	//Player UUID - RAW SLOT
	private Map<UUID, Integer> shulkerBoxSlots = new HashMap<>();
	private Set<UUID> shulkerBoxOnCursors = new HashSet<>();

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack itemInMainHand;
		Player player;
		if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
				&& (itemInMainHand = (player = event.getPlayer()).getInventory().getItemInMainHand()) != null
				&& isShulkerBox(itemInMainHand.getType())
				&& player.hasPermission(PERMISSION_OPEN)
				&& player.isSneaking() == OpenShulkerBoxPlugin.getInstance().openWhileSneaking()) {
			ShulkerBox shulkerBox = (ShulkerBox) ((BlockStateMeta) itemInMainHand.getItemMeta()).getBlockState();
			ItemMeta meta = itemInMainHand.getItemMeta();
			String title = meta.getDisplayName() == null ? DEFAULT_SHULKER_BOX_NAME : meta.getDisplayName();
			Inventory inv = Bukkit.createInventory(null, 27, title);
			inv.setContents(shulkerBox.getInventory().getContents());
			player.openInventory(inv);
			shulkerBoxSlots.put(player.getUniqueId(), toRawSlot(player.getInventory().getHeldItemSlot()));
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, .1F, 1.0F);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		HumanEntity player;
		if (shulkerBoxSlots.containsKey((player = event.getPlayer()).getUniqueId())) {
			ItemStack[] items = event.getInventory().getContents();
			saveShulkerBox(player, items);
			shulkerBoxSlots.remove(player.getUniqueId());
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, .1F, 1.0F);
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		HumanEntity player;
		if (shulkerBoxSlots.containsKey((player = event.getWhoClicked()).getUniqueId())) {

			//prevent shulker box from dropping into shulker box
			if (event.getCursor() != null
					&& isShulkerBox(event.getCursor().getType())
					&& isInShulkerBox(event.getRawSlot())) {
				event.setCancelled(true);
				return;
			}

			ItemStack[] items = event.getInventory().getContents();
			saveShulkerBox(player, items);

			//close inventory if opened shulker box is dropped
			if (shulkerBoxSlots.get(player.getUniqueId()).equals(event.getRawSlot())) {
				if (isPickupAction(event.getAction())) {
					shulkerBoxOnCursors.add(player.getUniqueId());
					return;
				} else if (event.getAction() == InventoryAction.DROP_ALL_SLOT
						|| event.getAction() == InventoryAction.DROP_ONE_SLOT) {

					//simulate item drop, this is buggy
					dropItem(event.getCurrentItem(), player);
					event.setCurrentItem(null);
					player.closeInventory();
					return;
				}
			}

			Integer newItemSlot = null;

			//close inventory if opened shulker box is dropped from cursor
			if (shulkerBoxOnCursors.contains(player.getUniqueId())) {
				if (event.getAction() == InventoryAction.DROP_ALL_CURSOR
						|| event.getAction() == InventoryAction.DROP_ONE_CURSOR) {
					player.closeInventory();
					return;
				} else if (isPlaceAction(event.getAction())) {
					newItemSlot = event.getRawSlot();
					shulkerBoxOnCursors.remove(player.getUniqueId());
				}
			}

			//use number keys to swap shulker box with hotbar
			if (event.getClick() == ClickType.NUMBER_KEY
					&& (event.getAction() == InventoryAction.HOTBAR_SWAP
					|| event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD)) {

				//cancel if shulker box is moved to slot inside shulker box
				if (isInShulkerBox(event.getRawSlot())
						&& player.getInventory().getItem(event.getHotbarButton()) != null
						&& isShulkerBox(player.getInventory().getItem(event.getHotbarButton()).getType())) {
					event.setCancelled(true);
					return;
				}

				//mouse on shulker box
				if (shulkerBoxSlots.get(player.getUniqueId()).equals(event.getRawSlot())) {
					newItemSlot = toRawSlot(event.getHotbarButton());
				} else if (shulkerBoxSlots.get(player.getUniqueId()).equals(toRawSlot(event.getHotbarButton()))) {
					newItemSlot = event.getRawSlot();
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

				if (newItemSlot != null && !shulkerBoxSlots.get(player.getUniqueId()).equals(event.getRawSlot())) {
					newItemSlot = null;
				}

				event.setCancelled(true);
			}

			if (newItemSlot != null) {
				shulkerBoxSlots.put(player.getUniqueId(), newItemSlot);
			}
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		HumanEntity player;

		if (shulkerBoxSlots.containsKey((player = event.getWhoClicked()).getUniqueId())
				&& isShulkerBox(event.getOldCursor().getType())) {
			if (event.getRawSlots().stream().anyMatch(a -> a < 27)
					|| event.getRawSlots().size() > 1) {
				event.setCancelled(true);
				return;
			}

			if (shulkerBoxOnCursors.contains(player.getUniqueId())) {
				shulkerBoxSlots.put(player.getUniqueId(), toRawSlot((int) event.getInventorySlots().toArray()[0]));
				shulkerBoxOnCursors.remove(player.getUniqueId());
			}
		}
	}

	private void saveShulkerBox(HumanEntity player, ItemStack[] items) {
		ItemStack shulkerbox = player.getInventory().getItem(toSlot(shulkerBoxSlots.get(player.getUniqueId())));
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
				return toRawSlot(i);
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
