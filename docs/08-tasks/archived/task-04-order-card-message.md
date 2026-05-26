# Task 04: 璁㈠崟鍗＄墖娑堟伅

## Metadata

- **Task ID**: task-04
- **Priority**: P1
- **Estimated effort**: 6-8 hours
- **Dependencies**: task-02 (鍥剧墖娑堟伅鏀寔)
- **Assignee**: P1 Worker A / P1 Worker B / Head Agent
- **Status**: done
- **Completed date**: 2026-05-26

---

## Context

### 闂鎻忚堪

鍞悗鍦烘櫙涓?涔板鍜屽崠瀹堕渶瑕佽璁鸿鍗曢棶棰橈紙鐗╂祦銆侀€€娆俱€佸晢鍝佽川閲忕瓑锛夈€傝鍗曞崱鐗囨秷鎭彲浠ュ揩閫熷叧鑱旇鍗曚俊鎭?鏂逛究鍙屾柟鏌ョ湅璁㈠崟璇︽儏銆?
### 涓氬姟浠峰€?
璁㈠崟鍗＄墖娑堟伅鑳藉鏄捐憲鎻愬崌鍞悗娌熼€氭晥鐜?鍙屾柟鍙互蹇€熷畾浣嶅埌鍏蜂綋璁㈠崟,鍑忓皯娌熼€氭垚鏈€?
---

## Goals

瀹炵幇浠ヤ笅鍔熻兘:

1. **璁㈠崟璇︽儏椤垫坊鍔?鑱旂郴鍗栧/涔板"鎸夐挳**: 鑷姩鍙戦€佽鍗曞崱鐗?2. **娑堟伅姘旀场鏄剧ず璁㈠崟鍗＄墖**: 鏄剧ず璁㈠崟鍙枫€佺姸鎬併€佸晢鍝併€侀噾棰?3. **鐐瑰嚮鍗＄墖璺宠浆鍒拌鍗曡鎯呴〉**: 鐐瑰嚮"鏌ョ湅璁㈠崟璇︽儏"鎸夐挳璺宠浆
4. **璁㈠崟鐘舵€佸疄鏃舵樉绀?*: 璁㈠崟鍗＄墖鏄剧ず鏈€鏂扮姸鎬?
---

## Database Schema

### `chat_messages` 琛ㄦ坊鍔犺鍗曞叧鑱斿瓧娈?
闇€瑕佹坊鍔犱互涓嬪瓧娈?
- `order_id` (BIGINT, NULL): 鍏宠仈璁㈠崟 ID锛堣鍗曞崱鐗囨秷鎭級
- 澶栭敭绾︽潫: `FOREIGN KEY (order_id) REFERENCES orders(id)`

闇€瑕佹坊鍔犵储寮?
- `idx_message_order`: 绱㈠紩 `(order_id)`,鐢ㄤ簬鏌ヨ璁㈠崟鐩稿叧鐨勬秷鎭?
**璁捐璇存槑**:
- `message_type = 'order_card'` 鏃?`order_id` 蹇呴渶
- `body` 瀛楁鍙€夛紙鐢ㄤ簬闄勫姞璇存槑,濡?鐗╂祦鏈夐棶棰?锛?- 璁㈠崟淇℃伅浠?orders 琛?JOIN 鑾峰彇

---

## API Contract

### 1. 鍙戦€佹秷鎭帴鍙ｏ紙淇敼鐜版湁鎺ュ彛锛?
**Endpoint**: `POST /api/chat/conversations/{id}/messages`

**Request**:
```json
{
  "body": "鐗╂祦鏈夐棶棰橈紙鍙€夛級",
  "messageType": "order_card",
  "orderId": 456
}
```

**Request Fields**:
- `body` (String, optional): 闄勫姞璇存槑
- `messageType` (String): 蹇呴』涓?"order_card"
- `orderId` (Long): 璁㈠崟 ID锛堝繀闇€锛?
**Business Logic**:
- 楠岃瘉璁㈠崟鏄惁瀛樺湪
- 楠岃瘉鐢ㄦ埛鏄鍗曞弬涓庤€咃紙涔板鎴栧崠瀹讹級
- 鎻掑叆娑堟伅鏃跺叧鑱旇鍗?ID

### 2. 鑾峰彇娑堟伅鎺ュ彛锛堜慨鏀圭幇鏈夋帴鍙ｏ級

