// Login request body
interface LoginBody {
  username: string;
  password: string;
}

// Login response from backend
interface LoginResponse {
  code: number;
  message: string;
  data: StudentBrief;
}

// Brief student info from login
interface StudentBrief {
  no: string;
  name: string;
}

// Student game profile from /api/students/{no}/profile
interface StudentProfile {
  nickname: string;
  level: number;
  hp: number;
  maxHp: number;
  attack: number;
  defense: number;
  exp: number;
  coins: number;
  energy: number;
  maxEnergy: number;
}

// Tower map node from /api/students/{no}/tower-map
interface TowerNode {
  id: number;
  name: string;
  roomType: string;
  floor: number;
  status: string;
  kpId: number;
  kpName: string;
}

// Mistake item from /api/students/{no}/mistakes
interface MistakeItem {
  questionId: number;
  questionStem: string;
  stem: string;
  knowledgePointName: string;
}

// Game event request body
interface GameEventBody {
  course_id: string;
  event_type: string;
  payload: Object;
}

// Generic API response wrapper
interface ApiResponse {
  code: number;
  message: string;
  data: Object;
}

// Maps items in the mistakes API response
interface MistakeListData {
  wrongList: MistakeItem[];
}

// Tower map API response data
interface TowerMapData {
  nodes: TowerNode[];
}
