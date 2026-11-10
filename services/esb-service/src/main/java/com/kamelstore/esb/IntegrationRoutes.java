package com.kamelstore.esb;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.util.*;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component public class IntegrationRoutes extends RouteBuilder {
 private final ObjectMapper json; private final String inventory,billing;
 IntegrationRoutes(ObjectMapper json,@Value("${app.inventario-url}") String inventory,@Value("${app.facturacion-url}") String billing){this.json=json;this.inventory=inventory;this.billing=billing;}
 @Override public void configure(){
  onException(IllegalArgumentException.class).handled(true).log("[${exchangeProperty.orderId}] Pedido inválido: ${exception.message}").process(e->e.getMessage().setBody(Map.of("status","DATOS_INVALIDOS","message","Pedido inválido")));
  onException(Exception.class).handled(true).log("[${exchangeProperty.orderId}] Error de integración: ${exception.message}").process(e->{String id=e.getProperty("orderId",String.class);String status=Boolean.TRUE.equals(e.getProperty("reserved",Boolean.class))?"PENDIENTE_FACTURACION":"ERROR_COMUNICACION";Map<String,Object> result=new LinkedHashMap<>();result.put("orderId",id==null?"":id);result.put("status",status);result.put("productName",e.getProperty("productName"));result.put("total",e.getProperty("total"));result.put("message",status.equals("PENDIENTE_FACTURACION")?"Inventario descontado; facturación pendiente":"Servicio temporalmente no disponible");e.getMessage().setBody(result);});
  from("direct:products").routeId("catalogo-inventario").setHeader(Exchange.HTTP_METHOD,constant("GET")).toD(inventory+"/api/inventory/products?bridgeEndpoint=true").convertBodyTo(String.class);
  from("direct:processOrder").routeId("procesar-pedido")
   .process(e->{JsonNode n=json.readTree(e.getIn().getBody(String.class));String id=n.path("orderId").asText();long product=n.path("productId").asLong();int quantity=n.path("quantity").asInt();if(id.isBlank()||product<=0||quantity<=0)throw new IllegalArgumentException("Pedido inválido");e.setProperty("orderId",id);e.setProperty("productId",product);e.setProperty("quantity",quantity);e.getIn().setHeader("X-Correlation-Id",id);})
   .log("[${exchangeProperty.orderId}] Consultando producto")
   .setHeader(Exchange.HTTP_METHOD,constant("GET")).toD(inventory+"/api/inventory/products/${exchangeProperty.productId}?bridgeEndpoint=true")
   .process(e->{JsonNode p=json.readTree(e.getIn().getBody(String.class));e.setProperty("productName",p.path("name").asText());e.setProperty("price",p.path("price").decimalValue());e.getIn().setBody(json.writeValueAsString(Map.of("orderId",e.getProperty("orderId"),"productId",e.getProperty("productId"),"quantity",e.getProperty("quantity"))));e.getIn().setHeader(Exchange.HTTP_METHOD,"POST");e.getIn().setHeader(Exchange.CONTENT_TYPE,"application/json");})
   .log("[${exchangeProperty.orderId}] Reservando inventario")
   .toD(inventory+"/api/inventory/reservations?bridgeEndpoint=true&throwExceptionOnFailure=false")
   .process(e->{JsonNode r=json.readTree(e.getIn().getBody(String.class));if(!r.path("reserved").asBoolean()){e.setProperty("rejected",true);e.getIn().setBody(Map.of("orderId",e.getProperty("orderId"),"status","RECHAZADO","message",r.path("message").asText()));}else{e.setProperty("reserved",true);BigDecimal total=((BigDecimal)e.getProperty("price")).multiply(BigDecimal.valueOf((int)e.getProperty("quantity")));e.setProperty("total",total);e.getIn().setBody(json.writeValueAsString(Map.of("orderId",e.getProperty("orderId"),"productName",e.getProperty("productName"),"quantity",e.getProperty("quantity"),"total",total)));e.getIn().setHeader(Exchange.HTTP_METHOD,"POST");e.getIn().setHeader(Exchange.CONTENT_TYPE,"application/json");}})
   .choice().when(exchangeProperty("rejected").isEqualTo(true)).log("[${exchangeProperty.orderId}] Pedido rechazado").otherwise()
    .log("[${exchangeProperty.orderId}] Generando factura")
    .toD(billing+"/api/invoices?bridgeEndpoint=true")
    .process(e->{JsonNode invoice=json.readTree(e.getIn().getBody(String.class));Map<String,Object> result=new LinkedHashMap<>();result.put("orderId",e.getProperty("orderId"));result.put("status","PROCESADO");result.put("productName",e.getProperty("productName"));result.put("total",e.getProperty("total"));result.put("invoiceId",invoice.path("id").asText());result.put("message","Pedido procesado correctamente");e.getIn().setBody(result);})
   .end();
 }
}


