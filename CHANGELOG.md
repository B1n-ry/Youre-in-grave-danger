# You're in Grave Danger 1.5.0

### Changes
 - Updated to 1.19.3
 - Changed blacklistDimensions config into taking dimension string IDs, and not integer IDs
 - Disabled some compatibilities as mods hasn't updated. These mods are:
   - FLAN
   - Ftb Chunks
   - LevelZ
   - Origins (inventory power)
   - Travelers backpack
 - Changed how glowing graves are rendered. This might be able to fix some rendering issues

### Fixes
 - Fixed some issues with item-loss config not working properly, and sometimes crashing

---

# You're in Grave Danger 1.4.6

### Changes
 - Added config to spawn the grave where you last touched the ground
 - Added origins keep inventory power support

### Fixes
 - Improved packet networking, meaning servers should no longer crash if too many people play on it
 - Travelers backpack were disappearing when "trinket mode" in travelers backpack config was enabled
 - XP progress did not save when dying above level 17
 - Minecells boss doesn't destroy graves
 - Fixed incompat (crash) with inventory tabs

---

# You're in Grave Danger 1.4.5

### Changes
 - You can now configure the graves to only be loot-able when holding a shovel. Default disabled
 - You can configure probability of any entity spawning when looting a grave. Default disabled (0%)
   - The entity can be configured to which entity, and also completely custom NBT
   - Nbt can contain special values representing player name (of grave owner or looter), player UUID
   - Nbt can also contain any item (based on which slot) in the grave, and even if this item should be "used up" (deleted from grave) or not
 - Improved grave area overrides to handle keep inventory zones
 - Added config for block not generating under graves in spawn protected area or claims
 - Added "inventory origins" compat

### Fixes
 - Grave scroll is now craft-able when not disabled
 - Twilight bosses can no longer destroy graves

---

# You're in Grave Danger 1.4.4

### Changes
 - When retrieving items, and an items assigned slot is occupied, it will look for a slot to stack in

### Fixes
 - Fixed an issue where when retrieving items, equipped items would get empty nbt elements, causing them unable to stack with other items of same type
 - Fixed an issue where when using the command `/yigd grave [player]` on a non OP, while having proper permissions, "cheat buttons" did not show up
 - Fixed an issue when using latest levelz version (1.4.0), game would crash on death

---

# You're in Grave Danger 1.4.3

### Changes
 - Added back FTB Chunks compat
 - Added config to break or click on graves to claim them (2 in 1)
 - Added config to make soulbound level decrease when used (on death)
 - Added command to see coordinates for your latest death

### Fixes
 - All custom resource files now support UTF-8 encoding
 - Trinket destroy and keep slot types are now supported

---

# You're in Grave Danger 1.4.2

### Changes
 - Added back FLAN compat
 - Updated to 1.19.2
 - You can now see who robbed your graves (configurable)
 - Added zh_cn translation, thanks to @deluxghost
 - TrySoft config is now more customizable, and you can config graves to keep same x and z, but change y coordinate
 - Graveyard spots can now be designated to specific players
 - You can now change in which dimension the graveyard should work in (default overworld)
 - You can now configure graveyards to generate graves at the closest place to the dying player
 - Dimension names in GUIs can now be changed with resource packs
 - Notifications for robbed graves can now be disabled
 - Graves can now be configured to stay after being claimed. You can click them to see when you died (which minecraft day) and you can move this grave with silk touch
 - Added config to keep all trinket items
 - Added grave compass item (normal compass pointing to your grave)

### Fixes
 - Fixed backslot compat
 - Fixed a crash with `/yigd moderate`
 - Grave timer for robbing and expiration is no longer reset on world restart
 - GUI buttons will now also check permission api permissions
 - Unlocking graves now works

---

# You're in Grave Danger 1.4.1

### Changes
 - Added back graveyard compat
 - Added back ftb chunks compat
 - Added back levelz compat
 - Commands can now restore/rob graves based on coordinates
 - Added compat with common protection api (land claim api)
 - Added back inventorio compat
 - Added back travelers backpack compat
 - Inventory will no longer be cleared when respawning, before soulbound items are given back (configurable)
 - User will now be prompted with a screen complaining, if the yigd config file is broken
 - Graves can now be configured as of what will cause them to generate. Player death or when the inventory would usually drop (both still when player dies)
 - Improved modded inventory storage in grave data and soulbound data

### Fixes
 - Removed error print in log when death scroll is disabled
 - Other bug fixes

---

# You're in Grave Danger 1.4.1-beta.1

### Changes
- Slightly toned the player color down in the `/yigd moderate` command from vibrant green to darker green
- Added compatibility with fabric permissions api. I'm no longer "half stupid"! :)
- Added a very basic search function for the player select in the GUI you get when executing `/yigd moderate`

### Fixes
- Filters will no longer reset when changing pages in the GUIs
- Fixed graves that very rarely was placed in the wrong dimension

---

# You're in Grave Danger 1.4.0

### Changes
 - Ported to 1.19