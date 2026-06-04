/**
 * Validates the checked-in demo expansion SQL asset.
 *
 * The large SQL file is maintained directly because it contains a fixed demo
 * catalog used by Docker seed runs. This guard catches the shop-owner conflict
 * that previously caused the shop insert batch to fail silently.
 *
 * Run:
 *   node scripts/generate-demo-expansion-sql.mjs
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const output = path.join(__dirname, '..', 'backend', 'src', 'main', 'resources', 'seed', 'demo-expansion.sql')

const sql = fs.readFileSync(output, 'utf8')

const requiredShopIds = [4201, 4202, 4203, 4204]
const missingShopIds = requiredShopIds.filter((id) => !sql.includes(`(${id},`))
if (missingShopIds.length > 0) {
  throw new Error(`demo-expansion.sql is missing required shop ids: ${missingShopIds.join(', ')}`)
}

for (const duplicateOwnerId of [1010, 1012]) {
  const pattern = new RegExp(`\\(42\\d{2}, ${duplicateOwnerId},`)
  if (pattern.test(sql)) {
    throw new Error(`demo-expansion.sql must not create an extra shop for existing owner ${duplicateOwnerId}`)
  }
}

console.log(`Validated ${path.relative(path.join(__dirname, '..'), output)}`)
