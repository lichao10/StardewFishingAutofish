# Third-party compatibility notes

Own addon code is MIT, authored by liu_li_chao_xi. This is an unofficial client addon for Stardew Fishing, not an official Minecraft, Stardew Valley or Stardew Fishing release.

Required game, loader and upstream files are listed in targets.json. No Minecraft binaries, dependency JARs or decompiled upstream sources are bundled. Upstream: https://github.com/BrownBear85/stardewfishing .

The NeoForge 1.20.1 client adapter provides small original compatibility bridges for ResourceLocation factories and the legacy mod loading context required by Stardew Fishing 3.7. It does not rewrite or bundle the upstream JAR.

The shared Forge/NeoForge 1.20.1 adapter includes the optional Aquaculture/Oculus line separator patch. The technique follows Oculus 1.8's vanilla fishing-line delimiter; third-party license requirements must be reviewed before publication. The NeoForge 1.21.1 adapter does not include this renderer patch.

common/src/platformTest, common/src/simulation and platform test source sets contain headless fixtures, excluded from install and sources JARs. Simulations run each target's upstream minigame bytecode with fixtures for UI, sound and networking. Most addon code was generated with AI assistance.

FiltPick was consulted for repository/build organization; no implementation code was copied. References: https://github.com/APeng215/FiltPick and https://github.com/APeng215/FiltPick-forge .
