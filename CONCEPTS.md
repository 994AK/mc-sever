# Concepts

Shared domain vocabulary for this project - entities, named processes, and status concepts with project-specific meaning. Seeded with core domain vocabulary, then accretes as ce-compound and ce-compound-refresh process learnings; direct edits are fine. Glossary only, not a spec or catch-all.

## Custom Plugin Testing

### Script-Based Plugin Test
A fast pre-server verification step for custom plugins that compiles plugin code and runs isolated Java tests without starting the Minecraft server.

It is the default first check for rules, state transitions, persistence helpers, command text formatting, and layout calculations; it does not prove server lifecycle, real player input, or cross-plugin behavior.

### MockBukkit Test
A developer-side Bukkit/Paper API test that uses mocked server, plugin, player, command, and event objects to verify plugin wiring without launching a real server.

It sits between script-based tests and runtime smoke tests: useful for command, permission, listener, GUI-click, and lifecycle seams, but not a replacement for real Paper/Leaf loading or client interaction.

### Runtime Smoke Test
A small server-run verification that confirms a plugin jar loads, registers its commands or integrations, reads its configuration, and has no obvious startup/runtime exceptions.

Runtime smoke tests validate the server boundary. They should be narrower than full gameplay testing and should not replace script-based or MockBukkit tests for core logic.

### In-Game Smoke Test
A targeted client-side verification of the player paths that require a real Minecraft client, world state, or multiple players.

Use it for right-click interactions, inventory GUI feel, visual effects, true two-player flows, and cross-plugin behavior that cannot be trusted from isolated tests alone.

## LeafMC Custom Plugins

### LeafGomoku
The server's custom Gomoku room-platform plugin: it owns Gomoku room state, board interaction, match lifecycle, stats, appearance choices, and runtime smoke requirements for the in-world game.

### LeafFriends
The server's custom lightweight social plugin: it owns friend relationships, friend-only messaging shortcuts, privacy settings, blacklist behavior, and consent-based friend teleport flows.

### LeafChainHarvest
The server's custom chain collection plugin: it owns crop right-click harvest and replant behavior, farm sow/fertilize/collect actions, chain wood cutting, chain ore mining, and the admin menu that controls allowed action/material toggles.

## Relationships

Script-based plugin tests are the first gate for custom plugin logic. MockBukkit tests cover Bukkit/Paper API wiring that still does not need a real server. Runtime smoke tests confirm the jar and server boundary. In-game smoke tests confirm the real player experience.
