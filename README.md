# Chunky
[![Discord Chat](https://img.shields.io/discord/767330242078834712.svg)](https://discord.gg/QcRRzXX)

Chunky is a Minecraft Bedrock client that allows grabbing chunks from Minecraft Bedrock Dedicated server,
and loading them in NukkitX as a normally generated chunks.

## Limitations

- Since the NukkitX supports only block ids up to 255, the most recent game blocks are converted to UpdateBlock
- Currently only simple block entities conversion is supported: *Chest, MobSpawner*
- NukkitX must be slightly modified in order to start the plugin (incoming NukkitX PR)

## Supported versions
- `Minecraft Bedrock 1.13 - 1.16.0`
- `Minecraft Bedrock 1.16.100`
- `Minecraft Bedrock 1.18.30`