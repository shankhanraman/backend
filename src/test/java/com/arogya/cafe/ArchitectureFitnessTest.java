package com.arogya.cafe;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture fitness functions — executable guards on design properties that erode silently.
 * Pure bytecode analysis: no Spring context, no database, runs in milliseconds in every build.
 */
@AnalyzeClasses(packages = "com.arogya.cafe", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureFitnessTest {

    /**
     * Money is always {@link java.math.BigDecimal} — never a binary float/double, which can't
     * represent decimal currency exactly. Directly guards the billing-accuracy risk: a single
     * {@code double total} field is how cent-level rounding errors enter a system.
     */
    @ArchTest
    static final ArchRule money_is_never_a_floating_point_type = fields().should()
            .notHaveRawType(double.class)
            .andShould()
            .notHaveRawType(float.class)
            .andShould()
            .notHaveRawType(Double.class)
            .andShould()
            .notHaveRawType(Float.class)
            .because("monetary and quantity values must use BigDecimal for exact decimal arithmetic");

    /** Dependencies point inward: services never reach back up into the web layer. */
    @ArchTest
    static final ArchRule services_do_not_depend_on_controllers = noClasses()
            .that()
            .resideInAPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..controller..")
            .because("the service layer must not know about the web layer");

    /** Domain entities stay free of web/service wiring so the model is reusable and testable. */
    @ArchTest
    static final ArchRule entities_depend_only_on_the_domain = noClasses()
            .that()
            .resideInAPackage("..entity..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..controller..", "..service..")
            .because("JPA entities are the domain model and must not depend on application layers");

    /** Spring Data repositories are interfaces — no hand-rolled data access hiding business logic. */
    @ArchTest
    static final ArchRule repositories_are_interfaces = classes()
            .that()
            .resideInAPackage("..repository..")
            .and()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .beInterfaces()
            .because("repositories must be Spring Data interfaces, not concrete classes")
            .allowEmptyShould(true);
}
