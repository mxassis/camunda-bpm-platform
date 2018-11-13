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
package org.camunda.bpm.engine.impl.cmd;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import java.util.Map;

import org.camunda.bpm.engine.BadUserRequestException;
import org.camunda.bpm.engine.impl.cfg.CommandChecker;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;


/**
 * @author Tom Baeyens
 */
public class SignalCmd implements Command<Object>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String executionId;
  protected String signalName;
  protected Object signalData;
  protected final Map<String, Object> processVariables;

  public SignalCmd(String executionId, String signalName, Object signalData, Map<String, Object> processVariables) {
    this.executionId = executionId;
    this.signalName = signalName;
    this.signalData = signalData;
    this.processVariables = processVariables;
  }

  public Object execute(CommandContext commandContext) {
    ensureNotNull(BadUserRequestException.class, "executionId is null", "executionId", executionId);

    ExecutionEntity execution = commandContext
          .getExecutionManager()
          .findExecutionById(executionId);
    ensureNotNull(BadUserRequestException.class, "execution "+executionId+" doesn't exist", "execution", execution);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateProcessInstance(execution);
    }

    if(processVariables != null) {
      execution.setVariables(processVariables);
    }

    execution.signal(signalName, signalData);
    return null;
  }

}
