/*
 * Copyright 2011-2018 the original author or authors.
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

import java.io.IOException;

import org.appng.api.FieldProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.testsupport.TestBase;
import org.appng.testsupport.validation.WritingXmlValidator;

// tag::all[]
@org.springframework.test.context.ContextConfiguration(locations = {
        TestBase.TESTCONTEXT_JPA }, initializers = EmployeesTest.class)
public class EmployeesTest extends TestBase {

    static {
        WritingXmlValidator.writeXml = false;
    }

    // tag::contructor[]
    public EmployeesTest() {
        super("myapp", "application-home");
        setEntityPackage("com.myapp.domain");
        setRepositoryBase("com.myapp.repository");
    }
    // end::contructor[]

    // tag::datasource[]
    @org.junit.Test
    public void testShowEmployees() throws ProcessingException, IOException {
        addParameter("selectedId", "1"); // <!--1-->
        initParameters(); // <!--2-->
        DataSourceCall dataSourceCall = getDataSource("employees");// <!--3-->
        CallableDataSource callableDataSource = dataSourceCall.getCallableDataSource(); // <!--4-->
        callableDataSource.perform("aPage"); // <!--5-->
        validate(callableDataSource.getDatasource());// <!--6-->
    }
    // end::datasource[]

    // tag::action[]
    @org.junit.Test
    public void testCreateEmployee() throws ProcessingException, IOException {
        ActionCall action = getAction("employeeEvent", "create");// <!--1-->
        action.withParam(FORM_ACTION, "create");// <!--2-->
        Employee formBean = new Employee("John", "Doe");// <!--3-->
        CallableAction callableAction = action.getCallableAction(formBean);// <!--4-->
        FieldProcessor fieldProcessor = callableAction.perform();// <!--5-->
        validate(fieldProcessor.getMessages(), "-messages");// <!--6-->
        validate(callableAction.getAction(), "-action");// <!--7-->
    }
    // end::action[]

}
// end::all[]
