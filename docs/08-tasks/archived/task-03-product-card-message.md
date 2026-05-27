# Task 03: 鍟嗗搧鍗＄墖娑堟伅

## Metadata

- **Task ID**: task-03
- **Priority**: P1
- **Estimated effort**: 6-8 hours
- **Dependencies**: task-02 (鍥剧墖娑堟伅鏀寔)
- **Assignee**: P1 Worker A / P1 Worker B / Head Agent
- **Status**: done
- **Completed date**: 2026-05-26

---

## Context

### 闂鎻忚堪

鐢靛晢鍦烘櫙涓?鐢ㄦ埛闇€瑕佸揩閫熷垎浜晢鍝侀摼鎺ャ€傚綋鍓嶅彧鑳藉鍒剁矘璐村晢鍝?URL,浣撻獙宸€傚晢鍝佸崱鐗囨秷鎭彲浠ュ湪鑱婂ぉ涓洿鎺ュ睍绀哄晢鍝佷俊鎭紙鍥剧墖銆佹爣棰樸€佷环鏍硷級,鐐瑰嚮璺宠浆鍒板晢鍝佽鎯呴〉銆?
### 涓氬姟浠峰€?
鍟嗗搧鍗＄墖娑堟伅鑳藉鏄捐憲鎻愬崌鍟嗗搧鍒嗕韩鐨勮浆鍖栫巼,涔板鍙互蹇€熶簡瑙ｅ晢鍝佷俊鎭?鍗栧鍙互蹇€熸帹鑽愬晢鍝併€?
---

## Goals

瀹炵幇浠ヤ笅鍔熻兘:

1. **鍟嗗搧璇︽儏椤垫坊鍔?鍒嗕韩鍒拌亰澶?鎸夐挳**: 鐢ㄦ埛鍙互閫夋嫨浼氳瘽鍒嗕韩鍟嗗搧
2. **娑堟伅姘旀场鏄剧ず鍟嗗搧鍗＄墖**: 鏄剧ず鍟嗗搧鍥剧墖銆佹爣棰樸€佷环鏍笺€佺姸鎬?3. **鐐瑰嚮鍗＄墖璺宠浆鍒板晢鍝佽鎯呴〉**: 鐐瑰嚮"鏌ョ湅璇︽儏"鎸夐挳璺宠浆
4. **鍟嗗搧涓嬫灦澶勭悊**: 鍟嗗搧涓嬫灦鍚?鍗＄墖鏄剧ず"宸蹭笅鏋?鐘舵€?鎸夐挳绂佺敤

---

## Database Schema

### `chat_messages` 琛ㄦ坊鍔犲晢鍝佸叧鑱斿瓧娈?
闇€瑕佹坊鍔犱互涓嬪瓧娈?
- `product_id` (BIGINT, NULL): 鍏宠仈鍟嗗搧 ID锛堝晢鍝佸崱鐗囨秷鎭級
- 澶栭敭绾︽潫: `FOREIGN KEY (product_id) REFERENCES products(id)`

闇€瑕佹坊鍔犵储寮?
- `idx_message_product`: 绱㈠紩 `(product_id)`,鐢ㄤ簬鏌ヨ鍟嗗搧鐩稿叧鐨勬秷鎭?
**璁捐璇存槑**:
- `message_type = 'product_card'` 鏃?`product_id` 蹇呴渶
- `body` 瀛楁鍙€夛紙鐢ㄤ簬闄勫姞鏂囧瓧璇存槑锛?- `media_url` 瀛楁涓嶄娇鐢紙鍟嗗搧鍥剧墖浠?products 琛ㄨ幏鍙栵級

---

## API Contract

### 1. 鍙戦€佹秷鎭帴鍙ｏ紙淇敼鐜版湁鎺ュ彛锛?
**Endpoint**: `POST /api/chat/conversations/{id}/messages`

**Request**:
```json
{
  "body": "杩欎釜鍟嗗搧涓嶉敊锛堝彲閫夛級",
  "messageType": "product_card",
  "productId": 123
}
```

**Request Fields**:
- `body` (String, optional): 闄勫姞鏂囧瓧璇存槑
- `messageType` (String): 蹇呴』涓?"product_card"
- `productId` (Long): 鍟嗗搧 ID锛堝繀闇€锛?
**Business Logic**:
- 楠岃瘉鍟嗗搧鏄惁瀛樺湪
- 楠岃瘉鍟嗗搧鐘舵€佹槸鍚︿负 `active`锛堝凡涓嬫灦鍟嗗搧涓嶈兘鍒嗕韩锛?- 鎻掑叆娑堟伅鏃跺叧鑱斿晢鍝?ID

### 2. 鑾峰彇娑堟伅鎺ュ彛锛堜慨鏀圭幇鏈夋帴鍙ｏ級

