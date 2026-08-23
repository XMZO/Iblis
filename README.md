# Iblis:Reboot

Iblis:Reboot（Iblis：重启）是原 Iblis 的 Minecraft 1.20.1 Forge 移植与维护版本。

本仓库在同一 `1.20.1-forge` 分支维护两个独立模组标识：

- `iblis`：技能成长、武器、材料与生存机制。
- `iblis_headshots`：可配置的爆头判定、伤害、防护、粒子与进度。

它们保留各自的 modid，并未合并为单一模组；当前由同一源码与发布包共同分发。

## 运行环境

- Minecraft 1.20.1
- Forge 47+
- Java 17

## 构建

Windows：

```powershell
.\gradlew.bat build
```

其他平台：

```bash
./gradlew build
```

构建产物位于 `build/libs`。

## 致谢与许可

原版 Iblis 由 Foghrye4 创作；Iblis:Reboot 由 XMZO 继续移植和维护。项目依照 [MIT License](LICENSE) 发布。
