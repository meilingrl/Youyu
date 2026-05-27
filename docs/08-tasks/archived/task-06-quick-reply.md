# Task 06: 蹇嵎鍥炲锛堝崠瀹跺伐鍏凤級

## Metadata

- **Task ID**: task-06
- **Priority**: P1
- **Estimated effort**: 6-8 hours
- **Dependencies**: None (鐙珛浠诲姟)
- **Assignee**: P1 Worker C / Head Agent
- **Status**: done
- **Completed date**: 2026-05-26

---

## Context

### 闂鎻忚堪

鍗栧闇€瑕侀绻佸洖澶嶄拱瀹剁殑甯歌闂锛?鍦ㄧ殑"銆?鍖呴偖鍚?銆?浠€涔堟椂鍊欏彂璐?绛夛級,鎵嬪姩杈撳叆鏁堢巼浣庛€傚揩鎹峰洖澶嶅姛鑳藉彲浠ラ璁惧父鐢ㄨ瘽鏈?涓€閿彂閫併€?
### 涓氬姟浠峰€?
蹇嵎鍥炲鑳藉鏄捐憲鎻愬崌鍗栧鐨勫搷搴旈€熷害,鍑忓皯閲嶅鍔冲姩,鎻愬崌涔板婊℃剰搴︺€?
---

## Goals

瀹炵幇浠ヤ笅鍔熻兘:

1. **鍗栧鍙互绠＄悊蹇嵎鍥炲**: 鍒涘缓銆佺紪杈戙€佸垹闄ゅ揩鎹峰洖澶?2. **娑堟伅杈撳叆妗嗘樉绀哄揩鎹峰洖澶嶆寜閽?*: 鐐瑰嚮鏄剧ず蹇嵎鍥炲鍒楄〃
3. **鐐瑰嚮蹇嵎鍥炲鑷姩濉厖**: 鐐瑰嚮蹇嵎鍥炲鑷姩濉厖鍒拌緭鍏ユ
4. **榛樿蹇嵎鍥炲**: 鎻愪緵榛樿璇濇湳,鐢ㄦ埛鍙嚜瀹氫箟

---

## Database Schema

### 鏂板缓 `quick_replies` 琛?
闇€瑕佸垱寤轰互涓嬭〃:

```sql
CREATE TABLE IF NOT EXISTS quick_replies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_quick_reply_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_sort (user_id, sort_order)
);
```

**瀛楁璇存槑**:
- `user_id`: 鐢ㄦ埛 ID锛堝崠瀹讹級
- `content`: 蹇嵎鍥炲鍐呭锛圱EXT 绫诲瀷,鏈€澶?65535 瀛楃锛?- `sort_order`: 鎺掑簭椤哄簭锛堢敤浜庤嚜瀹氫箟鎺掑簭锛?- `created_at`: 鍒涘缓鏃堕棿
- `updated_at`: 鏇存柊鏃堕棿

---

## API Contract

### 1. 鑾峰彇蹇嵎鍥炲鍒楄〃

**Endpoint**: `GET /api/chat/quick-replies`