**Endpoint**: `GET /api/chat/conversations/{id}/messages`

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 125,
        "messageType": "product_card",
        "body": "杩欎釜鍟嗗搧涓嶉敊",
        "productId": 123,
        "product": {
          "id": 123,
          "title": "鍟嗗搧鏍囬",
          "price": 99.00,
          "status": "active",
          "imageUrl": "https://example.com/product.jpg"
        },
        "createdAt": "2026-05-25T10:02:00"
      }
    ]
  }
}
```

**Business Logic**:
- 鏌ヨ娑堟伅鏃?濡傛灉 `message_type = 'product_card'`,鑷姩 JOIN `products` 琛?- 杩斿洖鍟嗗搧鐨勫繀瑕佷俊鎭? id, title, price, status, imageUrl锛堥鍥撅級
- 濡傛灉鍟嗗搧宸插垹闄?`product` 瀛楁涓?null

---

## Frontend Requirements

### 1. 鍟嗗搧璇︽儏椤垫坊鍔?鍒嗕韩鍒拌亰澶?鎸夐挳

鍦ㄥ晢鍝佽鎯呴〉娣诲姞鎸夐挳:
- 鐐瑰嚮鎸夐挳寮瑰嚭浼氳瘽閫夋嫨瀵硅瘽妗?- 鏄剧ず鐢ㄦ埛鐨勬墍鏈変細璇濆垪琛?- 閫夋嫨浼氳瘽鍚庡彂閫佸晢鍝佸崱鐗囨秷鎭?- 鍙戦€佹垚鍔熷悗璺宠浆鍒版秷鎭腑蹇?
### 2. 鍟嗗搧鍗＄墖缁勪欢

鍒涘缓鍟嗗搧鍗＄墖缁勪欢,鏄剧ず:
- 鍟嗗搧鍥剧墖锛?0x80px锛?- 鍟嗗搧鏍囬锛堟渶澶?2 琛?瓒呭嚭鐪佺暐锛?- 鍟嗗搧浠锋牸锛堢孩鑹?鍔犵矖锛?- 鍟嗗搧鐘舵€侊紙宸蹭笅鏋舵椂鏄剧ず鐏拌壊鏂囧瓧锛?- "鏌ョ湅璇︽儏"鎸夐挳锛堝晢鍝佷笅鏋舵椂绂佺敤锛?- 闄勫姞鏂囧瓧璇存槑锛堝鏋滄湁锛?
### 3. 娑堟伅姘旀场鏄剧ず鍟嗗搧鍗＄墖

鍦ㄦ秷鎭皵娉′腑鏄剧ず鍟嗗搧鍗＄墖:
- 鍟嗗搧鍗＄墖鏈€澶у搴?320px
- 鐐瑰嚮"鏌ョ湅璇︽儏"鎸夐挳璺宠浆鍒板晢鍝佽鎯呴〉
- 鍟嗗搧涓嬫灦鏃?鍗＄墖鏄剧ず"宸蹭笅鏋?鐘舵€?鎸夐挳绂佺敤

---

## Acceptance Criteria

### 鍚庣楠岃瘉

- [x] 鏁版嵁搴撹縼绉昏剼鏈墽琛屾垚鍔?瀛楁鍜岀储寮曞垱寤烘垚鍔?- [x] 鍙戦€佸晢鍝佸崱鐗囨秷鎭紙messageType=product_card, productId 蹇呴渶锛夋垚鍔?- [x] 鍟嗗搧涓嶅瓨鍦ㄦ椂杩斿洖閿欒
- [x] 鍟嗗搧宸蹭笅鏋舵椂杩斿洖閿欒
- [x] 鏌ヨ娑堟伅鏃惰嚜鍔ㄩ檮鍔犲晢鍝佷俊鎭紙id, title, price, imageUrl, status锛?- [x] 鍟嗗搧淇℃伅鍖呭惈棣栧浘 URL
- [x] 鍚庣娴嬭瘯鍏ㄩ儴閫氳繃 (`mvnw.cmd test`)

### 鍓嶇楠岃瘉

- [x] 鍟嗗搧璇︽儏椤垫樉绀?鍒嗕韩鍒拌亰澶?鎸夐挳
- [x] 鐐瑰嚮鎸夐挳寮瑰嚭浼氳瘽閫夋嫨瀵硅瘽妗?- [x] 閫夋嫨浼氳瘽鍚庢垚鍔熷彂閫佸晢鍝佸崱鐗?- [x] 娑堟伅姘旀场姝ｇ‘鏄剧ず鍟嗗搧鍗＄墖锛堝浘鐗囥€佹爣棰樸€佷环鏍硷級
- [x] 鐐瑰嚮"鏌ョ湅璇︽儏"璺宠浆鍒板晢鍝佽鎯呴〉
- [x] 鍟嗗搧宸蹭笅鏋舵椂鏄剧ず"宸蹭笅鏋?鐘舵€?鎸夐挳绂佺敤
- [x] 鍓嶇娴嬭瘯鍏ㄩ儴閫氳繃 (`npm test`)

### 闆嗘垚娴嬭瘯

- [x] 鐢ㄦ埛 A 鍦ㄥ晢鍝佽鎯呴〉鍒嗕韩鍟嗗搧缁欑敤鎴?B
- [x] 鐢ㄦ埛 B 鏀跺埌鍟嗗搧鍗＄墖娑堟伅,姝ｇ‘鏄剧ず
- [x] 鐢ㄦ埛 B 鐐瑰嚮"鏌ョ湅璇︽儏",璺宠浆鍒板晢鍝佽鎯呴〉
- [x] 鍟嗗搧涓嬫灦鍚?鍗＄墖鏄剧ず"宸蹭笅鏋?鐘舵€?
---

## Technical Constraints

### 蹇呴』閬靛畧鐨勭害鏉?
1. **渚濊禆 task-02**: 蹇呴』鍏堝畬鎴愬浘鐗囨秷鎭敮鎸侊紙`message_type` 瀛楁锛?2. **鍟嗗搧楠岃瘉**: 鍙戦€佹椂楠岃瘉鍟嗗搧瀛樺湪涓旂姸鎬佷负 `active`
3. **鍟嗗搧淇℃伅闄勫姞**: 鏌ヨ娑堟伅鏃惰嚜鍔?JOIN `products` 琛?4. **鍚戝悗鍏煎**: 鍟嗗搧涓嬫灦鍚?鍘嗗彶娑堟伅浠嶅彲鏌ョ湅,浣嗘樉绀?宸蹭笅鏋?鐘舵€?5. **鎬ц兘鑰冭檻**: 鍟嗗搧淇℃伅浠呰繑鍥炲繀瑕佸瓧娈碉紙id, title, price, imageUrl, status锛?6. **绾?JDBC**: 浣跨敤 `JdbcTemplate`,涓嶄娇鐢?ORM
7. **浜嬪姟绠＄悊**: 浣跨敤 `@Transactional` 淇濊瘉鏁版嵁涓€鑷存€?
---

## Files to Modify

### 鍚庣

- `backend/src/main/resources/schema.sql` - 娣诲姞瀛楁鍜岀储寮?- `backend/src/main/java/com/youyu/backend/controller/chat/SendMessageRequest.java` - 娣诲姞 productId 瀛楁
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 淇敼涓氬姟閫昏緫
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatMessageMapper.java` - 淇敼鏂规硶绛惧悕
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatMessageMapper.java` - 淇敼瀹炵幇
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java` - 淇敼鎺ュ彛

