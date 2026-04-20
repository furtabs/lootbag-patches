# LootBags Patched

## Legal Disclaimer

If you are the original mod developer and you have an issue with this project, please message me on Discord at `furtabs`. I am trying to provide a fix for dead or discontinued versions of mods and do not intend to harm any mod author or gain monetary value from their work.

## Overview

A patched version of LootBags for Minecraft 1.12.2.

This project is a modified build of LootBags released under the mod's custom license, which permits changes when the original developer is unavailable. I have informed Malorolam about this patch and will remove it if requested.

## Fixes

This patched version fixes issues: #178, #185, #188, #191, #215, #218.

- Add `ru_ru.lang` ([commit e3ee244](https://github.com/craftorio/LootBags/commit/e3ee2445d39d9919b76aa9bed8f5a5119ca56451))
- Fix storage issues ([#209](https://github.com/Malorolam/LootBags/pull/209))
- Fix potential null pointer ([#89](https://github.com/Malorolam/LootBags/pull/89))
- Update `tr_TR.lang` ([#154](https://github.com/Malorolam/LootBags/pull/154))

## License Note

> 1a: If a modified version of the code is to be used to update the mod in my absence, whether perceived or actual, regardless of whether the work is done independently or at the request of a pack developer, inform me of the action.

## Original Description

Lootbags is a mod which adds bags that drop other items. It is heavily configurable through its two config files, and the GitHub wiki contains more information on using the configs. By default, the mod is configured for a total of 16 bags with both typical bags and "secret" bags that only spawn in certain conditions. The typical bags drop portions of the vanilla world gen loot tables, with rarer bags relating to more consistent rare loot.

The mod includes:

- A loot recycler block that consumes items dropped by loot bags and generates a new loot bag once a value threshold is reached.
- A bag opener that can open bags automatically and supports the expected insert/extract behavior.
- A bag storage block that converts between bags using an auto-generated import/export list, retains the chosen output, and saves its inventory when broken.
- Several commands for inspecting and dumping loot data.

## Support

If you want to contact me directly, PM me through Curse or GitHub.

Documentation and tutorials for the config files are available on the original LootBags wiki: https://github.com/Malorolam/LootBags/wiki

90% of issues with missing textures, localized names, or bag loot tables are caused by bag config problems. If these issues occur and you have not edited the config files, delete `lootbags.cfg` and `lootbags_BagConfig.cfg` and run the game again.

Enable Verbose Mode and Debug Mode in `lootbags.cfg` for more detailed log output.

## Modpacks

You may use this in modpacks provided the original author is credited and this page is included in the modpack information. On Curse, this is automatic.
