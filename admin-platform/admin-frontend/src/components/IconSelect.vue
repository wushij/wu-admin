<template>
  <el-select
    v-model="selectedIcon"
    placeholder="请选择图标"
    clearable
    filterable
    @change="handleIconChange"
    class="icon-select"
  >
    <template #prefix>
      <el-icon v-if="selectedIcon">
        <component :is="selectedIcon" />
      </el-icon>
    </template>
    
    <el-option
      v-for="icon in iconList"
      :key="icon.name"
      :label="icon.name"
      :value="icon.name"
    >
      <div class="icon-option">
        <el-icon :size="18">
          <component :is="icon.component" />
        </el-icon>
        <span class="icon-name">{{ icon.name }}</span>
      </div>
    </el-option>
  </el-select>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const selectedIcon = ref(props.modelValue)

// 菜单常用图标（置顶，便于在「菜单管理」里快速选到）
const MENU_COMMON_ICONS = [
  'Folder', 'FolderOpened', 'Document', 'DocumentCopy', 'Files', 'List', 'Menu',
  'Setting', 'User', 'Key', 'UserFilled', 'Monitor', 'OfficeBuilding', 'Tickets',
  'HomeFilled', 'Management', 'Grid', 'Collection', 'Tools', 'Lock', 'Bell'
]

// Element Plus 所有图标（200+）
const allIcons = [
  // 基础图标
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
  'SetUp', 'SetUpFilled', 'Share', 'Ship', 'Shop', 'ShoppingBag', 
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

// 图标列表：常用置顶 + 其余按字母
const iconList = computed(() => {
  const valid = allIcons.filter((name) => ElementPlusIconsVue[name])
  const commonSet = new Set(MENU_COMMON_ICONS)
  const common = MENU_COMMON_ICONS.filter((name) => valid.includes(name))
  const rest = valid.filter((name) => !commonSet.has(name)).sort()
  return [...common, ...rest].map((name) => ({
    name,
    component: ElementPlusIconsVue[name]
  }))
})

// 监听外部变化
watch(() => props.modelValue, (newVal) => {
  selectedIcon.value = newVal
})

// 图标选择变化
const handleIconChange = (value) => {
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style scoped>
.icon-select {
  width: 100%;
}

.icon-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-name {
  font-size: 13px;
}
</style>
