# Cuboid Clearer

A powerful building utility mod for Fabric 26.1.2.

## Requirements
- **Minecraft**: 26.1.2
- **Fabric Loader**: [Download here](https://fabricmc.net/use/)
- **Fabric API**: [Download here](https://modrinth.com/mod/fabric-api)

## Features
- **Cuboid Selection**: Select two corner positions with commands or by sneaking and right-clicking a block with a **Stick**.
- **Clear Area**: `/cc clear` — breaks all blocks in the selection (uses tool durability).
- **Fill Area**: `/cc fill` — fills empty spaces with the block in your hand (consumes from inventory).
- **Hammer Mode**: `/cc hammer` — toggles 3×3 mining mode.
- **Selection Visualization**: Dust particles mark your selected region.
- **Join Message**: A welcome message appears when you join a world, pointing you to `/cc info`.

## Commands
| Command | Description |
|---|---|
| `/cc pos1` | Set the first corner |
| `/cc pos2` | Set the second corner |
| `/cc clear` | Break all blocks in the selection |
| `/cc fill` | Fill the selection with your held block |
| `/cc hammer` | Toggle 3×3 mining mode |
| `/cc cancel` | Clear your current selection |
| `/cc info` | Show mod info and hammer status |
| `/cc commands` | List all available commands |

## How to Build

1. **Install JDK 25** — [Eclipse Temurin 25](https://adoptium.net/temurin/releases/?version=25)
2. **Run the build** in the project folder:
   ```
   ./gradlew build
   ```
3. **Find the JAR** at `build/libs/cuboid-clearer-1.0.0.jar`

## Credits
Author: [@hungr1yuri](https://github.com/hungr1yuri)  
Contact: @hungryuri on Discord
