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