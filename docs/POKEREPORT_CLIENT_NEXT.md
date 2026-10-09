# POKEREPORT CLIENT NEXT-GEN SPECIFICATION & BUNDLE ARCHITECTURE

## 1. Release Candidate Scope
- **Candidate Bundle Version**: `0.2.1-rc1` (or `0.2.0-rc2`)
- **Compatibility**: Minecraft `1.21.1`, Fabric Loader `0.16.9` / `0.18.4`, Java 21 LTS
- **Core Mod**: `LunaEternal 0.1.0` (Staged Artifact SHA-256: `7DAF6E8988BD3FE5287A90905E4759E8A6E1D598EDED2834F8D51A67ADEFBDE4`)
- **Modpack Format**: Modrinth `.mrpack` Specification v1.0 + Direct Launcher Manifest (`client-latest.json`)

---

## 2. Client File Hierarchy & Bundle Layout

```text
PokeReport-Client-Candidate/
├── mods/
│   ├── lunaeternal-0.1.0.jar (Hardened battle fix)
│   ├── Cobblemon-fabric-1.8.0+1.21.1.jar
│   ├── rct-0.19.0-beta.jar
│   ├── rctapi-0.16.0-beta.jar
│   ├── sodium-fabric-0.6.13+mc1.21.1.jar
│   ├── iris-1.8.8+1.21.1-fabric.jar
│   ├── immediatelyfast-fabric-1.6.10+1.21.1.jar
│   ├── ferritecore-7.0.3-fabric.jar
│   ├── modernfix-fabric-5.25.1+mc1.21.1.jar
│   ├── entityculling-fabric-1.10.1-mc1.21.1.jar
│   ├── badorptimizations-2.4.1.jar
│   ├── bobby-5.2.4.1.jar
│   ├── chat_heads-0.13.19-fabric-1.21.1.jar
│   ├── ChatAnimation-fabric-1.21-1.0.3.jar
│   ├── jade-fabric-1.21.1-15.10.1.jar
│   ├── presencefootsteps-1.21.1-1.10.4.jar
│   └── ... (base utilities & cloth-config)
├── config/
│   ├── cobblemon.json
│   ├── sodium-options.json
│   ├── jade/
│   ├── chat_heads.json
│   └── presencefootsteps/
└── resourcepacks/
    └── PokeReport-Cosmeticos.zip
```

---

## 3. Launcher Manifest Specification (`client-latest.json`)

```json
{
  "version": "0.2.1-rc1",
  "minecraft": "1.21.1",
  "fabric_loader": "0.16.9",
  "release_timestamp": "2026-10-08T20:55:00Z",
  "channel": "release-candidate",
  "artifact": {
    "url": "https://launcher.pokereport.net/bundles/PokeReport-Client-0.2.1-rc1.zip",
    "sha256": "<CALCULATED_SHA256>",
    "size_bytes": 154829100
  },
  "modules": [
    {
      "name": "lunaeternal",
      "path": "mods/lunaeternal-0.1.0.jar",
      "sha256": "7daf6e8988bd3fe5287a90905e4759e8a6e1d598eded2834f8d51a67adefbde4",
      "size": 108710545
    }
  ]
}
```

---

## 4. Verification & Testing Procedures

### A. Clean Install Procedure
1. Create new empty profile directory in Launcher: `.pokereport-test/instances/clean-test/`.
2. Apply `PokeReport-Base-0.2.1.mrpack`.
3. Launch game with vanilla memory settings (4GB RAM).
4. Assert: Main menu loads in under 20s, Cobblemon title screen visible, singleplayer world creates without crash.

### B. Upgrade Install Procedure
1. Take existing `0.2.0` client directory.
2. Run differential updater: replaces `lunaeternal-0.1.0.jar` with hash `7DAF6E...`.
3. Assert: Existing keybindings, waypoints (`xaerominimap.txt`), and options are preserved.

### C. Digital Signature & Verification Gate
- Manifest is signed using the PokeReport Ed25519 private key (`tools/SignManifest.java`).
- Signature stored in `client-latest.json.sig`.
- Launcher verifies signature before initiating automated download.
- **DEPLOYMENT STATUS**: **Staged locally in `build/release-candidate/client/`. Live publish blocked until owner authorization.**
