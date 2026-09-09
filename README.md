# One Mace

A Fabric server mod for Minecraft 1.21.11. Limits the whole server to a single
Mace in existence at a time.

## What it does

- **One Mace, ever.** Once a Mace is forged (Heavy Core + Breeze Rod, crafting
  table or 2x2 grid), no more can be crafted until it's destroyed.
- **Announcements.** Broadcasts in chat when it's forged, when it changes
  hands (e.g. picked up after the holder died), and when it's destroyed.
- **No stashing it.** Can't be placed in chests, barrels, shulker boxes, ender
  chests, or bundles.
- **Can't hide on the locator bar.** The holder always shows up for other
  players, even while sneaking, wearing a mob head/carved pumpkin, or under
  Invisibility.
- **Dies normally.** Drops on death like any other item; picking it back up
  (by anyone) announces the new holder.
- **Comes back.** If it's destroyed - lava, void, cactus, an explosion,
  whatever - a message goes out and crafting opens back up.

## Building it

I can't compile or test this in my own sandbox (no access to Mojang's or
Fabric's servers from here), so please build it yourself:

1. Install **JDK 21**.
2. From this folder, run:
   - Linux/macOS: `./gradlew build`
   - Windows: `gradlew.bat build`
3. The mod jar comes out at `build/libs/onemace-1.0.0.jar`.

First build will take a few minutes - Gradle downloads Minecraft itself and
deobfuscates it locally.

## Installing it

1. Set up a **Fabric server** for 1.21.11 (Fabric installer → server download).
2. Drop in **Fabric API** for 1.21.11 (from Modrinth/CurseForge) alongside it.
3. Drop `onemace-1.0.0.jar` into the server's `mods` folder.
4. Start the server.

This is a server-side-only mod - nothing needs installing on players' clients.

## Design notes (read this if something misbehaves)

I've leaned on things I could actually verify rather than internal game
methods wherever possible:

- **Tracking who has the Mace, and whether one exists at all**, is done by
  directly checking player inventories once a second, plus watching the
  dropped item itself tick-by-tick - not by hooking crafting/pickup/death
  internals. This is the part I'm most confident works correctly regardless
  of any future 1.21.11 patch, since it only uses plain, stable APIs
  (inventory contents, entity lookup by UUID).
- **The locator bar override** uses `WaypointTransmitter.cannotReceive`,
  which I confirmed directly against Mojang's mappings for 1.21.11 - this is
  the one genuinely vanilla mechanism sneaking/pumpkins/invisibility rely on,
  so overriding it there is the correct fix rather than a workaround.
- **Storage blocking and the "don't waste your ingredients" crafting block**
  are mixins into more obscure internals I couldn't fully verify without a
  real compiler and game jar. I've marked them `require = 0` in the mixin
  config, so if Mojang's changed something and one fails to match, **the mod
  still loads fine and the core "one Mace" rule still holds** - you'd just
  lose that specific extra protection rather than the mod breaking outright.
  If storage-blocking doesn't work after you build it, that's the first
  place to look (`SlotMixin.java`, `BundleItemMixin.java`).
- **A natural despawn (5 minutes unpicked) is treated the same as
  destruction** - crafting reopens. You didn't ask for despawn-immunity
  specifically, and I didn't want to bolt on a private-field mixin for a
  "nice to have" that risks crashing the whole mod if the field's ever
  renamed. Say the word if you'd rather it never despawned instead - it's a
  clean addition on top of this.
- **Ops using `/give` or creative mode** can still spawn an extra Mace -
  no mod can fully stop that. It gets confiscated from them within about a
  second by the same inventory scan that does everything else, but the
  ingredients (if crafted) or the `/give`'d copy are just gone, not refunded.
- Compiled cleanly against a stub of the exact API surface it uses (real
  syntax/type-checked), but that's not the same as compiling against the
  actual Minecraft jar - that only happens when you run the real build.

## Resetting it

Delete `<world folder>/onemace/state.properties` on the server while it's
stopped. Next start treats it as if no Mace has ever existed.
