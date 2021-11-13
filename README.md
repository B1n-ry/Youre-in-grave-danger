# Youre-in-grave-danger
A minecraft mod which so far adds one single block. The grave, which will generate when you die, and store your items

### Configs
 * Generate Graves: If set to true, graves will spawn on death, and if set to false, they will not. You will instead drop your items
 * Retrieval Type: How you should interact with the grave to retrieve your items.
    * ON_USE: Right click the grave
    * ON_BREAK: Break the grave block
    * ON_STAND: Walk on top of the grave
    * ON_SNEAK: Walk on top of the grave sneaking
 * Drop Type: What would happen to your items as you interact with the grave
    * IN_INVENTORY: Put all your items back in your inventory, with the same layout as before you died
    * ON_GROUND: Drop all items from the grave similar to what happens to items in chests when broken
 * Remove Enchantments: A list with enchantment identifiers of which items should be deleted on death. (E.g. `minecraft:vanishing_curse`)
 * Soulbound Enchantments: A list with enchantment identifiers of which items should be kept after death. (E.g. `yigd:soulbound`)
 * Grave Replace Block Blacklist: Blocks that can not be overritten by a grave. Default is `minecraft:bedrock`. Block entities can't be overwritten
 * Grave Dimension Blacklist: Dimension IDs of what dimensions should be blacklisted from using graves. In these dimensions graves won't generate. You can find the dimension in the F3 screen in the top left
 * Spawn Grave in Void: Pretty self explanatory. If graves should generate when you fall out of the world.
 * Grave Spawn Height: If `Spawn Grave in Void` is set to true, this will determine the y-level graves will try to generate at when you fall in the void
 * Last Resort: If no valid block is found for your grave, what should happen?
    * SET_GRAVE: Override invalid block where you died with a grave
    * DROP_ITEMS: Drop your items where you died.
 * Default XP Drop: If the grave should opt for the default amount of XP dropped on minecraft, which goes to a max of 7 levels.
 * XP Drop Percent: If `Default XP Drop` is set to false, this will determine how many percent of your total XP will transfer through your grave
 * Grave Support Block: What block should generate under the grave if the block under matches a list of blocks (Defaulted to air, water, and lava)
    * Generate Block Under Grave: If a block should generate under the grave or not.
    * Overworld: What block should generate under the grave in the overworld
    * Nether: What block should generate under the grave in the nether
    * The End: What block should generate under the grave in the end
    * Custom Dimensions: What block should generate under the grave in custom dimensions
    * Whitelist Blocks: List with blocks that are allowed to get overwritten with the grave support block
 * Render Grave Owner Skull: If graves should render the head of the grave owner (CLIENT SIDE)
 * Render Grave Owner Name: If grave should render the playername of the grave owner (CLIENT SIDE). Does not affect non player graves
 * Tell Death Pos: If players should get a message with info about where their grave generated

You can build the mod yourself, or download it from here:<br>
CurseForge: https://www.curseforge.com/minecraft/mc-mods/youre-in-grave-danger<br>
Modrinth: https://modrinth.com/mod/yigd/versions
