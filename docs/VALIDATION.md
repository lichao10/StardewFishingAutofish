# 1.4.0 验证记录

日期：2026-10-27（Asia/Shanghai）。本记录针对当前多版本工程及新 MOD ID `stardewfishingautofish`，不是历史版本的测试结论。

## 布局与发布

根目录实际模块为 common/、forge/、neoforge/、buildSrc/；已移除 versions/、verification/ 的旧布局。共享构建使用 buildSrc 约定插件。用户提供的 1254×1254 PNG 原样保存到共享 assets/stardewfishingautofish/icon.png，SHA-256 为 49407829d31afc62d48ce3480dd47c2c9ce6c3581c2b0429565ac29b74cdd207。

本机发布路径使用 local.properties 的 releaseDir 配置为 D:\MODS\StardewFishingAutofish，publishLocal 先构建验证再复制到版本号子目录。

## 构建与适配隔离

- `clean build collectArtifacts` 成功：两平台从清理后的输出目录构建，Gradle 正常复用已验证的编译/测试缓存。
- Gradle 使用本机 JDK 21.0.9；Forge 目标使用 JDK 17.0.1，生产字节码为 Java 17；NeoForge 目标字节码为 Java 21。
- 分别使用 `-Ptarget=forge-1.20.1`、`-Ptarget=neoforge-1.21.1` 的完整构建与正确标签校验均成功。
- 错误目标 `forge-1.21.1` 被拒绝；Forge 目标配 NeoForge 发布标签被拒绝。
- 执行了 CI 文件中的实际 Python 计划代码：普通分支选择两目标，两种发布标签分别选择一个目标，未适配版本标签被拒绝。GitHub Actions 远程任务尚未运行。
- Stardew Fishing 两个文件的 SHA-512 均与 Modrinth 公布值一致；两个 3.7 文件的 ID 分别为 `UgWzAtGu` 和 `iJkNXCRB`。
- 最终生产 JAR 校验通过（包含原样图标和 logoFile 引用）：TOML 加载器隔离、运行时 MOD ID、MC/依赖元数据展开、两种语言、Mixin 配置、Forge refmap、Java class 版本、无旧资源命名空间或上游/测试类混入。Forge 检查对象是 `reobfJar` 输出的可安装包。
- 汇总目录内全部 JAR 的 ZIP 完整性、JSON 及 SHA-256 校验通过。
- NeoForge 实现由原独立工程迁入；本次修改与维护集中在当前仓库。

## 自动化验证

- 共享控制器 JUnit：4/4。
- Forge 上游字节码接口契约：3/3；NeoForge：3/3。合计 10 项 JUnit，无失败、无跳过。
- 各平台 16 项咬钩夹具检查通过，覆盖原版、Aquaculture 类型、岩浆类型、继承和状态重置；合计 32 项。
- NeoForge：生产包中的 accessor Mixin 成功应用于实际 Minecraft 1.21.1 FishingHook 字节码，正确读取一次同步咬钩字段。
- Forge：可选 Aquaculture/Oculus 渲染补丁保留，refmap 中 PoseStack 目标映射已生成并包含在生产包；本次未执行该可选组合的真实渲染注入或双人实测。

以下使用各目标固定依赖中的真实 FishingMinigame 字节码，替换 UI、声音、网络夹具，按固定种子各运行 1000 局。存在宝箱 800 局，其中普通箱和金箱各 400 局。

| 目标 | 成功钓鱼 | 同时获得宝箱 | 宝箱未满提前收鱼 | 超时 |
|---|---:|---:|---:|---:|
| 1.20.1 forge | 988/1000 | 788/800 | 0 | 0 |
| 1.21.1 neoforge | 988/1000 | 788/800 | 0 | 0 |

每个平台普通箱 393/400、金箱 395/400；无宝箱的鱼 200/200。高难鱼仍可能失败。这些结果验证控制策略及接口，不能证明正式客户端、网络、渲染和所有可选模组组合均已正常运行。

## 真实客户端与独立测试世界

- Forge 与 NeoForge 均完成真实客户端启动：到达 TitleScreen，检查实际 ModList 注册、新 MOD ID、F8 KeyMapping、活动资源管理器中的图标 PNG 后自动退出。
- Forge 与 NeoForge 各自在独立测试世界完成两次实际小游戏并获得奖励，自动抛竿、咬钩收竿、小游戏输入、背包暂停、关闭时恢复失焦设置、低耐久停止检查通过。
- 截图中的 ResourcePackInfo 警告来自测试模块缺少 pack.mcmeta，已为两种加载器补齐；修正后的 Forge 启动不再出现该警告。
- 所有探针源码、测试模组元数据均处于 src/clientSmoke，仅在 -PclientSmoke 时注册，不进入发布 JAR。

## 仍需可选组合验收

已启动工程内的独立开发客户端和测试世界；未修改原整合包实例。安装匹配版本的新 JAR 前移除旧版辅助 MOD，尤其是 `SunlitAutoFish` / `sunlit_autofish`。新 ID 会使旧自定义快捷键失去绑定，重新设置即可，默认仍是 F8。

仍需在用户实际整合包中确认手动改键、窗口最小化、岩浆鱼竿及 Aquaculture + Oculus 双人鱼线；真实普通/金宝箱组合也建议手动复核。宝箱策略已由每个平台 1000 局模拟覆盖。

未验证或宣称支持 1.19.2、1.21.11、26.x、Fabric 或单个游戏实例同时加载 Forge 和 NeoForge。

## 产物与日志

- 可安装 JAR 与源码包：根 `build/releases/` 按平台分目录，校验文件 `SHA256SUMS.txt`。
- 模拟详细数据：各平台 `build/simulation/treasure-1000.csv` 与 `.summary.txt`。
- JUnit 报告：`common/build/reports/tests/test/` 与各平台 `build/reports/tests/test/`。
- 本机最终构建日志：`.gradle/final-layout-release.log`（忽略提交）；真实客户端日志为 `.gradle/forge-world-smoke-3.log`、`.gradle/neoforge-world-smoke.log`。
- 启动及实际世界检查结果：各模块 `run/client/client-smoke-result.txt`、`world-smoke-result.txt`。
- 本地源码与版本管理：`main` 为双平台集成主线；`codex/1.20.1-forge`、`codex/1.21.1-neoforge` 固定各自目标，并分别使用 `v1.4.0-mc1.20.1-forge`、`v1.4.0-mc1.21.1-neoforge` 标签。未推送远程。
