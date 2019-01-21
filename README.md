# OpenShulkerBox
[![Build Status](https://travis-ci.org/Querz/OpenShulkerBox.svg?branch=master)](https://travis-ci.org/Querz/OpenShulkerBox)
#### A Spigot plugin to open Shulker Boxes from the Inventory
---

It supports all corner cases:
* The UI doesn't allow to put a shulker box into another shulker box
* The shulker box can be moved between slots inside of your inventory while it is open
* Dropping the shulker box (moving it outside the inventory or pressing Q) while it's open will save its contents
* Swapping items with the hotbar (pressing 1-9 while hovering over a slot)
* Shift-click
* Shift-double-click
* Dragging items and item stacks through your inventory and the opened shulker box

The shulker box can be opened when right-clicking while holding it in the main hand, and placed when right-clicking while sneaking.
This can be reversed by setting `open-while-sneaking` in the config file to `true`.

To be able to open a shulker box, a player requires the permission `openshulkerbox.open`.
