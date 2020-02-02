package io.github.graphqly.reflector.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.graphqly.reflector.data.Shared.map;

public class SharedTest {
  static void testMap() {

    {
      String ORG_NAME = "Facebook";
      Organization organization = Organization.of(ORG_NAME);
      organization.newEmployee(Employee.of("Andy", "Le"));
      Map org = map(organization);
      assert org.get("name").equals(ORG_NAME);
    }

    {
      Map empty = map(3);
      assert empty.size() == 0;
    }
  }

  public static void main(String[] args) {}

  public static class Employee {
    public String first = "Andy", last = "Le";

    public static Employee of(String first, String last) {
      Employee employee = new Employee();
      employee.first = first;
      employee.last = last;
      return employee;
    }
  }

  static class Organization {
    public String name;
    public List<Employee> employees = new ArrayList<>();

    public static Organization of(String name) {
      Organization org = new Organization();
      org.name = name;
      return org;
    }

    public Organization newEmployee(Employee employee) {
      this.employees.add(employee);
      return this;
    }
  }
}