**Endpoint**: `GET /api/chat/conversations/{id}/messages`

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 126,
        "messageType": "order_card",
        "body": "鐗╂祦鏈夐棶棰?,
        "orderId": 456,
        "order": {
          "id": 456,
          "orderNumber": "ORD-20260525-001",
          "status": "shipped",
          "totalAmount": 199.00,
          "productTitle": "鍟嗗搧鏍囬",
          "productImage": "https://example.com/product.jpg",
          "createdAt": "2026-05-25T09:00:00"
        },
        "createdAt": "2026-05-25T10:03:00"
      }
    ]
  }
}
```

**Business Logic**:
- 鏌ヨ娑堟伅鏃?濡傛灉 `message_type = 'order_card'`,鑷姩 JOIN `orders` 鍜?`order_items` 琛?- 杩斿洖璁㈠崟鐨勫繀瑕佷俊鎭? id, orderNumber, status, totalAmount, productTitle锛堢涓€涓晢鍝侊級, productImage锛堢涓€涓晢鍝佸浘鐗囷級
- 濡傛灉璁㈠崟宸插垹闄?`order` 瀛楁涓?null

---

## Frontend Requirements

### 1. 璁㈠崟璇︽儏椤垫坊鍔?鑱旂郴鍗栧/涔板"鎸夐挳

鍦ㄨ鍗曡鎯呴〉娣诲姞鎸夐挳:
- 涔板鐪嬪埌"鑱旂郴鍗栧"鎸夐挳
- 鍗栧鐪嬪埌"鑱旂郴涔板"鎸夐挳
- 鐐瑰嚮鎸夐挳鑷姩鏌ユ壘鎴栧垱寤轰細璇?- 鑷姩鍙戦€佽鍗曞崱鐗囨秷鎭?- 鍙戦€佹垚鍔熷悗璺宠浆鍒版秷鎭腑蹇?
### 2. 璁㈠崟鍗＄墖缁勪欢

鍒涘缓璁㈠崟鍗＄墖缁勪欢,鏄剧ず:
- 璁㈠崟鍙凤紙濡?"璁㈠崟 #ORD-20260525-001"锛?- 璁㈠崟鐘舵€侊紙甯﹂鑹叉爣璇嗭級
- 鍟嗗搧鍥剧墖锛?0x60px锛?- 鍟嗗搧鏍囬锛堟渶澶?2 琛?瓒呭嚭鐪佺暐锛?- 璁㈠崟閲戦锛堢孩鑹?鍔犵矖锛?- "鏌ョ湅璁㈠崟璇︽儏"鎸夐挳
- 闄勫姞璇存槑锛堝鏋滄湁锛?
### 3. 娑堟伅姘旀场鏄剧ず璁㈠崟鍗＄墖

鍦ㄦ秷鎭皵娉′腑鏄剧ず璁㈠崟鍗＄墖:
- 璁㈠崟鍗＄墖鏈€澶у搴?320px
- 鐐瑰嚮"鏌ョ湅璁㈠崟璇︽儏"鎸夐挳璺宠浆鍒拌鍗曡鎯呴〉
- 璁㈠崟鐘舵€侀鑹叉槧灏?
  - pending: 姗欒壊
  - paid: 钃濊壊
  - shipped: 绱壊
  - delivered: 缁胯壊
  - completed: 鐏拌壊
  - cancelled: 绾㈣壊

---

## Acceptance Criteria

### 鍚庣楠岃瘉

- [x] 鏁版嵁搴撹縼绉昏剼鏈墽琛屾垚鍔?瀛楁鍜岀储寮曞垱寤烘垚鍔?- [x] 鍙戦€佽鍗曞崱鐗囨秷鎭紙messageType=order_card, orderId 蹇呴渶锛夋垚鍔?- [x] 璁㈠崟涓嶅瓨鍦ㄦ椂杩斿洖閿欒
- [x] 闈炶鍗曞弬涓庤€呭彂閫佽鍗曞崱鐗囨椂杩斿洖閿欒
- [x] 鏌ヨ娑堟伅鏃惰嚜鍔ㄩ檮鍔犺鍗曚俊鎭紙orderNumber, status, totalAmount, productTitle, productImage锛?- [x] 鍚庣娴嬭瘯鍏ㄩ儴閫氳繃 (`mvnw.cmd test`)

### 鍓嶇楠岃瘉

- [x] 璁㈠崟璇︽儏椤垫樉绀?鑱旂郴鍗栧/涔板"鎸夐挳
- [x] 鐐瑰嚮鎸夐挳鑷姩鍙戦€佽鍗曞崱鐗?- [x] 娑堟伅姘旀场姝ｇ‘鏄剧ず璁㈠崟鍗＄墖锛堣鍗曞彿銆佺姸鎬併€佸晢鍝併€侀噾棰濓級
- [x] 鐐瑰嚮"鏌ョ湅璁㈠崟璇︽儏"璺宠浆鍒拌鍗曡鎯呴〉
- [x] 璁㈠崟鐘舵€侀鑹叉纭樉绀?- [x] 鍓嶇娴嬭瘯鍏ㄩ儴閫氳繃 (`npm test`)

### 闆嗘垚娴嬭瘯

- [x] 涔板鍦ㄨ鍗曡鎯呴〉鑱旂郴鍗栧,鍙戦€佽鍗曞崱鐗?- [x] 鍗栧鏀跺埌璁㈠崟鍗＄墖娑堟伅,姝ｇ‘鏄剧ず
- [x] 鍗栧鐐瑰嚮"鏌ョ湅璁㈠崟璇︽儏",璺宠浆鍒拌鍗曡鎯呴〉
- [x] 璁㈠崟鐘舵€佸彉鏇村悗,鍘嗗彶娑堟伅涓殑璁㈠崟鍗＄墖鏄剧ず鏈€鏂扮姸鎬?
---

## Technical Constraints

### 蹇呴』閬靛畧鐨勭害鏉?
1. **渚濊禆 task-02**: 蹇呴』鍏堝畬鎴愬浘鐗囨秷鎭敮鎸侊紙`message_type` 瀛楁锛?2. **璁㈠崟鏉冮檺楠岃瘉**: 鍙戦€佹椂楠岃瘉鐢ㄦ埛鏄鍗曞弬涓庤€咃紙涔板鎴栧崠瀹讹級
3. **璁㈠崟淇℃伅闄勫姞**: 鏌ヨ娑堟伅鏃惰嚜鍔?JOIN `orders` 鍜?`order_items` 琛?4. **璁㈠崟鐘舵€佸疄鏃舵€?*: 璁㈠崟鍗＄墖鏄剧ず鏈€鏂扮姸鎬侊紙涓嶇紦瀛橈級
5. **鎬ц兘鑰冭檻**: 璁㈠崟淇℃伅浠呰繑鍥炲繀瑕佸瓧娈靛拰绗竴涓晢鍝佷俊鎭?6. **绾?JDBC**: 浣跨敤 `JdbcTemplate`,涓嶄娇鐢?ORM
7. **浜嬪姟绠＄悊**: 浣跨敤 `@Transactional` 淇濊瘉鏁版嵁涓€鑷存€?
---

## Files to Modify

### 鍚庣

- `backend/src/main/resources/schema.sql` - 娣诲姞瀛楁鍜岀储寮?- `backend/src/main/java/com/youyu/backend/controller/chat/SendMessageRequest.java` - 娣诲姞 orderId 瀛楁
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 淇敼涓氬姟閫昏緫
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatMessageMapper.java` - 淇敼鏂规硶绛惧悕
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatMessageMapper.java` - 淇敼瀹炵幇
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java` - 淇敼鎺ュ彛

