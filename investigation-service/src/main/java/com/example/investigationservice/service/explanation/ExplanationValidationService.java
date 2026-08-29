package com.example.investigationservice.service.explanation;

import com.example.investigationservice.model.AiExplanationResponse;
import com.example.investigationservice.model.ExplanationValidationResult;
import com.example.investigationservice.model.ExplanationValidationResult.FailureReason;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationEvidence;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.contracts.lifecycle.CompensationType;
import org.example.messagingstarter.contracts.lifecycle.LifecycleReasonCode;
import org.example.messagingstarter.contracts.lifecycle.OrchestrationDecisionCode;
import org.example.messagingstarter.contracts.lifecycle.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.example.investigationservice.service.explanation.ai.prompt.InvestigationPromptFactory.MAX_EXPLANATION_CHARACTERS;

/**
 * Validates generated explanations against the response contract and the
 * evidence supplied to the generator.
 */
@Service
@Slf4j
public class ExplanationValidationService {

    /**
     * Validates an AI-generated explanation candidate.
     *
     * @param candidate structured generated response
     * @param context evidence used during generation
     * @return validation outcome and a bounded rejection reason
     */
    public ExplanationValidationResult validate(
            AiExplanationResponse candidate,
            InvestigationContext context
    ) {
        if (candidate == null) {
            return reject(FailureReason.MISSING_CANDIDATE, context);
        }
        if (context == null) {
            return reject(FailureReason.MISSING_CONTEXT, null);
        }
        if (!context.hasEvidence()) {
            return reject(FailureReason.MISSING_EVIDENCE, context);
        }
        if (candidate.explanation() == null || candidate.explanation().isBlank()) {
            return reject(FailureReason.MISSING_EXPLANATION, context);
        }
        if (candidate.explanation().trim().length() > MAX_EXPLANATION_CHARACTERS) {
            return reject(FailureReason.EXPLANATION_TOO_LONG, context);
        }
        if (containsUnsupportedControlCharacter(candidate.explanation())) {
            return reject(FailureReason.INVALID_CONTROL_CHARACTER, context);
        }

        InvestigationEvidence latestEvidence = context.evidence().getLast();
        ExplanationValidationResult statusResult = validateStatus(candidate, context);
        if (!statusResult.valid()) {
            return statusResult;
        }

        ExplanationValidationResult reasonResult =
                validateReason(candidate, latestEvidence, context);
        if (!reasonResult.valid()) {
            return reasonResult;
        }

        ExplanationValidationResult decisionResult =
                validateDecision(candidate, latestEvidence, context);
        if (!decisionResult.valid()) {
            return decisionResult;
        }

        return validateCompensation(candidate, latestEvidence, context);
    }

    private ExplanationValidationResult validateStatus(
            AiExplanationResponse candidate,
            InvestigationContext context
    ) {
        if (OrderStatus.fromCode(candidate.currentStatus()).isEmpty()) {
            return reject(FailureReason.UNSUPPORTED_STATUS, context);
        }
        if (!Objects.equals(candidate.currentStatus(), context.currentStatus())) {
            return reject(FailureReason.STATUS_MISMATCH, context);
        }
        return ExplanationValidationResult.accepted();
    }

    private ExplanationValidationResult validateReason(
            AiExplanationResponse candidate,
            InvestigationEvidence latestEvidence,
            InvestigationContext context
    ) {
        if (!Objects.equals(candidate.reasonCode(), latestEvidence.reasonCode())) {
            return reject(FailureReason.REASON_CODE_MISMATCH, context);
        }
        if (LifecycleReasonCode.fromCode(candidate.reasonCode()).isEmpty()) {
            return reject(FailureReason.UNSUPPORTED_REASON_CODE, context);
        }
        return ExplanationValidationResult.accepted();
    }

    private ExplanationValidationResult validateDecision(
            AiExplanationResponse candidate,
            InvestigationEvidence latestEvidence,
            InvestigationContext context
    ) {
        if (!Objects.equals(candidate.decisionCode(), latestEvidence.decisionCode())) {
            return reject(FailureReason.DECISION_CODE_MISMATCH, context);
        }
        if (candidate.decisionCode() != null
                && OrchestrationDecisionCode.fromCode(candidate.decisionCode()).isEmpty()) {
            return reject(FailureReason.UNSUPPORTED_DECISION_CODE, context);
        }
        return ExplanationValidationResult.accepted();
    }

    private ExplanationValidationResult validateCompensation(
            AiExplanationResponse candidate,
            InvestigationEvidence latestEvidence,
            InvestigationContext context
    ) {
        if (!Objects.equals(
                candidate.compensationType(),
                latestEvidence.compensationType()
        )) {
            return reject(FailureReason.COMPENSATION_TYPE_MISMATCH, context);
        }
        if (candidate.compensationType() != null
                && CompensationType.fromCode(candidate.compensationType()).isEmpty()) {
            return reject(FailureReason.UNSUPPORTED_COMPENSATION_TYPE, context);
        }
        return ExplanationValidationResult.accepted();
    }

    private boolean containsUnsupportedControlCharacter(String explanation) {
        return explanation.chars()
                .anyMatch(character -> Character.isISOControl(character)
                        && character != '\n'
                        && character != '\r'
                        && character != '\t');
    }

    private ExplanationValidationResult reject(
            FailureReason reason,
            InvestigationContext context
    ) {
        if (context == null) {
            log.warn(
                    "[INVESTIGATION-SERVICE][EXPLANATION-VALIDATION] "
                            + "Rejected AI explanation with reason {}; order is unavailable",
                    reason
            );
        } else {
            log.warn(
                    "[INVESTIGATION-SERVICE][EXPLANATION-VALIDATION] "
                            + "Rejected AI explanation for order {} with reason {}",
                    context.orderId(),
                    reason
            );
        }
        return ExplanationValidationResult.rejected(reason);
    }
}