**Request**:
- Headers: `Authorization: Bearer <token>`

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 2,
      "content": "浜?鍦ㄧ殑!鏈変粈涔堝彲浠ュ府鎮ㄧ殑鍚?",
      "sortOrder": 0,
      "createdAt": "2026-05-25T10:00:00",
      "updatedAt": "2026-05-25T10:00:00"
    }
  ]
}
```

**Business Logic**:
- 鎸?`sort_order` ASC, `created_at` ASC 鎺掑簭

### 2. 鍒涘缓蹇嵎鍥炲

**Endpoint**: `POST /api/chat/quick-replies`

**Request**:
```json
{
  "content": "鍖呴偖鍝?鍏ㄥ浗鍖呴偖!",
  "sortOrder": 1
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 2
  }
}
```

**Business Logic**:
- 楠岃瘉鍐呭涓嶄负绌?- 鍐呭闀垮害闄愬埗: 鏈€澶?500 瀛楃

### 3. 鏇存柊蹇嵎鍥炲

**Endpoint**: `PUT /api/chat/quick-replies/{id}`

**Request**:
```json
{
  "content": "浠婂ぉ涓嬪崟,鏄庡ぉ鍙戣揣!",
  "sortOrder": 2
}
```

**Response**:
```json
{
  "success": true,
  "data": null
}
```

**Business Logic**:
- 楠岃瘉蹇嵎鍥炲灞炰簬褰撳墠鐢ㄦ埛
- 楠岃瘉鍐呭涓嶄负绌?
### 4. 鍒犻櫎蹇嵎鍥炲

**Endpoint**: `DELETE /api/chat/quick-replies/{id}`

**Response**:
```json
{
  "success": true,
  "data": null
}
```

**Business Logic**:
- 楠岃瘉蹇嵎鍥炲灞炰簬褰撳墠鐢ㄦ埛

---

## Frontend Requirements

### 1. 蹇嵎鍥炲闈㈡澘缁勪欢

鍒涘缓蹇嵎鍥炲闈㈡澘缁勪欢,鏄剧ず:
- 蹇嵎鍥炲鍒楄〃锛堟渶澶氭樉绀?10 鏉★級
- 鐐瑰嚮蹇嵎鍥炲鑷姩濉厖鍒拌緭鍏ユ
- 濡傛灉鐢ㄦ埛娌℃湁鑷畾涔夊揩鎹峰洖澶?鏄剧ず榛樿璇濇湳

**榛樿蹇嵎鍥炲**:
- "浜?鍦ㄧ殑!鏈変粈涔堝彲浠ュ府鎮ㄧ殑鍚?"
- "鍖呴偖鍝?鍏ㄥ浗鍖呴偖!"
- "浠婂ぉ涓嬪崟,鏄庡ぉ鍙戣揣!"
- "鏈夌幇璐?鍙互绔嬪嵆鍙戣揣!"
- "鏀寔涓冨ぉ鏃犵悊鐢遍€€鎹㈣揣!"
- "鏈変换浣曢棶棰橀殢鏃惰仈绯绘垜!"

### 2. 娑堟伅杈撳叆鍖哄煙娣诲姞蹇嵎鍥炲鎸夐挳

鍦ㄦ秷鎭緭鍏ユ宸︿晶娣诲姞蹇嵎鍥炲鎸夐挳:
- 鎸夐挳鍥炬爣: 鈿?鎴?"蹇嵎鍥炲"
- 鐐瑰嚮鎸夐挳鏄剧ず蹇嵎鍥炲闈㈡澘
- 闈㈡澘鏄剧ず鍦ㄨ緭鍏ユ涓婃柟

### 3. 蹇嵎鍥炲绠＄悊椤甸潰锛堝彲閫夛級

鍒涘缓蹇嵎鍥炲绠＄悊椤甸潰锛坄/app/settings/quick-replies`锛?
- 鏄剧ず鎵€鏈夊揩鎹峰洖澶?- 鏀寔鍒涘缓銆佺紪杈戙€佸垹闄?- 鏀寔鎷栨嫿鎺掑簭

---

## Acceptance Criteria

### 鍚庣楠岃瘉

- [x] 鏁版嵁搴撹縼绉昏剼鏈墽琛屾垚鍔?琛ㄥ拰绱㈠紩鍒涘缓鎴愬姛
- [x] 鍒涘缓蹇嵎鍥炲鎴愬姛
- [x] 鑾峰彇蹇嵎鍥炲鍒楄〃鎴愬姛锛堟寜 sort_order 鎺掑簭锛?- [x] 鏇存柊蹇嵎鍥炲鎴愬姛
- [x] 鍒犻櫎蹇嵎鍥炲鎴愬姛
- [x] 闈炴墍鏈夎€呮棤娉曚慨鏀?鍒犻櫎浠栦汉鐨勫揩鎹峰洖澶?- [x] 鍚庣娴嬭瘯鍏ㄩ儴閫氳繃 (`mvnw.cmd test`)

### 鍓嶇楠岃瘉

- [x] 鐐瑰嚮"蹇嵎鍥炲"鎸夐挳鏄剧ず闈㈡澘
- [x] 闈㈡澘鏄剧ず榛樿蹇嵎鍥炲锛堝鏋滅敤鎴锋病鏈夎嚜瀹氫箟锛?- [x] 鐐瑰嚮蹇嵎鍥炲鑷姩濉厖鍒拌緭鍏ユ
- [x] 鐐瑰嚮蹇嵎鍥炲鍚庨潰鏉胯嚜鍔ㄥ叧闂?- [x] 蹇嵎鍥炲闈㈡澘婊氬姩姝ｅ父锛堣秴杩?6 鏉℃椂锛?- [x] 鍓嶇娴嬭瘯鍏ㄩ儴閫氳繃 (`npm test`)

