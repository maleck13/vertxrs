package org.deephacks.vertxrs;


import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.deephacks.vertxrs.TestResource.Data;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.Ignore;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JaxrsStressTest {
  static VertxRsServer vertxrs;
  static int connections = Runtime.getRuntime().availableProcessors() * 100;

  static {
    ResteasyDeployment deployment = new ResteasyDeployment();
    deployment.setApplication(new Application() {
      @Override
      public Set<Object> getSingletons() {
        HashSet<Object> singletons = new HashSet<>();
        singletons.add(new AsyncJaxrsStressResource());
        return singletons;
      }
    });
    Services services = Services.newBuilder()
            .withResteasy(deployment)
            .build();
    vertxrs = new VertxRsServer
            (Config.newBuilder().withHttpPort(8081).build(), services);
    vertxrs.start();
  }

  @Test
  public void stressTestAsync() throws Exception {
    Vertx vertx = vertxrs.getVertx();
    CountDownLatch latch = new CountDownLatch(connections);
    for (int j = 0; j < connections; j++) {
      HttpClient client = vertx.createHttpClient().setPort(8081).setHost("localhost");
      for (int i = 0; i < 100; i++) {
        String json = "{\"name\":\"name\", \"value\":\"value" + String.format("%07d", i) +"\"}";
        client.post("/stress/async", event -> {
          if (event.statusCode() != 200) {
            throw new RuntimeException(String.valueOf(event.statusCode()));
          }
          latch.countDown();
        }).putHeader("Content-Type", "application/json")
                .putHeader("Connection", "Keep-Alive")
                .putHeader("Content-Length", String.valueOf(json.length()))
                .write(json).end();
      }
    }
    latch.await(10, TimeUnit.SECONDS);
    assertThat(latch.getCount(), is(0L));
  }


  @Test
  public void stressTestSync() throws Exception {
    Vertx vertx = vertxrs.getVertx();
    CountDownLatch latch = new CountDownLatch(100 * connections);
    for (int j = 0; j < connections; j++) {
      HttpClient client = vertx.createHttpClient().setPort(8081).setHost("localhost");
      for (int i = 0; i < 100; i++) {
        String json = "{\"name\":\"name\", \"value\":\"value" + String.format("%07d", i) +"\"}";
        client.post("/stress/sync", event -> {
          if (event.statusCode() != 200) {
            throw new RuntimeException(String.valueOf(event.statusCode()));
          }
          latch.countDown();
        }).putHeader("Content-Type", "application/json")
          .putHeader("Connection", "Keep-Alive")
          .putHeader("Content-Length", String.valueOf(json.length()))
                .write(json).end();
      }
    }
    latch.await(10, TimeUnit.SECONDS);
    assertThat(latch.getCount(), is(0L));
  }

  @Path("/stress")
  public static class AsyncJaxrsStressResource {

    @POST @Path("async")
    @Consumes(MediaType.APPLICATION_JSON)
    public void send(Data data, @Suspended AsyncResponse response) {
      response.resume(data);
    }

    @POST @Path("sync")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Data sync(Data data) {
      return data;
    }
  }

  private void sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}