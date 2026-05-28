#!/usr/bin/env node

/**
 * 生成真实的校园商品种子数据（简化版）
 */

import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 商品数据模板
const categories = [
  { id: 1, name: '教材教辅', count: 20, priceRange: [20, 150], stockRange: [10, 50] },
  { id: 2, name: '文具用品', count: 25, priceRange: [5, 50], stockRange: [50, 200] },
  { id: 3, name: '数码配件', count: 25, priceRange: [15, 200], stockRange: [30, 150] },
  { id: 4, name: '生活用品', count: 25, priceRange: [10, 100], stockRange: [40, 180] },
  { id: 5, name: '零食饮料', count: 30, priceRange: [3, 30], stockRange: [100, 500] },
  { id: 6, name: '运动户外', count: 25, priceRange: [30, 300], stockRange: [20, 120] }
];

const products = {
  1: [
    '高等数学第七版同济大学', '大学英语四级真题详解', '线性代数第五版', 'C语言程序设计第三版',
    '概率论与数理统计第四版', '大学物理上册', '马克思主义基本原理', '大学计算机基础',
    '有机化学第五版', '微观经济学原理', '数据结构与算法', '大学英语综合教程',
    '机械制图第七版', '电路分析基础', '模拟电子技术', '数字电路与逻辑设计',
    '操作系统原理', '计算机网络第七版', '软件工程导论', '数据库系统概论'
  ],
  2: [
    '晨光中性笔12支装', '得力A5活页笔记本', '三菱UM-151中性笔', '国誉Campus笔记本',
    '斑马荧光笔6色套装', '得力订书机套装', '晨光修正带5个装', '3M便利贴组合装',
    '得力文件夹10个装', '晨光橡皮擦20块装', '得力美工刀', '晨光固体胶12支装',
    '得力剪刀不锈钢', '晨光铅笔2B考试专用', '得力笔袋大容量', '晨光书签创意套装',
    '得力尺子套装', '晨光涂改液12瓶装', '得力夹子套装', '晨光笔芯0.5mm黑色',
    '得力胶带透明', '晨光荧光笔套装', '得力计算器学生用', '晨光笔记本B5', '得力文具盒多功能'
  ],
  3: [
    '绿联USB-C转HDMI转接头', '罗技M185无线鼠标', '苹果Lightning数据线1米', '小米充电宝20000mAh',
    '绿联Type-C数据线', '罗技K380蓝牙键盘', '闪迪32GB U盘', '绿联USB分线器4口',
    '苹果AirPods耳机套', '小米蓝牙耳机', '绿联HDMI线2米', '罗技鼠标垫',
    '苹果20W充电器', '小米插线板6位', '绿联网线5米', '罗技无线鼠标垫',
    '苹果MagSafe充电器', '小米无线充电器', '绿联读卡器', '罗技C270摄像头',
    '苹果转接头套装', '小米手环7', '绿联车载充电器', '罗技游戏鼠标', '苹果清洁套装'
  ],
  4: [
    '宿舍床帘遮光', 'LED护眼台灯', '折叠晾衣架', '收纳箱塑料',
    '保温杯304不锈钢', '衣架20个装', '插座转换器', '垃圾桶宿舍用',
    '毛巾纯棉3条装', '洗漱杯套装', '床上书桌可折叠', '挂钩粘贴式20个',
    '收纳袋真空压缩', '台灯夹子式', '拖鞋室内防滑', '晾衣绳宿舍',
    '收纳盒桌面', '水杯塑料便携', '衣柜收纳挂袋', '床垫软垫',
    '枕头记忆棉', '被子四件套', '蚊帐宿舍上铺', '风扇USB小型', '加湿器迷你'
  ],
  5: [
    '三只松鼠每日坚果30包', '康师傅红烧牛肉面5连包', '卫龙辣条大面筋20包', '乐事薯片黄瓜味',
    '奥利奥饼干原味', '旺旺雪饼仙贝', '可口可乐330ml*12', '农夫山泉550ml*24',
    '统一冰红茶500ml*15', '三只松鼠碧根果', '良品铺子猪肉脯', '百草味夏威夷果',
    '来伊份牛肉干', '徐福记糖果混合装', '德芙巧克力礼盒', '旺旺牛奶',
    '蒙牛纯牛奶', '伊利酸奶', '康师傅绿茶', '雀巢咖啡速溶',
    '立顿红茶包', '香飘飘奶茶', '好丽友派', '达利园面包',
    '盼盼法式小面包', '卡夫威化饼干', '趣多多曲奇', '上好佳薯片',
    '乐事薯片番茄味', '三只松鼠开心果'
  ],
  6: [
    '李宁7号篮球', '迪卡侬瑜伽垫10mm', '安踏运动鞋跑步', '李宁羽毛球拍',
    '迪卡侬跳绳', '安踏运动袜5双装', '李宁运动背包', '迪卡侬哑铃5kg',
    '安踏运动毛巾', '李宁护腕护膝套装', '迪卡侬瑜伽球', '安踏运动水壶',
    '李宁乒乓球拍', '迪卡侬阻力带', '安踏运动帽', '李宁足球5号',
    '迪卡侬游泳镜', '安踏运动腰包', '李宁网球拍', '迪卡侬健身手套',
    '安踏运动护具', '李宁排球', '迪卡侬瑜伽服', '安踏运动短裤', '李宁运动T恤'
  ]
};

