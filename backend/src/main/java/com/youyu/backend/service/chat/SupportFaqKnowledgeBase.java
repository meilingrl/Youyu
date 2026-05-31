package com.youyu.backend.service.chat;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Rule-based FAQ knowledge base for the campus marketplace online customer
 * service bot. Matches the user's message against a small set of keyword groups
 * and returns a canned answer. No external LLM is involved.
 *
 * <p>The knowledge base intentionally lives in code so it can be seeded/changed
 * with the application and stays idempotent across restarts.</p>
 */
@Component
public class SupportFaqKnowledgeBase {

    /** Suffix appended to every matched answer to remind users they can escalate. */
    public static final String ESCALATE_HINT = "如果未能解决你的问题，可以点击「转人工」由平台客服跟进。";

    /** Fallback reply when no FAQ entry matches the message. */
    public static final String FALLBACK_REPLY =
            "抱歉，我暂时没能完全理解你的问题。你可以补充订单号、商品链接或问题截图，"
            + "也可以点击「转人工」，由平台客服为你跟进处理。";

    /** Greeting used for the very first automated message in a session. */
    public static final String GREETING =
            "你好，我是有语校园的在线客服助手。你可以咨询退款、发货、账号、实名认证、支付、举报等常见问题，"
            + "我会尽量帮你解答；需要人工协助时随时点击「转人工」。";

    private record FaqEntry(List<String> keywords, String answer) {
    }

    private static final List<FaqEntry> ENTRIES = List.of(
            new FaqEntry(
                    List.of("退款", "退货", "退钱", "refund"),
                    "关于退款：在「我的订单」中找到对应订单申请退款，卖家或平台通常会在 48 小时内处理，"
                            + "款项将原路退回到你的支付账户。若超过时限仍未到账，请提供订单号，我们会优先核查。"),
            new FaqEntry(
                    List.of("发货", "物流", "快递", "什么时候发", "自提", "收货"),
                    "关于发货与自提：校园交易支持线下自提和同城快递。卖家会在你付款后安排发货或约定自提时间，"
                            + "你可以在订单详情中查看物流与交接信息，也可以直接和卖家约定校内地点。"),
            new FaqEntry(
                    List.of("账号", "登录", "登陆", "密码", "封号", "注销", "account"),
                    "关于账号：忘记密码可在登录页通过账号绑定的邮箱找回；学生认证中的校园邮箱是独立流程。若账号被限制或异常，请说明具体情况，"
                            + "我们会协助你核查账号状态并尽快恢复正常使用。"),
            new FaqEntry(
                    List.of("实名", "认证", "学生认证", "学籍", "verify", "verification"),
                    "关于实名/学生认证：请在「我的-学生认证」提交学号、真实姓名与校园邮箱等信息，"
                            + "审核一般 1 个工作日内完成。认证通过后即可解锁发布、交易等校园专属能力。"),
            new FaqEntry(
                    List.of("支付", "付款", "支付失败", "扣款", "pay", "payment"),
                    "关于支付：当前为校园演示支付环境。若支付失败或重复扣款，请勿重复下单，"
                            + "提供订单号后我们会帮你核对支付记录并处理异常。"),
            new FaqEntry(
                    List.of("举报", "投诉", "违规", "骗", "诈骗", "report"),
                    "关于举报：如遇虚假商品、欺诈或违规行为，可在商品或订单页提交举报，并补充聊天记录、"
                            + "交易截图等证据。平台会尽快介入核实并处置，必要时进入调解流程。"),
            new FaqEntry(
                    List.of("评价", "好评", "差评", "review"),
                    "关于评价：完成交易并确认收货后，可在「交易中心-待评价」补充评价，"
                            + "客观的评价有助于其他同学判断商品和卖家的可靠程度。"),
            new FaqEntry(
                    List.of("你好", "在吗", "hi", "hello", "客服"),
                    "你好，我是在线客服助手。请描述你遇到的问题，例如退款、发货、账号、实名认证、支付或举报，"
                            + "我会尽量帮你解答。")
    );

    /**
     * Returns a matched FAQ answer (with the escalate hint appended) or the
     * fallback reply when nothing matches. Never returns {@code null}.
     */
    public String answer(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            for (FaqEntry entry : ENTRIES) {
                for (String keyword : entry.keywords()) {
                    if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                        return entry.answer() + ESCALATE_HINT;
                    }
                }
            }
        }
        return FALLBACK_REPLY;
    }
}
