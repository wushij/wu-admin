package com.admin.server.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ModuleDependencyTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.admin.server");

    @Test
    void testInfraTradeNoBusinessDeps() {
        noClasses().that().resideInAnyPackage("..modules.infra..", "..modules.trade..")
            .and().haveSimpleNameNotEndingWith("ListExportService")
            .and().haveSimpleNameNotEndingWith("SystemJobTask")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..modules.ticket..", "..modules.message..")
            .because("infra/trade 是底层模块，不得依赖业务模块")
            .check(classes);
    }

    @Test
    void testFrameworkMustNotDependOnModules() {
        noClasses().that().resideInAPackage("com.admin.server.framework..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.admin.server.modules..")
            .because("framework 是框架基础设施层，严禁反向依赖业务模块 modules")
            .check(classes);
    }

    @Test
    void testCommonMustNotDependOnModules() {
        noClasses().that().resideInAPackage("com.admin.server.common..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.admin.server.modules..")
            .because("common 是底层通用基础层，严禁反向依赖业务模块 modules")
            .check(classes);
    }
}
