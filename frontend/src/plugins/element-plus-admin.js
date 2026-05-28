import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/table-column/style/css'

const adminComponents = [
  ElTable,
  ElTableColumn
]

export function installElementPlusAdmin(app) {
  adminComponents.forEach((component) => {
    app.use(component)
  })
}
