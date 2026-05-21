# CampusMarket MVP 鏁版嵁瀛楀吀

## 1. 璁捐鑼冨洿

鏈枃浠朵粎瑕嗙洊 `mvp-database-cut.md` 瑕佹眰鐨?MVP 蹇呭仛琛紝骞朵笌浠ヤ笅鏂囨。淇濇寔涓€鑷达細

- `docs/05-roadmap/archived/mvp-scope.md`
- `docs/05-roadmap/archived/mvp-database-cut.md`
- `docs/02-requirements/functional-requirements.md`
- `docs/02-requirements/core-domain-model.md`

鏁版嵁搴撶被鍨嬫寜璇剧▼绾︽潫閲囩敤 `MySQL 8`銆?
## 2. 缁熶竴绾﹀畾

- 涓婚敭锛氱粺涓€浣跨敤 `BIGINT UNSIGNED AUTO_INCREMENT`
- 鏃堕棿瀹¤锛氭牳蹇冭〃缁熶竴淇濈暀 `created_at`銆乣updated_at`
- 閫昏緫鍒犻櫎锛歚category`銆乣product`銆乣shop`銆乣user_address` 浣跨敤 `is_deleted + deleted_at`
- 鐘舵€佸懡鍚嶏細
  - 瀹炰綋涓荤姸鎬佺粺涓€灏介噺浣跨敤 `status`
  - 瀹℃牳绫讳娇鐢?`review_status`
  - 璁よ瘉绫讳娇鐢?`verification_status`
  - 璁㈠崟涓荤姸鎬佷娇鐢?`order_status`
  - 灞ョ害鐘舵€佷娇鐢?`fulfillment_status`
  - 鏀粯鐘舵€佷娇鐢?`payment_status`
  - 閫€娆剧姸鎬佷娇鐢?`refund_status`
- 閲戦锛氱粺涓€浣跨敤 `DECIMAL(10,2)`
- 鍙墿灞曠瓥鐣ワ細鐘舵€佸瓧娈甸噰鐢?`VARCHAR`锛岄伩鍏嶉鐗堣鏁版嵁搴?`ENUM` 閿佹

## 3. 鏍稿績琛ㄨ鏄?
| 琛ㄥ悕 | 鐢ㄩ€?| 鍏抽敭鐘舵€?鎺у埗瀛楁 |
| --- | --- | --- |
| `user` | 骞冲彴缁熶竴鐢ㄦ埛涓讳綋 | `status` |
| `admin_profile` | `user` 鐨勭鐞嗗憳瀛愮被鎵╁睍 | `admin_role`銆乣status` |
| `student_verification` | 瀛︾敓璁よ瘉鐢宠銆佸巻鍙叉瘮瀵逛笌鍛ㄦ湡澶嶆牳 | `verification_status`銆乣is_current`銆乣risk_flag` |
| `user_privilege_profile` | 鐢ㄦ埛浜ゆ槗/鍙戝竷/寮€搴楄兘鍔涙。浣?| `can_purchase`銆乣can_publish`銆乣can_open_shop`銆乣is_restricted` |
| `user_address` | 鏀惰揣鍦板潃涓庣嚎涓嬪父鐢ㄤ氦浠樺湴鐐?| `address_type`銆乣is_default`銆乣is_deleted` |
| `category` | 鍟嗗搧鍒嗙被浣撶郴 | `status`銆乣is_deleted` |
| `product` | 鍟嗗搧涓诲疄浣擄紝鍏煎瀹炵墿/鐢靛瓙璧勬枡 | `product_type`銆乣status`銆乣review_status` |
| `product_media` | 鍟嗗搧涓诲浘銆佽鎯呭浘銆侀瑙堝浘 | `media_type` |
| `product_review_task` | 璧勬枡绫诲晢鍝佸鏍镐换鍔?| `review_type`銆乣review_status` |
| `shop` | 搴楅摵涓诲疄浣?| `status`銆乣review_status`銆乣is_deleted` |
| `shop_capability_profile` | 搴楅摵缁忚惀鑳藉姏妗ｄ綅 | `capability_level` |
| `cart_item` | 璐墿杞﹂」 | `is_selected` |
| `order` | 缁熶竴璁㈠崟涓诲崟 | `order_status`銆乣fulfillment_type`銆乣payment_status` |
| `order_item` | 璁㈠崟鍟嗗搧蹇収 | `product_type_snapshot` |
| `order_fulfillment` | 灞ョ害缁嗚妭锛屽吋瀹圭墿娴?绾夸笅/鐢靛瓙浜や粯 | `fulfillment_type`銆乣fulfillment_status`銆乣download_access_status` |
| `payment_record` | 妯℃嫙鏀粯涓庡悗缁敮浠樼綉鍏虫帴鍏ョ暀鍙?| `payment_method`銆乣payment_channel`銆乣payment_status` |
| `refund_record` | 閫€娆剧敵璇蜂笌澶勭悊璁板綍 | `refund_status` |
| `product_favorite` | 鐢ㄦ埛鍟嗗搧鏀惰棌鍏崇郴 | 鏃犵嫭绔嬬姸鎬?|
| `review` | 璁㈠崟瀹屾垚鍚庤瘎浠?| `status`銆乣is_anonymous` |
| `report` | 鍟嗗搧/搴楅摵/鐢ㄦ埛/璁㈠崟涓炬姤 | `target_type`銆乣status` |
| `credit_record` | 鐢ㄦ埛鎴栧簵閾轰俊鐢ㄤ簨浠?| `subject_type`銆乣change_direction` |
| `risk_restriction` | 鐢ㄦ埛鎴栧簵閾洪鎺ч檺鍒惰褰?| `subject_type`銆乣restriction_type`銆乣status` |
| `product_digital_asset` | 鐢靛瓙璧勬枡鍙楁帶璧勬簮 | `is_preview_asset`銆乣is_full_asset`銆乣status` |

