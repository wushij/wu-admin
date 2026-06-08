import { describe, expect, it } from 'vitest'
import {
  displayOrgTree,
  findSingleOrgRoot,
  resolveDeptRootParentId,
  type OrgTreeNode,
} from '@/utils/org-tree'

describe('org-tree', () => {
  it('unwraps single root with children', () => {
    const roots: OrgTreeNode[] = [
      { id: 1, parentId: 0, children: [{ id: 2, parentId: 1 }] },
    ]
    expect(displayOrgTree(roots)).toEqual([{ id: 2, parentId: 1 }])
  })

  it('keeps tree when multiple roots', () => {
    const roots: OrgTreeNode[] = [
      { id: 1, parentId: 0 },
      { id: 2, parentId: 0 },
    ]
    expect(displayOrgTree(roots)).toHaveLength(2)
  })

  it('resolveDeptRootParentId returns hidden root id', () => {
    const roots: OrgTreeNode[] = [{ id: 1, parentId: 0, children: [{ id: 2, parentId: 1 }] }]
    expect(resolveDeptRootParentId(roots)).toBe(1)
    expect(findSingleOrgRoot(roots)?.id).toBe(1)
  })
})
