# Scripts

本目录用于存放开发辅助脚本，例如：

- `generate-perf-catalog-sql.mjs`：生成 `backend/src/main/resources/seed/data-perf-catalog.sql`（性能测试用大量商品 `INSERT`）。用法：`node scripts/generate-perf-catalog-sql.mjs 4000`（数量可选，默认 **3500**，上限 9000）
- 数据导入导出脚本
- 数据清洗脚本
- 测试辅助脚本
- 报表生成脚本
