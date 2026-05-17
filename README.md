# MultiAgentPathPlanning / PathLab

PathLab 是一个基于 **JavaFX** 的路径搜索算法可视化项目。用户可以在网格地图上设置起点、终点和障碍物，然后选择 BFS、DFS、Dijkstra 或 A* 算法，动态观察搜索过程和最终路径。

本版本已经补全为可以直接运行、可以展示、可以写课程设计报告的完整项目。

## 一、已实现功能

### 1. 网格交互

- 30 × 25 网格地图
- 左键绘制 / 取消障碍物
- 鼠标左键拖动连续绘制障碍物
- 右键擦除格子
- Shift + 左键设置起点
- Ctrl + 左键设置终点
- 清除搜索痕迹
- 清空障碍物
- 重置地图

### 2. 算法可视化

- BFS 广度优先搜索
- DFS 深度优先搜索
- Dijkstra 最短路算法
- A* 启发式搜索算法
- 搜索过程动画播放
- 最终路径高亮显示
- 支持斜向移动开关
- 动画速度调节

### 3. 项目展示增强

- 随机障碍生成
- 迷宫生成
- 导出当前网格截图
- 显示访问节点数
- 显示路径长度
- 显示计算耗时
- 提供课程设计报告模板
- 提供答辩介绍稿
- 提供截图清单
- 提供 JAR / 应用打包脚本

### 4. C++ 部分

`cpp` 目录中保留了 A* 和简化版多智能体路径规划代码，适合用于算法对比或扩展。

## 二、运行环境

建议环境：

```text
JDK 17 或更高版本
JavaFX SDK 24.0.2
Windows 10 / Windows 11
VSCode 或直接双击 bat 运行
```

本项目需要按你的电脑配置好 JavaFX 路径：

```text
eg:  C:\Users\q1763\Desktop\javafx-sdk-24.0.2\lib
```

## 三、运行方式

### 方式一：双击运行

解压项目后，双击项目根目录中的：

```text
run_javafx.bat
```

如果这个脚本无法运行，可以再试：

```text
run_javafx_safe.cmd
```

### 方式二：VSCode 运行

1. 用 VSCode 打开项目根目录 `MultiAgentPathPlanning`。
2. 确认 `.vscode/launch.json` 中的 JavaFX 路径正确。
3. 按 F5 运行。

## 四、操作说明

| 操作 | 效果 |
|---|---|
| 左键点击 | 绘制或取消障碍物 |
| 左键拖动 | 连续绘制障碍物 |
| 右键点击 | 擦除当前格子 |
| Shift + 左键 | 设置起点 |
| Ctrl + 左键 | 设置终点 |
| 开始运行 | 播放算法可视化动画 |
| 停止 | 中止当前动画 |
| 清除痕迹 | 清除访问节点和路径，保留障碍 |
| 清空障碍 | 删除所有障碍物 |
| 随机障碍 | 自动生成随机障碍地图 |
| 生成迷宫 | 自动生成迷宫地图 |
| 导出截图 | 将当前网格保存为 PNG 图片 |

导出的截图会保存到项目根目录：

```text
screenshots/
```

## 五、颜色说明

| 颜色 | 含义 |
|---|---|
| 绿色 | 起点 |
| 红色 | 终点 |
| 黑色 | 障碍物 |
| 蓝色 | 已访问节点 |
| 黄色 | 最终路径 |
| 白色 | 可通行空格 |

## 六、项目结构

```text
MultiAgentPathPlanning/
├── java/
│   ├── Main.java              # JavaFX 程序入口
│   ├── Controller.java        # UI 控制、算法实现、动画逻辑
│   └── UI.fxml                # JavaFX 界面布局
├── cpp/
│   ├── Astar.cpp              # C++ A* 算法示例
│   ├── CBS.cpp                # 简化版多智能体规划示例
│   └── utils.h                # C++ 工具结构和函数声明
├── docs/
│   ├── 课程设计报告.md
│   ├── 答辩介绍稿.md
│   ├── 截图清单.md
│   └── 打包说明.md
├── .vscode/
│   ├── launch.json
│   ├── tasks.json
│   └── settings.json
├── run_javafx.bat             # 一键编译并运行
├── run_javafx_safe.cmd        # 备用运行脚本
├── build_jar.bat              # 打包 JAR
├── package_app.bat            # 使用 jpackage 打包应用文件夹
├── RUNNING_HELP.txt
└── README.md
```

## 七、推荐提交材料

如果这是课程设计，建议提交：

```text
1. 完整项目源码
2. docs/课程设计报告.md
3. docs/答辩介绍稿.md
4. screenshots/ 中的运行截图
5. README.md
```

如果老师要求 Word 文档，可以把 `docs/课程设计报告.md` 的内容复制到 Word 里，再插入运行截图即可。
