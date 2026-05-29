<template>
  <el-popover
    v-model:visible="popoverVisible"
    placement="bottom"
    :fallback-placements="['top', 'bottom-start', 'top-start']"
    :width="400"
    trigger="click"
    :show-arrow="true"
    popper-class="icon-select-popper"
  >
    <template #reference>
      <el-input
        :model-value="displayValue"
        placeholder="请选择图标"
        readonly
        clearable
        class="icon-select-trigger"
        @clear="handleClear"
      >
        <template #prefix>
          <el-icon v-if="modelValue" class="trigger-icon">
            <component :is="currentIconComponent" />
          </el-icon>
        </template>
      </el-input>
    </template>

    <div class="icon-picker-panel">
      <el-input
        v-model="searchText"
        placeholder="搜索图标"
        clearable
        :prefix-icon="Search"
        class="icon-search"
      />
      <div class="icon-grid-scroll">
        <div v-if="filteredIcons.length" class="icon-grid">
          <div
            v-for="item in filteredIcons"
            :key="item.name"
            class="icon-grid-item"
            :class="{ active: modelValue === item.name }"
            :title="item.name"
            @click="handleSelect(item.name)"
          >
            <el-icon :size="22">
              <component :is="item.component" />
            </el-icon>
            <span class="icon-label">{{ item.label }}</span>
          </div>
        </div>
        <el-empty v-else description="未找到匹配图标" :image-size="56" />
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, computed, watch, type Component } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { Search } from '@element-plus/icons-vue'
import { getIconLabel } from '@/utils/icon-labels'

const iconModules = ElementPlusIconsVue as Record<string, Component>

interface IconItem {
  name: string
  label: string
  component: Component
}

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const popoverVisible = ref(false)
const searchText = ref('')

/** 默认优先展示（置顶），其余图标跟在后面 */
const PRIORITY_ICON_NAMES = [
  'HomeFilled', 'Setting', 'User', 'UserFilled', 'Key', 'Lock',
  'Document', 'DocumentCopy', 'Files', 'Folder', 'FolderOpened', 'Grid',
  'ElementPlus', 'List', 'Menu', 'Search', 'Plus', 'Edit', 'Delete',
  'Upload', 'Download', 'Picture', 'Camera', 'VideoCamera', 'Headset',
  'Reading', 'Notebook', 'Collection', 'CollectionTag', 'Calendar', 'Clock',
  'AlarmClock', 'Monitor', 'OfficeBuilding', 'Management', 'Tickets',
  'Bell', 'Message', 'Tools', 'Operation', 'Phone', 'Printer', 'Link',
  'View', 'Refresh', 'Star', 'Warning', 'InfoFilled', 'Checked',
  'DataAnalysis', 'DataBoard', 'TrendCharts', 'PieChart', 'Histogram',
  'Suitcase', 'Promotion', 'Share', 'Connection', 'MapLocation', 'Wallet',
  'ShoppingCart', 'Shop', 'Service', 'Help', 'Guide', 'Compass', 'Flag',
  'ChatDotRound', 'Comment', 'Notification', 'Mute', 'Microphone',
  'VideoPlay', 'PictureFilled', 'FolderAdd', 'DocumentAdd', 'CopyDocument',
  'Filter', 'Sort', 'Expand', 'Fold', 'FullScreen', 'Hide', 'Location',
  'Cellphone', 'CreditCard', 'Goods', 'Platform', 'Memo', 'Timer',
  'Moon', 'Sunny', 'Cloudy', 'Brush', 'Medal', 'Trophy', 'Rank',
  'Open', 'TurnOff', 'Switch', 'More', 'Pointer', 'Present', 'PriceTag',
  'Postcard', 'Paperclip', 'Crop', 'Coordinate', 'MagicStick', 'Lightning',
  'Loading', 'CircleCheck', 'CircleClose', 'SuccessFilled', 'Failed',
  'RefreshRight', 'RefreshLeft', 'SortUp', 'SortDown', 'ZoomIn', 'ZoomOut',
  'Minus', 'Remove', 'EditPen', 'UploadFilled', 'Unlock',
  'Avatar', 'Male', 'Female', 'School', 'Van', 'Ship', 'Bicycle',
  'Coffee', 'Food', 'KnifeFork', 'Box', 'Handbag', 'Discount', 'Sell',
  'Finished', 'Select', 'Aim', 'Back', 'Right', 'Top', 'Bottom'
]

