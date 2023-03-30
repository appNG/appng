/*
 * Copyright 2011-2023 the original author or authors.
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
package org.example;

import java.util.Arrays;

import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.springframework.stereotype.Service;

@Service
public class Employees implements DataProvider, ActionProvider<Employee> {

    public DataContainer getData(Site site, Application application, Environment environment, Options options,
            Request request, FieldProcessor fieldProcessor) {
        DataContainer dataContainer = new DataContainer(fieldProcessor);
        Employee employee = new Employee("John", "Doe");
        if ("single".equals(options.getString("mode", "value"))) {
            dataContainer.setItem(employee);
        } else {
            dataContainer.setItems(Arrays.asList(employee));
        }
        return dataContainer;
    }

    public void perform(Site site, Application application, Environment environment, Options options, Request request,
            Employee formBean, FieldProcessor fieldProcessor) {
        fieldProcessor.addOkMessage("Employee created!");
    }

}
