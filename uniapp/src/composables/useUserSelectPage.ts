import { ref, computed } from 'vue'
import { getUserList } from '@/api/system/user'
import type { UserVO } from '@/types/user'

function userLabel(user: UserVO) {
  const name = user.nickname || user.username
  return user.deptName ? `${name}（${user.deptName}）` : name
}

function parseInitialId(raw: string | undefined): number | null {
  if (raw == null || raw === '') return null
  const id = Number(raw)
  return Number.isFinite(id) && id > 0 ? id : null
}

export function useUserSelectPage() {
  const loading = ref(false)
  const keyword = ref('')
  const users = ref<UserVO[]>([])
  const selectedId = ref<number | null>(null)
  const allowEmpty = ref(true)
  const emptyLabel = ref('不设置')
  const usersLoaded = ref(false)
  const activeSession = ref('')
  const userTouched = ref(false)

  const filteredUsers = computed(() => {
    const q = keyword.value.trim().toLowerCase()
    if (!q) return users.value
    return users.value.filter((user) => {
      const name = userLabel(user).toLowerCase()
      const mobile = (user.mobile || '').toLowerCase()
      const username = (user.username || '').toLowerCase()
      return name.includes(q) || mobile.includes(q) || username.includes(q)
    })
  })

  function normalizeId(id: number | null | undefined): number | null {
    if (id == null) return null
    const n = Number(id)
    return Number.isFinite(n) && n > 0 ? n : null
  }

  function initFromRoute(options: { initialId?: string }, session: string) {
    if (!session || session === activeSession.value) return
    activeSession.value = session
    userTouched.value = false
    selectedId.value = parseInitialId(options.initialId)
    keyword.value = ''
  }

  async function ensureUsers() {
    if (usersLoaded.value || loading.value) return
    loading.value = true
    try {
      const res = await getUserList()
      users.value = (res.data || []).filter((u) => u.status !== 0)
      usersLoaded.value = true
    } finally {
      loading.value = false
    }
  }

  function selectId(id: number | null) {
    userTouched.value = true
    selectedId.value = normalizeId(id)
  }

  function isSelected(id: number | null) {
    const picked = selectedId.value
    if (id == null) return picked == null
    return Number(picked) === Number(id)
  }

  function resolveSelectedUser(id = selectedId.value) {
    if (id == null) return null
    return users.value.find((u) => Number(u.id) === Number(id)) || null
  }

  function resolveSelectedLabel(id = selectedId.value) {
    if (id == null) return emptyLabel.value
    const user = resolveSelectedUser(id)
    return user ? userLabel(user) : emptyLabel.value
  }

  return {
    loading,
    keyword,
    users,
    filteredUsers,
    selectedId,
    allowEmpty,
    emptyLabel,
    initFromRoute,
    ensureUsers,
    selectId,
    isSelected,
    userLabel,
    resolveSelectedUser,
    resolveSelectedLabel,
  }
}
