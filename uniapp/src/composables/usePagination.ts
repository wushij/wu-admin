import { ref, computed } from 'vue'

/** 与后端 PageParam 字段一致 */
export interface PaginationQuery {
  pageNo: number
  pageSize: number
}

export function usePagination(initialPageSize = 15) {
  const pageNo = ref(1)
  const pageSize = ref(initialPageSize)
  const total = ref(0)

  const query = computed<PaginationQuery>(() => ({
    pageNo: pageNo.value,
    pageSize: pageSize.value,
  }))

  const hasMore = computed(() => pageNo.value * pageSize.value < total.value)

  function reset() {
    pageNo.value = 1
    total.value = 0
  }

  function nextPage() {
    if (hasMore.value) pageNo.value += 1
  }

  function setTotal(value: number) {
    total.value = value
  }

  return {
    pageNo,
    pageSize,
    total,
    query,
    hasMore,
    reset,
    nextPage,
    setTotal,
  }
}
