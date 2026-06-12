import { ref, computed } from 'vue'
import type { GroupMember } from '@/types/message'
import { groupMemberDisplayName } from '@/utils/chat-message'

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function useMention(
  getGroupMembers: () => GroupMember[],
  getGroupInput: () => string,
  setGroupInput: (v: string) => void,
  getCurrentUserId: () => number,
) {
  const mentionVisible = ref(false)
  const mentionKeyword = ref('')

  const mentionCandidates = computed(() => {
    const kw = mentionKeyword.value.trim().toLowerCase()
    return getGroupMembers()
      .filter((m) => m.userId !== getCurrentUserId())
      .filter((m) => {
        const name = groupMemberDisplayName(m).toLowerCase()
        return !kw || name.includes(kw)
      })
      .slice(0, 8)
  })

  function onGroupInput() {
    const val = getGroupInput()
    const at = val.lastIndexOf('@')
    if (at >= 0 && (at === 0 || /[\s\n]/.test(val.charAt(at - 1)))) {
      const tail = val.slice(at + 1)
      if (!tail.includes('\n') && !tail.includes(' ')) {
        mentionKeyword.value = tail
        mentionVisible.value = true
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
    setGroupInput(`${val.slice(0, at)}@${name} `)
    mentionVisible.value = false
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

  function resetMention() {
    mentionVisible.value = false
    mentionKeyword.value = ''
  }

  return {
    mentionVisible,
    mentionCandidates,
    onGroupInput,
    pickMention,
    parseMentionIds,
    resetMention,
  }
}
