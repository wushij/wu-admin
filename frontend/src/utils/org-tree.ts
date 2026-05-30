/** 组织树节点（children 可选，叶子节点可不声明 children） */
export interface OrgTreeNode {
  id?: number
  parentId?: number | null
  children?: OrgTreeNode[]
}

/** 组织树：隐藏唯一的 parentId=0 根节点，仅展示其下级（如「本部」不展示，直接展示各中心） */
export function findSingleOrgRoot<T extends OrgTreeNode>(
  roots: T[] | null | undefined,
  rootParentId = 0
): T | null {
  if (!roots?.length) return null
  const root = roots[0]
  if (roots.length === 1 && (root.parentId ?? 0) === rootParentId) return root
  return null
}

export function displayOrgTree<T extends OrgTreeNode>(
  roots: T[] | null | undefined,
  rootParentId = 0
): T[] {
  if (!roots?.length) return []
  const hidden = findSingleOrgRoot(roots, rootParentId)
  if (hidden?.children?.length) return hidden.children as T[]
  return roots
}

/** 部门树隐藏根节点后，「新增一级部门」应挂到该根 id 下 */
export function resolveDeptRootParentId(
  roots: OrgTreeNode[] | null | undefined,
  fallback = 1
): number {
  const hidden = findSingleOrgRoot(roots, 0)
  return hidden?.id ?? fallback
}
