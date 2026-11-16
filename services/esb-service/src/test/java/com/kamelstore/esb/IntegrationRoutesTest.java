package com.kamelstore.esb;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class IntegrationRoutesTest {
 static final ObjectMapper JSON=new ObjectMapper();
 static final AtomicReference<String> reservation=new AtomicReference<>();
 static final AtomicReference<String> invoice=new AtomicReference<>();
 static final Set<String> reserved=ConcurrentHashMap.newKeySet();
 static final Set<String> invoiced=ConcurrentHashMap.newKeySet();
 static final Set<String> failBillingOnce=ConcurrentHashMap.newKeySet();
 static final HttpServer server;
 static {
  try {
   server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
   server.createContext("/api/inventory/products/1",x->reply(x,200,"{\"id\":1,\"name\":\"Bolso Ruta\",\"price\":89000,\"stock\":12}"));
   server.createContext("/api/inventory/products/2",x->{if("timeout-route-1".equals(x.getRequestHeaders().getFirst("X-Correlation-Id"))){try{Thread.sleep(400);}catch(InterruptedException e){Thread.currentThread().interrupt();}}reply(x,200,"{\"id\":2,\"name\":\"Taza Oasis\",\"price\":38000,\"stock\":18}");});
   server.createContext("/api/inventory/reservations",x->{String body=new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);reservation.set(body);var request=JSON.readTree(body);String id=request.path("orderId").asText();if(request.path("quantity").asInt()==999){reply(x,409,"{\"orderId\":\""+id+"\",\"reserved\":false,\"duplicate\":false,\"message\":\"Inventario insuficiente\"}");return;}boolean duplicate=!reserved.add(id);reply(x,200,"{\"orderId\":\""+id+"\",\"reserved\":true,\"duplicate\":"+duplicate+",\"message\":\"Existencias descontadas\"}");});
   server.createContext("/api/invoices",x->{String body=new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);invoice.set(body);String id=JSON.readTree(body).path("orderId").asText();if(failBillingOnce.contains(id)){reply(x,503,"{\"message\":\"Facturación no disponible\"}");return;}invoiced.add(id);reply(x,201,"{\"id\":\"FAC-"+id+"\"}");});
   server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4,r->{Thread thread=new Thread(r,"esb-http-stub");thread.setDaemon(true);return thread;}));
   server.start();
  }catch(Exception e){throw new ExceptionInInitializerError(e);}
 }
 static void reply(com.sun.net.httpserver.HttpExchange x,int status,String body)throws java.io.IOException {byte[] bytes=body.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(status,bytes.length);try(var out=x.getResponseBody()){out.write(bytes);}catch(java.io.IOException ignored){}}
 @DynamicPropertySource static void urls(DynamicPropertyRegistry registry){String base="http://127.0.0.1:"+server.getAddress().getPort();registry.add("app.inventario-url",()->base);registry.add("app.facturacion-url",()->base);registry.add("camel.component.http.response-timeout",()->100);registry.add("camel.component.http.so-timeout",()->100);}

 @Autowired ProducerTemplate camel;
 @Autowired ObjectMapper json;
 com.fasterxml.jackson.databind.JsonNode process(String id,long product,int quantity){Object result=camel.requestBody("direct:processOrder","{\"orderId\":\""+id+"\",\"productId\":"+product+",\"quantity\":"+quantity+"}");return json.valueToTree(result);}

 @Test void transformsAndRoutesRealHttpMessages() throws Exception {var response=process("route-valid-1",1,2);var reservedBody=json.readTree(reservation.get());var billedBody=json.readTree(invoice.get());assertEquals("route-valid-1",reservedBody.path("orderId").asText());assertEquals(2,reservedBody.path("quantity").asInt());assertEquals(178000,billedBody.path("total").asInt());assertEquals("PROCESADO",response.path("status").asText());assertEquals("FAC-route-valid-1",response.path("invoiceId").asText());}
 @Test void rejectsInsufficientInventoryWithoutInvoice(){int before=invoiced.size();var response=process("route-rejected-1",1,999);assertEquals("RECHAZADO",response.path("status").asText());assertEquals(before,invoiced.size());}
 @Test void rejectsIncompleteAndMalformedMessages(){var incomplete=json.valueToTree(camel.requestBody("direct:processOrder","{\"orderId\":\"invalid-1\",\"productId\":1}"));var malformed=json.valueToTree(camel.requestBody("direct:processOrder","{not-json"));assertEquals("DATOS_INVALIDOS",incomplete.path("status").asText());assertEquals("DATOS_INVALIDOS",malformed.path("status").asText());}
 @Test void retriesBillingFailureWithoutDuplicatingEffects(){String id="route-pending-1";failBillingOnce.add(id);var first=process(id,1,1);failBillingOnce.remove(id);var second=process(id,1,1);assertEquals("PENDIENTE_FACTURACION",first.path("status").asText());assertEquals("PROCESADO",second.path("status").asText());assertEquals(1,reserved.stream().filter(id::equals).count());assertEquals(1,invoiced.stream().filter(id::equals).count());}
 @Test void repeatedOrderKeepsOneReservationAndInvoice(){String id="route-repeat-1";assertEquals("PROCESADO",process(id,1,1).path("status").asText());assertEquals("PROCESADO",process(id,1,1).path("status").asText());assertEquals(1,reserved.stream().filter(id::equals).count());assertEquals(1,invoiced.stream().filter(id::equals).count());}
 @Test void reportsHttpTimeoutAsRecoverableCommunicationError(){var response=process("timeout-route-1",2,1);assertEquals("ERROR_COMUNICACION",response.path("status").asText());assertTrue(response.path("message").asText().contains("Tiempo de espera"));}
}
