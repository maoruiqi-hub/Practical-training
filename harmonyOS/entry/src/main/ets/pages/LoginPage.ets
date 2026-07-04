import { ApiService } from '../service/ApiService';
import { ProfilePanel } from '../components/ProfilePanel';

const KEY_STUDENT_NO: string = 'studentNo';
const KEY_COURSE_ID: string = 'courseId';

interface LoginRequestBody {
  username: string;
  password: string;
}

@Component
export struct LoginPage {
  @Consume navPathStack: NavPathStack;
  @State baseUrl: string = 'http://10.0.2.2:8081/practical-training';
  @State username: string = '';
  @State password: string = '';
  @State courseId: string = '1';
  @State statusText: string = '请输入后端地址、账号和密码。';
  @State loading: boolean = false;

  aboutToAppear(): void {
    AppStorage.setOrCreate('studentNo', '');
    AppStorage.setOrCreate('courseId', '1');
  }

  private doLogin = async (): Promise<void> => {
    if (!this.username.trim() || !this.password.trim()) {
      this.statusText = '请填写账号和密码。';
      return;
    }
    this.loading = true;
    this.statusText = '正在登录...';
    try {
      ApiService.setBaseUrl(this.baseUrl);
      AppStorage.setOrCreate(KEY_COURSE_ID, this.courseId.trim());

      const loginBody: LoginRequestBody = {
        username: this.username.trim(),
        password: this.password
      };
      const raw: string = await ApiService.post('/api/students/login', loginBody);
      const response: Object = JSON.parse(raw) as Object;
      const resMap = response as Record<string, Object>;

      if (resMap['code'] === 200) {
        const data: Object = resMap['data'] as Object;
        const dataMap = data as Record<string, Object>;
        const studentNo: string = String(dataMap['studentNo']);
        AppStorage.setOrCreate(KEY_STUDENT_NO, studentNo);
        this.statusText = '登录成功';
        this.navPathStack.pushPathByName('TowerMap', undefined);
      } else {
        const msg: string = String(resMap['message']);
        this.statusText = '登录失败: ' + msg;
      }
    } catch (err) {
      this.statusText = '登录请求失败，请检查后端地址和网络。';
    } finally {
      this.loading = false;
    }
  };

  build() {
    NavDestination() {
      Scroll() {
        Column({ space: 16 }) {
          Text('爬塔学习')
            .fontSize(28)
            .fontWeight(FontWeight.Bold)
            .fontColor('#F8EDCF')
            .width('100%')
            .textAlign(TextAlign.Center)
            .margin({ top: 60 })
          
          Text('AI智慧课程 · 鸿蒙学生端')
            .fontSize(14)
            .fontColor('#DFA54F')
            .width('100%')
            .textAlign(TextAlign.Center)
            .margin({ bottom: 30 })

          TextInput({ text: this.baseUrl, placeholder: '后端地址 (http://IP:端口/上下文)' })
            .height(48)
            .width('100%')
            .fontSize(14)
            .backgroundColor('#1E1E2E')
            .fontColor('#F8EDCF')
            .borderRadius(8)
            .onChange((value: string) => {
              this.baseUrl = value;
            })

          TextInput({ text: this.username, placeholder: '学生账号 (username)' })
            .height(48)
            .width('100%')
            .fontSize(14)
            .backgroundColor('#1E1E2E')
            .fontColor('#F8EDCF')
            .borderRadius(8)
            .onChange((value: string) => {
              this.username = value;
            })

          TextInput({ text: this.password, placeholder: '密码 (password)' })
            .height(48)
            .width('100%')
            .fontSize(14)
            .type(InputType.Password)
            .backgroundColor('#1E1E2E')
            .fontColor('#F8EDCF')
            .borderRadius(8)
            .onChange((value: string) => {
              this.password = value;
            })

          TextInput({ text: this.courseId, placeholder: '课程ID (courseId)' })
            .height(48)
            .width('100%')
            .fontSize(14)
            .backgroundColor('#1E1E2E')
            .fontColor('#F8EDCF')
            .borderRadius(8)
            .onChange((value: string) => {
              this.courseId = value;
            })

          Button(this.loading ? '登录中...' : '登录')
            .width('100%')
            .height(48)
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .backgroundColor('#DFA54F')
            .fontColor('#1A1A2E')
            .borderRadius(8)
            .enabled(!this.loading)
            .onClick(() => {
              this.doLogin();
            })

          Text(this.statusText)
            .fontSize(14)
            .fontColor('#E7D1A2')
            .width('100%')
            .textAlign(TextAlign.Center)
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
