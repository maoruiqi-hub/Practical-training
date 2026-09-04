<template>
  <el-drawer
    v-model="visible"
    :title="panelTitle"
    size="420px"
    class="ai-tutor-drawer"
    append-to-body
  >
    <section class="ai-tutor">
      <header class="tutor-context">
        <small>{{ modeLabel }}</small>
        <strong>{{ knowledgePointName || knowledgePointId }}</strong>
      </header>

      <div ref="messageBox" class="message-list">
        <el-empty v-if="!messages.length" description="暂无对话" :image-size="72" />
        <article
          v-for="(message, index) in messages"
          :key="`${message.role}-${index}`"
          class="message"
          :class="message.role"
        >
          <span>{{ message.role === 'user' ? '学生' : 'AI' }}</span>
          <p>{{ message.content }}</p>
        </article>
      </div>

      <el-alert
        v-if="errorMessage"
        class="error-alert"
        type="warning"
        :closable="false"
        :title="errorMessage"
      />

      <footer class="ask-bar">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          resize="none"
          :placeholder="inputPlaceholder"
          :disabled="sending"
        />
        <el-button class="send-button" type="primary" :loading="sending" @click="sendQuestion">
          发送
        </el-button>
      </footer>
    </section>
  </el-drawer>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { askKnowledgePoint, explainKnowledgePoint } from '../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  knowledgePointId: { type: [String, Number], required: true },
  knowledgePointName: { type: String, default: '' },
  courseId: { type: [String, Number], default: '' },
  resourceId: { type: [String, Number], default: '' },
  mode: { type: String, default: 'qa' },
  initialQuestion: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const draft = ref('')
const sending = ref(false)
const messages = ref([])
const errorMessage = ref('')
const messageBox = ref(null)

const panelTitle = computed(() => props.mode === 'lecture' ? 'AI 讲解' : 'AI 导师')
const modeLabel = computed(() => props.mode === 'lecture' ? 'Lecture' : 'Question Answering')
const inputPlaceholder = computed(() =>
  props.mode === 'lecture' ? '输入希望重点讲解的内容' : '输入你想追问的问题'
)

watch(() => [props.knowledgePointId, props.resourceId, props.mode], () => {
  messages.value = []
  draft.value = ''
  errorMessage.value = ''
})

watch(() => [props.modelValue, props.initialQuestion], ([open, question]) => {
  if (open && question) {
    draft.value = question
    nextTick(() => sendQuestion())
  }
})

const previousMessages = () => messages.value.map(message =>
  `${message.role === 'user' ? '学生' : 'AI'}：${message.content}`
)

const extractAnswer = response => {
  const result = response?.data || {}
  if (result.code && result.code !== 200) {
    throw new Error(result.msg || 'AI 服务暂不可用，请稍后重试')
  }
  const agentic = result.data || result
  if (agentic.success === false) {
    throw new Error(agentic.message || 'AI 服务暂不可用，请稍后重试')
  }
  const data = agentic.data || {}
  if (typeof data === 'string') return data
  const text = data.answer ||
    data.content ||
    data.text ||
    data.result ||
    agentic.answer ||
    agentic.content ||
    agentic.text ||
    agentic.result
  if (text) return text

  const statusMessage = agentic.message || result.message || result.msg || ''
  if (statusMessage && !['ok', 'success'].includes(String(statusMessage).trim().toLowerCase())) {
    return statusMessage
  }
  return 'AI 已返回结果，但没有可显示的文本。'
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight
}

const sendQuestion = async () => {
  if (!props.knowledgePointId) {
    ElMessage.warning('缺少知识点')
    return
  }
  if (props.mode === 'lecture' && !props.resourceId) {
    errorMessage.value = '请选择 PPT 资源后再使用 AI 讲解'
    return
  }

  const question = String(draft.value || '').trim() ||
    (props.mode === 'lecture' ? '请讲解这个资源中与当前知识点相关的内容' : '')
  if (!question) {
    ElMessage.warning('请输入问题')
    return
  }

  const history = previousMessages()
  messages.value.push({ role: 'user', content: question })
  draft.value = ''
  errorMessage.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const payload = {
      question,
      previousMessages: history
    }
    if (props.resourceId) payload.resourceId = props.resourceId

    const response = props.mode === 'lecture'
      ? await explainKnowledgePoint(props.knowledgePointId, payload)
      : await askKnowledgePoint(props.knowledgePointId, payload)

    messages.value.push({ role: 'assistant', content: extractAnswer(response) })
  } catch {
    errorMessage.value = 'AI 服务暂不可用，请稍后重试'
    messages.value.push({ role: 'assistant', content: errorMessage.value })
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}
</script>

<style scoped>
.ai-tutor {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  gap: 12px;
  height: calc(100vh - 96px);
}

.tutor-context {
  display: grid;
  gap: 5px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.tutor-context small {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.tutor-context strong {
  overflow: hidden;
  color: #111827;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-list {
  display: grid;
  align-content: start;
  gap: 10px;
  overflow: auto;
  padding: 2px 2px 8px;
}

.message {
  display: grid;
  gap: 5px;
  max-width: 92%;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f1f5f9;
}

.message.user {
  justify-self: end;
  color: #fff;
  background: #2563eb;
}

.message.assistant {
  justify-self: start;
  background: #fff7ed;
}

.message span {
  font-size: 12px;
  font-weight: 800;
  opacity: .72;
}

.message p {
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
}

.error-alert {
  margin: 0;
}

.ask-bar {
  display: grid;
  gap: 10px;
}

.send-button {
  min-height: 40px;
  border-radius: 6px;
}
</style>
