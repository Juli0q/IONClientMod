#!/usr/bin/env python3
"""Regenerates assets/ionclient/sounds/legacy_names.txt, the modern -> 1.8 sound name table.

The table says what a 1.8.9 client should play when a server sends it a sound event name from a
newer version. It is derived from the data ViaVersion and ViaBackwards use for the same job, so a
modern name is played as exactly the sound a 1.8 player hears on a Via-proxied modern server:

  * ViaVersion/Mappings  mappings/mapping-<new>to<old>.json  "sounds": renames and the
    substitutes ViaBackwards picked for sounds the older version lacks, one step per version
  * ViaVersion/Mappings  mappings/mapping-<version>.json     "sounds": the sound list of a version
  * Mojang's 1.12.2 asset index, for the sound names of 1.12.2 as that client spells them: the
    Mappings repository lists 1.12 with the names 1.13 renamed them to (its 1.12 -> 1.13 step
    matches by name), while its 1.12 -> 1.11 step is written in the 1.12 spelling. Both lists
    are in registry order, which is alphabetical, so the two spellings pair up by position.

Each name is resolved from the newest version that has it, one step down at a time, until it
reaches a 1.8 name. Names that end in nothing (dropped by Via as well) are left out.

Usage: tools/legacy_sound_names.py [--cache DIR]
The mapping files are downloaded into DIR (default: ~/.cache/ionclient/via-mappings) and reused.
"""
import argparse
import json
import os
import sys
import urllib.request

RAW = "https://raw.githubusercontent.com/ViaVersion/Mappings/main/mappings/"
VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
RESOURCES = "https://resources.download.minecraft.net/"

# Newest first; each neighbour pair has a mapping-<new>to<old>.json in the Mappings repository
# (1.13.2 -> 1.13 has none: the sounds did not change).
CHAIN = [
    "26.4", "26.3", "26.2", "26.1",
    "1.21.11", "1.21.9", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.2", "1.21",
    "1.20.5", "1.20.3", "1.20.2", "1.20",
    "1.19.4", "1.19.3", "1.19",
    "1.18", "1.17", "1.16.2", "1.16", "1.15", "1.14", "1.13.2", "1.13",
    "1.12", "1.11", "1.10", "1.9.4", "1.9", "1.8",
]

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                   "assets", "ionclient", "sounds", "legacy_names.txt")


def fetch(cache, name, optional=False):
    path = os.path.join(cache, name)
    if not os.path.exists(path):
        try:
            with urllib.request.urlopen(RAW + name, timeout=60) as response:
                data = response.read()
        except urllib.error.HTTPError as e:
            if optional and e.code == 404:
                return None
            raise
        with open(path, "wb") as f:
            f.write(data)
    with open(path, "rb") as f:
        return json.load(f)


def fetch_json(url):
    with urllib.request.urlopen(url, timeout=60) as response:
        return json.load(response)


def mojang_sound_names(cache, version):
    """The sound event names of a Minecraft version, from its asset index's sounds.json."""
    path = os.path.join(cache, f"sounds-{version}.json")
    if not os.path.exists(path):
        manifest = fetch_json(VERSION_MANIFEST)
        version_json = fetch_json(next(v["url"] for v in manifest["versions"] if v["id"] == version))
        index = fetch_json(version_json["assetIndex"]["url"])
        digest = index["objects"]["minecraft/sounds.json"]["hash"]
        sounds = fetch_json(f"{RESOURCES}{digest[:2]}/{digest}")
        with open(path, "w") as f:
            json.dump(sorted(sounds), f)
    with open(path) as f:
        return json.load(f)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", default=os.path.expanduser("~/.cache/ionclient/via-mappings"))
    args = parser.parse_args()
    os.makedirs(args.cache, exist_ok=True)

    lists = {v: fetch(args.cache, f"mapping-{v}.json")["sounds"] for v in CHAIN}
    names = {v: set(lists[v]) for v in CHAIN}
    steps = {}
    for new, old in zip(CHAIN, CHAIN[1:]):
        diff = fetch(args.cache, f"mapping-{new}to{old}.json", optional=True) or {}
        steps[new] = dict(diff.get("sounds") or {})

    # Put 1.12 back into its own spelling: pair the repository's 1.13-spelled 1.12 list with the
    # real one by position, then route the 1.13 -> 1.12 step through those renames.
    real_1_12 = mojang_sound_names(args.cache, "1.12.2")
    if len(real_1_12) != len(lists["1.12"]):
        sys.exit("the 1.12 sound lists differ in length; the mapping data changed shape")
    renamed = {new: old for old, new in zip(real_1_12, lists["1.12"]) if old != new}
    names["1.12"] = set(real_1_12)
    steps["1.13"] = {k: renamed.get(v, v) for k, v in steps["1.13"].items()}
    steps["1.13"].update(renamed)

    real_1_8 = mojang_sound_names(args.cache, "1.8.9")
    if set(real_1_8) != names["1.8"]:
        sys.exit("the 1.8 sound list no longer matches Minecraft 1.8.9")

    def resolve(name, start):
        for new, old in zip(CHAIN[start:], CHAIN[start + 1:]):
            if name in steps[new]:
                name = steps[new][name]
            elif name not in names[old]:
                return None  # inconsistent data: the name vanished without a mapping
            if not name:
                return None  # Via drops it
        return name

    table = {}
    for i, version in enumerate(CHAIN[:-1]):
        for name in sorted(names[version]):
            if name in table or name in names["1.8"]:
                continue
            legacy = resolve(name, i)
            if legacy:
                table[name] = legacy

    with open(OUT, "w") as out:
        out.write("# Modern sound event names and the 1.8.9 sound each one is played as.\n")
        out.write("#\n")
        out.write("#   <modern name> = <1.8.9 name>\n")
        out.write("#\n")
        out.write("# Generated by tools/legacy_sound_names.py from the ViaVersion mapping data, so a modern\n")
        out.write("# name sounds the same here as it does for a 1.8 player on a Via-proxied modern server.\n")
        out.write(f"# Covers the sound names of Minecraft 1.9 to {CHAIN[0]}. Do not edit by hand.\n\n")
        for name in sorted(table):
            out.write(f"{name} = {table[name]}\n")
    print(f"{len(table)} names written to {os.path.normpath(OUT)}", file=sys.stderr)


if __name__ == "__main__":
    main()
