import { ApiService } from '../service/ApiService';

const KEY_STUDENT_NO: string = 'studentNo';
const KEY_COURSE_ID: string = 'courseId';
const KEY_TUTOR_NODE: string = 'tutorNodeKpId';

interface TowerNodeItem {
  nodeId: string;
  kpName: string;
  roomType: string;
  level: number;
  status: string;
  kpId: string;
}

interface NodePosition {
  x: number;
  y: number;
}

interface GameEventPayload {
  rewardName?: string;
  source_id?: number;
  question_id?: number;
}

interface GameEventBody {
  course_id: string;
  event_type: string;
  payload: GameEventPayload;
}

const NODE_POSITIONS: NodePosition[] = [
  // Aligned with 8 light points on the background map.
  { x: 221, y: -10 },
  { x: 247, y: 98 },
  { x: 371, y: 9 },
  { x: 282, y: 193 },
  { x: 312, y: 324 },
  { x: 374, y: 162 },
  { x: 448, y: 82 },
  { x: 439, y: 275 }
];

@Component
export struct TowerMapPage {
  @Consume navPathStack: NavPathStack;
  @State nodes: TowerNodeItem[] = [];
  @State selectedNode: TowerNodeItem | undefined = undefined;
  @State loading: boolean = true;
  @State errorText: string = '';

  aboutToAppear(): void {
    this.loadData();
  }

  private async loadData(): Promise<void> {
    const studentNo: string = AppStorage.get(KEY_STUDENT_NO) as string;
    const courseId: string = AppStorage.get(KEY_COURSE_ID) as string;
    this.loading = true;
    this.errorText = '';

    try {
      const mapRaw: string = await ApiService.get('/api/students/' + studentNo + '/tower-map?course_id=' + courseId);
      const mapObj = JSON.parse(mapRaw) as Record<string, Object>;
      if (mapObj['code'] === 200) {
        const dataArr: Object[] = mapObj['data'] as Object[];
        let parsedNodes: TowerNodeItem[] = [];
        const maxCount: number = Math.min(dataArr.length, NODE_POSITIONS.length);
        for (let i = 0; i < maxCount; i++) {
          const item = dataArr[i] as Record<string, Object>;
          const roomType: string = this.detectRoomType(item, i);
          const floorStatus: string = String(item['floorStatus'] !== undefined ? item['floorStatus'] : item['status']);
          parsedNodes.push({
            nodeId: String(i + 1),
            kpName: String(item['kpName'] !== undefined ? item['kpName'] : (item['name'] !== undefined ? item['name'] : '')),
            roomType: roomType,
            level: i + 1,
            status: this.normalizeStatus(floorStatus, i === 0),
            kpId: String(item['kpId'] !== undefined ? item['kpId'] : item['id'])
          });
        }
        this.nodes = parsedNodes;
      } else {
        this.errorText = String(mapObj['message'] !== undefined ? mapObj['message'] : '');
      }
    } catch (err) {
      if (this.errorText.length === 0) {
        this.errorText = '地图加载失败';
      }
    } finally {
      this.loading = false;
    }
  }

  private detectRoomType(item: Record<string, Object>, index: number): string {
    const typeVal: Object | undefined = item['roomType'];
    const typeStr: string = typeVal !== undefined ? String(typeVal) : '';
    if (typeStr.toLowerCase() === 'shop') {
      return 'shop';
    }
    const nodeLen: number = this.nodes.length;
    if (nodeLen > 0 && index === nodeLen - 1) {
      return 'boss';
    }
    return typeStr.length > 0 ? typeStr.toLowerCase() : 'battle';
  }

  private normalizeStatus(floorStatus: string, isFirst: boolean): string {
    const raw: string = floorStatus.toLowerCase();
    if (raw.includes('cleared') || raw.includes('mastered') || raw.includes('passed')) {
      return 'cleared';
    }
    if (raw.includes('weak') || raw.includes('remedial')) {
      return 'weak';
    }
    if (raw.includes('locked')) {
      return 'locked';
    }
    if (raw.includes('available') || raw.includes('open') || isFirst) {
      return 'available';
    }
    return 'available';
  }

  private getRoomIconRes(roomType: string): Resource {
    if (roomType === 'shop') {
      return $rawfile('game/merchant.png');
    } else if (roomType === 'battle') {
      return $rawfile('game/enemy.png');
    } else if (roomType === 'elite') {
      return $rawfile('game/elite.png');
    } else if (roomType === 'boss') {
      return $rawfile('game/boss-heart-flame.png');
    } else if (roomType === 'treasure') {
      return $rawfile('game/treasure.png');
    } else if (roomType === 'rest') {
      return $rawfile('game/rest.png');
    } else if (roomType === 'event') {
      return $rawfile('game/unknown.png');
    } else if (roomType === 'diagnosis') {
      return $rawfile('game/magic-orb.png');
    }
    return $rawfile('game/unknown.png');
  }

