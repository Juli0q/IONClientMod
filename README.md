# ION Client

The official ION Network client mod. It does two things:

- **Pins the ION Network server** to the top of the multiplayer server list, drawn in the
  network's look (the website's greys and periwinkle, the logo gradient on "ION" and a left-edge
  stripe, the live MOTD and player count), while staying a plain Minecraft list entry. The entry
  is never written to `servers.dat`: it can be joined, but not edited, deleted or moved, and the
  player's own servers keep their order.
- **Shows the player's ION coins** in the main menu's top-right corner (a coin and the number,
  "ION Coins" on hover). The balance comes from the public ION stats API for the logged-in
  account's UUID, refreshes every five minutes, and the display stays hidden when the account
  has no ION profile or the API cannot be reached.

| Minecraft | Loader   | Jar                                   |
|-----------|----------|---------------------------------------|
| 1.8.9     | Forge    | `ionclient-<version>-forge-1.8.9.jar`    |
| 1.21.1    | Fabric   | `ionclient-<version>-fabric-1.21.1.jar`  |
| 1.21.1    | NeoForge | `ionclient-<version>-neoforge-1.21.1.jar`|
| 1.21.1    | Forge    | `ionclient-<version>-forge-1.21.1.jar`   |

No other mods are required on any loader (not even Fabric API: the icon is loaded from the jar
directly instead of through the resource manager).

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
on its own, waits for the ping, writes `screenshots/ionclient-autotest.png` and quits. That is
how both displays are checked without a person at the keyboard. Dev sessions are offline and
have no ION profile, so `IONCLIENT_DEV_UUID` and `IONCLIENT_DEV_NAME` point the coin lookup at
a real account.

The Forge 1.21.1 dev run does not launch under Architectury Loom 1.11 (FML's
`ImmediateWindowHandler` cannot find the `minecraft` module in the userdev game layer, with or
without the early window). The Forge 1.21.1 jar is therefore compile-verified only; its entry
code is the same as the Fabric and NeoForge jars, which were run.
