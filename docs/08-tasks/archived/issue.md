1为什么后台管理控制台-资料审核，有的商品没有操作按钮
2为什么举报处理每个对象的操作都不同？有的有驳回，有的有标记处理中，有的为啥没有。店铺管理也有类似的
3

Validation failed for argument [0] in public com.campusmarket.backend.common.api.ApiResponse<java.util.Map<java.lang.String, java.lang.Object>> com.campusmarket.backend.controller.auth.AuthController.register(com.campusmarket.backend.controller.auth.dto.RegisterRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'registerRequest' on field 'username': rejected value [null]; codes [NotBlank.registerRequest.username,NotBlank.username,NotBlank.java.lang.String,NotBlank]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [registerRequest.username,username]; arguments []; default message [username]]; default message [用户名不能为空]]
为啥不能注册

4UI优化，问题很多

5偏好设置，暗夜没有实现，主题色切换大概率也没有，而且颜色太少，自主性不够

6设计逻辑本身是对的（只有买了才能评），但缺了两个东西：

  1. 商品详情页评论区没有"写评价"入口，哪怕点一下弹提示"完成订单后可评价"都没有
  2. 种子数据没有已完成的订单 → 整个评价流程永远跑不通

7网页里很多东西都用的代码名字比如xxxx_xxxxx完全没法看，全是代码没有语义转化：
你说的是枚举值直接裸展示的问题。数据库存的
  offline_face_to_face、pending_payment、mock_payment 这种 snake_case
  英文，很多地方没有转成中文就直接渲染到页面上了。

  Searched for 2 patterns (ctrl+o to expand)

● 确认了，大量地方直接把数据库的 snake_case 值渲染到了页面上：

  订单相关（最严重）：
  - OrdersView.vue:204 — {{ order.fulfillmentType }} → 页面显示 offline_face_to_face
  - OrdersView.vue:208 — {{ order.orderStatus }} → 页面显示 pending_payment
  - OrdersView.vue:226,228 — 订单详情抽屉里同样裸显
  - PaymentView.vue:88,90 — 支付页同样 orderStatus、fulfillmentType 裸显
  - OrderManageView.vue:161,163 — 管理员订单详情也是裸显

  其他：
  - ShopManageView.vue:171 — 审核状态显示 pending_review
  - SellerProductsView.vue:103 — 商品审核状态显示 pending_review

  做了映射的地方（少数）：
  - CartView.vue 和 CheckoutView.vue 用了三元表达式 === 'digital' ? '电子资料' :
  '实物商品'
  - ProductListView.vue 和 HomeView.vue 用了 productTypeLabels 映射表

  结论就是：标签映射做了一半，有些地方有 productTypeLabels，但订单状态、履约方式、支付
  状态、审核状态这些都没有统一的中文映射，直接裸吐数据库值。所以你看到的
  offline_face_to_face、pending_payment 到处都是。

8 一到我的发布界面就会卡加载，每次都是
