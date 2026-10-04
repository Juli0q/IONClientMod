# Third-party notices

## LLLLLwjgl3

This module is derived from
[xiaoyu1738/LLLLLwjgl3-1.8.9-Forge](https://github.com/xiaoyu1738/LLLLLwjgl3-1.8.9-Forge),
commit `689a96c9eaa6fbe00fc0b57c9f9f5194d440449f`, licensed under the GNU General Public
License v3.0 or later and used here under GPL-3.0 as part of ION Client (see `LICENSE`).
The notices below are carried over from it. ION Client's changes:

- Built as the Gradle subproject `mc-1.8.9-lwjgl3` instead of with ForgeGradle 2.1 and merged
  into the ION Client 1.8.9 jar, which declares the coremod in its manifest. Only the Linux natives
  are bundled, NanoVG and STB are dropped, and the separate `@Mod` and `mcmod.info` are removed.
- `org.lwjglx.opengl.Display` reports the framebuffer size and the window scale, and
  `GLFWMouseImplementation` converts cursor positions to framebuffer pixels, so scaled
  (HiDPI / fractional) Wayland outputs fill the window and clicks line up.
- Native Wayland is the default backend (`PlatformInfo.DEFAULT_WAYLAND_IME`).
- `Lwjgl3ClassTransformer` turns off Forge's loading screen, which needs LWJGL2's
  `SharedDrawable`.
- The coremod only activates on Linux, by default only in Wayland sessions;
  `-Dionclient.lwjgl3=true|false` overrides that on Linux (`PlatformInfo.shouldActivate`).

The jar also contains the following third-party components under their own licenses,
all of which are GPL-3.0 compatible.

## legacy-lwjgl3 source

The vendored compatibility sources under `src/main/java/org/lwjglx/` and
`src/main/java/com/github/zarzelcow/legacylwjgl3/` are derived from
[Zarzelcow/legacy-lwjgl3](https://github.com/Zarzelcow/legacy-lwjgl3), commit
`78643b2a9621ab04d13e3ec0a222874d793516d2`. They are included as source rather
than consumed as a binary facade, and are distributed under the GNU LGPL v2.1
or later (`licenses/LGPL-2.1.txt`).

## MC-LWJGL3 compatibility API

The sources under `src/main/java/org/lwjglx/` (the `openal` package, `Sys`,
`LWJGLUtil` and `LWJGLException`) are derived from
[gudenau/MC-LWJGL3](https://github.com/gudenau/MC-LWJGL3), commit
`026bb8683732ad1b7362e65b252e5d90e309613f`, licensed under the GNU LGPL v3
(`licenses/LGPL-3.0.txt`).

## LWJGL 2

`org.lwjglx.BufferUtils` and parts of the API surface reproduced by the
compatibility layer originate from LWJGL 2, Copyright (c) 2002-2008
Lightweight Java Game Library Project, licensed under the BSD 3-Clause
license (`licenses/BSD-3-Clause-LWJGL.txt`).

## LWJGL 3

LWJGL 3.3.3 Java bindings and platform natives (GLFW, OpenAL Soft, and
others) are obtained from Maven Central (`org.lwjgl`) and licensed under the
BSD 3-Clause license. The upstream JARs carry no license file, so the text is
provided in `licenses/BSD-3-Clause-LWJGL.txt`.

The bundled natives include libraries under their own licenses:

- GLFW: zlib/libpng license (`licenses/Zlib-GLFW.txt`).
- OpenAL Soft: GNU LGPL v2 or later (`licenses/LGPL-2.0-OpenAL-Soft.txt`).

The ION Client jar carries this file and the `licenses/` directory under
`META-INF/lwjgl3/`.
