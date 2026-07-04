import { ApiService } from '../service/ApiService';

const KEY_STUDENT_NO: string = 'studentNo';
const KEY_COURSE_ID: string = 'courseId';
const KEY_TUTOR_NODE: string = 'tutorNodeKpId';

interface ChatMessage {
  role: string;
  content: string;
}

interface QaRequestBody {
  question: string;
  courseId: string;
  previousMessages: string[];
}

interface EventPayload {
}

interface EventRequestBody {
  course_id: string;
  event_type: string;
  payload: EventPayload;
}

@Component
export struct AiTutorPage {
  @Consume navPathStack: NavPathStack;
  @State nodeKpId: string = '';
  @State nodeKpName: string = '';
  @State messages: ChatMessage[] = [];
  @State inputText: string = '';
  @State loading: boolean = false;
  @State errorText: string = '';

  aboutToAppear(): void {
    const kpIdObj: Object | undefined = AppStorage.get(KEY_TUTOR_NODE);
    if (kpIdObj !== undefined) {
      this.nodeKpId = String(kpIdObj);
    }
    const kpNameObj: Object | undefined = AppStorage.get('tutorNodeKpName');
    if (kpNameObj !== undefined) {
      this.nodeKpName = String(kpNameObj);
    }
  }

  private async sendQuestion(): Promise<void> {
    const question: string = this.inputText.trim();
    if (question.length === 0) {
      return;
    }

    let userMsg: ChatMessage = {
      role: 'user',
      content: question
    };
    let newMessages: ChatMessage[] = [];
    for (let i = 0; i < this.messages.length; i++) {
      newMessages.push(this.messages[i]);
    }
    newMessages.push(userMsg);
    this.messages = newMessages;
    this.inputText = '';
    this.loading = true;
    this.errorText = '';

    const studentNo: string = String(AppStorage.get(KEY_STUDENT_NO));
    const courseId: string = String(AppStorage.get(KEY_COURSE_ID));

    try {
      let previousMessages: string[] = [];
      for (let i = 0; i < this.messages.length; i++) {
        if (this.messages[i].role === 'user') {
          previousMessages.push(this.messages[i].content);
        }
      }

      let qaBody: QaRequestBody = {
        question: question,
        courseId: courseId,
        previousMessages: previousMessages
      };

      const raw: string = await ApiService.post('/api/knowledge-points/' + this.nodeKpId + '/qa', qaBody);
      const response = JSON.parse(raw) as Record<string, Object>;

      let answerText: string = '';
      if (response['code'] === 200) {
        const data = response['data'] as Record<string, Object>;
        if (data !== undefined) {
          if (data['answer'] !== undefined) {
            answerText = String(data['answer']);
          } else if (data['content'] !== undefined) {
            answerText = String(data['content']);
          } else if (data['text'] !== undefined) {
            answerText = String(data['text']);
          } else if (data['result'] !== undefined) {
            answerText = String(data['result']);
          } else if (data['data'] !== undefined) {
            const innerData = data['data'] as Record<string, Object>;
            answerText = String(innerData['answer'] !== undefined ? innerData['answer'] : innerData['content'] !== undefined ? innerData['content'] : '');
          } else if (data['message'] !== undefined) {
            answerText = String(data['message']);
          } else {
            answerText = 'AI 返回了空内容，请重试。';
          }
        } else {
          answerText = 'AI 服务暂时不可用，请稍后重试。';
        }
      } else {
        answerText = 'AI 请求失败: ' + String(response['message'] !== undefined ? response['message'] : '未知错误');
      }

      let aiMsg: ChatMessage = {
        role: 'assistant',
        content: answerText
      };
      let updated: ChatMessage[] = [];
      for (let i = 0; i < this.messages.length; i++) {
        updated.push(this.messages[i]);
      }
      updated.push(aiMsg);
      this.messages = updated;

      try {
        let emptyPayload: EventPayload = {};
        let eventBody: EventRequestBody = {
          course_id: courseId,
          event_type: 'ai_tutor_called',
          payload: emptyPayload
        };
        ApiService.post('/api/students/' + studentNo + '/game-event', eventBody);
      } catch (err) {
      }
    } catch (err) {
      this.errorText = 'AI 服务请求失败，请检查网络。';
    } finally {
      this.loading = false;
    }
  }

  build() {
    NavDestination() {
      Column() {
        Row() {
          Text('AI 导师')
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

        if (this.nodeKpName.length > 0) {
          Row() {
            Text('知识点: ' + this.nodeKpName)
              .fontSize(13)
              .fontColor('#DFA54F')
              .padding({ left: 16, right: 16, top: 8, bottom: 8 })
          }
          .width('100%')
          .backgroundColor('#0D0D1A')
          .border({ width: { bottom: 1 }, color: '#FFFFFF10' })
        }

        Scroll() {
          Column({ space: 12 }) {
            if (this.messages.length === 0) {
              Text('向 AI 导师提问，获取知识点讲解和答疑。')
                .fontSize(14)
                .fontColor('#8A8A9A')
                .width('100%')
                .textAlign(TextAlign.Center)
                .margin({ top: 40 })
            }

            ForEach(this.messages, (msg: ChatMessage, index: number) => {
              Row() {
                Column() {
                  Text(msg.role === 'user' ? '你' : 'AI')
                    .fontSize(10)
                    .fontColor(msg.role === 'user' ? '#60A5FA' : '#DFA54F')
                    .fontWeight(FontWeight.Bold)
                    .margin({ bottom: 4 })

                  Text(msg.content)
                    .fontSize(14)
                    .fontColor(msg.role === 'user' ? '#E0E7FF' : '#F8EDCF')
                    .width('100%')
                }
                .padding(12)
                .borderRadius(8)
                .backgroundColor(msg.role === 'user' ? '#1E3A5F' : '#2A2018')
                .layoutWeight(1)
              }
              .width('100%')
              .justifyContent(msg.role === 'user' ? FlexAlign.End : FlexAlign.Start)
            }, (msg: ChatMessage, index: number): string => 'msg_' + String(index))

            if (this.loading) {
              Text('AI 正在思考...')
                .fontSize(13)
                .fontColor('#8A8A9A')
                .width('100%')
                .margin({ top: 10 })
            }

            if (this.errorText.length > 0) {
              Text(this.errorText)
                .fontSize(13)
                .fontColor('#F44336')
                .width('100%')
                .margin({ top: 10 })
            }
          }
          .padding(16)
          .width('100%')
        }
        .layoutWeight(1)
        .width('100%')
        .backgroundColor('#0D0D1A')

        Row({ space: 8 }) {
          TextInput({ text: this.inputText, placeholder: '输入问题...' })
            .layoutWeight(1)
            .height(40)
            .fontSize(14)
            .backgroundColor('#1E1E2E')
            .fontColor('#F8EDCF')
            .borderRadius(8)
            .onChange((value: string) => {
              this.inputText = value;
            })
            .onSubmit(() => {
              this.sendQuestion();
            })

          Button('发送')
            .fontSize(14)
            .fontColor('#1A1A2E')
            .backgroundColor('#DFA54F')
            .borderRadius(8)
            .height(40)
            .padding({ left: 16, right: 16 })
            .enabled(!this.loading && this.inputText.trim().length > 0)
            .onClick(() => {
              this.sendQuestion();
            })
        }
        .width('100%')
        .padding({ left: 12, right: 12, top: 8, bottom: 8 })
        .backgroundColor('#1A1A2E')
        .border({ width: { top: 1 }, color: '#FFFFFF10' })
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
