# Changelog

## 1.4.0 — multi-loader foundation

- Match the reference root layout: common/, forge/, neoforge/ and buildSrc/ convention plugins.
- Include the supplied icon in both mod lists and provide publishLocal for the configured local release folder.
- Consolidate Minecraft 1.20.1 Forge and 1.21.1 NeoForge adapters in one repository.
- Share Java-only bar/treasure control and translations; preserve loader-specific event APIs and Mixins.
- Rename MOD ID from `sunlit_autofish` to `stardewfishingautofish`, including language keys and resource/Mixin paths. Remove the old JAR before upgrading; custom key bindings must be set again.
- Share the newer wide-bar treasure escape fix and its regression test across both targets.
- Pin each target's exact Stardew Fishing 3.7 file and SHA-512; add build matrix, release tag validation, isolated outputs and packaged-JAR checks.
- Keep both targets client-only; F8 toggles automatic casting, bite response, minigame control, treasure prioritization and background fishing.

See docs/VALIDATION.md for actual verification results and remaining in-game checks.
