import { ApiService } from '../service/ApiService';

const KEY_STUDENT_NO: string = 'studentNo';
const KEY_COURSE_ID: string = 'courseId';

interface AbilityPointItem {
  pointId: string;
  name: string;
  description: string;
  masteryScore: number;
  kpCount: number;
}

@Component
export struct AbilityMapPage {
  @Consume navPathStack: NavPathStack;
  @State abilityPoints: AbilityPointItem[] = [];
  @State loading: boolean = true;
  @State errorText: string = '';

  aboutToAppear(): void {
    this.loadData();
  }

  private async loadData(): Promise<void> {
    const studentNo: string = String(AppStorage.get(KEY_STUDENT_NO));
    const courseId: string = String(AppStorage.get(KEY_COURSE_ID));
    this.loading = true;
    this.errorText = '';

    try {
      const mapRaw: string = await ApiService.get('/api/ability-map?courseCode=' + courseId);
      const mapResponse = JSON.parse(mapRaw) as Record<string, Object>;
      let parsedPoints: AbilityPointItem[] = [];

      if (mapResponse['code'] === 200) {
        const data = mapResponse['data'] as Record<string, Object>;
        if (data !== undefined) {
          const points: Object[] = data['abilityPoints'] as Object[];
          if (points !== undefined) {
            for (let i = 0; i < points.length; i++) {
              const p = points[i] as Record<string, Object>;
              parsedPoints.push({
                pointId: String(p['abilityPointId']),
                name: String(p['name']),
                description: String(p['description'] !== undefined ? p['description'] : ''),
                masteryScore: 0,
                kpCount: 0
              });
            }
          }
        }
      }

      this.abilityPoints = parsedPoints;
      if (parsedPoints.length === 0) {
        this.errorText = '暂无能力图谱数据';
      }
    } catch (err) {
      this.errorText = '能力图谱加载失败';
    } finally {
      this.loading = false;
    }
  }

  private getMasteryColor(score: number): string {
    if (score >= 80) {
      return '#4CAF50';
    } else if (score >= 60) {
      return '#FFC107';
    }
    return '#F44336';
  }

  private getMasteryLabel(score: number): string {
    if (score >= 80) {
      return '已掌握';
    } else if (score >= 60) {
      return '学习中';
    }
    return '需加强';
  }

  build() {
    NavDestination() {
      Column() {
        Row() {
          Text('能力图谱')
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor('#F8EDCF')
            .layoutWeight(1)

          Button('返回')
            .fontSize(12)
            .fontColor('#DFA54F')
            .backgroundColor('#00000000')
            .onClick(() => {
              this.navPathStack.pop();
            })
        }
        .width('100%')
        .height(44)
        .padding({ left: 16, right: 16 })
        .backgroundColor('#1A1A2E')

        Scroll() {
          Column({ space: 16 }) {
            if (this.loading) {
              Text('正在加载能力图谱...')
                .fontSize(14)
                .fontColor('#E7D1A2')
                .width('100%')
                .textAlign(TextAlign.Center)
                .margin({ top: 60 })
            }

            if (!this.loading && this.errorText.length > 0 && this.abilityPoints.length === 0) {
              Text(this.errorText)
                .fontSize(14)
                .fontColor('#F44336')
                .width('100%')
                .textAlign(TextAlign.Center)
                .margin({ top: 60 })
            }

            if (!this.loading && this.abilityPoints.length > 0) {
              Text('能力点详情')
                .fontSize(18)
                .fontWeight(FontWeight.Bold)
                .fontColor('#F8EDCF')
                .width('100%')
                .margin({ top: 10 })

              ForEach(this.abilityPoints, (point: AbilityPointItem) => {
                Column({ space: 8 }) {
                  Row() {
                    Text(point.name)
                      .fontSize(15)
                      .fontWeight(FontWeight.Bold)
                      .fontColor('#F8EDCF')
                      .layoutWeight(1)

                    Text(this.getMasteryLabel(point.masteryScore))
                      .fontSize(11)
                      .fontColor(this.getMasteryColor(point.masteryScore))
                      .fontWeight(FontWeight.Bold)
                      .padding({ left: 8, right: 8, top: 2, bottom: 2 })
                      .border({
                        width: 1,
                        color: this.getMasteryColor(point.masteryScore) + '55'
                      })
                      .borderRadius(4)
                  }
                  .width('100%')

                  if (point.description.length > 0) {
                    Text(point.description)
                      .fontSize(12)
                      .fontColor('#8A8A9A')
                      .width('100%')
                      .maxLines(2)
                      .textOverflow({ overflow: TextOverflow.Ellipsis })
                  }

                  Progress({
                    value: point.masteryScore,
                    total: 100,
                    type: ProgressType.Linear
                  })
                    .width('100%')
                    .color(this.getMasteryColor(point.masteryScore))
                    .style({ strokeWidth: 6 })

                  Row({ space: 16 }) {
                    Text('知识点: ' + String(point.kpCount))
                      .fontSize(11)
                      .fontColor('#8A8A9A')

                    Text('掌握度: ' + String(point.masteryScore) + '%')
                      .fontSize(11)
                      .fontColor(this.getMasteryColor(point.masteryScore))
                  }
                  .width('100%')
                }
                .width('100%')
                .padding(14)
                .backgroundColor('#FFFFFF08')
                .borderRadius(8)
                .border({
                  width: { left: 3 },
                  color: this.getMasteryColor(point.masteryScore)
                })
              }, (point: AbilityPointItem): string => point.pointId)
            }

            Column().height(20)
          }
          .padding(16)
          .width('100%')
        }
        .layoutWeight(1)
        .width('100%')
        .backgroundColor('#0D0D1A')
      }
      .width('100%')
      .height('100%')
    }
    .width('100%')
    .height('100%')
    .title('')
    .hideTitleBar(true)
  }
}
