# KaOneBlock / KaOneBlock

[English](#english) | [中文](#中文)

---

## 中文

## 项目介绍

KaOneBlock 是一个基于 Spigot/Paper 的 Minecraft 插件，实现了类似"空岛生存"(OneBlock)的游戏玩法。玩家从单一方块开始，每次破坏方块后会在原位置生成新的方块，同时包含阶段进度系统和丰富的宝箱奖励机制。

## 功能特点

### 核心功能
- **方块生成系统**: 玩家从初始方块开始游戏，破坏方块后在原位置生成新方块
- **阶段进度系统**: 分为多个游戏阶段（平原、下界、末地等），通过破坏方块数量推进阶段
- **宝箱系统**: 定期生成宝箱代替普通方块，提供丰富奖励
- **数据持久化**: 使用 SQLite 数据库存储玩家进度和方块信息

### 高级功能
- **多语言支持**: 支持中文和英文界面
- **ItemsAdder 集成**: 兼容 ItemsAdder 插件，支持自定义物品和方块
- **自定义配置**: 高度可配置的方块生成概率和宝箱内容
- **命令系统**: 丰富的命令集，支持服务器管理和调试

## 安装指南

### 前置要求
- Java 17 或更高版本
- Minecraft 1.18.2 (兼容 1.13+)
- Spigot/Paper 服务器
- [ItemsAdder] 插件 (可选，用于自定义物品和方块)

### 安装步骤
1. 下载最新版本的 KaOneBlock.jar
2. 将 JAR 文件放入服务器的 `plugins` 目录
3. 重启服务器或使用 `/reload` 命令
4. 配置插件（可选）

### 基本配置
- 调试模式: `config.yml` 中的 `debug: true/false`
- 日志设置: `config.yml` 中的 `log: true/false`
- 默认语言: `config.yml` 中的 `language: "zh_CN"` 或 `"en_US"`

## 使用指南

### 玩家命令
- `/kaoneblock start` - 在当前位置生成初始方块
- `/kaoneblock stop` - 移除当前世界的方块
- `/kaoneblock help` - 显示帮助信息

### 管理员命令
- `/kaoneblock reload` - 重载插件配置
- `/kaoneblock log [on/off]` - 启用或禁用日志
- `/kaoneblock debug [on/off]` - 启用或禁用调试模式
- `/kaoneblock set <玩家> <阶段>` - 设置玩家到指定阶段
- `/kaoneblock reset-stage` - 重置当前玩家的阶段
- `/kaoneblock debugchest` - 调试宝箱内容
- `/kaoneblock ia-status` - 显示 ItemsAdder 状态
- `/kaoneblock checkdb` - 检查数据库表结构

### 命令别名
- `/kob` 是 `/kaoneblock` 的简短别名

### 权限系统
- `kaoneblock.help` - 访问帮助命令
- `kaoneblock.start` - 开始游戏
- `kaoneblock.stop` - 停止游戏
- `kaoneblock.reload` - 重载配置
- `kaoneblock.log` - 管理日志
- `kaoneblock.debug` - 调试功能
- `kaoneblock.admin` - 高级管理功能

## 配置说明

### 方块列表配置 (blocks/*.yml)
每个阶段配置文件定义：
- `amount`: 进入下一阶段需要破坏的方块数
- `next`: 下一阶段配置文件名
- `message`: 进入阶段时的提示消息
- `chests`: 各种宝箱的生成概率
- `blocks`: 方块及其生成权重

### 宝箱配置 (chests/*.yml)
每个宝箱配置文件定义：
- `name`: 宝箱显示名称
- `amount.min/max`: 生成物品数量范围
- `items`: 物品列表，包括材质、数量范围和权重

## 开发指南

### 构建要求
- Java 17
- Gradle 7.0+
- Spigot API 1.18.2

### 构建步骤
```bash
# 克隆项目
git clone https://github.com/yourusername/KaOneBlock.git
cd KaOneBlock

# 构建项目
./gradlew build

# 找到构建的插件
# 构建产物位于 build/libs/KaOneBlock-1.0.jar
```

### 项目结构
```
src/main/java/org/katacr/kaOneBlock/
├── KaOneBlock.java          # 主插件类
├── BlockBreakListener.java  # 方块破坏事件监听器
├── BlockGenerator.java      # 方块生成器
├── StageManager.java        # 阶段进度管理
├── DatabaseManager.java     # 数据库管理
├── EnhancedChestManager.java # 宝箱系统管理
└── chest/                 # 宝箱相关类
    ├── ContainerItem.java
    ├── ContainerPoll.java
    └── EnhancedChestManager.java
```

## 常见问题

### Q: 如何添加自定义方块阶段？
A: 在 `plugins/KaOneBlock/blocks/` 目录下创建新的 `.yml` 文件，按照现有格式配置方块列表和概率。

### Q: 如何自定义宝箱内容？
A: 在 `plugins/KaOneBlock/chests/` 目录下修改或创建新的 `.yml` 文件，定义物品和权重。

### Q: 插件不兼容其他版本的 Minecraft？
A: 插件设计为兼容 Minecraft 1.13+，主要测试版本为 1.18.2。

### Q: 数据存储在哪里？
A: 数据存储在 SQLite 数据库文件 `plugins/KaOneBlock/data.db` 中。

## 贡献指南

欢迎贡献代码、报告问题或提出建议！请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 [MIT 许可证](LICENSE)。

## 更新日志

### v1.0
- 初始发布
- 实现基本方块生成和阶段系统
- 添加宝箱奖励机制
- 支持多语言和 ItemsAdder 集成
- 完整的命令和权限系统

---

## English

## Project Introduction

KaOneBlock is a Minecraft plugin based on Spigot/Paper that implements a "One Block" survival gameplay. Players start with a single block, and each time a block is broken, a new block is generated at the original location. The plugin includes a stage progression system and rich chest reward mechanisms.

## Features

### Core Features
- **Block Generation System**: Players start with an initial block, and new blocks are generated at the original location after breaking
- **Stage Progression System**: Multiple game stages (Plains, Nether, End, etc.), progressing through breaking blocks
- **Chest System**: Periodically generates chests instead of regular blocks, providing rich rewards
- **Data Persistence**: Uses SQLite database to store player progress and block information

### Advanced Features
- **Multi-language Support**: Supports Chinese and English interfaces
- **ItemsAdder Integration**: Compatible with ItemsAdder plugin for custom items and blocks
- **Custom Configuration**: Highly configurable block generation probabilities and chest contents
- **Command System**: Rich command set for server management and debugging

## Installation Guide

### Prerequisites
- Java 17 or higher
- Minecraft 1.18.2 (compatible with 1.13+)
- Spigot/Paper server
- [ItemsAdder] plugin (optional, for custom items and blocks)

### Installation Steps
1. Download the latest version of KaOneBlock.jar
2. Place the JAR file in your server's `plugins` directory
3. Restart the server or use `/reload` command
4. Configure the plugin (optional)

### Basic Configuration
- Debug Mode: `debug: true/false` in `config.yml`
- Logging Settings: `log: true/false` in `config.yml`
- Default Language: `language: "zh_CN"` or `"en_US"` in `config.yml`

## Usage Guide

### Player Commands
- `/kaoneblock start` - Generate an initial block at current location
- `/kaoneblock stop` - Remove the block in the current world
- `/kaoneblock help` - Display help information

### Admin Commands
- `/kaoneblock reload` - Reload plugin configuration
- `/kaoneblock log [on/off]` - Enable or disable logging
- `/kaoneblock debug [on/off]` - Enable or disable debug mode
- `/kaoneblock set <player> <stage>` - Set player to specified stage
- `/kaoneblock reset-stage` - Reset current player's stage
- `/kaoneblock debugchest` - Debug chest contents
- `/kaoneblock ia-status` - Display ItemsAdder status
- `/kaoneblock checkdb` - Check database table structure

### Command Aliases
- `/kob` is a short alias for `/kaoneblock`

### Permission System
- `kaoneblock.help` - Access help command
- `kaoneblock.start` - Start the game
- `kaoneblock.stop` - Stop the game
- `kaoneblock.reload` - Reload configuration
- `kaoneblock.log` - Manage logging
- `kaoneblock.debug` - Debug functionality
- `kaoneblock.admin` - Advanced admin functions

## Configuration Guide

### Block List Configuration (blocks/*.yml)
Each stage configuration file defines:
- `amount`: Number of blocks needed to break to advance to the next stage
- `next`: Next stage configuration file name
- `message`: Notification message when entering the stage
- `chests`: Generation probabilities for various chest types
- `blocks`: Blocks and their generation weights

### Chest Configuration (chests/*.yml)
Each chest configuration file defines:
- `name`: Chest display name
- `amount.min/max`: Range of generated items
- `items`: List of items, including material, quantity range, and weight

## Development Guide

### Build Requirements
- Java 17
- Gradle 7.0+
- Spigot API 1.18.2

### Build Steps
```bash
# Clone the project
git clone https://github.com/yourusername/KaOneBlock.git
cd KaOneBlock

# Build the project
./gradlew build

# Find the built plugin
# Build artifact located at build/libs/KaOneBlock-1.0.jar
```

### Project Structure
```
src/main/java/org/katacr/kaOneBlock/
├── KaOneBlock.java          # Main plugin class
├── BlockBreakListener.java  # Block break event listener
├── BlockGenerator.java      # Block generator
├── StageManager.java        # Stage progress management
├── DatabaseManager.java     # Database management
├── EnhancedChestManager.java # Chest system management
└── chest/                 # Chest-related classes
    ├── ContainerItem.java
    ├── ContainerPoll.java
    └── EnhancedChestManager.java
```

## FAQ

### Q: How do I add custom block stages?
A: Create a new `.yml` file in the `plugins/KaOneBlock/blocks/` directory, configuring block lists and probabilities according to the existing format.

### Q: How do I customize chest contents?
A: Modify or create new `.yml` files in the `plugins/KaOneBlock/chests/` directory, defining items and weights.

### Q: Is the plugin compatible with other versions of Minecraft?
A: The plugin is designed to be compatible with Minecraft 1.13+, primarily tested on version 1.18.2.

### Q: Where is the data stored?
A: Data is stored in an SQLite database file at `plugins/KaOneBlock/data.db`.

## Contributing

Contributions of code, bug reports, and suggestions are welcome! Please follow these steps:

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the [MIT License](LICENSE).

## Changelog

### v1.0
- Initial release
- Implemented basic block generation and stage system
- Added chest reward mechanism
- Support for multi-language and ItemsAdder integration
<<<<<<< HEAD
- Complete command and permission system
=======
- Complete command and permission system
>>>>>>> 98a088fbaf8739c33aba726d944b9acf828079f8
