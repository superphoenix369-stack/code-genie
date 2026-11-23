# Code Genie

**A retro cheat cartridge for Minecraft.** Press **G**, punch in a famous cheat code (a Game Genie code like `SXIOPO`, or a classic like `IDDQD`, `HESOYAM` or ↑↑↓↓←→←→BA), and get a Minecraft version of what it did in the original game.

Inspired by the classic NES Game Genie. Every code in the codebook is a real, published Game Genie code. The mod decodes it with the same bit layout as the original hardware, so it shows you the ROM address and byte it would have patched (`SXIOPO → $91D9:AD`).

## How it works

Press **G** (rebindable). The screen has two parts, and the game pauses while it's open in singleplayer:

- **Classic Cheat Codes** has the cursor as soon as the screen opens, so you can just type `iddqd` and hit Enter. The arrow keys type arrows, so the Konami code goes in the way it should: ↑↑↓↓←→←→BA.
- **Game Genie Codes** works like the original cartridge: two rows of eight letters and up to **3 codes at once**. Some cheats need all three. Press Tab (or click a letter) to switch to it.

No client mod? Use chat: `/cheat power overwhelming` or `/genie SXIOPO`. These work with vanilla clients as long as the mod is on the server.

- **Toggle** codes (↺) stay plugged in through death, relogs and restarts. Enter the code again to turn it off, or `/genie off` to pull the cartridge.
- **Instant** codes (★) fire each time you enter them.
- **Unknown codes glitch.** Any valid code that isn't in the book pokes a "random byte": you'll get a harmless, weird effect. The same code always does the same thing, so go hunting.

## Game Genie codes (NES)

| Code | Original game | Original effect | In Minecraft |
|---|---|---|---|
| ↺ `SXIOPO` | Super Mario Bros. | Infinite lives | A fatal blow becomes a 1-UP: you pop back up at full health |
| ↺ `APZLGG` | Super Mario Bros. | Super jump | Jump Boost IV, no fall damage |
| ↺ `YZXLNY` | Super Mario Bros. | Super speed game | Speed III + Haste II |
| ★ `UATIUE` | Super Mario Bros. | Invincibility, walk through enemies | 20 s Starman: invincible, and hostile mobs you touch are knocked out (bosses shrug it off) |
| ★ `YSAOPE` + `YEAOZA` + `YEAPYA` | Super Mario Bros. | Start on World 8 | Warp to The End (onto a safe obsidian pad). Enter it again in The End to warp back |
| ↺ `SXVUZUSE` | The Legend of Zelda | Infinite health | Endless Regeneration III |
| ★ `SZVXASVK` | The Legend of Zelda | Infinite rupees | A stack of emeralds |
| ↺ `SXXTPSSE` | Mega Man 2 | Infinite energy | Your hunger bar never drains |
| ↺ `EIUGVTEY` | Mega Man 2 | Invincibility | No damage (except `/kill` and the void) |
| ★ `IPVGZGZE` | Contra | Start with 30 lives | Banks 30 extra lives (up to 99) |
| ★ `XXUAAA` | Metroid | Enemies are frozen | Freezes hostile mobs within 16 blocks for 15 s |
| ↺ `KZUIUZKA` | Castlevania | Infinite hearts | 10 golden hearts that refill every 10 s |
| ↺ `AAVETLGA` | Mike Tyson's Punch-Out!! | KO with one punch | Strength high enough to drop almost anything in one hit |
| ↺ `NNOXXGEE` | Super Mario Bros. 3 | Fly forever until hit | Survival flight, until something hits you |

## Classic cheat codes

Spaces, capitals and punctuation don't matter: `Power Overwhelming`, `poweroverwhelming` and `POWER-OVERWHELMING` all work.

