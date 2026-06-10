# DynWorld

A lightweight library for creating and manipulating worlds on Minecraft 1.8.

DynWorld lets you spin up empty (or custom-generated) worlds at runtime and edit them
through a fast, low-level chunk API that bypasses Bukkit's per-block overhead. It works
directly on NMS chunk sections (`char[]` block arrays), so capturing and pasting large
regions stays cheap even on the legacy 1.8 block format.

## Features

- Create runtime worlds with a fluent builder (environment, gamemode, gamerules, generator).
- Adapt existing Bukkit worlds with `DynamicWorld.fromBukkit(world)`.
- Fast block, section, and region access via `ChunkView`.
- Capture regions and save/load them as MCEdit-style `.schematic` files.
- Off-thread / time-sliced region pasting through an `OperationPool`.

## Installation

```xml
<dependency>
    <groupId>dev.tommyjs</groupId>
    <artifactId>dynworld</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Creating a dynamic world

Use the builder to create a world. By default it uses a void generator, adventure mode,
and a `NORMAL` environment.

```java
DynamicWorld world = DynamicWorld.builder()
    .setName("arena")
    .setEnvironment(World.Environment.NORMAL)
    .setGameMode(GameMode.ADVENTURE)
    .setGameRule(GameRules.DO_DAYLIGHT_CYCLE, "false")
    .create();

World bukkit = world.getBukkitWorld();
```

You can supply a custom generator (called per-chunk with a writable `ChunkView`):

```java
DynamicWorld flat = DynamicWorld.builder()
    .setName("flat")
    .setChunkGenerator((chunkX, chunkZ, chunk) -> {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.setBlock(x, 0, z, BlockState.of(Material.BEDROCK, 0));
                chunk.setBlock(x, 1, z, BlockState.of(Material.GRASS, 0));
            }
        }
    })
    .create();
```

To edit a world the server already has loaded, wrap it instead of creating one:

```java
DynamicWorld existing = DynamicWorld.fromBukkit("world");
```

When you're done with a created world, unload it through its manager:

```java
DynamicWorldManager.DEFAULT.unload(world);
```

## Fast editing with chunks

`DynamicWorld` implements `ChunkProvider`, so you can grab a `ChunkView` and edit blocks
directly. Coordinates passed to `setBlock` are world coordinates. Call `refreshChunk`
afterwards to push changes to nearby players.

```java
ChunkView chunk = world.getChunkAt(0, 0);

chunk.setBlock(8, 64, 8, BlockState.of(Material.DIAMOND_BLOCK, 0));
chunk.setBlock(9, 64, 8, 35, 14);

world.refreshChunk(0, 0);
```

For bulk loads you can also load a rectangle of chunks up front:

```java
world.loadChunkRegion(-4, -4, 4, 4);
```

## Saving and loading schematics

Capture a cuboid into a `CapturedRegion`, then write it to a `.schematic` file. The
capture path reads whole chunk sections at once, making it fast even for large areas.

```java
CapturedRegion region = CapturedRegion.capture(
    world,
    0, 60, 0,
    32, 80, 32,
    0, 60, 0);

SchematicUtil.write(region, Path.of("arena.schematic"));
```

Load it back and paste it into any `DynamicWorld` (or per-chunk via `ChunkView`):

```java
CapturedRegion region = SchematicUtil.read(Path.of("arena.schematic"));

world.setRegion(region, 100, 64, 100, PasteOptions.create()
    .ignoreAir(true)   // don't overwrite existing blocks with air
    .refresh(true));   // resend affected chunks to players
```

### Pasting off the main thread

Large pastes can be time-sliced or run through an `OperationPool` so they don't stall the
server tick. `prepareSetRegion` returns an `Operation` you can submit to a pool:

```java
OperationPool pool = OperationPool.create(plugin, 5, 3); // max 5 milliseconds per tick, 3 concurrent operations
pool.start();

Operation paste = world.prepareSetRegion(region, 100, 64, 100, PasteOptions.create());
pool.submit(paste).thenRun(() -> getLogger().info("Paste complete"));
```

The pool spreads work across ticks using the configured millisecond budget, so a single
large schematic is applied incrementally instead of in one blocking burst.

