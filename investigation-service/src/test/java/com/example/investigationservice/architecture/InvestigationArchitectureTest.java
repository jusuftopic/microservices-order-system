package com.example.investigationservice.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Protects the Investigation Service boundaries that keep model integration,
 * deterministic fallback and public response mapping independently evolvable.
 */
@AnalyzeClasses(
        packages = "com.example.investigationservice",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class InvestigationArchitectureTest {

    /**
     * Keeps AI generation, deterministic fallback and output validation under
     * one orchestration boundary.
     */
    @ArchTest
    static final ArchRule explanation_service_coordinates_required_components =
            classes()
                    .that().haveSimpleName("InvestigationExplanationService")
                    .should().dependOnClassesThat().haveSimpleName("AiExplanationGenerator")
                    .andShould().dependOnClassesThat().haveSimpleName("ExplanationValidationService")
                    .andShould().dependOnClassesThat().haveSimpleName("DeterministicExplanationGenerator");

    /**
     * Ensures the external model adapter obtains validated and versioned prompts
     * from the application-owned prompt construction boundary.
     */
    @ArchTest
    static final ArchRule model_generator_uses_investigation_prompt_factory =
            classes()
                    .that().haveSimpleName("ModelAiExplanationGenerator")
                    .should().dependOnClassesThat().haveSimpleName("InvestigationPromptFactory");

    /**
     * Prevents Spring AI and provider integration types from leaking beyond the
     * outbound AI adapter package.
     */
    @ArchTest
    static final ArchRule spring_ai_is_confined_to_ai_adapters =
            noClasses()
                    .that().resideOutsideOfPackage("..service.explanation.ai..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework.ai..");

    /**
     * Keeps conversion from the internal timeline into explanation input and
     * the public HTTP representation behind the same mapping boundary.
     */
    @ArchTest
    static final ArchRule investigation_query_uses_timeline_mapper =
            classes()
                    .that().haveSimpleName("InvestigationQueryService")
                    .should().dependOnClassesThat().haveSimpleName("OrderTimelineMapper");

    /**
     * Keeps internal investigation models independent from web, persistence and
     * AI framework contracts.
     */
    @ArchTest
    static final ArchRule internal_models_are_framework_independent =
            noClasses()
                    .that().resideInAPackage("..model..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta..",
                            "javax..",
                            "..controller..",
                            "..dto.."
                    );

    /**
     * Prevents top-level service packages from becoming mutually dependent as
     * the investigation capability grows.
     */
    @ArchTest
    static final ArchRule top_level_packages_are_free_of_cycles =
            slices()
                    .matching("com.example.investigationservice.(*)..")
                    .should().beFreeOfCycles();
}
