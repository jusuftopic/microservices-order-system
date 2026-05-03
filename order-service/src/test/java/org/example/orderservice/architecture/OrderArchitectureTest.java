package org.example.orderservice.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;


/**
 * Architecture tests for the Order Service module.
 *
 * <p>These rules enforce a clean layered architecture and restrict
 * illegal dependencies between components.</p>
 *
 * <p>Scope: {@code org.example.orderservice}</p>
 */
@AnalyzeClasses(packages = "org.example.orderservice",
importOptions = ImportOption.DoNotIncludeTests.class)
public class OrderArchitectureTest {

    /**
     * Enforces a strict layered architecture:
     * Controller → Service → Repository.
     *
     * <ul>
     *     <li>Controllers must not be accessed by other layers</li>
     *     <li>Services may only be accessed by Controllers</li>
     *     <li>Repositories may only be accessed by Services</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule layering =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Controller").definedBy("..controller..")
                    .layer("Service").definedBy("..service..")
                    .layer("Repository").definedBy("..repository..")

                    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    /**
     * Restricts usage of OutboxRepository to approved services only.
     *
     * <p>Only OrderService and OutboxEventPublisherService may access it.</p>
     */
    @ArchTest
    static final ArchRule outbox_must_be_used =
            methods()
                    .that().areDeclaredInClassesThat()
                    .haveSimpleName("OutboxRepository")
                    .should().onlyBeCalled()
                    .byClassesThat()
                    .haveSimpleName("OrderService")
                    .orShould()
                    .onlyBeCalled()
                    .byClassesThat()
                    .haveSimpleName("OutboxEventPublisherService");



    /**
     * Prevents direct usage of KafkaTemplate outside approved infrastructure layers.
     *
     * <p>Kafka access is restricted to configuration and publisher components.</p>
     */
    @ArchTest
    static final ArchRule no_direct_kafka =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("..config..")
                    .and().resideOutsideOfPackage("..service.publisher..")
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleName("KafkaTemplate");

    /**
     * Ensures controllers do not access repositories directly.
     *
     * <p>All persistence must go through the service layer.</p>
     */
    @ArchTest
    static final ArchRule no_service_should_use_repository_directly_in_controller =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..");


    /**
     * Prevents leakage of Payment domain into Order service.
     *
     * <p>Ensures bounded context isolation between Payment and Order services.</p>
     */
    @ArchTest
    static final ArchRule no_order_domain_leak =
            noClasses()
                    .that().resideInAPackage("..orderservice..")
                    .should().dependOnClassesThat().resideInAnyPackage("..paymentservice..");
}
