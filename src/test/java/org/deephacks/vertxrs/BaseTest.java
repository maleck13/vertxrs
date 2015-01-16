package org.deephacks.vertxrs;

import com.squareup.okhttp.*;

import java.io.IOException;

public class BaseTest {
  static VertxRsServer stiletto;
  static MediaType APPLICATION_JSON = MediaType.parse("application/json");
  static OkHttpClient client;
  static Config config;
  static {
    if (stiletto == null) {
      client = new OkHttpClient();
      Services services = Services.newBuilder()
              .withResteasy(new Resteasy().getDeployment())
              .withSockJsService("test", new TestResource())
              .build();
      config = Config.defaultConfig();
      stiletto = new VertxRsServer(config, services);
      stiletto.start();
    }
  }

  static Response PUT(String path, String body) throws IOException {
    Request req = new Request.Builder()
            .put(RequestBody.create(APPLICATION_JSON, body))
            .url(config.getHttpHostPortUrl(path)).build();
    return client.newCall(req).execute();
  }

  static Response GET(String path) throws IOException {
    Request req = new Request.Builder().get().url(config.getHttpHostPortUrl(path)).build();
    return client.newCall(req).execute();
  }

  static Response POST(String path, String body) throws IOException {
    Request req = new Request.Builder()
            .post(RequestBody.create(APPLICATION_JSON, body))
            .url(config.getHttpHostPortUrl(path))
            .build();
    return client.newCall(req).execute();
  }
}