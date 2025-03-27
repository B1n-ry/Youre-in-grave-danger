# You're in Grave Danger 2.0.11

### Changes
* Added config option for treating curse of binding items with soulbound on respawn.

---

# You're in Grave Danger 2.0.10

### Changes
* Mods adding external inventories, without specific compat, should now have their items
handled by YiGD (meaning they might get stored in a grave instead of dropping)
* Item-loss can now be weighted to prefer selection items for destruction of larger stacks
* Losing individual items though item loss will now be registered as destroyed by the
backup system
* Item-loss can be configured to apply other drop rules than the DESTROY drop rule
* Added some common soulbound enchantments to standard soulbound enchantment tag

### Fixes
* Fixed random error when applying item loss to modded inventories, which would cause death
handling to be ignored
* Graveyards will no longer try to generate graves where graves have already been generated
* Graveyards can now successfully generate graves cross-dimensionally
* When graves drop items on the ground when destroyed/claimed, it will now only contain
items contained in the grave (no soulbound, already dropped, or destroyed items)

---

# You're in Grave Danger 2.0.9

### Changes
* Config values for glowing graves, glowing grave distance, breakable graves, and
death sight enchantment range are now properly synced to the server

### Fixes
* Item loss is now applied after checking items required for generating a grave

---

# You're in Grave Danger 2.0.8

### Changes
* Item loss can now optionally be applied to modded inventories
* Soulbound can now be enchanted on all curios items
* Added configurable use time and cooldown for death scroll
* Replaced the `onlyMurderer` rob config with `killerSkipWaitTime`
allowing the killer of a player to skip the grave robbing cooldown

### Fixes
* Made graves indestructible to a lot of ways they could be destroyed by previously
* Item loss will now not try and remove the same item twice, and count it as 2
items (more reliable how much is lost)
* Running the /clear command after retrieving items from a grave no longer clears
the grave backup
* Soulbound now works with curios
* Graves being moved (like with carry-on mod) will now be detected when they reappear
* Fixed crash with travelers backpack
* Now prevents "fake players" from looting graves

---

# You're in Grave Danger 2.0.7

### Changes
* Added compat with [Cosmetic Armor Reworked](https://www.curseforge.com/minecraft/mc-mods/cosmetic-armor-reworked)

### Fixes
* Graves will no longer overwrite other graves when dying in exact same places
outside the world

---

# You're in Grave Danger 2.0.6

### Changes
* Added new config to look downward for ground to place a grave on, when dying in
the air
* Changed default max grave count per player to 100 in the config (previously 50)
* Improved rendering efficiency in yigd GUIs with a scroll-bar

### Fixes
* If dying for the first time in a world with an empty inventory, players will
no longer be disconnected.
* Grave data should no longer be generated for one player in two profiles

---

# You're in Grave Danger 2.0.5

### Fixes
* Game no longer has a chance to crash when loading in with traveler's backpack and accessories

---

# You're in Grave Danger 2.0.4

### Fixes
* Graves can now generate below y=0 (if blocks can exist there) when `generateGraveInVoid`
config is set to `false`
* When selecting graves in the GUI, it will no longer tell you that you have your xp point
total number of levels
* Empty graves will now generate if they are configured to
* Graves will no longer delete modded inventory contents from graves when restarting the
instance.

---

# You're in Grave Danger 2.0.3

### Fixes
* The mod can now launch when using a dedicated server

---

# You're in Grave Danger 2.0.2

### Fixes
* Added a translation for enabling the soulbound enchantment in the configs
* Improved accessories compat implementation (thanks @Dragon-Seeker!)

---

# You're in Grave Danger 2.0.1

### Fixes
* Fixed issue related to graves clearing your inventory when trying to claim it, with a
specific difference of curios/accessories slots between the grave and your own inventory

### Changes
* Added syncing from client claim priority configs to server

---

# You're in Grave Danger 2.0.0 for NeoForge

### Changes
* Ported to NeoForge