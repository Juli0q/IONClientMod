# ION Client

The official ION Network client mod. It does three things:

- **Pins the ION Network server** to the top of the multiplayer server list, drawn in the
  network's look (the website's greys and periwinkle, the logo gradient on "ION" and a left-edge
  stripe, the live MOTD and player count), while staying a plain Minecraft list entry. The entry
  is never written to `servers.dat`: it can be joined, but not edited, deleted or moved, and the
  player's own servers keep their order. Any ION entries the player had saved themselves (any
  spelling of the address, with or without a port, any subdomain) are dropped so the pinned
  entry is always the only one.
- **Shows the player's ION coins** in the main menu's top-right corner (a coin and the number,
  "ION Coins" on hover). The balance comes from the public ION stats API for the logged-in
  account's UUID, refreshes every five minutes, and the display stays hidden when the account
  has no ION profile or the API cannot be reached.
- **Replaces the main menu panorama** with shots of the ION lobby (two by day, three by night,
  one indoors, rendered with Complementary Reimagined + Euphoria Patches). One set is picked at
  random per launch. 1.21.1 ships 1024px faces like vanilla; 1.8.9 blurs its panorama heavily, so
  it gets 512px faces to keep the jar small.

| Minecraft | Loader   | Jar                                   |
|-----------|----------|---------------------------------------|
| 1.8.9     | Forge    | `ionclient-<version>-forge-1.8.9.jar`    |
| 1.21.1    | Fabric   | `ionclient-<version>-fabric-1.21.1.jar`  |
| 1.21.1    | NeoForge | `ionclient-<version>-neoforge-1.21.1.jar`|
| 1.21.1    | Forge    | `ionclient-<version>-forge-1.21.1.jar`   |

No other mods are required on any loader (not even Fabric API: the coin texture is loaded from the jar
directly instead of through the resource manager).

## Settings

Options has an **ION Client...** button (on 1.8.9 it takes the slot of "Broadcast Settings...",
whose Twitch API is long gone; on 1.21.1 it is a full-width row under the grid). Forge's and
NeoForge's mod lists open the same screen from their "Config" button. It looks like vanilla's
options screens, with "ION" in the logo gradient and a gradient line under the header:

| Setting       | Values                                         | Default |
|---------------|------------------------------------------------|---------|
| Panorama      | Random, one of the lobby sets, Minecraft       | Random  |
| ION Coins     | ON / OFF                                       | ON      |
| Pinned Server | ON / OFF (off leaves the server list vanilla)  | ON      |
| Desktop Scale | ON / OFF, 1.8.9 only; active under LWJGL3 on a scaled desktop | ON |

plus links to the website and Discord. Changes apply right away (the server list on its next
load) and are saved to `config/ionclient.json` when the screen closes. The wording and layout
live in `common/` (`IonSettings`, `IonSettingsMenu`); each version only draws them.

## LWJGL3 on Linux (1.8.9)

