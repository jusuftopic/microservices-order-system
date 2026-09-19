package org.example.paymentservice.service.provider;

import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentIntent.NextAction;
import com.stripe.model.StripeError;
import org.example.paymentservice.enums.PaymentProviderStatus;
import org.example.paymentservice.mapper.StripePaymentResultMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripePaymentResultMapperTest {

    private final StripePaymentResultMapper mapper =
            new StripePaymentResultMapper();

    @Test
    void mapsSupportedStripeStatuses() {
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
    void preservesProviderDetails() {
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
    void rejectsUnsupportedStripeStatus() {
        assertThatThrownBy(() -> mapper.toResult(intent("unknown")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    private PaymentIntent intent(String status) {
        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_test");
        intent.setStatus(status);
        return intent;
    }
}
