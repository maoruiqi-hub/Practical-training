# Slay the Spire Pinterest 参考图清单

本目录图片来自 Pinterest 公开页面下载，用于课堂展示、前端重构参考和临时原型沟通。图片未重新授权，不建议作为正式发布素材或商业素材使用。

## 目录结构

```text
slay-the-spire-pinterest/
  cards/
  characters/
  combat/
  enemies/
  icons/
  map/
  rooms/
```

## 地图参考

| 文件 | 尺寸 | 来源 | 建议用途 |
|---|---:|---|---|
| `map/map-journey-layout.png` | 2546x1486 | https://au.pinterest.com/pin/slay-the-spire-journey-map--328762841567459030/ | 主地图路线、节点分支、图例布局参考 |
| `map/map-unity-layout.jpg` | 480x360 | https://www.pinterest.com/pin/silveruaslaythespiremapinunity-implementation-of-the-slay-the-spire-map-in-unity3d--539376492877498412/ | 分支地图生成算法和结构参考；尺寸较小，不适合做背景 |

## 地图图标参考

| 文件 | 尺寸 | 来源 | 建议用途 |
|---|---:|---|---|
| `icons/map-icons-magnets.jpg` | 2136x2848 | https://www.pinterest.com/pin/773352567308279391/ | 普通战斗、精英、Boss、营火、商店、宝箱等节点图标参考 |

## 战斗 UI 参考

| 文件 | 尺寸 | 来源 | 建议用途 |
|---|---:|---|---|
| `combat/combat-ironclad-hand.jpg` | 2560x1080 | https://www.pinterest.com/pin/slay-the-spire--513691901241499617/ | 战斗舞台、左右角色、底部手牌、能量/牌堆位置参考 |
| `combat/combat-lightning-hand.jpg` | 1399x787 | https://www.pinterest.com/pin/slay-the-spire-screenshots--308637380720884149/ | 技能释放、手牌高亮、战斗反馈参考 |
| `combat/steam-store-cardbattle.jpg` | 600x337 | https://www.pinterest.com/pin/slay-the-spire-on-steam--81838918213602719/ | 整体战斗 UI 和官方截图参考；尺寸较小 |
| `combat/steam-power-through-art.png` | 383x383 | https://www.pinterest.com/pin/slay-the-spire--575897871097096046/ | 技能动作和战斗特效氛围参考 |

## 卡牌参考

| 文件 | 尺寸 | 来源 | 建议用途 |
|---|---:|---|---|
| `cards/card-exhume-template.png` | 678x874 | https://www.pinterest.com/pin/413275703325493845/ | 知识卡牌版式：费用、标题、插画、类型、效果文本 |
| `cards/cards-transparent-pack.png` | 666x871 | https://www.pinterest.com/pin/594193744629014920/ | 卡牌资源组织和透明图参考 |
| `cards/nintendo-product-cards.jpg` | 488x488 | https://www.pinterest.com/pin/698620960980406309/ | 卡牌构筑宣传图参考；尺寸较小 |

## 入口与角色参考

| 文件 | 尺寸 | 来源 | 建议用途 |
|---|---:|---|---|
| `characters/entry-character-select.png` | 2558x1481 | https://au.pinterest.com/pin/slay-the-spire-character-selection-screen-in-2025--339881103147972573/ | 课程 Run 入口、角色选择、开始界面构图参考 |
| `characters/entry-ascension-buttons.jpg` | 1280x720 | https://www.pinterest.com/pin/slay-the-spire-ironclad-ascension-5-insane-floor-2-drop--414823815685989979/ | 难度选择、入口按钮和大角色图参考 |

## 房间参考

| 文件 | 尺寸 | 来源 | 建议用途 |
|---|---:|---|---|
| `rooms/merchant-room-art.jpg` | 735x826 | https://www.pinterest.com/pin/574842339959780613/ | 商店房间角色氛围参考 |
| `rooms/merchant-alt-art.jpg` | 735x826 | https://mx.pinterest.com/pin/662803270201033060/ | 与 `merchant-room-art.jpg` 内容重复，保留作来源备份 |
| `rooms/rest-campfire-wallpaper.png` | 1080x2340 | https://in.pinterest.com/pin/746119863306117701/ | 休息点/营火氛围参考；偏 fanart，不建议直接做正式背景 |

## 敌人与 Boss 参考

| 文件 | 尺寸 | 来源 | 建议用途 |
|---|---:|---|---|
| `enemies/boss-slime-sticker.jpg` | 1500x1500 | https://www.pinterest.com/pin/slay-the-spire-slime-boss-sticker-for-sale-by-dumclo--180003317647084791/ | Boss/敌人头像轮廓参考 |
| `enemies/boss-corrupt-heart-sticker.jpg` | 1500x1500 | https://in.pinterest.com/pin/corrupt-heart-sticker-for-sale-by-asta434--180003317645394173/ | 终章 Boss 图标和心脏核心参考 |
| `enemies/boss-donu-deca-pin.jpg` | 2577x1665 | https://www.pinterest.com/pin/donu-deca-enamel-pin-from-slay-the-spire--4610067665641064448/ | 双 Boss、精英敌人、徽章式敌人图标参考 |

## 接入建议

- 地图重构优先参考 `map/map-journey-layout.png` 和 `icons/map-icons-magnets.jpg`。
- 战斗房重构优先参考 `combat/combat-ironclad-hand.jpg`。
- 奖励与卡牌重构优先参考 `cards/card-exhume-template.png`。
- 商店、休息点、Boss 图片建议只做风格参考，正式 UI 背景仍按规格文档使用 image2 生成。
