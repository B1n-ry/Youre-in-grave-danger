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
