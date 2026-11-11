# Minecraft Maze and Minecoins Plugin

Maze Market Escape is a Paper Minecraft plugin that adds a generated mob maze, player tasks, a virtual Minecoins currency, a shop, purchasable items, and a command that lets players leave the server. Players enter the maze, defeat monsters, collect emeralds, complete the exit objective, and earn Minecoins to spend in the shop.

## Install

1. Build the plugin with Java 21 and Maven:

   ```text
   mvn package
   ```

2. Copy the generated `target/plugin.jar` file into your Paper server's `plugins` folder.

3. Start or restart the server.

4. The plugin generates the maze near the first world's spawn location when it enables.

Requirements:

- Paper 1.21.11 or a compatible 1.21 server
- Java 21
- Maven 3.9 or newer for building from source

## Use

Players can use these commands:

- `/maze` teleports the player into the generated maze and starts the maze tasks.
- `/shop` opens the Minecoins shop.
- `/minecoins` displays the player's Minecoins balance.
- `/exitserver` disconnects the player from the server.

To complete the maze, a player must:

1. Defeat three maze monsters.
2. Collect three emeralds.
3. Reach the gold block exit at the far end of the maze.

Completing the maze awards 25 Minecoins and returns the player to their previous location.

The shop sells:

- Maze Blink: an ender pearl for 10 Minecoins
- Maze Snack: a golden apple for 8 Minecoins
- Maze Blade: an iron sword for 15 Minecoins

Minecoins balances are stored in the plugin data folder in `balances.yml` and persist between server restarts.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/dcf74eaf-72f7-49b7-ba97-9bf9a309975b?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=dcf74eaf-72f7-49b7-ba97-9bf9a309975b
