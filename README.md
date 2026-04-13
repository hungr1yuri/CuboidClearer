# Cuboid Clearer

A powerful building utility mod for Fabric.

## Features
- **Cuboid Selection**: Select two positions easily.
  - **Commands**: `/cc pos1` and `/cc pos2`
  - **Interaction**: Sneak + Right-click with a **Stick** to set positions.
- **Clear Area**: `/cc clear` - Breaks all blocks in the selection (uses tool durability).
- **Fill Area**: `/cc fill` - Fills empty spaces with the block in your hand (consumes from inventory).
- **Hammer Mode**: `/cc hammer` - Toggle 3x3 mining mode.
- **Selection Visualization**: See your selection with dust particles.

## Usage
1. Set Position 1 and 2 (using a stick or commands).
2. Use `/cc clear` to empty the area (hold a tool in your hand).
3. Use `/cc fill` to fill the area with blocks (hold the block you want to use).

## How to Build (For Developers)

If you want to build the mod yourself:

1.  **Install JDK 21**: You need Java 21 to build this mod.
    *   👉 [Download OpenJDK 21 for Windows](https://api.adoptium.net/v3/installer/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk)
2.  **Run the Build**: Open a terminal in this folder and run:
    ```powershell
    .\gradlew build
    ```
3.  **Get the JAR**: Once finished, your mod file will be here:
    `build/libs/cuboid-clearer-1.0.0.jar`

## Credits
Author: [@hungr1yuri](https://github.com/hungr1yuri)
Contact: @hungryuri on Discord
