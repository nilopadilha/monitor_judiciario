package br.com.monitorjudicial;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;

/** Regras da seção 7 do documento de arquitetura, verificadas a cada build. */
@AnalyzeClasses(packages = "br.com.monitorjudicial", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final List<String> MODULES = List.of("authentication", "clients", "tribunals", "processes", "djen",
            "monitoring", "notifications", "audit", "dashboard");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                    "..infrastructure..", "..web..", "com.fasterxml..", "lombok..")
            .because("o domínio é Java puro (ADR-0002)");

    @ArchTest
    static final ArchRule controllersNeverTouchInfrastructure = noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("Controller -> Repository é proibido pelo PDF");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnWeb = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..web..");

    @ArchTest
    static final ArchRule modulesTalkOnlyThroughApi = noClasses()
            .should(accessAnotherModuleOutsideItsApi())
            .because("um módulo só pode usar o pacote api de outro (ADR-0001)");

    @ArchTest
    static final ArchRule noCyclesBetweenModules = slices()
            .matching("br.com.monitorjudicial.(*)..")
            .should().beFreeOfCycles();

    private static ArchCondition<JavaClass> accessAnotherModuleOutsideItsApi() {
        return new ArchCondition<>("access another module outside its api package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String originModule = moduleOf(origin.getPackageName());
                if (originModule == null) {
                    return;
                }
                origin.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    String targetModule = moduleOf(target.getPackageName());
                    if (targetModule != null && !targetModule.equals(originModule)
                            && !target.getPackageName().startsWith("br.com.monitorjudicial." + targetModule + ".api")) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                });
            }
        };
    }

    private static String moduleOf(String packageName) {
        return MODULES.stream()
                .filter(m -> packageName.equals("br.com.monitorjudicial." + m)
                        || packageName.startsWith("br.com.monitorjudicial." + m + "."))
                .findFirst().orElse(null);
    }

    @SuppressWarnings("unused")
    private static final DescribedPredicate<JavaClass> UNUSED = DescribedPredicate.alwaysTrue();
}