### 鍓嶇

- `frontend/src/api/modules/chat.js` - 淇敼 API 鏂规硶
- `frontend/src/stores/chat.js` - 淇敼鐘舵€佸拰鏂规硶
- `frontend/src/components/chat/ProductCardMessage.vue` - 鍒涘缓鍟嗗搧鍗＄墖缁勪欢
- `frontend/src/views/app/MessagesView.vue` - 娣诲姞鍟嗗搧鍗＄墖鏄剧ず
- `frontend/src/views/app/ProductDetailView.vue` - 娣诲姞鍒嗕韩鎸夐挳

### 鏂囨。

- `docs/06-http/chat.http` - 娣诲姞 HTTP 娴嬭瘯鐢ㄤ緥
- `docs/09-api-spec/chat.md` - 鏇存柊 API 鏂囨。
- `CHANGELOG.md` - 娣诲姞鍙樻洿璁板綍

---

## Notes

- 鏈换鍔′緷璧?task-02,闇€绛?task-02 瀹屾垚鍚庢墠鑳藉紑濮?- 鍟嗗搧鍗＄墖鏄數鍟嗗満鏅殑鐗硅壊鍔熻兘,鑳藉鏄捐憲鎻愬崌鍟嗗搧鍒嗕韩鐨勮浆鍖栫巼
- 鍟嗗搧涓嬫灦鍚?鍘嗗彶娑堟伅浠嶅彲鏌ョ湅,浣嗘樉绀?宸蹭笅鏋?鐘舵€?
---

## Completion Checklist

瀹屾垚鍚?鍚戝ご Agent 鎶ュ憡:

- [x] 鎵€鏈夐獙鏀舵爣鍑嗗凡婊¤冻
- [x] 鎵€鏈夋祴璇曢€氳繃
- [x] 鏂囨。宸叉洿鏂?- [x] CHANGELOG.md 宸叉洿鏂?- [x] 浠ｇ爜宸叉彁浜ゅ埌鍒嗘敮
- [x] 閬囧埌鐨勯棶棰樺拰瑙ｅ喅鏂规(濡傛湁): 涓庤鍗曞崱鐗囧叡浜?chat message 鍚庣鍜屾秷鎭〉娓叉煋锛岀敱澶?Agent 缁熶竴闆嗘垚銆?