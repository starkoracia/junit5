/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import org.junit.jupiter.api.MethodOrderer.Alphanumeric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.platform.commons.util.CollectionUtils;
import org.junit.platform.commons.util.ModuleUtils;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

@TestMethodOrder(Alphanumeric.class)
class JupiterIntegrationTests {

	@Test
	void javaPlatformModuleSystemIsAvailable() {
		assertTrue(ModuleUtils.isJavaPlatformModuleSystemAvailable());
	}

	@Test
	void packageName() {
		assertEquals("integration", getClass().getPackageName());
	}

	@Test
	void moduleIsNamed() {
		assumeTrue(getClass().getModule().isNamed(), "not running on the module-path");
		assertEquals("integration", getClass().getModule().getName());
	}

	@Test
	void resolve() {
		assumeTrue(getClass().getModule().isNamed(), "not running on the module-path");

		ModuleSelector selector = DiscoverySelectors.selectModule(getClass().getModule().getName());
		assertEquals(getClass().getModule().getName(), selector.getModuleName());

		TestPlan testPlan = LauncherFactory.create().discover(request()
				.selectors(selector)
				.filters(includeEngines("junit-jupiter"))
				.build());

		TestIdentifier engine = CollectionUtils.getOnlyElement(testPlan.getRoots());

		assertEquals(1, testPlan.getChildren(engine).size()); // JupiterIntegrationTests.class
		assertEquals(5, testPlan.getChildren(getOnlyElement(testPlan.getChildren(engine))).size()); // 5 test methods
	}

	@Test
	@EnabledIf("1 == 1")
	void javaScriptingModuleIsAvailable() {
		/* empty */
	}

}
