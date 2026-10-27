# 多版本、多加载器维护 / Multi-version maintenance

当前仓库同时维护两个独立安装包。Forge 与 NeoForge 是不同的游戏运行环境；这里的“共存”指同一源码仓库内共存，并非把两个加载器或两个安装包同时放进一个游戏实例。

## 参考项目与取舍

2026-10-27 查阅 APeng215 的两个仓库：

- [旧 Forge 仓库分支](https://github.com/APeng215/FiltPick-forge/branches)：以 `1.19.2`、`1.20.1`、`1.20.4` 等游戏版本分支隔离 API。
- [旧 Forge 标签](https://github.com/APeng215/FiltPick-forge/tags)：如 `v1.1.2-1.20.4`，绑定模组版本与 MC 版本，安装包名同时包含加载器。
- [新版设置](https://github.com/APeng215/FiltPick/blob/master/settings.gradle)：包含 `common`、`fabric`、`neoforge`。
- [新版共享源码配置](https://github.com/APeng215/FiltPick/blob/master/common/build.gradle)：共享源码/资源供加载器模块消费。

本项目采用与参考仓库相同的根目录模块分工：common、forge、neoforge、buildSrc。集成分支同时保留两套已适配平台；版本维护分支固定各自目标，标签固定对应发布源码。没有复制 FiltPick 的业务代码，也没有引入 Fabric 支持。

## 结构和约束

```text
common/                       纯 Java 17 控制算法、共享翻译、控制器测试
forge/         Forge 事件、咬钩反射、Aquaculture/Oculus Mixin
neoforge/      NeoForge 事件、咬钩 accessor Mixin
common/src/platformTest/      各平台上游接口契约（不发布）
common/src/simulation/        共享小游戏模拟夹具（不发布）
targets.json                  每个目标的 MC/加载器/JDK/上游文件和 SHA-512
buildSrc/src/main/groovy/     multiloader-common / loader / verification 约定插件
gradle/wrapper/               Gradle Wrapper
```

控制算法不导入 Minecraft、Forge 或 NeoForge。每个平台重新编译共享源码并装入自己的 JAR，玩家无需另外安装 common。各平台独立使用 `build/`、`run/client/`；共享 Gradle 下载缓存是正常行为。

MOD ID 的唯一配置位于根 `gradle.properties`。生成的 `ModIdentity` 提供入口常量，TOML 使用同一配置展开。更改 ID 时仍须同步资源命名空间、翻译键及 Mixin 文件名；`verifyDistribution` 会检查最终 JAR。

## 构建和验证

Gradle Wrapper 9.2.1 使用 JDK 21 启动；另外需要 JDK 17 以编译/运行 Forge 目标。可由 Gradle toolchain 自动下载，或在本机用户配置中指定 JDK 路径。不要提交私人路径或代理。

```powershell
.\gradlew.bat build
.\gradlew.bat collectArtifacts
# 只配置、构建一个平台，另一个平台不参与：
.\gradlew.bat build '-Ptarget=forge-1.20.1'
.\gradlew.bat build '-Ptarget=neoforge-1.21.1'
# 启动独立开发客户端：
.\gradlew.bat :forge:runClient '-Ptarget=forge-1.20.1'
.\gradlew.bat :neoforge:runClient '-Ptarget=neoforge-1.21.1'
```

安装包位于 `forge/build/libs/` 与 `neoforge/build/libs/`；汇总目录为根 `build/releases/`，包含按平台分隔的安装包、源码包和 SHA256SUMS.txt。只安装不带 `-sources` 的 JAR。

`build` 包含：共享控制器测试、每个平台的上游接口检查、16 项咬钩夹具检查、1000 局真实上游小游戏字节码模拟、依赖哈希及生产 JAR 检查。NeoForge 另有真实 Minecraft 字节码的 accessor Mixin 注入检查。夹具替代 UI/声音/网络，不代表完整游戏实测；Forge 岩浆测试中的反射辅助也是夹具，真实映射和可选渲染组合仍需游戏验证。

## 真实客户端检查

可执行以下命令启动独立测试客户端，检查模组注册、F8 和图标，并自动退出：

```powershell
.\gradlew.bat :forge:runClient '-Ptarget=forge-1.20.1' -PclientSmoke
.\gradlew.bat :neoforge:runClient '-Ptarget=neoforge-1.21.1' -PclientSmoke
# 加上 -PclientSmokeWorld：另建独立测试世界，验证实际钓鱼、安全停机。
```

测试模块位于 `src/clientSmoke`，只在指定参数时注册，不进入任何发布包；检查结果写入各模块 `run/client/`。

## 本机发布目录与图标

用户指定的本机发布目录为 `D:\MODS\StardewFishingAutofish`。路径保存在忽略提交的 `local.properties` 的 `releaseDir` 中；其他机器可设置自己的路径或使用 `-PreleaseDir=<folder>`。

运行 `.\gradlew.bat publishLocal`，会先构建和验证，再复制到发布目录的 `<mod_version>/`，附上说明、许可、原始图标和带 SHA-256 的 `release-manifest.json`。此任务不会清空发布目录或删除旧版本。

图标源文件为用户提供的 PNG，原样位于 `common/src/main/resources/assets/stardewfishingautofish/icon.png`。两版 TOML 的 `logoFile` 均引用该资源，生产包校验会确认图标字节与原文件一致。

## Branch 与 Tag

- `main`：所有受支持目标的集成主线。
- `codex/multiloader-foundation`：本次开发分支，验证后快进合并到本地主线。
- `codex/1.20.1-forge`、`codex/1.21.1-neoforge`：版本维护分支，固定各自的 `targets.json`；根目录布局保持相同，对应标签指向各自提交。
- 标签格式：`v<mod_version>-mc<minecraft_version>-<loader>`，例如 `v1.4.0-mc1.20.1-forge`、`v1.4.0-mc1.21.1-neoforge`。同一仓库明确标出加载器，避免文件归属不清。
- Tag 是已提交源码的不可变发布快照。不要在未提交改动上打发布标签，不要移动已公开标签。

完成审阅、提交以及游戏验证后，可按实际平台执行：

```powershell
.\gradlew.bat build verifyReleaseTag '-Ptarget=forge-1.20.1' '-PreleaseTag=v1.4.0-mc1.20.1-forge'
git tag -a v1.4.0-mc1.20.1-forge -m "1.4.0 / Minecraft 1.20.1 / Forge"
# 明确决定发布后再推送该标签：
git push origin v1.4.0-mc1.20.1-forge
```

CI 从 `targets.json` 生成矩阵，常规提交验证全部平台；上述标签只选择匹配的平台，并校验标签与源码版本一致。CI 仅上传构建附件，不自动向平台发布。本次修正会完成本地源码提交、版本分支及标签；远程发布由维护者执行。

## 新版本接入流程

1. 确认目标版本上游 Stardew Fishing 已提供相应加载器文件；核实其 API、依赖和小游戏行为。
2. 在相应版本维护分支的根目录 `<loader>/` 中增加适配代码、该版元数据、pack 格式及所需 Mixin。不要通过放宽 MC 范围宣称兼容。
3. 在 `targets.json` 登记目标 ID、根目录加载器模块名、Java 版本、加载器版本和经过上游核实的文件 ID/SHA-512。同一加载器跨 MC 版本由分支隔离，避免多份嵌套模块。
4. 保持平台导入、测试夹具、运行目录互相隔离；版本专用补丁逐项核实。新旧字节码或包结构不同时，升级该目标的构建插件/Wrapper，必要时再拆维护分支。
5. 通过该目标构建、接口契约、模拟、安装包检查，并在游戏内完成抛竿、咬钩、宝箱、岩浆、后台切换及退出世界验证，再扩大支持范围。

当前没有实现 1.19.2、1.21.11、26.x 或 Fabric。已有结构是增加这些适配的基础，不是它们已受支持的声明。
