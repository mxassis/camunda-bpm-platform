/*
 * Copyright © 2013-2018 camunda services GmbH and various authors (info@camunda.com)
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
package org.camunda.bpm.engine.test.bpmn.usertask;

import java.util.List;

import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;

/**
 * @author Joram Barrez
 */
public class TaskCandidateTest extends PluggableProcessEngineTestCase {

  private static final String KERMIT = "kermit";

  private static final String GONZO = "gonzo";

  public void setUp() throws Exception {
    super.setUp();
    
    Group accountants = identityService.newGroup("accountancy");
    identityService.saveGroup(accountants);
    Group managers = identityService.newGroup("management");
    identityService.saveGroup(managers);
    Group sales = identityService.newGroup("sales");
    identityService.saveGroup(sales);

    User kermit = identityService.newUser(KERMIT);
    identityService.saveUser(kermit);
    identityService.createMembership(KERMIT, "accountancy");

    User gonzo = identityService.newUser(GONZO);
    identityService.saveUser(gonzo);
    identityService.createMembership(GONZO, "management");
    identityService.createMembership(GONZO, "accountancy");
    identityService.createMembership(GONZO, "sales");
  }

  public void tearDown() throws Exception {
    identityService.deleteUser(KERMIT);
    identityService.deleteUser(GONZO);
    identityService.deleteGroup("sales");
    identityService.deleteGroup("accountancy");
    identityService.deleteGroup("management");
    
    super.tearDown();
  }

  @Deployment
  public void testSingleCandidateGroup() {

    // Deploy and start process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("singleCandidateGroup");

    // Task should not yet be assigned to kermit
    List<Task> tasks = taskService
      .createTaskQuery()
      .taskAssignee(KERMIT)
      .list();
    assertTrue(tasks.isEmpty());

    // The task should be visible in the candidate task list
    tasks = taskService.createTaskQuery().taskCandidateUser(KERMIT).list();
    assertEquals(1, tasks.size());
    Task task = tasks.get(0);
    assertEquals("Pay out expenses", task.getName());

    // Claim the task
    taskService.claim(task.getId(), KERMIT);

    // The task must now be gone from the candidate task list
    tasks = taskService.createTaskQuery().taskCandidateUser(KERMIT).list();
    assertTrue(tasks.isEmpty());

    // The task will be visible on the personal task list
    tasks = taskService
      .createTaskQuery()
      .taskAssignee(KERMIT)
      .list();
    assertEquals(1, tasks.size());
    task = tasks.get(0);
    assertEquals("Pay out expenses", task.getName());

    // Completing the task ends the process
    taskService.complete(task.getId());

    assertProcessEnded(processInstance.getId());
  }

  @Deployment
  public void testMultipleCandidateGroups() {

    // Deploy and start process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multipleCandidatesGroup");

    // Task should not yet be assigned to anyone
    List<Task> tasks = taskService
      .createTaskQuery()
      .taskAssignee(KERMIT)
      .list();
    
    assertTrue(tasks.isEmpty());
    tasks = taskService
      .createTaskQuery()
      .taskAssignee(GONZO)
      .list();
    
    assertTrue(tasks.isEmpty());

    // The task should be visible in the candidate task list of Gonzo and Kermit
    // and anyone in the management/accountancy group
    assertEquals(1, taskService.createTaskQuery().taskCandidateUser(KERMIT).list().size());
    assertEquals(1, taskService.createTaskQuery().taskCandidateUser(GONZO).list().size());
    assertEquals(1, taskService.createTaskQuery().taskCandidateGroup("management").count());
    assertEquals(1, taskService.createTaskQuery().taskCandidateGroup("accountancy").count());
    assertEquals(0, taskService.createTaskQuery().taskCandidateGroup("sales").count());

    // Gonzo claims the task
    tasks = taskService.createTaskQuery().taskCandidateUser(GONZO).list();
    Task task = tasks.get(0);
    assertEquals("Approve expenses", task.getName());
    taskService.claim(task.getId(), GONZO);

    // The task must now be gone from the candidate task lists
    assertTrue(taskService.createTaskQuery().taskCandidateUser(KERMIT).list().isEmpty());
    assertTrue(taskService.createTaskQuery().taskCandidateUser(GONZO).list().isEmpty());
    assertEquals(0, taskService.createTaskQuery().taskCandidateGroup("management").count());

    // The task will be visible on the personal task list of Gonzo
    assertEquals(1, taskService
      .createTaskQuery()
      .taskAssignee(GONZO)
      .count());

    // But not on the personal task list of (for example) Kermit
    assertEquals(0, taskService.createTaskQuery().taskAssignee(KERMIT).count());

    // Completing the task ends the process
    taskService.complete(task.getId());

    assertProcessEnded(processInstance.getId());
  }

  @Deployment
  public void testMultipleCandidateUsers() {
    runtimeService.startProcessInstanceByKey("multipleCandidateUsersExample");

    assertEquals(1, taskService.createTaskQuery().taskCandidateUser(GONZO).list().size());
    assertEquals(1, taskService.createTaskQuery().taskCandidateUser(KERMIT).list().size());
  }

  @Deployment
  public void testMixedCandidateUserAndGroup() {
    runtimeService.startProcessInstanceByKey("mixedCandidateUserAndGroupExample");

    assertEquals(1, taskService.createTaskQuery().taskCandidateUser(GONZO).list().size());
    assertEquals(1, taskService.createTaskQuery().taskCandidateUser(KERMIT).list().size());
  }

}