  private getRoomLabel(roomType: string): string {
    if (roomType === 'diagnosis') {
      return '侦察';
    } else if (roomType === 'battle') {
      return '战斗';
    } else if (roomType === 'elite') {
      return '精英';
    } else if (roomType === 'boss') {
      return 'Boss';
    } else if (roomType === 'treasure') {
      return '宝箱';
    } else if (roomType === 'shop') {
      return '商店';
    } else if (roomType === 'rest') {
      return '休息';
    } else if (roomType === 'event') {
      return '事件';
    }
    return roomType;
  }

  private getStatusColor(status: string): string {
    if (status === 'cleared') {
      return '#4CAF50';
    } else if (status === 'available') {
      return '#DFA54F';
    } else if (status === 'weak') {
      return '#F44336';
    }
    return '#666666';
  }

  private getNodeSize(roomType: string): number {
    if (roomType === 'diagnosis' || roomType === 'elite') {
      return 56;
    }
    if (roomType === 'rest' || roomType === 'shop') {
      return 54;
    }
    return 40;
  }

  private getIconSize(roomType: string): number {
    if (roomType === 'diagnosis' || roomType === 'elite') {
      return 50;
    }
    if (roomType === 'rest' || roomType === 'shop') {
      return 49;
    }
    return 36;
  }

  private getNodeBorderWidth(roomType: string): number {
    if (roomType === 'diagnosis' || roomType === 'elite') {
      return 3.5;
    }
    if (roomType === 'rest' || roomType === 'shop') {
      return 4;
    }
    return 2;
  }

  private getNodeBgColor(roomType: string, status: string): string {
    if (roomType === 'diagnosis' || roomType === 'elite') {
      return '#FFFFFF';
    }
    if (roomType === 'rest' || roomType === 'shop') {
      return '#5D4037';
    }
    if (status === 'cleared') {
      return '#4CAF5050';
    }
    if (status === 'available') {
      return '#DFA54F50';
    }
    if (status === 'weak') {
      return '#F4433650';
    }
    return '#66666630';
  }

  private getNodeBorderColor(roomType: string, status: string, isSelected: boolean): string {
    if (isSelected) {
      return '#FFD700';
    }
    if (roomType === 'diagnosis' || roomType === 'elite') {
      return '#4CAF50';
    }
    if (roomType === 'rest' || roomType === 'shop') {
      return '#FF5722';
    }
    return this.getStatusColor(status);
  }

  private onNodeClick(node: TowerNodeItem): void {
    this.selectedNode = node;
    AppStorage.setOrCreate(KEY_TUTOR_NODE, node.kpId);
    AppStorage.setOrCreate('tutorNodeKpName', node.kpName);
  }

  private onEnterShop(): void {
    this.navPathStack.pushPathByName('Shop', undefined);
  }

  private onOpenAiTutor(): void {
    this.navPathStack.pushPathByName('AITutor', undefined);
  }

  private onOpenAbilityMap(): void {
    this.navPathStack.pushPathByName('AbilityMap', undefined);
  }

