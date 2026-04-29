# MoneyHUD

## Project Identity
- **Name:** MoneyHUD
- **Mod ID:** `moneyhud`
- **Version:** `${version}` (Resolved at build time)

## Technical Summary
The **MoneyHUD** mod acts as a client-side economy overlay tailored for roleplay and custom maps. While its core rendering environment is set to the client, it registers server-side commands (`MoneyHudServerCommand`) to enable centralized control by server operators or command blocks. The mod utilizes Fabric's `ServerPlayNetworking` to broadcast `MoneyHudPayload` packets to targeted clients, dynamically instructing them to toggle the HUD visibility or adjust its visual tier based on server events.

## Feature Breakdown
- **Premium Economy Overlay:** Renders a visual HUD tracking a scoreboard objective, eliminating the need for text-heavy sidebars.
- **Dynamic Visual Tiers:** Supports three distinct visual tiers (1, 2, and 3), allowing the HUD's appearance to evolve dynamically as players accumulate wealth or progress.
- **Centralized Server Control:** Designed to be strictly controlled via server commands, making it ideal for integration with command blocks, NPC dialogues, or quest rewards.
- **Client-Server Synchronization:** Leverages custom networking payloads (S2C) to ensure the client HUD state precisely matches the server's instructions.

## Command Registry
*Note: All commands require OP Permission Level 2. The `[<targets>]` argument is optional; if omitted, the command broadcasts to all online players.*

| Command | Description | Permission Level |
| :--- | :--- | :--- |
| `/moneyhud on [<targets>]` | Instructs the targeted client(s) to render the Money HUD on-screen. | OP (2) |
| `/moneyhud off [<targets>]` | Instructs the targeted client(s) to hide the Money HUD. | OP (2) |
| `/moneyhud tier <1\|2\|3> [<targets>]` | Forces the targeted client(s) to switch their HUD visual to the specified tier. | OP (2) |

## Configuration Schema
*Note: This specific mod does not generate a JSON configuration file in the `config/` folder. All HUD states and tier transitions are controlled entirely dynamically via the registered in-game commands (`/moneyhud`) and network payloads.*

## Developer Info
- **Author:** el_this_boy
- **Platform:** Fabric 1.21.1
