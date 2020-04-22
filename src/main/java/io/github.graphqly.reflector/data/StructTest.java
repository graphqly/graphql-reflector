package io.github.graphqly.reflector.data;

import com.google.gson.JsonObject;

import static io.github.graphqly.reflector.data.Shared.*;

public class StructTest {
  static void testMap() {
    String APP_NAME = "std";
    assert map()
        .key("name")
        .value(APP_NAME)
        .key("version")
        .value("0.1.0")
        .mapTo(Application.class)
        .name
        .equals(APP_NAME);

    assert map(key("name").value(APP_NAME), key("version").value("0.1.0"))
        .mapTo(Application.class)
        .name
        .equals(APP_NAME);

    JsonObject data =
        json(
            key("name").value(APP_NAME),
            key("version").value("0.1.0"),
            key("maintainer").value("Andy Le, anhld2@vng.com.vn"));
    assert data.getAsJsonPrimitive("name").getAsString().equals(APP_NAME);

    Race human = struct(Race.class, key("longevity").value(90).asJson());
    assert human.longevity == 90;
    System.out.println(json(human).toString());

    Application app = Application.newInstance(APP_NAME, "0.1.0");
    System.out.println(map(app).asJson().toString());
  }

  public static void main(String[] args) {
    testMap();
  }

  static class Race {
    public String name = "human";
    public int longevity = 70;
  }

  static class Application {
    public String name;
    public String version;

    public static Application newInstance(String name, String version) {
      Application app = new Application();
      app.name = name;
      app.version = version;
      return app;
    }
  }
}
