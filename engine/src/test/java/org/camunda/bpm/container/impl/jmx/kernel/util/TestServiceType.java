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
package org.camunda.bpm.container.impl.jmx.kernel.util;

import org.camunda.bpm.container.impl.spi.PlatformServiceContainer.ServiceType;

public enum TestServiceType implements ServiceType {

    TYPE1("test.type1"),
    TYPE2("test.type2");

    protected String serviceRealm;

    private TestServiceType(String serviceRealm) {
      this.serviceRealm = serviceRealm;
    }

    public String getTypeName() {
      return serviceRealm;
    }

}
