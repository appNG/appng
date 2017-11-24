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
