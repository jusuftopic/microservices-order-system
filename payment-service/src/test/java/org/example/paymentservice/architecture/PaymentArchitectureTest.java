package org.example.paymentservice.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.example.paymentservice.repository.InboxRepository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests for the Payment Service module.
 *
 * <p>This test suite enforces structural rules such as layering,
 * dependency direction, and isolation from other bounded contexts.</p>
 *
 * <p>Scope: {@code org.example.paymentservice}</p>
 */
@AnalyzeClasses(packages = "org.example.paymentservice",
importOptions = ImportOption.DoNotIncludeTests.class)
public class PaymentArchitectureTest {

    /**
     * Enforces a strict layered architecture:
     * Kafka Listener → Service → Repository.
     *
     * <ul>
     *     <li>Kafka listeners are entry points and must not be accessed by other layers</li>
     *     <li>Services may only be accessed by Kafka listeners</li>
     *     <li>Repositories may only be accessed by Services</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule layering =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Listener").definedBy("..listener.kafka..")
                    .layer("Service").definedBy("..service..")
                    .layer("Repository").definedBy("..repository..")

                    .whereLayer("Listener").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Listener")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");


    /**
     * Ensures that PaymentService is enforced to use InboxRepository.
     *
     * <p>This enforces correct usage of the inbox pattern for idempotency handling.</p>
     */
    @ArchTest
    static final ArchRule payment_service_uses_inbox =
            classes()
                    .that().haveSimpleName("PaymentService")
                    .should().dependOnClassesThat()
                    .haveSimpleName("InboxRepository");

    /**
     * Restricts Kafka usage to infrastructure components only.
     *
     * <p>KafkaTemplate must not be used inside service layer classes.</p>
     */
    @ArchTest
    static final ArchRule kafka_usage_only_in_listener_and_config =
            noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().haveSimpleName("KafkaTemplate");

    /**
     * Prevents leakage of Order domain into Payment service.
     *
     * <p>Ensures bounded context isolation between Payment and Order services.</p>
     */
    @ArchTest
    static final ArchRule no_order_domain_leak =
            noClasses()
                    .that().resideInAPackage("..paymentservice..")
                    .should().dependOnClassesThat().resideInAnyPackage("..orderservice..");

    /**
     * Prevents using payment provider inside the same transactional context
     *  as persistence of payment details
     */
    @ArchTest
    static final ArchRule external_calls_not_in_repository_layer =
            noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat()
                    .haveSimpleName("PaymentProviderWrapper");
}
