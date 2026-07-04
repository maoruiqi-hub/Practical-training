import { ApiService } from '../service/ApiService';

const KEY_STUDENT_NO: string = 'studentNo';
const KEY_COURSE_ID: string = 'courseId';

interface ShopMistakeItem {
  questionId: number;
  questionStem: string;
  knowledgePointName: string;
}

@Component
export struct ShopPage {
  @Consume navPathStack: NavPathStack;
  @State mistakes: ShopMistakeItem[] = [];
  @State coins: number = 0;
  @State loading: boolean = true;
  @State errorText: string = '';
  @State hintDone: boolean = false;
  @State cleanseDone: boolean = false;
  @State statusText: string = '';

  aboutToAppear(): void {
    this.hintDone = false;
    this.cleanseDone = false;
    this.statusText = '';
    this.loadShopData();
  }

  private async loadShopData(): Promise<void> {
    const studentNo: string = AppStorage.get(KEY_STUDENT_NO) as string;
    const courseId: string = AppStorage.get(KEY_COURSE_ID) as string;
    this.loading = true;
    this.errorText = '';

    try {
      const profileRaw: string = await ApiService.get('/api/students/' + studentNo + '/profile?course_id=' + courseId);
      const profileResponse = JSON.parse(profileRaw) as Record<string, Object>;
      if (profileResponse['code'] === 200) {
        const data = profileResponse['data'] as Record<string, Object>;
        const profile = data['profile'] as Record<string, Object>;
        if (profile !== undefined) {
          this.coins = Number(profile['coins']);
        }
      }
    } catch (err) {
      this.errorText = '加载金币失败';
    }

    try {
      const raw: string = await ApiService.get('/api/students/' + studentNo + '/mistakes');
      const response = JSON.parse(raw) as Record<string, Object>;
      if (response['code'] === 200) {
        const data = response['data'] as Record<string, Object>;
        const wrongList: Object[] = data['wrongList'] as Object[];
        let parsedMistakes: ShopMistakeItem[] = [];
        const maxItems: number = Math.min(wrongList.length, 5);
        for (let i = 0; i < maxItems; i++) {
          const item = wrongList[i] as Record<string, Object>;
          const stem: string = String(item['questionStem'] !== undefined ? item['questionStem'] : item['stem']);
          parsedMistakes.push({
            questionId: Number(item['questionId']),
            questionStem: stem,
            knowledgePointName: String(item['knowledgePointName'])
          });
        }
        this.mistakes = parsedMistakes;
      }
    } catch (err) {
      if (this.errorText.length === 0) {
        this.errorText = '错题加载失败';
      }
    } finally {
      this.loading = false;
    }
  }

  private async doBuyHint(): Promise<void> {
    if (this.coins < 10 || this.hintDone) {
      return;
    }
    this.statusText = '正在购买提示卡...';
    await this.sendGameEvent('shop_purchased', 'hint_card', 0);
    this.hintDone = true;
    this.statusText = '提示卡购买成功！';
  }

  private async doCleanseWrong(): Promise<void> {
    if (this.coins < 8 || this.mistakes.length === 0 || this.cleanseDone) {
      return;
    }
    this.statusText = '正在净化错题...';
    const targetId: number = this.mistakes[0].questionId;
    await this.sendGameEvent('shop_purchased', 'clean_wrong_card', targetId);
    this.cleanseDone = true;
    this.statusText = '错题净化成功！';
  }

