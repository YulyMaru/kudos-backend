package co.edu.uco.kudos.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "co.edu.uco.kudos", importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasArquitecturaTest {

    @ArchTest
    static final ArchRule dominioIndependiente = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..aplicacion..", "..infraestructura..", "..configuracion..",
                    "org.springframework..", "jakarta.persistence..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule aplicacionSoloDependeDelDominio = noClasses()
            .that().resideInAPackage("..aplicacion..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infraestructura..", "..configuracion..",
                    "org.springframework..", "jakarta.persistence..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controladoresSoloUsanPuertosDeEntrada = noClasses()
            .that().resideInAPackage("..infraestructura.entrada..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infraestructura.salida..", "..aplicacion.casouso..")
            .allowEmptyShould(true);
}
