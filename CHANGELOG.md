# You're in Grave Danger 1.4.2

### Changes
 - Added back FLAN compat

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