## 4. 鍏抽敭鍏崇郴璇存槑

- 涓€涓?`user` 瀵瑰簲闆舵垨涓€鏉?`student_verification`
- 涓€涓?`user` 鍙搴旈浂鎴栦竴鏉?`admin_profile`
- 涓€涓?`user` 鍙湁澶氭潯 `student_verification` 鍘嗗彶璁板綍
- 涓€涓?`user` 瀵瑰簲涓€鏉?`user_privilege_profile`
- 涓€涓?`user` 鍙湁澶氭潯 `user_address`
- 涓€涓?`user` 鍙彂甯冨鏉?`product`
- 涓€涓?`user` 鍙嫢鏈夐浂鎴栦竴涓?`shop`
- 涓€涓?`shop` 瀵瑰簲涓€鏉?`shop_capability_profile`
- 涓€涓?`product` 鍙湁澶氭潯 `product_media`
- 涓€涓?`product` 鍙湁澶氭潯 `product_review_task`
- 涓€涓?`product` 鍙湁澶氭潯 `product_digital_asset`
- 涓€涓?`order` 灞炰簬涓€涓拱瀹躲€佷竴涓崠瀹讹紝骞跺彲鍖呭惈澶氭潯 `order_item`
- 涓€涓?`order` 瀵瑰簲涓€鏉?`order_fulfillment`
- 涓€涓?`order` 鍙搴斿鏉?`payment_record` 涓?`refund_record`

## 5. 涓绘帶纭鐨勫瓧娈典簤璁偣

1. `student_verification` 宸茶皟鏁翠负鈥滄敮鎸佸娆¤璇佸巻鍙测€濇ā寮忥紝涓嶅啀瀵?`user_id`銆乣student_no`銆乣campus_email` 鍋氱粷瀵瑰敮涓€绾︽潫銆?杩欐牱鑳芥敮鎸佸鏍搞€佹崲鍛ㄦ湡閲嶈璇佸拰椋庨櫓杩借釜锛屼絾鈥滀竴瀛﹀彿涓€璐﹀彿鈥濈殑寮虹害鏉熼渶瑕佺敱搴旂敤灞傜粨鍚?`is_current + verification_status` 鍋氭牎楠屻€?
2. `report`銆乣credit_record`銆乣risk_restriction` 浣跨敤浜?`target_type/subject_type + target_id/subject_id` 鐨勫鎬佸叧鑱旓紝娌℃湁寮哄閿€?杩欐槸鍥犱负瀹冧滑瑕佸悓鏃舵寚鍚?`user/shop/product/order` 绛変笉鍚屼富浣擄紱濡傛灉涓绘帶鍚庣画鏇村己璋冨己绾︽潫锛岄渶瑕佸啀鎷嗗垎涓哄寮犻鍩熷瓙琛ㄣ€?
3. `product.status` 涓?`product.review_status` 褰撳墠鍒嗙锛宍shop.status` 涓?`shop.review_status` 涔熷凡鍒嗙銆?杩欐牱鏇寸鍚堚€滃晢鍝佷笂涓嬫灦鈥濆拰鈥滆祫鏂欏鏍糕€濅袱鏉＄姸鎬佺嚎骞跺瓨鐨勮姹傦紝浣嗗悗绔疄鐜版椂闇€瑕佹槑纭姸鎬佹祦杞鍒欙紝閬垮厤鍑虹幇 `draft + approved` 涔嬬被鏃犳剰涔夌粍鍚堛€?
4. `order_fulfillment` 閲囩敤鍗曡〃鎵胯浇涓夌被灞ョ害鏄庣粏锛屽苟鐢ㄥ彲绌哄瓧娈靛尯鍒嗙墿娴?绾夸笅/鐢靛瓙浜や粯銆?杩欐洿璐村悎 `MVP` 鐨勭粺涓€璁㈠崟涓荤姸鎬佽璁★紱濡傛灉鍚庣画灞ョ害瀛楁鏄捐憲澧炲锛屽啀鑰冭檻鎷嗘垚 `order_fulfillment_logistics` 绛夋墿灞曡〃銆?
5. 褰撳墠宸查噰鐢ㄢ€渀user` 瓒呯被 + `admin_profile` 瀛愯〃鈥濇柟鍚戯紝浣嗗鏍?澶勭悊瀛楁浠嶆殏鏈姞澶栭敭銆?杩欐牱鍙互鍏堜繚鎸佽剼鏈畝鍗曪紱鑻ュ悗缁富鎺х‘璁ゆ墍鏈夊悗鍙版搷浣滀汉蹇呴』鏉ヨ嚜绠＄悊鍛樺瓙绫伙紝鍙繘涓€姝ユ妸杩欎簺瀛楁缁熶竴鏀舵暃鍒扮鐞嗗憳涓讳綋寮曠敤绛栫暐銆?
