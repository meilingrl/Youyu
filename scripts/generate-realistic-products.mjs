#!/usr/bin/env node

/**
 * 生成真实的校园商品种子数据
 * 使用 Agent 工具批量生成商品信息、评价、图片
 */

import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 配置
const CONFIG = {
  totalProducts: 500,
  batchSize: 20, // 每批生成 20 个商品
  reviewsPerProduct: [3, 5], // 每个商品 3-5 条评价
  imagesPerProduct: [1, 3], // 每个商品 1-3 张图片
  outputDir: path.join(__dirname, '..', 'backend', 'src', 'main', 'resources', 'seed'),
  outputFile: 'data-realistic-products.sql'
};

// 商品类别（对应数据库中的 categories）
const CATEGORIES = [
  { id: 1, name: '教材教辅', keywords: ['教材', '习题集', '考研资料', '四六级', '专业书'] },
  { id: 2, name: '文具用品', keywords: ['笔记本', '中性笔', '荧光笔', '便利贴', '文件夹'] },
  { id: 3, name: '数码配件', keywords: ['充电器', '数据线', '耳机', 'U盘', '鼠标'] },
  { id: 4, name: '生活用品', keywords: ['水杯', '衣架', '收纳盒', '台灯', '插座'] },
  { id: 5, name: '零食饮料', keywords: ['薯片', '饼干', '泡面', '饮料', '坚果'] },
  { id: 6, name: '运动户外', keywords: ['篮球', '跳绳', '瑜伽垫', '运动鞋', '水壶'] }
];

// 店铺 ID 范围（假设已有 10 个店铺）
const SHOP_IDS = Array.from({ length: 10 }, (_, i) => i + 1);

// 用户 ID 范围（假设已有 20 个用户）
const USER_IDS = Array.from({ length: 20 }, (_, i) => i + 1);

/**
 * 生成商品数据的 prompt
 */
function generateProductPrompt(category, startId, count) {
  return `你是一个校园电商平台的数据生成助手。请生成 ${count} 个"${category.name}"类别的真实商品数据。

要求：
1. 商品名称：真实的学生用品名称，简洁明了（10-30字）
2. 商品描述：突出使用场景（宿舍/自习室/课堂），50-100字
3. 价格：符合学生消费水平
   - 教材教辅：20-150元
   - 文具用品：5-50元
   - 数码配件：15-200元
   - 生活用品：10-100元
   - 零食饮料：3-30元
   - 运动户外：30-300元
4. 库存：根据商品类型合理设置（教材少量 10-50，零食大量 100-500）
5. 商品类型：80% 实物商品（type: 'physical'），20% 数字商品（type: 'digital'，如电子书、网课）

请以 JSON 数组格式返回，每个商品包含：
{
  "id": ${startId}, // 从 ${startId} 开始递增
  "name": "商品名称",
  "description": "商品描述",
  "price": 价格（数字，保留2位小数）,
  "stock": 库存数量（整数）,
  "type": "physical" 或 "digital",
  "category_id": ${category.id},
  "shop_id": 随机选择 1-10,
  "images": ["图片URL1", "图片URL2"], // 1-3张，使用 unsplash.com 占位图
  "reviews": [
    {
      "user_id": 随机选择 1-20,
      "rating": 3-5 星（整数）,
      "content": "真实的学生评价（30-80字，提到使用场景）",
      "created_at": "2024-01-01 到 2024-05-25 之间的随机日期时间"
    }
    // 每个商品 3-5 条评价
  ]
}

只返回 JSON 数组，不要其他解释。`;
}

/**
 * 解析 Agent 返回的 JSON
 */
function parseAgentResponse(response) {
  // 提取 JSON 部分（可能包含 markdown 代码块）
  const jsonMatch = response.match(/```json\s*([\s\S]*?)\s*```/) ||
                    response.match(/\[[\s\S]*\]/);

  if (!jsonMatch) {
    throw new Error('Agent 返回的内容中未找到 JSON 数据');
  }

  const jsonStr = jsonMatch[1] || jsonMatch[0];
  return JSON.parse(jsonStr);
}

