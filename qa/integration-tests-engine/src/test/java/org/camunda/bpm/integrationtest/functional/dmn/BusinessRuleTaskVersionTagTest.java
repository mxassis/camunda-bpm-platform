/*
 * Copyright © 2012 - 2018 camunda services GmbH and various authors (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.integrationtest.functional.dmn;

import static org.junit.Assert.assertEquals;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.integrationtest.functional.dmn.beans.VersionTagBean;
import org.camunda.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.camunda.bpm.integrationtest.util.DeploymentHelper;
import org.camunda.bpm.integrationtest.util.TestContainer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class BusinessRuleTaskVersionTagTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createProcessArchiveDeployment() {
    return initWebArchiveDeployment()
      .addClass(VersionTagBean.class)
      .addAsResource("org/camunda/bpm/integrationtest/functional/dmn/BusinessRuleTaskVersionTagTest.testDecisionVersionTagOkay.dmn11.xml")
      .addAsResource("org/camunda/bpm/integrationtest/functional/dmn/BusinessRuleTaskVersionTagTest.bpmn20.xml");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addClass(AbstractFoxPlatformIntegrationTest.class)
            .addAsLibraries(DeploymentHelper.getEngineCdi());

    TestContainer.addContainerSpecificResourcesForNonPa(deployment);

    return deployment;
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  public void testStartInstanceWithBeanCondition() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    assertEquals("okay", runtimeService.getVariable(processInstance.getId(), "result"));
  }
}
