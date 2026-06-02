package com.youyu.backend.service.payment.impl;

import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.payment.PaymentRecordMapper;
import com.youyu.backend.service.notification.NotificationService;
import com.youyu.backend.service.payment.PaymentAttemptLifecycleService;
import com.youyu.backend.service.payment.PaymentGatewayRouter;
import com.youyu.backend.service.payment.PaymentGatewayService;
import com.youyu.backend.service.transaction.support.TransactionDataStore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceImplTest {

    private FakePaymentRecordMapper paymentRecordMapper;
    private TrackingTransactionDataStore transactionDataStore;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentRecordMapper = new FakePaymentRecordMapper();
        transactionDataStore = new TrackingTransactionDataStore();
        paymentService = new PaymentServiceImpl(
                new PaymentGatewayRouter(List.of(new CallbackGateway())),
                paymentRecordMapper,
                transactionDataStore,
                new NoopNotificationService(),
                new PaymentAttemptLifecycleService(paymentRecordMapper),
                30
        );
    }

    @Test
    void callbackRejectsProviderAmountMismatchBeforeRecordingEvent() {
        paymentRecordMapper.payment = payment("12.34");

        assertThatThrownBy(() -> paymentService.processGatewayCallback(
                "alipay_sandbox", Map.of("amount", "13.00", "fingerprint", "fingerprint-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("amount mismatch");
        assertThat(paymentRecordMapper.recordedCallback).isFalse();
    }

    @Test
    void replayedCallbackDoesNotAdvanceOrderState() {
        paymentRecordMapper.payment = payment("12.34");
        paymentRecordMapper.firstDelivery = false;

        Map<String, Object> result = paymentService.processGatewayCallback(
                "alipay_sandbox", Map.of("amount", "12.34", "fingerprint", "fingerprint-2"));

        assertThat(result).containsEntry("replayed", true);
        assertThat(transactionDataStore.mutableOrderRequested).isFalse();
    }

    @Test
    void successfulPaymentIgnoresLaterDistinctProviderStatusWithoutDowngrade() {
        paymentRecordMapper.payment = payment("12.34");
        paymentRecordMapper.payment.put("paymentStatus", "success");

        Map<String, Object> result = paymentService.processGatewayCallback(
                "alipay_sandbox",
                Map.of("amount", "12.34", "fingerprint", "fingerprint-3", "status", "cancelled"));

        assertThat(result).containsEntry("paymentStatus", "success");
        assertThat(result).containsEntry("replayed", false);
        assertThat(paymentRecordMapper.recordedCallback).isTrue();
        assertThat(transactionDataStore.mutableOrderRequested).isFalse();
    }

    @Test
    void resumingExternalGatewayCreatesFreshPaymentAttempt() {
        paymentRecordMapper.payment = payment("12.34");
        paymentRecordMapper.payment.put("paymentMethod", "alipay_sandbox");
        paymentRecordMapper.payment.put("providerTradeNo", null);
        transactionDataStore.mutableOrder = order();

        Map<String, Object> result = paymentService.resumePayment(1010L, "PAY-1");

        assertThat(paymentRecordMapper.updatedPaymentStatus).isEqualTo("cancelled");
        assertThat(paymentRecordMapper.updatedFailedReason).isEqualTo("Payment entry regenerated");
        assertThat(transactionDataStore.createdPayment).isNotNull();
        assertThat(result).containsKeys("payment", "gateway");
        assertThat(((Map<?, ?>) result.get("payment")).get("paymentNo")).isEqualTo("PAY-2");
    }

    private Map<String, Object> payment(String amount) {
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("orderId", 10L);
        payment.put("paymentNo", "PAY-1");
        payment.put("paymentChannel", "alipay_sandbox");
        payment.put("paymentStatus", "initiated");
        payment.put("paymentAmount", new BigDecimal(amount));
        payment.put("paymentMethod", "alipay_sandbox");
        return payment;
    }

    private Map<String, Object> order() {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("id", 10L);
        order.put("orderNo", "ORDER-10");
        order.put("buyerUserId", 1010L);
        order.put("orderStatus", "pending_payment");
        return order;
    }

    private static class CallbackGateway implements PaymentGatewayService {

        @Override
        public String paymentMethod() { return "alipay_sandbox"; }

        @Override
        public String gatewayCode() { return "alipay_sandbox"; }

        @Override
        public boolean available() { return true; }

        @Override
        public Map<String, Object> createPayment(PaymentInitiationRequest request) {
            return Map.of(
                    "gateway", gatewayCode(),
                    "paymentMethod", paymentMethod(),
                    "paymentNo", request.paymentNo(),
                    "status", "pending",
                    "qrCode", "https://qr.example/pay"
            );
        }

        @Override
        public GatewayCallbackResult verifyCallback(Map<String, String> parameters) {
            return new GatewayCallbackResult(
                    "PAY-1", "ALI-1", parameters.getOrDefault("status", "success"),
                    new BigDecimal(parameters.get("amount")),
                    parameters.get("fingerprint"),
                    "trade_status=TRADE_SUCCESS"
            );
        }

        @Override
        public Map<String, Object> refund(RefundRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakePaymentRecordMapper implements PaymentRecordMapper {

        private Map<String, Object> payment;
        private boolean firstDelivery = true;
        private boolean recordedCallback;
        private String updatedPaymentStatus;
        private String updatedFailedReason;

        @Override
        public Map<String, Object> findByPaymentNo(String paymentNo) { return payment; }

        @Override
        public boolean recordCallback(String gatewayCode, String eventFingerprint,
                                      String paymentNo, String callbackSummary) {
            recordedCallback = true;
            return firstDelivery;
        }

        @Override
        public void updateStatus(String paymentNo, String paymentStatus, String providerTradeNo,
                                 String failedReason, String callbackSummary, LocalDateTime succeededAt) {
            updatedPaymentStatus = paymentStatus;
            updatedFailedReason = failedReason;
        }

        @Override
        public int markTimedOutPayments(Long orderId, LocalDateTime initiatedBefore) { return 0; }
    }

    private static class TrackingTransactionDataStore extends TransactionDataStore {

        private boolean mutableOrderRequested;
        private Map<String, Object> mutableOrder;
        private Map<String, Object> createdPayment;

        TrackingTransactionDataStore() {
            super(null, null);
        }

        @Override
        public synchronized Map<String, Object> getMutableOrder(Long orderId) {
            mutableOrderRequested = true;
            return mutableOrder;
        }

        @Override
        public synchronized Map<String, Object> createPayment(Long orderId, BigDecimal amount,
                                                              String paymentMethod, String gatewayCode) {
            createdPayment = new LinkedHashMap<>();
            createdPayment.put("id", 11L);
            createdPayment.put("orderId", orderId);
            createdPayment.put("paymentNo", "PAY-2");
            createdPayment.put("paymentMethod", paymentMethod);
            createdPayment.put("paymentChannel", gatewayCode);
            createdPayment.put("paymentStatus", "initiated");
            createdPayment.put("paymentAmount", amount);
            return createdPayment;
        }
    }

    private static class NoopNotificationService implements NotificationService {

        @Override
        public Map<String, Object> listNotifications(Long userId, int page, int size) { return Map.of(); }
        @Override
        public Map<String, Object> getUnreadCount(Long userId) { return Map.of(); }
        @Override
        public Map<String, Object> createNotification(Long userId, String type, String title,
                                                      String body, String actionUrl) { return Map.of(); }
        @Override
        public Map<String, Object> publishSystemNotification(Long adminUserId, String title,
                                                             String body, String actionUrl) { return Map.of(); }
        @Override
        public void createOrderStatusNotification(Map<String, Object> order, String statusText,
                                                  boolean notifySeller) {
        }
        @Override
        public void createSupportTicketNotification(Long userId, Map<String, Object> ticket,
                                                    String title, String body) {
        }
        @Override
        public void createMediationNotification(Long userId, Map<String, Object> mediationCase,
                                                String title, String body) {
        }
        @Override
        public void markRead(Long userId, Long notificationId) {
        }
        @Override
        public void markAllRead(Long userId) {
        }
    }
}
