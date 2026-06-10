package org.example.inventoryservice.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests for the Inventory Service module.
 *
 * <p>These rules enforce a clean layered architecture and restrict
 * illegal dependencies between components.</p>
 *
 * <p>Scope: {@code org.example.inventoryservice}</p>
 */
@AnalyzeClasses(packages = "org.example.inventoryservice",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class InventoryArchitectureTest {

    /**
     * Enforces a strict layered architecture:
     * Listener → Service → Repository.
     *
     * <ul>
     *     <li>Listeners are entry points and must not be accessed by other layers</li>
     *     <li>Services may only be accessed by Listeners</li>
     *     <li>Repositories may only be accessed by Services</li>
     * </ul>
     */
    @ArchTest
    static final ArchRule layering =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Listener").definedBy("..listener..")
                    .layer("Service").definedBy("..service..")
                    .layer("Repository").definedBy("..repository..")
                    .layer("Initializer").definedBy("..initializer..")

                    .whereLayer("Listener").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Listener")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Initializer");

    /**
     * Ensures that InventoryService is enforced to use inbox and outbox pattern.
     *
     * <p>This enforces correct usage of the inbox pattern for idempotency handling
     * and outbox pattern for reliable message handling.</p>
     */
    @ArchTest
    static final ArchRule inventory_service_uses_inbox_and_outbox =
            classes()
                    .that().haveSimpleName("InventoryService")
                    .should().dependOnClassesThat()
                    .haveSimpleName("InboxRepository")
                    .andShould()
                    .dependOnClassesThat()
                    .haveSimpleName("OutboxRepository");


    /**
     * Ensures that KafkaTemplate is not used directly outside the allowed layers.
     *
     * <p>
     * Kafka access must be encapsulated within the KafkaPublisherService
     * (outbound adapter) or infrastructure configuration. This prevents
     * leaking Kafka-specific logic into business components such as
     * services, listeners, and repositories.
     * </p>
     */
    @ArchTest
    static final ArchRule kafka_template_should_only_be_user_in_wrapper_service =
            noClasses()
                    .that()
                    .resideInAPackage("org.example.inventoryservice..")
                    .and()
                    .resideOutsideOfPackages(
                            "org.example.inventoryservice.config..",
                            "org.example.inventoryservice.service.publisher.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleName("KafkaTemplate");

    /**
     * Prevents leakage of Order domain into Payment service.
     *
     * <p>Ensures bounded context isolation between Payment and Order services.</p>
     */
    @ArchTest
    static final ArchRule no_order_domain_leak =
            noClasses()
                    .that().resideInAPackage("..inventoryservice..")
                    .should().dependOnClassesThat().resideInAnyPackage("..orderservice..");
}