  build() {
    NavDestination() {
      Stack() {
        Image($rawfile('game/bg-route-map-act1.jpg'))
          .width('200%')
          .height('100%')
          .objectFit(ImageFit.Cover)
          .scale({ x: 1.25, y: 1.25 })
          .offset({ x: '10%', y: 0 })

        Column() {
          Column() {
            Text('第 1 幕')
              .fontSize(11)
              .fontColor('#DFA54F')
              .fontWeight(FontWeight.Bold)

            Text('路线图')
              .fontSize(24)
              .fontWeight(FontWeight.Bold)
              .fontColor('#F8EDCF')
          }
          .padding({ left: 14, right: 14, top: 8, bottom: 10 })
          .backgroundColor('#0D0D1ABB')
          .borderRadius(8)
        }
        .position({ x: 28, y: 18 })

        ForEach(this.nodes, (node: TowerNodeItem, index: number) => {
          Stack() {
            Image(this.getRoomIconRes(node.roomType))
              .width(this.getIconSize(node.roomType))
              .height(this.getIconSize(node.roomType))
              .objectFit(ImageFit.Contain)
            .width(this.getNodeSize(node.roomType))
            .height(this.getNodeSize(node.roomType))
            .borderRadius(this.getNodeSize(node.roomType) / 2)
            .backgroundColor(this.getNodeBgColor(node.roomType, node.status))
            .border({
              width: this.getNodeBorderWidth(node.roomType),
              color: this.getNodeBorderColor(
                node.roomType,
                node.status,
                this.selectedNode !== undefined && this.selectedNode.nodeId === node.nodeId
              )
            })
            .onClick(() => {
              this.onNodeClick(node);
            })

            Text(this.getRoomLabel(node.roomType))
              .fontSize(node.roomType === 'diagnosis' || node.roomType === 'elite' ||
                node.roomType === 'rest' || node.roomType === 'shop' ? 10 : 8)
              .fontColor('#F8EDCF')
              .position({ x: 0, y: this.getNodeSize(node.roomType) - 2 })
              .width(this.getNodeSize(node.roomType))
              .textAlign(TextAlign.Center)
          }
          .width(this.getNodeSize(node.roomType))
          .height(this.getNodeSize(node.roomType) + 10)
          .position({
            x: index < NODE_POSITIONS.length ? NODE_POSITIONS[index].x : 490,
            y: index < NODE_POSITIONS.length ? NODE_POSITIONS[index].y : 267
          })
          .opacity(node.status === 'locked' ? 0.35 : 1.0)
        }, (node: TowerNodeItem, index: number): string => 'node_' + String(index))

        Stack()
          .width('100%')
          .height('100%')
          .hitTestBehavior(HitTestMode.None)
          .backgroundImage('radial-gradient(ellipse at center, transparent 55%, #00000029 100%)')

        Row() {
          Column({ space: 12 }) {
            if (this.loading) {
              Text('正在加载地图...')
                .fontSize(14)
                .fontColor('#E7D1A2')
                .margin({ top: 16 })
            } else if (this.errorText.length > 0) {
              Text(this.errorText)
                .fontSize(14)
                .fontColor('#F44336')
                .margin({ top: 16 })
            } else if (this.selectedNode !== undefined) {
              Text(this.selectedNode.kpName)
                .fontSize(20)
                .fontWeight(FontWeight.Bold)
                .fontColor('#F8EDCF')
                .maxLines(2)
                .textOverflow({ overflow: TextOverflow.Ellipsis })

              Row({ space: 8 }) {
                Text(this.getRoomLabel(this.selectedNode.roomType))
                  .fontSize(12)
                  .fontColor('#DFA54F')
                  .padding({ left: 10, right: 10, top: 4, bottom: 4 })
                  .border({ width: 1, color: '#DFA54F55' })
                  .borderRadius(4)

                Text('Lv.' + String(this.selectedNode.level))
                  .fontSize(12)
                  .fontColor('#E7D1A2')
              }

              if (this.selectedNode.roomType === 'shop' && this.selectedNode.status !== 'locked') {
                Button('进入错题商店')
                  .width('100%')
                  .height(48)
                  .fontSize(15)
                  .fontWeight(FontWeight.Bold)
                  .backgroundColor('#DFA54F')
                  .fontColor('#1A1A2E')
                  .borderRadius(8)
                  .onClick(() => {
                    this.onEnterShop();
                  })
              }

              Text('')
                .height(1)
                .width('100%')
                .backgroundColor('#FFFFFF15')

              Text('快速入口')
                .fontSize(13)
                .fontColor('#8A8A9A')

              Row({ space: 10 }) {
                Button('AI 导师')
                  .layoutWeight(1)
                  .height(44)
                  .fontSize(14)
                  .backgroundColor('#FFFFFF0D')
                  .fontColor('#DFA54F')
                  .border({ width: 1, color: '#DFA54F55' })
                  .borderRadius(8)
                  .onClick(() => {
                    this.onOpenAiTutor();
                  })

                Button('能力图谱')
                  .layoutWeight(1)
                  .height(44)
                  .fontSize(14)
                  .backgroundColor('#FFFFFF0D')
                  .fontColor('#DFA54F')
                  .border({ width: 1, color: '#DFA54F55' })
                  .borderRadius(8)
                  .onClick(() => {
                    this.onOpenAbilityMap();
                  })
              }
              .width('100%')
            } else {
              Text('选择一个路线节点')
                .fontSize(18)
                .fontWeight(FontWeight.Bold)
                .fontColor('#F8EDCF')

              Text('点击地图上的节点查看详情')
                .fontSize(13)
                .fontColor('#C9B98F')
                .lineHeight(20)
            }
          }
          .width('100%')
          .padding(16)
          .backgroundColor('#1A1A2EDA')
          .borderRadius(16)
          .border({ width: 1, color: '#FFFFFF18' })
          .shadow({ radius: 12, color: '#00000040', offsetX: 0, offsetY: 4 })
        }
        .width(300)
        .justifyContent(FlexAlign.Start)
        .align(Alignment.TopEnd)
        .position({ x: '100%', y: 16 })
        .translate({ x: -316, y: 0 })
      }
      .width('100%')
      .height('100%')
      .backgroundColor('#0D0D1A')
    }
    .width('100%')
    .height('100%')
    .title('')
    .hideTitleBar(true)
  }
}
