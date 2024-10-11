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