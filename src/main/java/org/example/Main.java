package org.example;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import org.bson.Document;
import org.bson.conversions.Bson;

public class Main {

  private static final Charset CHARSET = StandardCharsets.UTF_8;

  static String uri = "Removed The URL for now";

  static MongoClient mongoClient = MongoClients.create(uri);
  static MongoDatabase db = mongoClient.getDatabase("list");
  static MongoCollection<Document> collection = db.getCollection("channels");

  public static void main(String[] args) throws IOException {
    HttpServer app = HttpServer.create(
      new InetSocketAddress("localhost", 3001),
      0
    );

    app.createContext("/", new BaseHandler());
    app.createContext("/json", new JsonHandler());
    app.createContext("/mongo", new MongoHandler());
    app.setExecutor(Executors.newSingleThreadExecutor());
    app.start();

    System.out.println("HTTP server started!");
  }

  public static class BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if ("GET".equals(exchange.getRequestMethod())) {
        OutputStream os = exchange.getResponseBody();

        String response = "Hello waht sup!";

        exchange.sendResponseHeaders(200, response.length());

        os.write(response.getBytes());
        os.flush();
        os.close();
      }
    }
  }

  public static class JsonHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      Headers headers = exchange.getResponseHeaders();

      if ("GET".equals(exchange.getRequestMethod())) {
        OutputStream os = exchange.getResponseBody();

        String query = exchange.getRequestURI().getRawQuery();

        Map<String, String> params = queryToMap(query == null ? "" : query);

        String name = params.get("name");

        if (name == null || name.length() == 0) name = "Shisui";

        String response =
          "{ \"id\": 699, \"name\": \"" +
          name +
          "\", \"hobby\": [\"programming\", \"earning\"] }";
        headers.set(
          "Content-Type",
          String.format("application/json; charset=%s", CHARSET)
        );
        byte[] rawResponseBody = response.getBytes(CHARSET);

        exchange.sendResponseHeaders(200, response.length());

        os.write(rawResponseBody);
        os.flush();
        os.close();
      }
    }
  }

  public static class MongoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      Headers headers = exchange.getResponseHeaders();

      if ("GET".equals(exchange.getRequestMethod())) {
        OutputStream os = exchange.getResponseBody();

        String query = exchange.getRequestURI().getRawQuery();

        Map<String, String> params = queryToMap(query == null ? "" : query);

        String name = params.get("name");

        if (name == null || name.length() == 0) name = "Shisui";

        Document doc = collection.find(eq("name", name)).first();

        String response = null;
        byte[] rawResponseBody;

        if (doc != null) {
          response = doc.toJson().toString();

          headers.set(
            "Content-Type",
            String.format("application/json; charset=%s", CHARSET)
          );

          exchange.sendResponseHeaders(200, response.length());
        } else {
          response = "Document Not Found!";

          exchange.sendResponseHeaders(200, response.length());
        }

        rawResponseBody = response.getBytes(CHARSET);

        os.write(rawResponseBody);
        os.flush();
        os.close();
      } else if ("POST".equals(exchange.getRequestMethod())) {
        OutputStream os = exchange.getResponseBody();

        String query = exchange.getRequestURI().getRawQuery();

        Map<String, String> params = queryToMap(query == null ? "" : query);

        String name = params.get("name");
        String rawAge = params.get("age");
        int age = Integer.parseInt(rawAge == null ? "0" : rawAge);
        boolean skip = false;

        String response = null;
        byte[] rawResponseBody;

        if (name == null || name.length() == 0) {
          response = "No name was provided";
          skip = true;
        }

        if (rawAge == null || rawAge.length() == 0) {
          response = "No age was provided";
          skip = true;
        }

        if (!skip) {
          Document doc = collection.find(eq("name", name)).first();

          Document rawDoc = new Document()
            .append("name", name)
            .append("age", age);

          if (doc != null) {
            Bson updates = Updates.combine(Updates.set("age", age));

            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
              .returnDocument(ReturnDocument.AFTER);

            Document newDoc = collection.findOneAndUpdate(
              eq("name", name),
              updates,
              options
            );

            response = newDoc.toJson().toString();
          } else {
            collection.insertOne(rawDoc);
            response = rawDoc.toJson().toString();
          }

          headers.set(
            "Content-Type",
            String.format("application/json; charset=%s", CHARSET)
          );
        }

        exchange.sendResponseHeaders(200, response.length());

        rawResponseBody = response.getBytes(CHARSET);

        os.write(rawResponseBody);
        os.flush();
        os.close();
      } else if ("DELETE".equals(exchange.getRequestMethod())) {
        OutputStream os = exchange.getResponseBody();

        String query = exchange.getRequestURI().getRawQuery();

        Map<String, String> params = queryToMap(query == null ? "" : query);

        String name = params.get("name");
        boolean skip = false;

        String response = null;
        byte[] rawResponseBody;

        if (name == null || name.length() == 0) {
          response = "No name was provided";
          skip = true;
        }

        if (!skip) {
          Document doc = collection.find(eq("name", name)).first();

          if (doc == null) {
            response = "Document Not Found";
          } else {
            response = "Document Deleted";

            collection.deleteOne(eq("name", name));
          }
        }

        exchange.sendResponseHeaders(200, response.length());

        rawResponseBody = response.getBytes(CHARSET);

        os.write(rawResponseBody);
        os.flush();
        os.close();
      }
    }
  }

  public static Map<String, String> queryToMap(String query) {
    if (query == null) {
      return null;
    }
    Map<String, String> result = new HashMap<>();
    for (String param : query.split("&")) {
      String[] entry = param.split("=");
      if (entry.length > 1) {
        result.put(entry[0], entry[1]);
      } else {
        result.put(entry[0], "");
      }
    }
    return result;
  }
}
