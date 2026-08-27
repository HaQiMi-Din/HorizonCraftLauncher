<h1 align="center">🌅 Horizon Craft Launcher</h1>

<p align="center">
  <b>跨平台 Minecraft Java 版启动器</b><br>
  Material Design 3 · 顶部 Dock · HMCL 式二级导航 · 高度自定义
</p>

<p align="center">
  <a href="https://github.com/HaQiMi-Din/HorizonCraftLauncher/actions"><img src="https://github.com/HaQiMi-Din/HorizonCraftLauncher/actions/workflows/android.yml/badge.svg" alt="Android CI"></a>
</p>

---

## ✨ 项目定位

Horizon Craft Launcher（地平线方块启动器）是一款全新 UI 的 Minecraft Java 版启动器：

- **交互范式**：顶部常驻 Dock 一级导航（启动 / 版本 / 下载 / 整合包 / 账户 / 设置），点击后下方内容区占满剩余屏幕；部分页面内部自带 HMCL 式二级侧边栏。
- **视觉体系**：Material Design 3 + Dock栏 大圆角、tonal 卡片、柔和层次；深色 / 浅色主题、自定义强调色、Android 12+ 动态取色（Material You）。
- **横屏平板优化**：界面默认横屏，充分利用宽屏幕做双栏布局。
- **规划平台**：Android（当前）/ Windows / Linux（未来），全平台统一品牌与交互。

## 🔧 技术架构

- Android 端底层基于 [Amethyst‑Android](https://github.com/PojavLauncherTeam/pojavLauncher)（已归档）`v3_openjdk` 分支，**完全复用游戏启动、版本下载、账号、模组安装等核心逻辑**，仅重写上层 UI 外壳。
- 单 Activity + 多 Fragment 架构：`LauncherActivity` 持有全局顶部 Dock，下方 `FragmentContainerView` 装载页面。
- 主题：`Theme.Material3.DayNight` 派生，配合自定义 MD3 色彩角色（primary / container / surface 体系）。
- 全部自定义配置存入原有 `LauncherPreferences`，不新增冗余配置文件。

## 📲 Android 构建（GitHub Actions）

本项目使用上游原有的 GitHub Actions 工作流构建，无需本地环境即可产出 APK：

1. Fork / clone 本仓库；
2. 在你 fork 的 **Actions** 页面手动触发或直接 push 代码触发构建；
3. 构建完成后在 **Actions → 最新一次运行 → Artifacts** 下载 APK。

本地构建（需 JDK 17 + Android SDK 34）：
```bash
./gradlew :app_pojavlauncher:assembleDebug
```

## 🗺️ 路线图

- [x] 顶部 Dock 导航（启动 / 版本 / 下载 / 整合包 / 账户 / 设置）
- [x] MD3 + GNOME 视觉主题（深色/浅色、自定义强调色、动态取色）
- [x] 设置页 HMCL 式二级侧边导航（外观 / 布局 / 启动 / 高级）
- [ ] 各页面与 Pojav 底层功能完整打通
- [ ] 图标、包名、发布签名规范化
- [ ] Windows / Linux 版本

## 📄 开源许可

- 本项目 UI 外壳遵循 [GPL‑3.0](LICENSE)。
- Android 内核基于 Amethyst‑Android / PojavLauncher，遵循其原始开源许可（GPL‑3.0）。
- Minecraft 是 Mojang Synergies AB 的商标，本项目与 Mojang / Microsoft 无关。
