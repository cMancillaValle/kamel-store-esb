package com.kamelstore.ventas;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class SalesControllerTest {
 static final ObjectMapper JSON=new ObjectMapper();
 static final HttpServer esb;
 static {
  try {
   esb=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
   esb.createContext("/api/esb/orders",exchange->{
    JsonNode request=JSON.readTree(exchange.getRequestBody());
    String id=request.path("orderId").asText();
    if(id.startsWith("timeout-")){try{Thread.sleep(400);}catch(InterruptedException e){Thread.currentThread().interrupt();}reply(exchange,200,result(id,"PROCESADO","FAC-LATE"));return;}
    if(id.startsWith("rejected-")){reply(exchange,200,"{\"orderId\":\""+id+"\",\"status\":\"RECHAZADO\",\"message\":\"Inventario insuficiente\"}");return;}
    if(id.startsWith("pending-")){reply(exchange,200,"{\"orderId\":\""+id+"\",\"status\":\"PENDIENTE_FACTURACION\",\"productName\":\"Bolso Ruta\",\"total\":89000,\"message\":\"Inventario descontado; facturación pendiente\"}");return;}
    reply(exchange,200,result(id,"PROCESADO","FAC-TEST"));
   });
   esb.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4,r->{Thread thread=new Thread(r,"sales-esb-stub");thread.setDaemon(true);return thread;}));
   esb.start();
  }catch(Exception e){throw new ExceptionInInitializerError(e);}
 }
 static String result(String id,String status,String invoice){return "{\"orderId\":\""+id+"\",\"status\":\""+status+"\",\"productName\":\"Bolso Ruta\",\"total\":89000,\"invoiceId\":\""+invoice+"\",\"message\":\"Pedido procesado correctamente\"}";}
 static void reply(com.sun.net.httpserver.HttpExchange exchange,int status,String body)throws java.io.IOException {byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(status,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}catch(java.io.IOException ignored){}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("app.esb-url",()->"http://127.0.0.1:"+esb.getAddress().getPort());registry.add("app.http-request-timeout-ms",()->100);registry.add("spring.datasource.url",()->"jdbc:h2:mem:salestest;DB_CLOSE_DELAY=-1");}

 @Autowired TestRestTemplate http;
 @Autowired JdbcTemplate db;

 JsonNode post(String id,long product,int quantity){ResponseEntity<JsonNode> response=http.postForEntity("/api/orders",Map.of("orderId",id,"productId",product,"quantity",quantity),JsonNode.class);assertEquals(200,response.getStatusCode().value());return response.getBody();}

 @Test void registersAValidOrder(){JsonNode order=post("valid-sales-1",1,1);assertEquals("PROCESADO",order.path("status").asText());assertEquals("FAC-TEST",order.path("invoiceId").asText());}
 @Test void rejectsIncompleteData(){ResponseEntity<JsonNode> response=http.postForEntity("/api/orders",Map.of("productId",1),JsonNode.class);assertEquals(400,response.getStatusCode().value());assertEquals("Producto y cantidad positiva son obligatorios",response.getBody().path("message").asText());}
 @Test void storesInventoryRejection(){JsonNode order=post("rejected-sales-1",1,999);assertEquals("RECHAZADO",order.path("status").asText());assertTrue(order.path("invoiceId").isNull()||order.path("invoiceId").isMissingNode());}
 @Test void storesRecoverableBillingFailure(){JsonNode order=post("pending-sales-1",1,1);assertEquals("PENDIENTE_FACTURACION",order.path("status").asText());}
 @Test void recoversAnExistingReceivedOrderWithOriginalId(){db.update("INSERT INTO orders(id,product_id,quantity,status,message) VALUES('received-sales-1',1,1,'RECIBIDO','Pedido registrado')");ResponseEntity<JsonNode> response=http.postForEntity("/api/orders/received-sales-1/retry",null,JsonNode.class);assertEquals(200,response.getStatusCode().value());assertEquals("received-sales-1",response.getBody().path("id").asText());assertEquals("PROCESADO",response.getBody().path("status").asText());}
 @Test void turnsHttpTimeoutIntoRecoverableState(){JsonNode order=post("timeout-sales-1",1,1);assertEquals("ERROR_COMUNICACION",order.path("status").asText());assertTrue(order.path("message").asText().contains("Tiempo de espera"));}
}
