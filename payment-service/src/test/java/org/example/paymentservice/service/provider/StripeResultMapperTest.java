package org.example.paymentservice.service.provider;

import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentIntent.NextAction;
import com.stripe.model.Refund;
import com.stripe.model.StripeError;
import org.example.paymentservice.enums.PaymentProviderStatus;
import org.example.paymentservice.mapper.StripeResultMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeResultMapperTest {

    private final StripeResultMapper mapper = new StripeResultMapper();

    @Test
    void mapsSupportedStripePaymentStatuses() {
        assertThat(mapper.toResult(intent("succeeded")).status())
                .isEqualTo(PaymentProviderStatus.SUCCEEDED);
        assertThat(mapper.toResult(intent("processing")).status())
                .isEqualTo(PaymentProviderStatus.PROCESSING);
        assertThat(mapper.toResult(intent("requires_confirmation")).status())
                .isEqualTo(PaymentProviderStatus.PROCESSING);
        assertThat(mapper.toResult(intent("requires_action")).status())
                .isEqualTo(PaymentProviderStatus.REQUIRES_ACTION);
        assertThat(mapper.toResult(intent("requires_payment_method")).status())
                .isEqualTo(PaymentProviderStatus.FAILED);
        assertThat(mapper.toResult(intent("canceled")).status())
                .isEqualTo(PaymentProviderStatus.CANCELED);
    }

    @Test
    void preservesPaymentProviderDetails() {
        PaymentIntent intent = intent("requires_action");
        StripeError error = new StripeError();
        error.setCode("card_declined");
        intent.setLastPaymentError(error);
        NextAction nextAction = new NextAction();
        nextAction.setType("use_stripe_sdk");
        intent.setNextAction(nextAction);

        var result = mapper.toResult(intent);

        assertThat(result.transactionId()).isEqualTo("pi_test");
        assertThat(result.failureReason()).isEqualTo("card_declined");
        assertThat(result.nextActionType()).isEqualTo("use_stripe_sdk");
        assertThat(result.provider()).isEqualTo("STRIPE");
    }

    @Test
    void rejectsUnsupportedStripePaymentStatus() {
        assertThatThrownBy(() -> mapper.toResult(intent("unknown")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void mapsSuccessfulRefund() {
        Refund refund = refund("re_success", "succeeded");

        var result = mapper.toResult(refund);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.providerRefundId()).isEqualTo("re_success");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void mapsPendingRefund() {
        Refund refund = refund("re_pending", "pending");
        refund.setPendingReason("processing");

        var result = mapper.toResult(refund);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.failureReason()).isEqualTo("processing");
    }

    @Test
    void mapsFailedRefundReason() {
        Refund refund = refund("re_failed", "failed");
        refund.setFailureReason("insufficient_funds");

        var result = mapper.toResult(refund);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.failureReason()).isEqualTo("insufficient_funds");
    }

    private PaymentIntent intent(String status) {
        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_test");
        intent.setStatus(status);
        return intent;
    }

    private Refund refund(String id, String status) {
        Refund refund = new Refund();
        refund.setId(id);
        refund.setStatus(status);
        return refund;
    }
}