/**
 * 生成 SQL INSERT 语句
 */
function generateSQL(products) {
  const lines = [];

  lines.push('-- 真实的校园商品数据（AI 生成）');
  lines.push('-- 生成时间: ' + new Date().toISOString());
  lines.push('');

  // 商品表
  lines.push('-- 商品基本信息');
  lines.push('INSERT INTO products (id, name, description, price, stock, type, category_id, shop_id, status, created_at, updated_at) VALUES');

  const productValues = products.map(p => {
    const now = new Date().toISOString().slice(0, 19).replace('T', ' ');
    return `(${p.id}, '${escapeSql(p.name)}', '${escapeSql(p.description)}', ${p.price}, ${p.stock}, '${p.type}', ${p.category_id}, ${p.shop_id}, 'active', '${now}', '${now}')`;
  });

  lines.push(productValues.join(',\n'));
  lines.push('ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);');
  lines.push('');

  // 商品图片
  lines.push('-- 商品图片');
  lines.push('INSERT INTO product_media (product_id, media_url, media_type, display_order) VALUES');

  const mediaValues = [];
  products.forEach(p => {
    p.images.forEach((url, idx) => {
      mediaValues.push(`(${p.id}, '${escapeSql(url)}', 'image', ${idx})`);
    });
  });

  lines.push(mediaValues.join(',\n'));
  lines.push('ON DUPLICATE KEY UPDATE media_url = VALUES(media_url);');
  lines.push('');

  // 商品评价
  lines.push('-- 商品评价');
  lines.push('INSERT INTO reviews (product_id, user_id, rating, content, status, created_at) VALUES');

  const reviewValues = [];
  products.forEach(p => {
    p.reviews.forEach(r => {
      reviewValues.push(`(${p.id}, ${r.user_id}, ${r.rating}, '${escapeSql(r.content)}', 'approved', '${r.created_at}')`);
    });
  });

  lines.push(reviewValues.join(',\n'));
  lines.push('ON DUPLICATE KEY UPDATE updated_at = NOW();');
  lines.push('');

  return lines.join('\n');
}

/**
 * SQL 字符串转义
 */
function escapeSql(str) {
  return str.replace(/'/g, "''").replace(/\\/g, '\\\\');
}

/**
 * 主函数
 */
async function main() {
  console.log('开始生成真实的校园商品数据...');
  console.log(`目标数量: ${CONFIG.totalProducts} 个商品`);
  console.log(`批次大小: ${CONFIG.batchSize} 个/批`);
  console.log('');

  const allProducts = [];
  let currentId = 20000; // 起始 ID，避免与现有数据冲突

  const batches = Math.ceil(CONFIG.totalProducts / CONFIG.batchSize);

  for (let i = 0; i < batches; i++) {
    const batchNum = i + 1;
    const remainingCount = CONFIG.totalProducts - allProducts.length;
    const batchCount = Math.min(CONFIG.batchSize, remainingCount);

    // 随机选择一个类别
    const category = CATEGORIES[Math.floor(Math.random() * CATEGORIES.length)];

    console.log(`[${batchNum}/${batches}] 生成 ${category.name} 类商品 ${batchCount} 个...`);

    const prompt = generateProductPrompt(category, currentId, batchCount);

    // 这里需要调用 Agent 工具
    // 由于脚本无法直接调用 Agent，需要 Claude Code 来执行
    console.log(`  提示词已准备，等待 Agent 生成...`);
    console.log(`  [AGENT_CALL] category=${category.name}, startId=${currentId}, count=${batchCount}`);

    // 占位：实际执行时由 Claude Code 调用 Agent
    // const agentResponse = await callAgent(prompt);
    // const products = parseAgentResponse(agentResponse);

    currentId += batchCount;
  }

  console.log('');
  console.log('生成完成！');
  console.log(`总计: ${allProducts.length} 个商品`);
  console.log(`输出文件: ${path.join(CONFIG.outputDir, CONFIG.outputFile)}`);
}

main().catch(err => {
  console.error('生成失败:', err);
  process.exit(1);
});
