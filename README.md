# Touhou Little Maid: Tsumugi — 静态 Maven 仓库

面向 [`gege-tlph/TouhouLittleMaid-Tsumugi`](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi)
的静态 Maven 仓库，经 raw.githubusercontent.com 直供，无需任何认证。

```gradle
repositories {
    maven {
        name = "TouhouLittleMaid Tsumugi"
        url = "https://raw.githubusercontent.com/gege-tlph/TouhouLittleMaid-Tsumugi/maven/"
    }
}

dependencies {
    // Minecraft 1.21.11
    modCompileOnly "io.github.gege-tlph:touhoulittlemaid-fabric:0.8.8-neo1.5.3+mc1.21.11"

    // Minecraft 26.1.2
    // modCompileOnly "io.github.gege-tlph:touhoulittlemaid-fabric:1.0.23-beta.1+mc26.1.2"
}
```

## 当前版本

| 版本 | Minecraft | 性质 |
|---|---|---|
| `0.8.8-neo1.5.3+mc1.21.11` | 1.21.11 | 正式版 |
| `1.0.23-beta.1+mc26.1.2` | 26.1.2 | Beta |

## 两件需要知道的事

**主 jar 与 Release 页的那一份逐字节相同。** 这里的 jar 直接取自对应
[Release](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases) 的附件，不是另行构建的——
同一个版本号不应该存在两个不同的二进制。每个文件都附 `.md5` / `.sha1` / `.sha256` / `.sha512`。

**POM 不声明被打包进 jar 的那些库。** 本模组把若干第三方库重定位到
`com.github.tartaricacid.touhoulittlemaid.libs` 打进了 jar 内部；把它们列进 POM 会让使用方
再拉一份重复的。POM 只声明真实的外部要求：Fabric Loader、Fabric API、Forge Config API Port。

## 归属与许可

本项目是 [TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid)（TartaricAcid）的
Fabric 移植分支，早期基于 [TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)（Sh1roCu）
的 1.21.1 Fabric 移植继续开发。代码 MIT，美术资源 CC BY-NC-SA 4.0。
