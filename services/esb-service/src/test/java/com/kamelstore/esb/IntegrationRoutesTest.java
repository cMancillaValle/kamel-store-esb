package com.kamelstore.esb;

import static org.junit.jupiter.api.Assertions.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class IntegrationRoutesTest {
 static final AtomicReference<String> reservation=new AtomicReference<>();
 static final AtomicReference<String> invoice=new AtomicReference<>();
 static final HttpServer server;
 static {
  try {
   server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
   server.createContext("/api/inventory/products/1",x->reply(x,200,"{\"id\":1,\"name\":\"Bolso Ruta\",\"price\":89000,\"stock\":12}"));
   server.createContext("/api/inventory/reservations",x->{reservation.set(new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));reply(x,200,"{\"orderId\":\"route-test-1\",\"reserved\":true,\"duplicate\":false}");});
   server.createContext("/api/invoices",x->{invoice.set(new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));reply(x,201,"{\"id\":\"FAC-TEST\"}");});
   server.start();
  } catch(Exception e){throw new ExceptionInInitializerError(e);}
 }
 static void reply(com.sun.net.httpserver.HttpExchange x,int status,String body)throws java.io.IOException {byte[] bytes=body.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(status,bytes.length);try(var out=x.getResponseBody()){out.write(bytes);}}
 @DynamicPropertySource static void urls(DynamicPropertyRegistry registry){String base="http://127.0.0.1:"+server.getAddress().getPort();registry.add("app.inventario-url",()->base);registry.add("app.facturacion-url",()->base);}
 @Autowired ProducerTemplate camel;
 @Autowired ObjectMapper json;
 @Test void transformsAndRoutesRealHttpMessages() throws Exception {
  Object result=camel.requestBody("direct:processOrder","{\"orderId\":\"route-test-1\",\"productId\":1,\"quantity\":2}");
  var reserved=json.readTree(reservation.get());var billed=json.readTree(invoice.get());var response=json.valueToTree(result);
  assertEquals("route-test-1",reserved.path("orderId").asText());
  assertEquals(2,reserved.path("quantity").asInt());
  assertEquals(178000,billed.path("total").asInt());
  assertEquals("PROCESADO",response.path("status").asText());
  assertEquals("FAC-TEST",response.path("invoiceId").asText());
 }
}
