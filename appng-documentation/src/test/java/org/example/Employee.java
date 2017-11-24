package org.example;

public class Employee {

    private String name;
    private String lastname;

    public Employee() {

    }

    public Employee(String name, String lastname) {
        super();
        this.name = name;
        this.lastname = lastname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

}
