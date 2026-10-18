# 黄队的奇妙大冒险 (Sepker Mod)

Mindustry 模组：一个自定义星球「**赛普克尔 / Sepker**」，围绕铀矿展开的一套生产链、单位与方块。

A Mindustry mod adding a custom planet **Sepker**, with a uranium-centred production chain, units and blocks.

## 当前版本 / Current version

**V0.6.10** — `minGameVersion: 159`。

## 安装 / Install

1. 下载仓库根目录下的 `黄队的奇妙大冒险V<版本>.jar`
2. 打开游戏 → 主菜单「模组」→「导入模组」→ 选择该 jar
3. 重启或回到主菜单，进入「**赛普克尔**」星球开始

> Android 用户如果从浏览器下载，jar 可能被改成 .zip，改回 .jar 即可。

## 目录结构 / Layout

| 路径 | 说明 |
|---|---|
| `src/com/Sepker/` | mod 源码（包名 `com.sepker`） |
| `src/com/Sepker/blocks/` | 方块实现，一类一个文件 |
| `src/com/Sepker/content/` | 内容注册：物品、方块、科技树、星球、扇区 |
| `src/com/Sepker/units/` | 单位与 AI |
| `src/com/Sepker/logic/` | 世界处理器指令扩展 |
| `src/com/Sepker/ui/` | 自定义 UI |
| `assets/bundles/` | 中英文文本（`bundle.properties` = 英文，`bundle_zh_CN.properties` = 中文） |
| `assets/sprites/`、`assets/sprites-override/` | 贴图（每世界单位 4 像素） |
| `assets/maps/` | 扇区地图 |
| `assets/changelog.txt` | 更新日志（中英双语） |
| `mod.hjson` | mod 元信息 |

## 构建 / Build

需要 JDK 17 + Android `d8`，以及 `Mindustry.jar`（游戏本体，用作编译 classpath，不入库）。

```bash
V="0.6.10"

find src -name "*.java" | sort > sources.txt
rm -rf build && mkdir build
javac --release 17 -d build -classpath ".:mindustry.jar" @sources.txt

rm -f classes.jar SepkerMod.jar "黄队的奇妙大冒险V$V.jar"
jar cfM classes.jar -C build com
rm -rf .dexout && mkdir -p .dexout
d8 --min-api 28 --output .dexout classes.jar

mkdir .pkg
cp -a build/com .pkg/ && cp -a assets/. .pkg/ && cp mod.hjson icon.png .pkg/ && cp .dexout/classes.dex .pkg/
cp -f .dexout/classes.dex .
( cd .pkg && zip -r -X -q ../SepkerMod.jar . && zip -r -X -q "../黄队的奇妙大冒险V$V.jar" . )
rm -rf .pkg .dexout
```

打包要点（Android 必需）：

- jar 根下只放 `com/` 与平展后的资源，**不要** `assets/`、`build/`、`META-INF/` 层级
- 必须包含 `classes.dex`（`--min-api 28` → dex 039）
- 校验：jar 内 class 数 == dex 的 class_defs_size == build/ class 数