### 鍓嶇

- `frontend/src/api/modules/chat.js` - 淇敼 API 鏂规硶
- `frontend/src/stores/chat.js` - 淇敼鐘舵€佸拰鏂规硶
- `frontend/src/components/chat/OrderCardMessage.vue` - 鍒涘缓璁㈠崟鍗＄墖缁勪欢
- `frontend/src/views/app/MessagesView.vue` - 娣诲姞璁㈠崟鍗＄墖鏄剧ず
- `frontend/src/views/app/TradeOrderDetailView.vue` - 娣诲姞鑱旂郴鎸夐挳

### 鏂囨。

- `docs/06-http/chat.http` - 娣诲姞 HTTP 娴嬭瘯鐢ㄤ緥
- `docs/09-api-spec/chat.md` - 鏇存柊 API 鏂囨。
- `CHANGELOG.md` - 娣诲姞鍙樻洿璁板綍

---

## Notes

- 鏈换鍔′緷璧?task-02,闇€绛?task-02 瀹屾垚鍚庢墠鑳藉紑濮?- 璁㈠崟鍗＄墖鏄數鍟嗗敭鍚庡満鏅殑鏍稿績鍔熻兘,鑳藉鏄捐憲鎻愬崌娌熼€氭晥鐜?- 璁㈠崟鐘舵€佸疄鏃舵樉绀?涓嶇紦瀛樿鍗曚俊鎭?
---

## Completion Checklist

瀹屾垚鍚?鍚戝ご Agent 鎶ュ憡:

- [x] 鎵€鏈夐獙鏀舵爣鍑嗗凡婊¤冻
- [x] 鎵€鏈夋祴璇曢€氳繃
- [x] 鏂囨。宸叉洿鏂?- [x] CHANGELOG.md 宸叉洿鏂?- [x] 浠ｇ爜宸叉彁浜ゅ埌鍒嗘敮
- [x] 閬囧埌鐨勯棶棰樺拰瑙ｅ喅鏂规(濡傛湁): 璁㈠崟鍒楄〃琛ュ厖 `buyerUserId` / `sellerUserId` / `shopId`锛岀‘淇濆墠绔彲绋冲畾鍒涘缓浼氳瘽銆?