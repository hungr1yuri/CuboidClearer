# Cuboid Clearer

[![Build](https://github.com/hungr1yuri/CuboidClearer/actions/workflows/build.yml/badge.svg)](https://github.com/hungr1yuri/CuboidClearer/actions/workflows/build.yml)
![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-brightgreen)
![Loader](https://img.shields.io/badge/Loader-Fabric-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

A small server-side building tool for Fabric. Pick two corners, then wipe the
whole area or fill it with a block in one command. There's also a 3x3 hammer
mode for chewing through walls faster.

It runs on the server (or in singleplayer), so players don't need it installed
on the client.

> Also available for NeoForge: [CuboidClearer-NeoForge](https://github.com/hungr1yuri/CuboidClearer-NeoForge).

## Requirements

- Minecraft 26.1.2
- [Fabric Loader](https://fabricmc.net/use/)
- [Fabric API](https://modrinth.com/mod/fabric-api)

## How it works

Select two corners, either with `/cc pos1` and `/cc pos2`, or by sneaking and
right-clicking two blocks with a stick. Dust particles trace the box so you can
see what you've picked. Then run `/cc clear` to break everything inside, or
`/cc fill` to fill the gaps with whatever block you're holding.

A few details worth knowing:

- `clear` uses your held tool and spends its durability, the same as mining by
  hand. Bedrock is left alone.
- `fill` pulls blocks from your inventory and only fills empty space.
- Selections are capped at 5000 blocks so a typo can't wipe half the world.
- Hammer mode breaks the 3x3 around each block you mine, costing durability per
  block.

## Commands

| Command | What it does |
|---|---|
| `/cc pos1` | Set the first corner (your position, or pass coordinates) |
| `/cc pos2` | Set the second corner |
| `/cc clear` | Break every block in the selection |
| `/cc fill` | Fill the selection with the block in your hand |
| `/cc hammer` | Toggle 3x3 mining mode |
| `/cc cancel` | Clear your current selection |
| `/cc info` | Show mod info and hammer status |
| `/cc commands` | List the commands |

## Building

You'll need [JDK 25](https://adoptium.net/temurin/releases/?version=25). From the
project folder:

```
./gradlew build
```

The jar lands in `build/libs/cuboid-clearer-2.0.0.jar`.

## License

Released under the MIT License. See [LICENSE](LICENSE).

## Contact

Made by [@hungr1yuri](https://github.com/hungr1yuri). Questions: `@hungryuri` on Discord.