  private async sendGameEvent(eventType: string, rewardName: string, sourceId: number): Promise<void> {
    const studentNo: string = String(AppStorage.get(KEY_STUDENT_NO) !== undefined ? AppStorage.get(KEY_STUDENT_NO) : '');
    const courseId: string = String(AppStorage.get(KEY_COURSE_ID) !== undefined ? AppStorage.get(KEY_COURSE_ID) : '');

    let payload: Record<string, Object> = {
      'rewardName': rewardName
    };
    if (sourceId > 0) {
      payload['source_id'] = sourceId;
      payload['question_id'] = sourceId;
    }

    let body: Record<string, Object> = {
      'course_id': courseId,
      'event_type': eventType,
      'payload': payload
    };

    try {
      const raw: string = await ApiService.post('/api/students/' + studentNo + '/game-event', body);
      const response = JSON.parse(raw) as Record<string, Object>;
      if (response['code'] === 200) {
        const data = response['data'] as Record<string, Object>;
        if (data !== undefined) {
          const profile = data['profile'] as Record<string, Object>;
          if (profile !== undefined && profile['coins'] !== undefined) {
            this.coins = Number(profile['coins']);
          }
        }
      }
    } catch (err) {
      this.statusText = '操作失败，请重试';
    }
  }

  build() {
    NavDestination() {
      Stack() {
        Image($rawfile('game/bg-shop-mystic-bookstore.jpg'))
          .width('100%')
          .height('100%')
          .objectFit(ImageFit.Cover)

        Scroll() {
          Column({ space: 16 }) {
            Row({ space: 14 }) {
              Image($rawfile('game/merchant.png'))
                .width(40)
                .height(40)
                .objectFit(ImageFit.Contain)

              Column({ space: 2 }) {
                Text('Shop')
                  .fontSize(11)
                  .fontColor('#DFA54F')
                  .fontWeight(FontWeight.Bold)

                Text('错题商店')
                  .fontSize(22)
                  .fontWeight(FontWeight.Bold)
                  .fontColor('#F8EDCF')
              }
              .layoutWeight(1)

              Text('金币 ' + String(this.coins))
                .fontSize(14)
                .fontWeight(FontWeight.Bold)
                .fontColor('#FFE2A3')
                .padding({ left: 14, right: 14, top: 6, bottom: 6 })
                .border({ width: 1, color: '#DFA54F' })
                .borderRadius(999)
            }
            .width('100%')
            .alignItems(VerticalAlign.Center)

            if (this.statusText.length > 0) {
              Text(this.statusText)
                .fontSize(13)
                .fontColor('#4CAF50')
                .width('100%')
                .textAlign(TextAlign.Center)
            }

            if (this.loading) {
              Text('正在加载...')
                .fontSize(14)
                .fontColor('#E7D1A2')
                .width('100%')
                .textAlign(TextAlign.Center)
                .margin({ top: 30 })
            } else if (this.errorText.length > 0) {
              Text(this.errorText)
                .fontSize(14)
                .fontColor('#F44336')
                .width('100%')
                .textAlign(TextAlign.Center)
                .margin({ top: 30 })
            } else {
              Row() {
                Column() {
                  Text('待处理错题')
                    .fontSize(14)
                    .fontWeight(FontWeight.Bold)
                    .fontColor('#F8EDCF')
                    .width('100%')
                    .margin({ bottom: 10 })

                  if (this.mistakes.length === 0) {
                    Text('暂无错题卡')
                      .fontSize(13)
                      .fontColor('#8A8A9A')
                      .width('100%')
                      .textAlign(TextAlign.Center)
                      .margin({ top: 20 })
                      .height(60)
                  } else {
                    Scroll() {
                      Column({ space: 8 }) {
                        ForEach(this.mistakes, (item: ShopMistakeItem, index: number) => {
                          Row() {
                            Text(String(index + 1))
                              .fontSize(12)
                              .fontWeight(FontWeight.Bold)
                              .fontColor('#1A1A2E')
                              .backgroundColor('#DFA54F')
                              .borderRadius(12)
                              .width(24)
                              .height(24)
                              .textAlign(TextAlign.Center)

                            Column({ space: 3 }) {
                              Text(item.questionStem.length > 0 ? item.questionStem : '错题卡')
                                .fontSize(13)
                                .fontColor('#F8EDCF')
                                .width('100%')

                              if (item.knowledgePointName.length > 0) {
                                Text(item.knowledgePointName)
                                  .fontSize(10)
                                  .fontColor('#8A8A9A')
                                  .width('100%')
                              }
                            }
                            .layoutWeight(1)
                            .margin({ left: 10 })
                          }
                          .width('100%')
                          .padding(10)
                          .backgroundColor('#FFFFFF0D')
                          .borderRadius(6)
                        }, (item: ShopMistakeItem, index: number): string => 'm_' + String(index))
                      }
                      .width('100%')
                    }
                    .height(200)
                    .scrollable(ScrollDirection.Vertical)
                    .scrollBar(BarState.Off)
                    .width('100%')
                  }
                }
                .layoutWeight(1)
                .padding(14)
                .backgroundColor('#FFFFFF08')
                .borderRadius(8)
                .border({ width: 1, color: '#FFFFFF10' })
                .margin({ right: 8 })

                Column({ space: 10 }) {
                  Column() {
                    Text('Cost 10')
                      .fontSize(11)
                      .fontColor('#DFA54F')
                      .fontWeight(FontWeight.Bold)

                    Text(this.hintDone ? '已完成' : '购买提示卡')
                      .fontSize(15)
                      .fontWeight(FontWeight.Bold)
                      .fontColor(this.hintDone ? '#8A8A9A' : '#F8EDCF')
                      .margin({ top: 6 })

                    Text(this.hintDone ? '操作已执行' : '换取下一场战斗的攻击准备')
                      .fontSize(12)
                      .fontColor('#E7D1A2')
                      .margin({ top: 6, bottom: 4 })
                  }
                  .width('100%')
                  .padding(14)
                  .backgroundColor(this.hintDone ? '#FFFFFF05' : '#FFFFFF0D')
                  .borderRadius(8)
                  .border({ width: 1, color: this.hintDone ? '#FFFFFF08' : '#DFA54F55' })
                  .opacity(this.coins < 10 || this.hintDone ? 0.55 : 1.0)
                  .onClick(() => {
                    if (this.coins >= 10 && !this.hintDone) {
                      this.doBuyHint();
                    }
                  })

                  Column() {
                    Text('Cost 8')
                      .fontSize(11)
                      .fontColor('#DFA54F')
                      .fontWeight(FontWeight.Bold)

                    Text(this.cleanseDone ? '已完成' : '净化错题卡')
                      .fontSize(15)
                      .fontWeight(FontWeight.Bold)
                      .fontColor(this.cleanseDone ? '#8A8A9A' : '#F8EDCF')
                      .margin({ top: 6 })

                    Text(this.cleanseDone ? '操作已执行' : '选择一张错题转化为防御和经验收益')
                      .fontSize(12)
                      .fontColor('#E7D1A2')
                      .margin({ top: 6, bottom: 4 })
                  }
                  .width('100%')
                  .padding(14)
                  .backgroundColor(this.cleanseDone ? '#FFFFFF05' : '#FFFFFF0D')
                  .borderRadius(8)
                  .border({
                    width: 1,
                    color: this.cleanseDone ? '#FFFFFF08' : '#DFA54F55'
                  })
                  .opacity(this.coins < 8 || this.mistakes.length === 0 || this.cleanseDone ? 0.55 : 1.0)
                  .onClick(() => {
                    if (this.coins >= 8 && this.mistakes.length > 0 && !this.cleanseDone) {
                      this.doCleanseWrong();
                    }
                  })
                }
                .layoutWeight(1)
              }
              .width('100%')
              .alignItems(VerticalAlign.Top)
            }

            Column().height(20)
          }
          .padding({ left: 16, right: 16, top: 20 })
          .width('100%')
          .backgroundColor('rgba(8, 10, 14, 0.78)')
        }
        .height('100%')
        .width('100%')
        .scrollable(ScrollDirection.Vertical)
        .scrollBar(BarState.Off)
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
