package com.example.investigationservice.service.explanation.deterministic;

import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationEvidence;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.contracts.lifecycle.CompensationType;
import org.example.messagingstarter.contracts.lifecycle.LifecycleReasonCode;
import org.example.messagingstarter.contracts.lifecycle.OrchestrationDecisionCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Produces evidence-based explanations without an external model provider.
 */
@Service
@Slf4j
public class DeterministicExplanationGenerator {

    /**
     * Generates a deterministic explanation from collected evidence.
     *
     * @param context authoritative investigation context
     * @return deterministic explanation, when one can be produced
     */
    public Optional<String> generate(InvestigationContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.debug(
                "[INVESTIGATION-SERVICE][DETERMINISTIC-EXPLANATION] "
                        + "Generating deterministic explanation for order {}",
                context.orderId()
        );

        if (!context.hasEvidence()) {
            log.debug(
                    "[INVESTIGATION-SERVICE][DETERMINISTIC-EXPLANATION] "
                            + "No evidence is available for order {}",
                    context.orderId()
            );
            return Optional.empty();
        }

        InvestigationEvidence latestEvidence =
                context.evidence().get(context.evidence().size() - 1);
        List<String> explanationParts = new ArrayList<>();
        explanationParts.add(explainReason(latestEvidence));
        explainDecision(latestEvidence).ifPresent(explanationParts::add);
        explainCompensation(latestEvidence).ifPresent(explanationParts::add);

        String explanation = String.join(" ", explanationParts);
        log.debug(
                "[INVESTIGATION-SERVICE][DETERMINISTIC-EXPLANATION] "
                        + "Generated deterministic explanation for order {} from reason {}",
                context.orderId(),
                latestEvidence.reasonCode()
        );
        return Optional.of(explanation);
    }

    private String explainReason(InvestigationEvidence evidence) {
        return LifecycleReasonCode.fromCode(evidence.reasonCode())
                .map(reason -> switch (reason) {
                    case ORDER_STATUS_UPDATED -> explainTransition(evidence);
                    case INVENTORY_RESERVED ->
                            "Inventory was reserved successfully for the order.";
                    case INVENTORY_RESERVATION_FAILED ->
                            "Inventory could not be reserved for the order.";
                    case PAYMENT_COMPLETED -> "Payment was completed successfully.";
                    case PAYMENT_FAILED -> "Payment processing failed.";
                    case INVENTORY_COMMITTED ->
                            "The reserved inventory was committed successfully.";
                    case ORDER_WORKFLOW_COMPLETED ->
                            "The order workflow completed successfully.";
                    case COMPENSATION_COMPLETED ->
                            "The compensation completed successfully, and the order was finalized as failed.";
                    case INVENTORY_COMMIT_FAILED ->
                            "The reserved inventory could not be committed.";
                    case PAYMENT_REFUND_REQUIRED ->
                            "The order was marked as failed because inventory could not be committed after payment.";
                    case ORDER_PROCESSING_TIMEOUT ->
                            "Order processing timed out before the workflow reached a final state.";
                })
                .orElseGet(() -> explainTransition(evidence));
    }

    private Optional<String> explainDecision(InvestigationEvidence evidence) {
        if (evidence.decisionCode() == null || evidence.decisionCode().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(
                OrchestrationDecisionCode.fromCode(evidence.decisionCode())
                        .map(decision -> switch (decision) {
                            case PROCESS_PAYMENT -> "Payment processing has been requested.";
                            case COMMIT_INVENTORY ->
                                    "Committing the reserved inventory has been requested.";
                            case RELEASE_INVENTORY ->
                                    "Releasing the reserved inventory has been requested.";
                            case SEND_NOTIFICATION ->
                                    "Sending a customer notification has been requested.";
                            case REFUND_PAYMENT -> "A payment refund has been requested.";
                        })
                        .orElseGet(() -> explainUnknownDecision(evidence))
        );
    }

    private Optional<String> explainCompensation(InvestigationEvidence evidence) {
        if (!evidence.compensationRequired()) {
            return Optional.empty();
        }

        return Optional.of(
                CompensationType.fromCode(evidence.compensationType())
                        .map(type -> switch (type) {
                            case INVENTORY_RELEASE ->
                                    "The inventory release is a compensating action.";
                            case PAYMENT_REFUND ->
                                    "The payment refund is a compensating action.";
                            case STATUS_DEPENDENT_COMPENSATION ->
                                    "The required compensation depends on the last completed workflow step.";
                        })
                        .orElse("A compensating action is required.")
        );
    }

    private String explainTransition(InvestigationEvidence evidence) {
        return "The order moved from "
                + humanizeCode(evidence.previousStatus())
                + " to "
                + humanizeCode(evidence.newStatus())
                + ".";
    }

    private String explainUnknownDecision(InvestigationEvidence evidence) {
        if (evidence.decisionTargetService() == null
                || evidence.decisionTargetService().isBlank()) {
            return "A follow-up action has been requested.";
        }

        return "A follow-up action has been requested from "
                + humanizeCode(evidence.decisionTargetService())
                + ".";
    }

    private String humanizeCode(String code) {
        if (code == null || code.isBlank()) {
            return "an unknown state";
        }

        String[] words = code.trim().toLowerCase(Locale.ROOT).split("_+");
        List<String> formattedWords = new ArrayList<>(words.length);
        for (String word : words) {
            if (!word.isEmpty()) {
                formattedWords.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
            }
        }
        return String.join(" ", formattedWords);
    }
}
