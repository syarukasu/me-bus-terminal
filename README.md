# ME Bus Terminal

Manage AE2 storage, import and export buses from a cable-mounted terminal or its wireless counterpart. Minecraft 1.20.1 / Forge and 1.21.1 / NeoForge share the interface and behavior.

- Search by bus name, item ID, attached block, coordinates and configured filter items.
- Edit ghost filters, copy/paste settings, change priority and supported bus options.
- Select multiple buses to apply compatible changes together.
- Display each bus with its own installed item icon.
- Optional ME Subnet Bus support: nine filters, priority, mode, reverse direction and exclusion settings. Its tab uses the installed mod's icon and is hidden when absent.
- AE2-derived buses, including CrazyAE import/export buses, retain their own names and icons. Addons with unrelated APIs need dedicated integration.
- Wireless access uses native AE2 links, access points, range, energy and permissions.

Applied Energistics 2 is the only required mod. Install the matching jar on both client and server. The mod ID remains `me_omnibus` to preserve existing worlds. Old full-block terminals can be picked up and placed again as cable parts.

Subnet buses are indexed on their host cable's grid. The terminal does not merge networks, transfer actual upgrade cards, or accelerate item transport.

## Downloads

Distribution jars, checksums and release notes are available in [GitHub Releases](https://github.com/syarukasu/me-bus-terminal/releases).
The mod is also submitted to [CurseForge](https://www.curseforge.com/minecraft/mc-mods/me-bus-terminal).

## Building From Source

Install JDK 17 and JDK 21, then run:

```text
./gradlew :forge-1201:build :neoforge-1211:build
```

Windows users can use `gradlew.bat`. Outputs are in each platform's `build/libs` directory.

## License

Original code: LGPL-3.0-only. AE2-derived assets retain their original CC BY-NC-SA 3.0 terms. See LICENSE and THIRD_PARTY_NOTICES.md.
