import type { MenuTreeNode } from '@/types/api'

/** 侧栏/菜单树节点（含 children） */
export interface MenuNode extends MenuTreeNode {
  children?: MenuNode[]
}
