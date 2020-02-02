package io.github.graphqly.reflector.data;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.graphqly.reflector.data.JsonUtils.*;
import static io.github.graphqly.reflector.data.Shared.json;
import static io.github.graphqly.reflector.data.Shared.key;

public class JsonUtilsTest {
  static void testJson() {
    Person person = new Person();

    System.out.printf(">> First: %s\n", jsonize(person).toString());

    JsonObject update = new JsonObject();
    Integer newAge = new Integer(28);
    update.addProperty("age", newAge);

    Person clone = JsonUtils.createObject(Person.class, jsonize(person, update));

    assert clone.age == newAge; // passed
    System.out.printf(">> First: %s\n", jsonize(clone).toString());
  }

  static void testMore() {
    Integer value = 1987;
    Person person = new Person();
    Map object = objectToMap(new Family());
    System.out.println(jsonize(object).toString());
    System.out.println(objectToMap(value));
  }

  static void testUnwrap() {
    String NAME = "andy";
    JsonObject json = json(key("name").value(NAME));
    Object unwrapped = unwrapJsonObject(json);
    assert unwrapped instanceof Map;

    Map unwrappedMap = (Map) unwrapped;
    assert unwrappedMap.get("name").equals(NAME);
  }

  public static void main(String[] args) throws JsonUtils.JsonConflictException {
    testUnwrap();
    //    testJson();
    //    testMore();
  }

  static class Person {
    public String first = "Andy";
    public String last = "Le";

    public Integer age = 30;

    static Person of(String first, String last) {
      Person person = new Person();
      person.first = first;
      person.last = last;
      return person;
    }
  }

  static class Family {
    public List<Person> personList = new ArrayList<>();

    {
      personList.add(Person.of("Joan", "Beth"));
      personList.add(Person.of("Saigon", "Vietnam"));
    }
  }
}