const ALL_ICON_NAMES = [
  'AddLocation', 'Aim', 'AlarmClock', 'Apple', 'ArrowDown', 'ArrowDownBold',
  'ArrowLeft', 'ArrowLeftBold', 'ArrowRight', 'ArrowRightBold', 'ArrowUp',
  'ArrowUpBold', 'Avatar', 'Back', 'Baseball', 'Basketball', 'Bell',
  'BellFilled', 'Bicycle', 'Bottom', 'BottomLeft', 'BottomRight', 'Bowl',
  'Box', 'Briefcase', 'Brush', 'BrushFilled', 'Burger', 'Calendar',
  'Camera', 'CameraFilled', 'CaretBottom', 'CaretLeft', 'CaretRight',
  'CaretTop', 'Cellphone', 'ChatDotRound', 'ChatDotSquare', 'ChatLineRound',
  'ChatLineSquare', 'ChatRound', 'ChatSquare', 'Check', 'Checked', 'Cherry',
  'Chicken', 'ChromeFilled', 'CircleCheck', 'CircleCheckFilled', 'CircleClose',
  'CircleCloseFilled', 'CirclePlus', 'CirclePlusFilled', 'Clock', 'Close',
  'CloseBold', 'Cloudy', 'Coffee', 'CoffeeCup', 'Coin', 'ColdDrink',
  'Collection', 'CollectionTag', 'Comment', 'Compass', 'Connection',
  'Coordinate', 'CopyDocument', 'CreditCard', 'Crop', 'DArrowLeft',
  'DArrowRight', 'DArrowUp', 'DArrowDown', 'DataAnalysis', 'DataBoard',
  'DataLine', 'Delete', 'DeleteFilled', 'DeleteLocation', 'Dessert',
  'Discount', 'Dish', 'DishDot', 'Document', 'DocumentAdd', 'DocumentChecked',
  'DocumentCopy', 'DocumentDelete', 'DocumentRemove', 'Download', 'Drizzling',
  'Edit', 'EditPen', 'Eleme', 'ElemeFilled', 'ElementPlus', 'Expand',
  'Failed', 'Female', 'Files', 'Film', 'Filter', 'Finished', 'FirstAidKit',
  'Flag', 'Fold', 'Folder', 'FolderAdd', 'FolderChecked', 'FolderDelete',
  'FolderOpened', 'FolderRemove', 'Food', 'Football', 'ForkSpoon', 'Fries',
  'FullScreen', 'Goblet', 'GobletFull', 'GobletSquare', 'GobletSquareFull',
  'GoldMedal', 'Goods', 'GoodsFilled', 'Grape', 'Grid', 'Guide',
  'Handbag', 'Headset', 'Help', 'HelpFilled', 'Hide', 'Histogram',
  'HomeFilled', 'HotWater', 'House', 'IceCream', 'IceCreamRound',
  'IceCreamSquare', 'IceDrink', 'IceTea', 'InfoFilled', 'Iphone', 'Key',
  'KnifeFork', 'Lightning', 'Link', 'List', 'Loading', 'Location',
  'LocationFilled', 'LocationInformation', 'Lock', 'Lollipop', 'MagicStick',
  'Magnet', 'Male', 'Management', 'MapLocation', 'Medal', 'Memo',
  'Menu', 'Message', 'MessageBox', 'Mic', 'Microphone', 'Minus',
  'Money', 'Monitor', 'Moon', 'MoonNight', 'More', 'MoreFilled',
  'MostlyCloudy', 'MostlySunny', 'Mug', 'Mute', 'MuteNotification',
  'NoSmoking', 'Notebook', 'Notification', 'Odometer', 'OfficeBuilding',
  'Open', 'Operation', 'Opportunity', 'Orange', 'Paperclip', 'PartlyCloudy',
  'Pear', 'Phone', 'PhoneFilled', 'Picture', 'PictureFilled', 'PictureRounded',
  'PieChart', 'Place', 'Platform', 'Plus', 'Pointer', 'Position',
  'Postcard', 'Pouring', 'Present', 'PriceTag', 'Printer', 'Promotion',
  'QuartzWatch', 'QuestionFilled', 'Rank', 'Reading', 'ReadingLamp',
  'Refresh', 'RefreshLeft', 'RefreshRight', 'Refrigerator', 'Remove',
  'RemoveFilled', 'Right', 'ScaleToOriginal', 'School', 'Scissor', 'Search',
  'Select', 'Sell', 'SemiSelect', 'Service', 'SetUp', 'Setting',
  'Share', 'Ship', 'Shop', 'ShoppingBag',
  'ShoppingCart', 'ShoppingCartFull', 'ShoppingTrolley', 'Smoking',
  'Soccer', 'SoldOut', 'Sort', 'SortDown', 'SortUp', 'Stamp',
  'Star', 'StarFilled', 'Stopwatch', 'SuccessFilled', 'Sunny',
  'Sunrise', 'Sunset', 'Switch', 'SwitchButton', 'SwitchFilled',
  'TakeawayBox', 'Ticket', 'Tickets', 'Timer', 'ToiletPaper',
  'Tools', 'Top', 'TopLeft', 'TopRight', 'TrendCharts', 'Trophy',
  'TrophyBase', 'TurnOff', 'Umbrella', 'Unlock', 'Upload', 'UploadFilled',
  'User', 'UserFilled', 'Van', 'VideoCamera', 'VideoCameraFilled',
  'VideoPause', 'VideoPlay', 'View', 'Wallet', 'WalletFilled', 'Warning',
  'WarningFilled', 'Watch', 'Watermelon', 'WindPower', 'ZoomIn', 'ZoomOut'
]

