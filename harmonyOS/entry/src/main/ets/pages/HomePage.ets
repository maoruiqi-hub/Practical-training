import { ApiService } from '../service/ApiService';
import { ProfilePanel } from '../components/ProfilePanel';

const KEY_STUDENT_NO: string = 'studentNo';
const KEY_COURSE_ID: string = 'courseId';

@Component
export struct HomePage {
  @Consume navPathStack: NavPathStack;
  @State nickname: string = '';
  @State hp: number = 0;
  @State maxHp: number = 0;
  @State attack: number = 0;
  @State defense: number = 0;
  @State exp: number = 0;
  @State coins: number = 0;
  @State level: number = 0;
  @State loading: boolean = true;
  @State errorText: string = '';

  aboutToAppear(): void {
    this.loadProfile();
  }

  private async loadProfile(): Promise<void> {
    const studentNo: string = AppStorage.get(KEY_STUDENT_NO) as string;
    const courseId: string = AppStorage.get(KEY_COURSE_ID) as string;
    this.loading = true;
    this.errorText = '';
    try {
      const raw: string = await ApiService.get('/api/students/' + studentNo + '/profile?course_id=' + courseId);
      const response: Object = JSON.parse(raw) as Object;
      const resMap = response as Record<string, Object>;
      if (resMap['code'] === 200) {
        const data: Object = resMap['data'] as Object;
        const dataMap = data as Record<string, Object>;
        const profile: Object = dataMap['profile'] as Object;
        const profileMap2 = profile as Record<string, Object>;
        this.nickname = String(profileMap2['nickname'] !== undefined ? profileMap2['nickname'] : '');
        this.hp = Number(profileMap2['hp']);
        this.maxHp = Number(profileMap2['maxHp']);
        this.attack = Number(profileMap2['attack']);
        this.defense = Number(profileMap2['defense']);
        this.exp = Number(profileMap2['exp']);
        this.coins = Number(profileMap2['coins']);
        this.level = Number(profileMap2['level']);
      } else {
        this.errorText = '获取画像失败: ' + String(resMap['message']);
      }
    } catch (err) {
      this.errorText = '画像接口请求失败，请检查网络和后端状态。';
    } finally {
      this.loading = false;
    }
  }

  build() {
    NavDestination() {
      Scroll() {
        Column({ space: 16 }) {
          // Header
          Text('AI智慧课程')
            .fontSize(13)
            .fontColor('#DFA54F')
            .width('100%')
            .textAlign(TextAlign.Center)
            .margin({ top: 30 })

          Text('欢迎, ' + (this.nickname.length > 0 ? this.nickname : '同学'))
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor('#F8EDCF')
            .width('100%')
            .textAlign(TextAlign.Center)

          if (this.loading) {
            Text('正在加载画像...')
              .fontSize(14)
              .fontColor('#E7D1A2')
              .width('100%')
              .textAlign(TextAlign.Center)
              .margin({ top: 20 })
          } else if (this.errorText.length > 0) {
            Column() {
              Text(this.errorText)
                .fontSize(14)
                .fontColor('#F44336')
                .width('100%')
                .textAlign(TextAlign.Center)

              Button('重试')
                .backgroundColor('#DFA54F')
                .fontColor('#1A1A2E')
                .borderRadius(6)
                .margin({ top: 10 })
                .onClick(() => {
                  this.loadProfile();
                })
            }
            .width('100%')
            .margin({ top: 20 })
          } else {
            // Profile panel
            ProfilePanel({
              hp: this.hp,
              maxHp: this.maxHp,
              attack: this.attack,
              defense: this.defense,
              exp: this.exp,
              coins: this.coins
            })
              .borderRadius(8)
              .margin({ top: 10, bottom: 10 })

            // Quick stats
            Text('Lv.' + this.level.toString() + '  |  ' + 'EXP ' + this.exp.toString())
              .fontSize(13)
              .fontColor('#E7D1A2')
              .width('100%')
              .textAlign(TextAlign.Center)

            // Enter tower button
            Button('进 入 爬 塔')
              .width('100%')
              .height(56)
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .backgroundColor('#DFA54F')
              .fontColor('#1A1A2E')
              .borderRadius(12)
              .margin({ top: 30 })
              .onClick(() => {
                this.navPathStack.pushPathByName('TowerMap', undefined);
              })
          }
        }
        .padding(24)
        .width('100%')
      }
      .height('100%')
      .width('100%')
      .backgroundColor('#0D0D1A')
    }
    .title('')
    .hideTitleBar(true)
  }
}