const descriptions = {
  1: '经典教材，适合课堂学习和自习室复习，知识点讲解详细，例题丰富',
  2: '学生文具必备，适合课堂笔记和考试使用，质量可靠性价比高',
  3: '数码配件，适合宿舍和自习室使用，兼容性好质量稳定',
  4: '宿舍生活用品，实用便捷，提升宿舍生活品质',
  5: '零食饮料，宿舍囤货必备，自习室充饥良品',
  6: '运动装备，适合操场健身房使用，质量可靠耐用'
};

const reviews = [
  '质量不错，性价比很高，宿舍室友都说好用',
  '自习室必备，用了一学期还很好，推荐购买',
  '课堂上用很方便，老师也推荐这个牌子',
  '操场训练专用，质量扎实，值得入手',
  '宿舍囤货神器，价格实惠，会回购',
  '图书馆学习必备，效果很好，五星好评',
  '考试周救命神器，帮了大忙，感谢店家',
  '体育课指定装备，大家都在用，质量放心'
];

function random(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomFloat(min, max) {
  return (Math.random() * (max - min) + min).toFixed(2);
}

function randomDate() {
  // 使用 2026-05-25 确保这些商品排在压力测试商品（2026-05-10）之后
  const date = new Date('2026-05-25T' + String(Math.floor(Math.random() * 24)).padStart(2, '0') + ':' + String(Math.floor(Math.random() * 60)).padStart(2, '0') + ':' + String(Math.floor(Math.random() * 60)).padStart(2, '0'));
  return date.toISOString().slice(0, 19).replace('T', ' ');
}

function escapeSql(str) {
  return str.replace(/'/g, "''");
}

async function main() {
  console.log('开始生成真实的校园商品数据...');

  const lines = [];
  lines.push('-- 真实的校园商品数据（简化版生成）');
  lines.push('-- 生成时间: ' + new Date().toISOString());
  lines.push('');

  // 生成商品数据
  const productLines = [];
  const mediaLines = [];
  const reviewLines = [];

  let currentId = 1000;

  for (const cat of categories) {
    const items = products[cat.id];

    for (let i = 0; i < cat.count; i++) {
      const name = items[i % items.length];
      const price = randomFloat(cat.priceRange[0], cat.priceRange[1]);
      const stock = random(cat.stockRange[0], cat.stockRange[1]);
      const type = Math.random() < 0.9 ? 'physical' : 'digital';
      const shopId = random(1, 10);
      const desc = descriptions[cat.id];

      productLines.push(
        `(${currentId}, '${escapeSql(name)}', '${escapeSql(desc)}', ${price}, ${stock}, '${type}', ${cat.id}, ${shopId}, 'on_sale', '2024-05-25 21:00:00', '2024-05-25 21:00:00')`
      );

      // 生成 1-3 张图片
      const imageCount = random(1, 3);
      for (let j = 0; j < imageCount; j++) {
        const imageId = random(1000000, 9999999);
        mediaLines.push(
          `(${currentId}, 'https://images.unsplash.com/photo-${imageId}?w=800', 'image', ${j})`
        );
      }

      // 生成 3-5 条评价
      const reviewCount = random(3, 5);
      for (let j = 0; j < reviewCount; j++) {
        const userId = random(1, 20);
        const rating = random(3, 5);
        const content = reviews[random(0, reviews.length - 1)];
        const createdAt = randomDate();
        reviewLines.push(
          `(${currentId}, ${userId}, ${rating}, '${escapeSql(content)}', 'approved', '${createdAt}')`
        );
      }

      currentId++;
    }
  }

  // 商品表
  lines.push('-- 商品基本信息');
  lines.push('INSERT INTO products (id, name, description, price, stock, type, category_id, shop_id, status, created_at, updated_at) VALUES');
  lines.push(productLines.join(',\n'));
  lines.push('ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);');
  lines.push('');

  // 商品图片
  lines.push('-- 商品图片');
  lines.push('INSERT INTO product_media (product_id, media_url, media_type, display_order) VALUES');
  lines.push(mediaLines.join(',\n'));
  lines.push('ON DUPLICATE KEY UPDATE media_url = VALUES(media_url);');
  lines.push('');

  // 商品评价
  lines.push('-- 商品评价');
  lines.push('INSERT INTO reviews (product_id, user_id, rating, content, status, created_at) VALUES');
  lines.push(reviewLines.join(',\n'));
  lines.push('ON DUPLICATE KEY UPDATE updated_at = NOW();');
  lines.push('');

  const sql = lines.join('\n');
  const outputPath = path.join(__dirname, '..', 'backend', 'src', 'main', 'resources', 'seed', 'data-realistic-products.sql');

  await fs.writeFile(outputPath, sql, 'utf-8');

  console.log('');
  console.log('生成完成！');
  console.log(`总计: ${currentId - 1000} 个商品`);
  console.log(`输出文件: ${outputPath}`);
}

main().catch(err => {
  console.error('生成失败:', err);
  process.exit(1);
});