### 闆嗘垚娴嬭瘯

- [x] 鍗栧鍒涘缓鑷畾涔夊揩鎹峰洖澶?- [x] 鍗栧鍦ㄨ亰澶╀腑浣跨敤蹇嵎鍥炲
- [x] 蹇嵎鍥炲鍐呭姝ｇ‘濉厖鍒拌緭鍏ユ
- [x] 鍙戦€佸揩鎹峰洖澶嶆秷鎭垚鍔?
---

## Technical Constraints

### 蹇呴』閬靛畧鐨勭害鏉?
1. **鏉冮檺鎺у埗**: 鍙兘淇敼/鍒犻櫎鑷繁鐨勫揩鎹峰洖澶?2. **鍐呭闀垮害闄愬埗**: 鏈€澶?500 瀛楃
3. **榛樿蹇嵎鍥炲**: 鍓嶇鎻愪緵 6 鏉￠粯璁よ瘽鏈?鐢ㄦ埛鍙嚜瀹氫箟瑕嗙洊
4. **鎺掑簭**: 鎸?`sort_order` ASC, `created_at` ASC 鎺掑簭
5. **鐙珛鍔熻兘**: 涓嶄緷璧栧叾浠栦换鍔?鍙嫭绔嬪紑鍙?6. **绾?JDBC**: 浣跨敤 `JdbcTemplate`,涓嶄娇鐢?ORM
7. **浜嬪姟绠＄悊**: 浣跨敤 `@Transactional` 淇濊瘉鏁版嵁涓€鑷存€?
---

## Files to Modify

### 鍚庣

- `backend/src/main/resources/schema.sql` - 鍒涘缓琛ㄥ拰绱㈠紩
- `backend/src/main/java/com/youyu/backend/entity/chat/` - 鍒涘缓 Entity 绫?- `backend/src/main/java/com/youyu/backend/mapper/chat/` - 鍒涘缓 Mapper 鎺ュ彛鍜屽疄鐜?- `backend/src/main/java/com/youyu/backend/service/chat/` - 鍒涘缓 Service 鎺ュ彛鍜屽疄鐜?- `backend/src/main/java/com/youyu/backend/controller/chat/` - 鍒涘缓 Controller

### 鍓嶇

- `frontend/src/api/modules/chat.js` - 娣诲姞 API 鏂规硶
- `frontend/src/stores/chat.js` - 娣诲姞鐘舵€佸拰鏂规硶
- `frontend/src/components/chat/QuickReplyPanel.vue` - 鍒涘缓蹇嵎鍥炲闈㈡澘缁勪欢
- `frontend/src/views/app/MessagesView.vue` - 娣诲姞蹇嵎鍥炲鎸夐挳

### 鏂囨。

- `docs/06-http/chat.http` - 娣诲姞 HTTP 娴嬭瘯鐢ㄤ緥
- `docs/09-api-spec/chat.md` - 鏇存柊 API 鏂囨。
- `CHANGELOG.md` - 娣诲姞鍙樻洿璁板綍

---

## Notes

- 鏈换鍔′笌鍏朵粬浠诲姟瀹屽叏鐙珛,鍙苟琛屽紑鍙?- 蹇嵎鍥炲鏄崠瀹舵彁鏁堝伐鍏?鑳藉鏄捐憲鎻愬崌鍝嶅簲閫熷害
- task-11锛堣嚜鍔ㄥ洖澶嶏級渚濊禆鏈换鍔?
---

## Completion Checklist

瀹屾垚鍚?鍚戝ご Agent 鎶ュ憡:

- [x] 鎵€鏈夐獙鏀舵爣鍑嗗凡婊¤冻
- [x] 鎵€鏈夋祴璇曢€氳繃
- [x] 鏂囨。宸叉洿鏂?- [x] CHANGELOG.md 宸叉洿鏂?- [x] 浠ｇ爜宸叉彁浜ゅ埌鍒嗘敮
- [x] 閬囧埌鐨勯棶棰樺拰瑙ｅ喅鏂规(濡傛湁): 涓庢秷鎭緭鍏ュ尯鍏变韩 `MessagesView.vue`锛岀敱澶?Agent 淇濈暀蹇嵎鍥炲闈㈡澘鍜屽崱鐗囨秷鎭覆鏌撱€?- [x] 閫氱煡澶?Agent: task-11 鍙互寮€濮?