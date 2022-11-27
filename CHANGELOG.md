# You're in Grave Danger 1.3.12

### Changes
- Added config to spawn the grave where you last touched the ground
- Added origins keep inventory power support

### Fixes
- Improved packet networking, meaning servers should no longer crash if too many people play on it
- Travelers backpack were disappearing when "trinket mode" in travelers backpack config was enabled
- XP progress did not save when dying above level 17
- Fixed incompat (crash) with inventory tabs

---

# You're in Grave Danger 1.3.11

### Changes
- You can now configure the graves to only be loot-able when holding a shovel. Default disabled
- You can configure probability of any entity spawning when looting a grave. Default disabled (0%)
    - The entity can be configured to which entity, and also completely custom NBT
    - Nbt can contain special values representing player name (of grave owner or looter), player UUID
    - Nbt can also contain any item (based on which slot) in the grave, and even if this item should be "used up" (deleted from grave) or not
- Grave area overrides now use drop types (keep, drop or grave) instead of just saying grave or no grave
- Added config for block not generating under graves in spawn protected area or claims
- Added "inventory origins" compat

### Fixes
- Grave scroll is now craft-able when not disabled
- Twilight bosses can no longer destroy graves
- Item name of the grave compass is now properly translated

---

# You're in Grave Danger 1.3.10

### Changes
- When retrieving items, and an items assigned slot is occupied, it will look for a slot to stack in

### Fixes
- Fixed issue where when retrieving items, equipped items would get empty nbt elements, causing them unable to stack with other items of same type
- Fixed issue where when using the command `/yigd grave [player]` on a non OP, while having proper permissions, "cheat buttons" did not show up

---

# You're in Grave Danger 1.3.9

### Changes
 - Added config to break or click on graves to claim them (2 in 1)
 - Added config to make soulbound level decrease when used (on death)
 - Added command to see coordinates for your latest death

### Fixes
 - All custom resource files now support UTF-8 encoding
 - Trinket destroy and keep slot types are now supported

---

# You're in Grave Danger 1.3.8

### Changes
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

# You're in Grave Danger 1.3.7

### Changes
 - Re-added requiem compat (for fake players and possessed mobs)
 - Graves can now be restored/robbed using coordinates
 - When a player respawn after dying their inventory is no longer cleared (can be toggled with config)
 - Replaced goml compat with common protection api compat
 - Players will now be told when starting their game if the config is broken, and how to fix it
 - You can now configure any of two places in the code where graves can generate, this to fix potential issues

### Fixes
 - Graves can now be robbed when configured to claim on destroy
 - The game will now start when the config file is broken, but will print an error
 - When opening grave GUI data will be sent in smaller parts allowing much more data to be sent
 - Game will no longer crash when graves drop items on the ground
 - Stopped parsing error with death scroll item from showing
 - Fixed an issue where levelz could cause soulbound items from inventorio and/or travelers backpack to disappear
 - Fixed botania resolute ivy behaviour

---

# You're in Grave Danger 1.3.6

### Changes
 - Added compatibility with The Graveyard!
   - You can configure so that dying close to a graveyard will have your grave spawn in that graveyard. Your grave will change place with a grave there, and if there are no free graves at that structure, your grave will generate where you died as normally
   - Change the tag `data/yigd/tags/worldgen/configured_structure_feature/graveyard_structures.json` to configure which structures graves can be replaced in. Default to all graveyards
   - Search radius for closest acceptable graveyard can be configured
 - Slightly toned the player color down in the `/yigd moderate` command from vibrant green to darker green
 - Added compatibility with fabric permissions api. I'm no longer "half stupid"! :)
 - Added a very basic search function for the player select in the GUI you get when executing `/yigd moderate`
 - Some commands can now be used from server console, command blocks, and similar
 - Added back travelers backpack compat
 - Added compatibility with botania resolute ivy

### Fixes
 - Filters will no longer reset when changing pages in the GUIs
 - Fixed graves that very rarely was placed in the wrong dimension
 - Graves will now ignore blocks as grass, snow layers, ferns, and dead bushes when generating (added to block whitelist)

---

# You're in Grave Danger 1.3.5

### Fixes
 - You will no longer lose all your items if you die in spectator mode
 - Fixed a potential issue if player had their inventory full of soulbound items, and had both curse of binding and soulbound on their armor, the armor would either disappear, or the game would hang, or it would work. Anyway now it works better
 - Fixed `/yigd` and `/yigd grave` not working for non OPs, even if selfView config was enabled

### Changes
 - Added 2 new configs. voidSlots and soulboundSlots
   - voidSlots: You can configure slot IDs for some slots that will never get saved in your grave.
   - soulboundSlots: You can configure slot IDs for some slots that will always receive the soulbound trait.
   - Slot IDs are available to read in the wiki. If any mod extending the inventory is included, the IDs will not be the same (for example BigInv mod)
 - Made enchantments more configurable. You can now decide where and when you can receive them (requires change in config file)

---

# You're in Grave Danger 1.3.4

### Fixes
 - Fixed another issue where the mod would crash servers

---

# You're in Grave Danger 1.3.3

### Fixes
 - Fixed an issue where servers would crash on startup

---

# You're in Grave Danger 1.3.2

### Changes
- Added soulbound item blacklist tag
- Added a button in the grave GUI to restore grave (for OPs)
- Added a button in the grave GUI to rob graves (for OPs)
- Grave backups are no longer deleted once the grave is claimed
- Grave backup status is now tracked, meaning you can see which graves have been claimed or destroyed in some way
- Added filters and display options for the newly tracked grave statuses when using the GUI
- Added command to clear grave backups
- If grave overrides a block when it spawns, this block will replace the grave once it's claimed
- Changed default configs to now place grave more accurately where you died.
- You can now right click with skulls on decorative graves to attach the skull to the grave
- Core command name is now configurable
- The `/yigd` command now has the same functionality as `/yigd grave`
- Added commands to whitelist/blacklist users from generating graves on death
- Added datapack file that can create areas in your world where graves can't spawn
- Added grave keys (configurable)
    - Paired to either a grave, a player, or will work on all graves
    - Retrieved from death and/or GUI
    - If made craftable with datapack they are bound to the player crafting them
    - Rebindable with shift + right click (configurable on/off)
- Combining soulbound with curse of binding will not equip armor when you respawn anymore
- Added config that can allow players to unlock their graves through GUI for everyone to rob it
- You can now decide what happens with your items when you die in spawn protected area (grave/keep items/drop items)

### Compatibility
- Added compatibility with levelz's XP (configurable)
- Added compatibility with 3 claim mods. You can now configure what happens when you die in claims from these mods. Keep items/drop items/generate grave.
    - GoML ReServed
    - FTB Chunks
    - FLAN

### Bug fixes
- "Magna tools" (like indrev drill or hammers) can no longer destroy graves with the help of extended mining area
- Fixed position error when gamerule doImmediateRespawn enabled
- Graves should not be able to spawn where the end portal or dragon egg can generate
- Fixed a crash connected to glowing graves
- Fixed an issue where the game would sometimes freeze with the itemLoss config enabled
