import { ref, computed } from 'vue'
import type { GroupMember } from '@/types/message'
import { groupMemberDisplayName } from '@/utils/chat-message'

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function useMention(getGroupMembers: () => GroupMember[], getGroupInput: () => string, setGroupInput: (v: string) => void, getCurrentUserId: () => number) {
  const mentionVisible = ref(false)
  const mentionKeyword = ref('')
  const mentionIndex = ref(0)
  const pendingMentionIds = ref<number[]>([])

  const mentionCandidates = computed(() => {
    const kw = mentionKeyword.value.trim().toLowerCase()
    return getGroupMembers()
      .filter(m => m.userId !== getCurrentUserId())
      .filter(m => {
        const name = groupMemberDisplayName(m).toLowerCase()
        return !kw || name.includes(kw)
      })
      .slice(0, 10)
  })

  function onGroupInput() {
    const val = getGroupInput()
    const at = val.lastIndexOf('@')
    if (at >= 0 && (at === 0 || /[\s\n]/.test(val.charAt(at - 1)))) {
      const tail = val.slice(at + 1)
      if (!tail.includes('\n') && !tail.includes(' ')) {
        mentionKeyword.value = tail
        mentionVisible.value = true
        mentionIndex.value = 0
        return
      }
    }
    mentionVisible.value = false
  }

  function pickMention(m: GroupMember) {
    const name = groupMemberDisplayName(m)
    const val = getGroupInput()
    const at = val.lastIndexOf('@')
    if (at < 0) return
    setGroupInput(val.slice(0, at) + `@${name} `)
    mentionVisible.value = false
    if (!pendingMentionIds.value.includes(m.userId)) pendingMentionIds.value.push(m.userId)
  }

  function parseMentionIds(text: string): number[] {
    const ids: number[] = []
    for (const m of getGroupMembers()) {
      const name = groupMemberDisplayName(m)
      if (!name) continue
      const re = new RegExp(`@${escapeRegExp(name)}(?:\\s|$|[，。！？,.!?])`)
      if (re.test(text)) ids.push(m.userId)
    }
    return [...new Set(ids)]
  }

  function onGroupInputKeydown(e: KeyboardEvent) {
    if (!mentionVisible.value || !mentionCandidates.value.length) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      mentionIndex.value = (mentionIndex.value + 1) % mentionCandidates.value.length
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      mentionIndex.value = (mentionIndex.value - 1 + mentionCandidates.value.length) % mentionCandidates.value.length
    } else if (e.key === 'Escape') {
      mentionVisible.value = false
    }
  }

  function resetMention() {
    mentionVisible.value = false
    pendingMentionIds.value = []
  }

  return {
    mentionVisible, mentionKeyword, mentionIndex, pendingMentionIds,
    mentionCandidates, onGroupInput, pickMention, parseMentionIds,
    onGroupInputKeydown, resetMention,
  }
}
