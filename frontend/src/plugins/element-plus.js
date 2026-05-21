import { ElAlert } from 'element-plus/es/components/alert/index.mjs'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElCheckbox, ElCheckboxGroup } from 'element-plus/es/components/checkbox/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElDrawer } from 'element-plus/es/components/drawer/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElMenu, ElMenuItem } from 'element-plus/es/components/menu/index.mjs'
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs'
import { ElRadio, ElRadioButton, ElRadioGroup } from 'element-plus/es/components/radio/index.mjs'
import { ElRate } from 'element-plus/es/components/rate/index.mjs'
import { ElSegmented } from 'element-plus/es/components/segmented/index.mjs'
import { ElSelect, ElOption } from 'element-plus/es/components/select/index.mjs'
import { ElSkeleton } from 'element-plus/es/components/skeleton/index.mjs'
import { ElSwitch } from 'element-plus/es/components/switch/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { ElTabs, ElTabPane } from 'element-plus/es/components/tabs/index.mjs'
import { ElTag } from 'element-plus/es/components/tag/index.mjs'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/es/components/alert/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/checkbox/style/css'
import 'element-plus/es/components/checkbox-group/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/drawer/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/form-item/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/input-number/style/css'
import 'element-plus/es/components/menu/style/css'
import 'element-plus/es/components/menu-item/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/option/style/css'
import 'element-plus/es/components/pagination/style/css'
import 'element-plus/es/components/radio/style/css'
import 'element-plus/es/components/radio-button/style/css'
import 'element-plus/es/components/radio-group/style/css'
import 'element-plus/es/components/rate/style/css'
import 'element-plus/es/components/segmented/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/skeleton/style/css'
import 'element-plus/es/components/switch/style/css'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/table-column/style/css'
import 'element-plus/es/components/tab-pane/style/css'
import 'element-plus/es/components/tabs/style/css'
import 'element-plus/es/components/tag/style/css'

const components = [
  ElAlert,
  ElButton,
  ElCheckbox,
  ElCheckboxGroup,
  ElDialog,
  ElDrawer,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElRadio,
  ElRadioButton,
  ElRadioGroup,
  ElRate,
  ElSegmented,
  ElSelect,
  ElSkeleton,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag
]

export function installElementPlus(app) {
  components.forEach((component) => {
    app.use(component)
  })
}