| Code | Original game and effect | In Minecraft |
|---|---|---|
| ★ ↑↑↓↓←→←→BA | Contra: the Konami code, 30 lives | Banks 30 extra lives (also `UUDDLRLRBA`) |
| ★ `JUSTIN BAILEY` | Metroid: start mid-game, powered up, suit off | 10 min of extra hearts, speed and jump, plus a crossbow and rocket "missiles" |
| ↺ `ABACABB` | Mortal Kombat (Genesis): the blood code | Anything you hit bleeds, and your kills burst in red (everyone sees it) |
| ↺ `DK MODE` | GoldenEye 007: big heads | Every creature's head is huge. Only you see it, and it needs the mod on your client |
| ↺ `IDDQD` / `POWER OVERWHELMING` | Doom / StarCraft: god mode | No damage (except `/kill` and the void) |
| ★ `IDKFA` | Doom: all weapons, ammo, keys, armor | Diamond armor, diamond sword, bow, crossbow, shield, 128 arrows |
| ↺ `WHOSYOURDADDY` | Warcraft III: invulnerable, one-hit kills | No damage, and almost anything dies in one hit |
| ★ `HESOYAM` | GTA: San Andreas: health, armor, $250k | Full health and food, 8 golden hearts of "armor", 25 emeralds |
| ↺ `BAGUVIX` | GTA: San Andreas: infinite health | Immune to projectiles, fire, lava and explosions (falls and drowning still hurt) |
| ↺ `AEZAKMI` | GTA: San Andreas: never wanted | Permanent invisibility, so mobs mostly leave you alone |
| ★ `ROCKETMAN` | GTA: San Andreas: jetpack | Elytra and 32 firework rockets |
| ↺ `KANGAROO` | GTA: San Andreas: mega jump | Jump Boost VII, no fall damage |
| ↺ `CVWKXAM` | GTA: San Andreas: infinite oxygen | Permanent Water Breathing |
| ★ `AIYPWZQP` | GTA: San Andreas: parachute | 60 s of Slow Falling |
| ★ `SHOW ME THE MONEY` | StarCraft: 10,000 minerals and gas | 16 diamonds, 64 iron ingots |
| ★ `BLACK SHEEP WALL` | StarCraft: reveal the map | Night Vision for 2 min, and every creature within 96 blocks glows |
| ↺ `OPERATION CWAL` | StarCraft: build faster | Haste V |
| ★ `THERE IS NO COW LEVEL` | StarCraft / Diablo | There is now. Eight cows appear around you |
| ★ `ROSEBUD` | The Sims: +§1,000 | 10 emeralds |
| ★ `MOTHERLODE` | The Sims: +§50,000 | 16 emerald blocks |
| ★ `GREEDISGOOD` | Warcraft III: gold and lumber | 64 gold ingots, 64 oak logs |

A typed cheat that isn't in the book just gets a "doesn't work in this game" message.

## Commands

- `/cheat <phrase>`: enter a classic cheat code (`/genie cheat <phrase>` works too)
- `/genie <code> [code] [code]`: enter Game Genie codes
- `/genie list`: the codebook in chat
- `/genie active`: what's plugged in, banked lives, Starman time left
- `/genie off`: switch off all toggles

`/codegenie` works as an alias.

## Server owners

Config: `config/codegenie.json`

```json
{
  "requireOp": false,
  "allowGlitchCodes": true,
  "allowWarp": true,
  "disabledCheats": []
}
```

- `requireOp: true` limits code entry to operators. With a permissions mod (e.g. LuckPerms), use the node `codegenie:use`.
- `disabledCheats` takes names like `"INVINCIBILITY"`, `"P_WING"`, `"WARP_WORLD_8"`, `"GOD_MODE"`, `"BLOOD_CODE"`. The full list of names is in `Cheat.java`.

**Install:** required on the server. Optional on the client, where it adds the G-key screen and lets you see DK Mode. Needs Fabric API.

---

*Not affiliated with or endorsed by the makers of the Game Genie, Nintendo, Konami, Capcom, Rare, id Software, Rockstar, Blizzard, Midway, EA or Mojang. Game titles are used only to say where each code came from.*
