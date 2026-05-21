/**
 * Generates backend/src/main/resources/seed/data-perf-catalog.sql
 * Run: node scripts/generate-perf-catalog-sql.mjs [count]
 * Default count: 3500
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const count = Math.min(9000, Math.max(1, parseInt(process.argv[2] || '3500', 10)))
const startId = 10000
const endId = startId + count

const catNames = ['学习资料', '学习工具', '宿舍生活', '数码配件']

function esc(s) {
  return `'${String(s).replace(/'/g, "''")}'`
}

function productRow(i) {
  const id = startId + i
  const seller = i % 2 === 0 ? 1001 : 1004
  const shop = i % 2 === 0 ? 4001 : 4002
  const categoryId = 1 + (i % 4)
  const digital = i % 9 === 0
  const productType = digital ? 'digital' : 'physical'
  const reviewStatus = digital ? 'approved' : 'not_required'
  const supportsLogistics = !digital && i % 3 !== 0
  const supportsDigital = digital
  const allowPreview = digital
  const supportsLogisticsSql = supportsLogistics ? 'TRUE' : 'FALSE'
  const supportsDigitalSql = supportsDigital ? 'TRUE' : 'FALSE'
  const allowPreviewSql = allowPreview ? 'TRUE' : 'FALSE'
  const previewRule = digital ? esc('预览版性能测试占位') : 'NULL'
  const title = `【压力测试SQL#${id}】${catNames[categoryId - 1]} 样本`
  const subtitle = `batch seed seller=${seller} cat=${categoryId}`
  const description = `Synthetic catalog row ${i} for list and API performance testing.`
  const sale = (5 + (i % 250) + 0.9).toFixed(2)
  const original = (parseFloat(sale) + 10 + (i % 80)).toFixed(2)
  const stock = 1 + (i % 120)
  const viewCount = 20 + (i * 37) % 8000
  const favoriteCount = (i * 13) % 200
  const image = `https://picsum.photos/seed/cm${id}/640/480`

  return `(${id}, ${seller}, ${shop}, ${categoryId}, ${esc(title)}, ${esc(subtitle)}, ${esc(description)}, ${esc(description)}, ${esc(productType)}, 'on_sale', ${esc(reviewStatus)}, NULL, ${esc(image)}, ${sale}, ${original}, ${stock}, ${supportsLogisticsSql}, TRUE, ${supportsDigitalSql}, ${allowPreviewSql}, ${previewRule}, ${viewCount}, ${favoriteCount}, FALSE, TIMESTAMP '2026-05-10 12:00:00', CURRENT_TIMESTAMP)`
}

const lines = []
lines.push(
  '-- Auto-generated perf catalog (on-sale products). Reserved id range; safe to re-run after DELETE.',
  `DELETE FROM product_media WHERE product_id >= ${startId} AND product_id < ${endId};`,
  `DELETE FROM products WHERE id >= ${startId} AND id < ${endId};`,
  ''
)

const chunk = 40
for (let offset = 0; offset < count; offset += chunk) {
  const lim = Math.min(chunk, count - offset)
  const rows = []
  for (let j = 0; j < lim; j++) {
    rows.push(productRow(offset + j))
  }
  lines.push(
    'INSERT INTO products (id, seller_user_id, shop_id, category_id, title, subtitle, description, detail_content, product_type, status, review_status, review_reject_reason, main_image_url, sale_price, original_price, stock_quantity, supports_logistics, supports_offline_delivery, supports_digital_delivery, allow_preview, preview_rule_text, view_count, favorite_count, is_deleted, created_at, updated_at) VALUES'
  )
  lines.push(rows.join(',\n') + ';')
  lines.push('')
}

const mediaRows = []
for (let i = 0; i < count; i++) {
  const id = startId + i
  const url = `https://picsum.photos/seed/cm${id}/640/480`
  mediaRows.push(`(${id}, 'cover', '${url}', 1)`)
}
lines.push('INSERT INTO product_media (product_id, media_type, media_url, sort_order) VALUES')
lines.push(mediaRows.join(',\n') + ';')
lines.push('')

const out = path.join(__dirname, '..', 'backend', 'src', 'main', 'resources', 'seed', 'data-perf-catalog.sql')
fs.writeFileSync(out, lines.join('\n'), 'utf8')
console.log(`Wrote ${count} products to ${out} (ids ${startId}..${endId - 1})`)
