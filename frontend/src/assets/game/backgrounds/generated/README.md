# 智慧课程游戏化背景图

本目录图片由 image2 生成，用于课程爬塔系统前端原型和课堂展示。PNG 为原始生成图，JPG 为前端实际引用的压缩版本。

## 背景清单

| 文件 | 用途 |
|---|---|
| `bg-run-entry-python-tower.jpg` | 课程 Run 入口 / 开始副本 |
| `bg-route-map-act1.jpg` | 概念地图 / 第一幕路线 |
| `bg-diagnosis-lab.jpg` | 开局诊断 / 楼层诊断 |
| `bg-combat-classroom-arena.jpg` | 普通知识战斗 |
| `bg-rest-site-study-campfire.jpg` | 休息点 / 复习回血 |
| `bg-treasure-library-vault.jpg` | 宝箱 / 课程资源奖励 |
| `bg-shop-mystic-bookstore.jpg` | 商店 / 提示与错题强化 |
| `bg-boss-python-core.jpg` | Boss 战 / 章节综合挑战 |
| `bg-reward-chamber.jpg` | 战斗胜利后的三选一奖励 |

## 使用建议

- 背景尺寸均为 `1672x941`，接近 16:9。
- 图片不含可读文字，适合在前端上方叠加 HUD、卡牌、题目和地图节点。
- 地图背景 `bg-route-map-act1.png` 已带淡路线氛围，实际节点仍建议用前端组件绘制，避免图片与数据状态冲突。
- 战斗背景 `bg-combat-classroom-arena.png` 左右两侧留有角色站位，底部适合放手牌。
