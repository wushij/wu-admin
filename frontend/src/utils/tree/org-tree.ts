/** 组织树节点（children 可选，叶子节点可不声明 children） */
export interface OrgTreeNode {
  id?: number
  parentId?: number | null
  children?: OrgTreeNode[]
}

/** 查找唯一的 parentId=0 根节点（供上级部门解析等场景使用） */
export function findSingleOrgRoot<T extends OrgTreeNode>(
  roots: T[] | null | undefined,
  rootParentId = 0
): T | null {
  if (!roots?.length) return null
  const root = roots[0]
  if (roots.length === 1 && (root.parentId ?? 0) === rootParentId) return root
  return null
}

/** 组织部门树完整展示（保留总公司等顶级根节点，与岗位体系层级保持一致） */
export function displayOrgTree<T extends OrgTreeNode>(
  roots: T[] | null | undefined,
  _rootParentId = 0
): T[] {
  if (!roots?.length) return []
  return roots
}

/** 部门树「新增一级部门」应默认挂到顶级公司节点（如总公司）下 */
export function resolveDeptRootParentId(
  roots: OrgTreeNode[] | null | undefined,
  fallback = 1
): number {
  const hidden = findSingleOrgRoot(roots, 0)
  return hidden?.id ?? fallback
}

/** 树默认展开前 maxDepth 层（0=全折叠，2=展开到第二级，与岗位体系默认展示一致） */
export function collectExpandKeysByDepth<T extends { id?: number; children?: T[] }>(
  nodes: T[] | null | undefined,
  maxDepth: number,
  depth = 0,
  keys: number[] = []
): number[] {
  if (!nodes?.length) return keys
  for (const node of nodes) {
    const id = node.id
    if (id != null && node.children?.length) {
      if (depth < maxDepth) {
        keys.push(id)
      }
      collectExpandKeysByDepth(node.children, maxDepth, depth + 1, keys)
    }
  }
  return keys
}
