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