Minecraft 1.8.9 ships LWJGL 2, which only speaks X11. On a Wayland desktop it runs through XWayland,
where the camera snaps on clicks (faked pointer warps), fullscreen can crash ("No modes available")
and scaled outputs are blurry. The 1.8.9 jar therefore also carries a coremod, built from
`mc-1.8.9-lwjgl3/`, that swaps LWJGL 2 for LWJGL 3/GLFW with native Wayland, real pointer lock and
full-resolution rendering, plus LWJGL 3 and its Linux natives (about 3 MB). It is derived from
[LLLLLwjgl3](https://github.com/xiaoyu1738/LLLLLwjgl3-1.8.9-Forge); see
`mc-1.8.9-lwjgl3/THIRD_PARTY_NOTICES.md` for its origin and ION's changes.

Forge ignores `FMLCorePlugin` in jars that start through a `TweakClass`; the bundled Mixin picks it up
instead and hands the coremod to FML. It only activates on Linux Wayland sessions and otherwise leaves
the game on LWJGL 2; on Linux, `-Dionclient.lwjgl3=true` or `=false` overrides the detection. Forge's
loading screen is turned off while it is active. ION Client also multiplies the chosen GUI scale by the
desktop scale (`ScaledResolutionMixin`), so "Large" keeps its size on a 1.45x display.

## Glowing and modern sounds (1.8.9)

ION's servers talk the current protocol, and two things in it mean nothing to a vanilla 1.8.9 client.
The mod reads both as a 1.9 client would, so the server needs no special casing for 1.8 players:

- **Glowing.** Since 1.9 an entity glows when bit 6 of its entity flags byte is set (the byte that also
  carries on fire, sneaking and invisible). ViaRewind passes that byte through unchanged and a 1.8
  server can set the bit itself, so it arrives either way; vanilla just ignores it. `RenderGlobalMixin`
  draws the outline with the spectator-outline framebuffer and shader 1.8.9 already has, through
  walls, in the entity's team colour (the Teams packet's colour field, which 1.8 already carries; the
  team prefix's colour as the 1.8.9 fallback; white otherwise). Invisible glowing entities still show
  their outline, as in 1.9. Dropped items, arrows, boats and the like are coloured by replacing their
  texture with the flat colour, which is what 1.9's outline mode does. `-Dionclient.glow=false` turns
  the outlines off.
- **Modern sound names.** The sound packet has carried the event name as a string since 1.8, so a
  server can send `entity.player.levelup` or `block.note_block.pling` to a 1.8 client, which knows
  them as `random.levelup` and `note.pling` and would otherwise drop them. `SoundHandlerMixin` plays a
  name the registry lacks as its 1.8.9 sound, from `assets/ionclient/sounds/legacy_names.txt`: 2013
  names of 1.9 to 26.4, generated by `mc-1.8.9/tools/legacy_sound_names.py` from the mapping data
  ViaVersion and ViaBackwards use, so each one sounds exactly as it does for a 1.8 player on a
  Via-proxied modern server (including Via's substitutes for sounds 1.8 never had). Resource packs
  that define a modern name themselves still win, because the registry is asked first.

`IONCLIENT_AUTOTEST=glow ./gradlew :mc-1.8.9:forgeRunClient` opens a flat world, flags a few entities and
saves `screenshots/ionclient-autotest-glow.png` to eyeball the outline.

## Telling the server (both versions)

Right after every Join Game the mod sends a plugin message on `ionclient:hello`: the format version,
the mod version and a bit set of what the client renders beyond its Minecraft version (glow outlines,
modern sound names). ION's servers decide per player what to send from the client's protocol version,
which says "1.8" for the 1.8.9 mod, so the hello is what lets IONCore send the glowing flag and modern
sound names to it. Join Game is sent again on every server switch behind the proxy, so each backend
hears its own; the channel name is a valid 1.13 identifier within 1.8's 20-character limit, so Velocity
and ViaVersion pass it through unchanged. The encoder is `common/.../IonClientHello.java`; the server
side is `IonClientContract` in IONPlugins' `core/utilities`, read by IONCore (Spigot), IONCore-Velocity
and the Minestom nodes into `ClientProfiles`.

## Modern textures (1.8.9)

The 1.8.9 jar builds `resourcepacks/ION Modern Textures.zip`, the block and item textures of
Minecraft 26.3 renamed to their 1.8.9 paths, which the player can turn on in the vanilla Resource
Packs screen. No Mojang texture ships in the ION jar: they are taken from Mojang's own client jar on
the player's machine. At the start of every launch a background thread checks the pack's stamp
(source version, converter revision, table hash, stored as the zip comment) and only rebuilds it
when that changed; the build takes a few seconds.

The client jar (41 MB) is taken from, in order: `-Dionclient.modernTextures.jar=<path>`,
`<gameDir>/ionclient/modern-textures/client-26.3.jar`, `<gameDir>/versions/26.3/26.3.jar`, and only
then downloaded from `piston-data.mojang.com` into the second location. Every copy is checked
against the pinned SHA-1. `-Dionclient.modernTextures=false` turns the feature off.

`mc-1.8.9/src/main/resources/assets/ionclient/modern_textures/mapping.txt` says where each 1.8.9
texture comes from (renames since 1.13, the clock and compass frames stitched back into strips,
water tinted blue). Textures it doesn't list are taken by the same name. Beds, banners, spawn eggs
and redstone dust stay vanilla, as do the GUI, particles and entities.

**Precaching from the ION Launcher.** Put the jar where the mod looks for it, before the game starts.
Helios already does this for a `File` module in the server's `modules`, with no launcher changes
(`path` is relative to the instance directory, which is the game directory):

```json
{
    "id": "net.minecraft:client-textures:26.3",
    "name": "Minecraft 26.3 textures (for ION Modern Textures)",
    "type": "File",
    "artifact": {
        "size": 41483720,
        "MD5": "cdfae0ad6fd58d11ed5500f22abe205a",
        "path": "ionclient/modern-textures/client-26.3.jar",
        "url": "https://piston-data.mojang.com/v1/objects/e877b6a07acd633fb3bb475002175cec036e7b87/client.jar"
    }
}
```

When the pinned version changes (`ModernTextureSource`), this entry has to change with it,
otherwise the mod ignores the precached jar and downloads the new one itself.

## The coins API

The main menu asks `GET <api>/api/stats/player/<uuid>/coins` (answer `{"uuid", "coins"}`, 404
for an unknown player), a public read-only route added to the Strapi backend
(`API-IONNNetworkNewWebsite`, `src/api/stats`). Until that route is deployed, and on any 404,
the mod falls back to the public player search (`/api/stats/players/search?q=<name>`) and picks
the row with the player's UUID. Nothing but the player's own UUID and name is sent.

The base URL defaults to `https://beta-api.ion-network.de` and can be changed with
`-Dionclient.api=https://...`.

## Building

The build needs a JDK 21 for Gradle (Gradle 8.14 does not run on Java 25) and provisions the
Java 8 and 21 toolchains it compiles with by itself.

```bash
export JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2   # or any JDK 21
./gradlew buildAll
```

`buildAll` builds every target and collects the four jars in `build/libs/`. The targets can also
be built one at a time:

```bash
./gradlew :mc-1.8.9:build                 # Forge 1.8.9
./gradlew -p mc-1.21.1 :fabric:build      # or :neoforge:build, :forge:build
```

## Layout

```
common/      brand constants and the entry's layout rules; pure Java 8, compiled into every target
mc-1.8.9/    unimined build: src/main (MCP names: the entry, its mixins) + src/forge (the @Mod)
mc-1.8.9-lwjgl3/  plain Java build of the LWJGL3 coremod merged into the 1.8.9 jar (no Minecraft classes)
mc-1.21.1/   Architectury Loom build, included from the root: src/main (mojmap: the entry, its
             mixins) + fabric/, neoforge/, forge/ (one entrypoint and metadata file each)
```

Two build systems because neither covers everything: unimined handles Forge 1.8.9 but cannot
apply the binary patches of NeoForge 21.1 / Forge 52, and Architectury Loom handles the 1.21.1
trio but not 1.8.9. They cannot share a buildscript classpath, so `mc-1.21.1` is an included
build. Versions live in the root `gradle.properties`, which the included build reads too.

### How the pin works

`ServerList` gets a mixin that inserts an `IonServerData` (a `ServerData` subclass) at index 0
after loading and takes it out again around saving, and refuses `remove`/`swap`/`replace` on it.
Because the pinned entry sits in the same list the screen indexes into, every index-based
operation on the player's own servers stays correct. The selection list swaps the vanilla row
for `IonServerEntry`, which extends the vanilla entry (so joining works untouched) and only
overrides the drawing and clicks. The screen's edit and delete buttons are disabled while the
pinned entry is selected.

## Running in the IDE / self-test

```bash
./gradlew :mc-1.8.9:forgeRunClient
./gradlew -p mc-1.21.1 :fabric:runClient    # :neoforge:runClient, :forge:runClient
```

With `IONCLIENT_AUTOTEST=1` in the environment the client saves
`screenshots/ionclient-autotest-title.png` from the main menu, then opens the multiplayer screen
on its own, waits for the ping, writes `screenshots/ionclient-autotest.png`, then shows Options
and the ION Client settings (`ionclient-autotest-options.png`, `ionclient-autotest-settings.png`)
and quits. That is how the displays are checked without a person at the keyboard. Dev sessions are offline and
have no ION profile, so `IONCLIENT_DEV_UUID` and `IONCLIENT_DEV_NAME` point the coin lookup at
a real account.

### Capturing panoramas

With `IONCLIENT_PANORAMA=1` set, or a file named `ionclient-panorama` in the game directory
(handy in a launcher instance), pressing F6 in-world hides the HUD, sets FOV 90, turns the camera
through the six cube faces starting from the current view direction (a second per face, so
shader TAA and exposure settle) and saves `panoramas/<time>/panorama_0..5.png`. It captures the
normal frame rather than using vanilla's offscreen panorama grabber, which Sodium and Iris leave
empty. New sets go under `assets/ionclient/textures/gui/title/background/<set>/` in both
versions and into `IonPanoramas.SETS`.

The Forge 1.21.1 dev run does not launch under Architectury Loom 1.11 (FML's
`ImmediateWindowHandler` cannot find the `minecraft` module in the userdev game layer, with or
without the early window). The Forge 1.21.1 jar is therefore compile-verified only; its entry
code is the same as the Fabric and NeoForge jars, which were run.

## License

ION Client is free software, licensed under the [GNU General Public License v3.0](LICENSE)
(GPL-3.0-only). You may use, change and redistribute it, but anything you distribute that is
based on it must be released under the same license, with its source code.