function toIconItems(names: string[]): IconItem[] {
  return names
    .filter((name) => iconModules[name])
    .map((name) => ({
      name,
      label: getIconLabel(name),
      component: iconModules[name]
    }))
}

const fullIconCatalog = computed(() => {
  const valid = ALL_ICON_NAMES.filter((name) => iconModules[name])
  const prioritySet = new Set(PRIORITY_ICON_NAMES)
  const priority = PRIORITY_ICON_NAMES.filter((name) => valid.includes(name))
  const rest = valid.filter((name) => !prioritySet.has(name)).sort()
  return toIconItems([...priority, ...rest])
})

const filteredIcons = computed(() => {
  const q = searchText.value.trim().toLowerCase()
  if (!q) {
    return fullIconCatalog.value
  }
  return fullIconCatalog.value.filter(
    (item) =>
      item.name.toLowerCase().includes(q) ||
      item.label.toLowerCase().includes(q)
  )
})

const currentIconComponent = computed(() => {
  if (!props.modelValue) return null
  return iconModules[props.modelValue] || null
})

const displayValue = computed(() => {
  if (!props.modelValue) return ''
  return getIconLabel(props.modelValue)
})

watch(popoverVisible, (visible) => {
  if (!visible) {
    searchText.value = ''
  }
})

const handleSelect = (name: string) => {
  emit('update:modelValue', name)
  emit('change', name)
  popoverVisible.value = false
  searchText.value = ''
}

const handleClear = () => {
  emit('update:modelValue', '')
  emit('change', '')
}
</script>

<style scoped>
.icon-select-trigger {
  width: 100%;
  cursor: pointer;
}

.icon-select-trigger :deep(.el-input__inner) {
  cursor: pointer;
}

.trigger-icon {
  font-size: 18px;
}

.icon-picker-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  max-width: 376px;
  overflow: hidden;
}

.icon-search {
  width: 100%;
  flex-shrink: 0;
}

.icon-grid-scroll {
  max-height: 300px;
  overflow-y: auto;
  overflow-x: hidden;
  width: 100%;
}

.icon-grid-scroll::-webkit-scrollbar {
  width: 6px;
}

.icon-grid-scroll::-webkit-scrollbar-thumb {
  background: var(--el-border-color);
  border-radius: 3px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 4px;
  width: 100%;
  box-sizing: border-box;
  padding: 2px 0 6px;
}

.icon-grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 4px;
  min-width: 0;
  padding: 8px 2px 6px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s;
}

.icon-grid-item:hover {
  background: var(--el-fill-color-light);
}

.icon-grid-item.active {
  background: var(--el-fill-color);
  color: var(--el-text-color-primary);
}

.icon-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.2;
  text-align: center;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 2px;
  box-sizing: border-box;
}

.icon-grid-item.active .icon-label {
  color: var(--el-text-color-primary);
}
</style>

<style>
.icon-select-popper {
  padding: 8px !important;
  overflow: hidden !important;
}

.icon-select-popper .el-popover__title {
  margin: 0;
}
</style>
