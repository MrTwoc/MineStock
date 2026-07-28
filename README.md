# MineQuiz

本插件基于另一个开源插件fork修改而来：https://github.com/oneHerobrine/MineStock
一个[Vibe Coding] Minecraft Paper 服务器知识竞赛问答插件，支持轮次自动出题、抢答/问答双模式、分数持久化与后台日志开关。
适用MC 26.1.2 服务器，支持 Paper 服务器，其他版本未经测试。

## 功能概览

- **轮次答题** — 每轮可配置多道题目，题目之间自动切换，全部完成后等待间隔进入下一轮
- **双模式随机出题** — 每道题在「抢答」和「问答」两种模式间随机分配，增加竞技趣味
- **抢答模式** — 第一个答对的玩家获胜即结束本题；答错不影响其他玩家继续抢答
- **问答模式** — 答题时间结束后统一公布正确答案和全部答对玩家
- **每人一次** — 每道题每位玩家只能选择一次答案，不可重复提交
- **延迟公布** — 答题期间不公开对错，时间截止后统一揭晓答案
- **分数持久化** — 使用 SQLite 数据库存储玩家分数，重启不丢失
- **分数管理** — 管理员可查询/增加/扣除/设置任意玩家分数
- **后台日志开关** — 可配置是否在控制台输出发送给玩家的消息

## 依赖

| 依赖 | 版本 |
|------|------|
| Paper | 26.1.2+ |
| Java | 25 |

## 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/quiz start` | 手动开始新一轮问答 | `minequiz.admin` |
| `/quiz reload` | 重载配置文件和题库 | `minequiz.admin` |
| `/quizscore <玩家>` | 查询指定玩家分数 | `minequiz.admin` |
| `/quizscore <玩家> add <数值>` | 增加玩家分数 | `minequiz.admin` |
| `/quizscore <玩家> remove <数值>` | 扣除玩家分数（最低 0） | `minequiz.admin` |
| `/quizscore <玩家> set <数值>` | 设置玩家分数 | `minequiz.admin` |

玩家在聊天框点击选项即可作答（无需手动输入命令）。

## 权限

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `minequiz.use` | 允许参与知识竞赛 | 所有人 |
| `minequiz.admin` | 管理知识竞赛（开始、重载、管理分数等） | OP |

## 配置文件

```yaml
# 每轮题目数量
questions-per-round: 5

# 轮次间隔（分钟）
round-interval: 30

# 每题答题时间（秒）
answer-time: 30

# 每题之间间隔（秒）
between-questions-delay: 10

# 抢答题每题得分
race-points: 10

# 问答题（不限人数）每题得分
quiz-points: 5

# 开启答题的最低在线玩家数（少于该人数则跳过本轮）
min-players: 1

# 是否在控制台输出发送给玩家的消息
log-to-console: true
```

## 题目配置

在 `plugins/MineQuiz/questions.yml` 中配置题目，格式如下：

```yaml
questions:
  - id: 1
    category: "历史"
    question: "中国的首都是？"
    options:
      - "上海"
      - "北京"
      - "广州"
      - "深圳"
    answer: 1

  - id: 2
    category: "科学"
    question: "光年是什么单位？"
    options:
      - "时间"
      - "速度"
      - "距离"
      - "亮度"
    answer: 2
```

- `id` — 题目唯一编号
- `category` — 分类标签（显示在题目头）
- `question` — 题目文本
- `options` — 选项列表（2~6 个）
- `answer` — 正确答案的索引（从 0 开始）

## 数据存储

使用 SQLite（`data.db`）本地持久化玩家分数，无需额外数据库服务。数据库文件位于 `plugins/MineQuiz/data.db`。

## 答题流程

```
[轮次开始]  ⚔️ 新的一轮开始！本轮共 5 道题
   │
   ├─ 第 1 题（随机抢答）→ 30秒答题 → 公布答案
   │  ↓ 10秒间隔
   ├─ 第 2 题（随机问答）→ 30秒答题 → 公布答案
   │  ↓ 10秒间隔
   ├─ 第 3 题   ...
   │  ↓
[轮次结束]  ===== 本轮知识竞赛结束！感谢参与！=====
   │
   └─ 等待 30 分钟后 → 下一轮
```

## 构建

```bash
mvn clean package
```

产物位于 `target/MineQuiz-1.0.0.jar`，放入服务器 `plugins/` 目录后重启即可。

## 作者

MrTwoc
