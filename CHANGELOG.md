# You're in Grave Danger 2.0.0-beta.9

### Changes
* Added `/yigd whitelist list` command to list all whitelisted/blacklisted players
* Improved precision of player position when dropping items that couldn't fit in
player inventory after merge of grave and player inventory, when using commands
* Now adds items in inventory after inventory merge, if they can fit, instead of
dropping them from the grave
* If a grave would happen to be destroyed due to a bug, the game will let that player
know, and give directions to how to restore it (can be disabled through config)
* Added a new config option to drop all contents of a grave when it's destroyed
(false by default, as this includes breaking graves in creative mode)
* Added translation for "the bumblezone" dimension in grave GUI
* Added compatibility with orpheus' "orpheus lyre"

### Fixes
* Columns of items added by mod inventories that are shown in the grave overview GUI,
will now be rendered on the same row as the other columns
* Fixed curse of binding now being stuck when claiming a grave, (unless the cursed
item is in the grave)
* Curse of binding trinkets will now be removed from its slot in a grave when dying,
as well as stay in the player inventory when claiming a grave
* Curse of binding soulbounded will no longer be stuck in the same slot upon respawn
* Theoretically fixed duplication when standard drop rule was set to DROP (untested)

---

# You're in Grave Danger 2.0.0-beta.8

### Changes
* Added overlay messages when failing to claim/rob graves due to a few reasons
* Added new config options for XP drops. These are BEST_OF_BOTH and WORST_OF_BOTH,
dropping the highest of VANILLA and PERCENTAGE, respectively the lowest of the
two options.
* Modified layout in grave overview GUI, and added more buttons
* New GUI button (if grave keys are enabled, and button is allowed) to create a key 
to the grave
* New GUI button (if player has a recovery compass in inventory) to create a grave
compass to the grave
* Grave compasses are now called "Grave Compass"
* Graves can now point to the closest grave, either owned by the player, or all
graves. This is configurable in the config file

### Fixes
* Fixed compatibility issue with "utility belt". Items inside belt should no longer be
deleted on death.
* Travelers backpack will no longer be deleted on death whenever trinket integration is
enabled.

---

# You're in Grave Danger 2.0.0-beta.7

### Changes
* Improved backup system. Now all items are backed up, no matter drop rules.
Through the backup in game GUI you can now also restore items with specific drop rules
* Updated enchantment configs so that you can configure how the enchantments are accessed.
Note that if you had soulbound disabled you'll need to disable it again in the config
* Added grave compass. Configurable in the config file

### Fixes
* Game will no longer crash when dying if Travelers backpack is installed
* Keep inventory will no longer duplicate items

---

# You're in Grave Danger 2.0.0-beta.6

### Changes
* Added back soulbound blacklist tag. Items in this tag can't be enchanted with soulbound

### Fixes
* Players can't retrieve their graves while dead anymore
* Restore command should now delete the grave appropriately
* Restore/rob command should no longer crash the game if being called twice

---

# You're in Grave Danger 2.0.0-beta.5

### Fixes
* Fixed issue where no graves generated while origins was loaded

---

# You're in Grave Danger 2.0.0-beta.4

### Fixes
* Graves generating inside the world when dying outside the world (like falling
out of the end) will now store the grave data
* Origins inventories will now properly handle its own drop rules

---

# You're in Grave Danger 2.0.0-beta.3

### Changes
* Added config to drop contents of deleted graves, when deleted due to the max grave
count reached.
* Config now uses GSON instead of jankson (config file is now yigd.json instead of yigd.json5)

### Fixes
* Graves will no longer unlock when closing and reopening the game
* Added translation keys for enchantments
* Fixed error where game could not read config file, so you could not change the configs

---

# You're in Grave Danger 2.0.0-beta.2
Small update to fix an issue with iris

### Fixes
* Using iris, graves used to render completely white when glowing grave render config
was enabled. It should now appear normally with just a white glowing outline

---

# You're in Grave Danger 2.0.0-beta.1
**NOTE: This version is a rewrite of previous versions of the mod for minecraft 1.20
and above. It will not be backwards compatible with previous versions of the mod.**

Also note this is a beta release, and may contain bugs. If you encounter such
things, please report them on the mod's github.

Every feature has been reimplemented from scratch. If there's something from
previous versions that didn't make the update, also let me know over on github.
I (the mod dev) have tried to remember everything, but might've still missed some stuff

### Notable features
* Changed the config file. Both in structure, but also format. Should hopefully
be easier to traverse
* Although it's been in the mod previously, graves are optional. However, in
this version, the mod has been developed with this more in mind than previously.
This means that it is easier to use YiGD as a death handler mod, without graves,
than it has previously been.
* Added new respawn configurations. You can now respawn with custom potion effects,
you can configure respawn health, hunger, and more.
* Compatibility is now more configurable, where you can configure drop rules
from different mod inventories, and also toggle the compatibility implementations
from loading.
* Inventory NBT is no longer stored in the block entity itself. If you have previously
had trouble with this overloading chunks with NBT data, it should now be fixed.
All data is now stored in the backup file, which means if it's lost, the grave contents
will be too.