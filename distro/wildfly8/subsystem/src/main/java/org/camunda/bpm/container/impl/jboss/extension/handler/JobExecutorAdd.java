/*
 * Copyright (C) 2011, 2012 camunda services GmbH
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
package org.camunda.bpm.container.impl.jboss.extension.handler;

import org.camunda.bpm.container.impl.jboss.extension.SubsystemAttributeDefinitons;
import org.camunda.bpm.container.impl.jboss.service.MscExecutorService;
import org.camunda.bpm.container.impl.jboss.service.ServiceNames;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.threads.BoundedQueueThreadPoolService;
import org.jboss.as.threads.ManagedQueueExecutorService;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.as.threads.TimeSpec;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


/**
 * Installs the JobExecutor service into the container.
 *
 * @author Christian Lipphardt
 */
public class JobExecutorAdd extends AbstractAddStepHandler {

  public static final String THREAD_POOL_GRP_NAME = "Camunda BPM ";

  public static final JobExecutorAdd INSTANCE = new JobExecutorAdd();

  private JobExecutorAdd() {
    super(SubsystemAttributeDefinitons.JOB_EXECUTOR_ATTRIBUTES);
  }

  @Override
  protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
          ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
          throws OperationFailedException {

    String jobExecutorThreadPoolName = SubsystemAttributeDefinitons.THREAD_POOL_NAME.resolveModelAttribute(context, model).asString();
    ServiceName jobExecutorThreadPoolServiceName = ServiceNames.forManagedThreadPool(jobExecutorThreadPoolName);

    performRuntimeThreadPool(context, model, jobExecutorThreadPoolName, jobExecutorThreadPoolServiceName, verificationHandler, newControllers);

    MscExecutorService service = new MscExecutorService();
    ServiceController<MscExecutorService> serviceController = context.getServiceTarget().addService(ServiceNames.forMscExecutorService(), service)
        .addDependency(jobExecutorThreadPoolServiceName, ManagedQueueExecutorService.class, service.getManagedQueueInjector())
        .addListener(verificationHandler)
        .setInitialMode(Mode.ACTIVE)
        .install();

    newControllers.add(serviceController);

  }

  protected void performRuntimeThreadPool(OperationContext context, ModelNode model, String name, ServiceName jobExecutorThreadPoolServiceName,
      ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
      throws OperationFailedException {

    ServiceTarget serviceTarget = context.getServiceTarget();

    ThreadFactoryService threadFactory = new ThreadFactoryService();
    threadFactory.setThreadGroupName(THREAD_POOL_GRP_NAME + name);

    ServiceName threadFactoryServiceName = ServiceNames.forThreadFactoryService(name);

    ServiceBuilder<ThreadFactory> factoryBuilder = serviceTarget.addService(threadFactoryServiceName, threadFactory);
    if (verificationHandler != null) {
      factoryBuilder.addListener(verificationHandler);
    }
    if (newControllers != null) {
      newControllers.add(factoryBuilder.install());
    } else {
      factoryBuilder.install();
    }

    final BoundedQueueThreadPoolService threadPoolService = new BoundedQueueThreadPoolService(
        SubsystemAttributeDefinitons.CORE_THREADS.resolveModelAttribute(context, model).asInt(),
        SubsystemAttributeDefinitons.MAX_THREADS.resolveModelAttribute(context, model).asInt(),
        SubsystemAttributeDefinitons.QUEUE_LENGTH.resolveModelAttribute(context, model).asInt(),
        false,
        new TimeSpec(TimeUnit.SECONDS, SubsystemAttributeDefinitons.KEEPALIVE_TIME.resolveModelAttribute(context,model).asInt()),
        SubsystemAttributeDefinitons.ALLOW_CORE_TIMEOUT.resolveModelAttribute(context, model).asBoolean()
    );

    ServiceBuilder<ManagedQueueExecutorService> builder = serviceTarget.addService(jobExecutorThreadPoolServiceName, threadPoolService)
        .addDependency(threadFactoryServiceName, ThreadFactory.class, threadPoolService.getThreadFactoryInjector())
        .setInitialMode(ServiceController.Mode.ACTIVE);
    if (verificationHandler != null) {
      builder.addListener(verificationHandler);
    }
    if (newControllers != null) {
      newControllers.add(builder.install());
    } else {
      builder.install();
    }
  }

}
