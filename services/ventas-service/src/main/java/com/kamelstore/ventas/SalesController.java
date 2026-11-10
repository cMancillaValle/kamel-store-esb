package com.kamelstore.ventas;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
record OrderRequest(Long productId,Integer quantity,String orderId){}
record OrderView(String id,long productId,int quantity,String status,String productName,BigDecimal total,String invoiceId,String message,LocalDateTime createdAt){}
@RestController @RequestMapping("/api") public class SalesController {
 private final JdbcTemplate db; private final ObjectMapper json; private final HttpClient http=HttpClient.newHttpClient(); private final String esb;
 SalesController(JdbcTemplate db,ObjectMapper json,@Value("${app.esb-url}") String esb){this.db=db;this.json=json;this.esb=esb;}
 private OrderView map(java.sql.ResultSet r,int n)throws java.sql.SQLException{return new OrderView(r.getString("id"),r.getLong("product_id"),r.getInt("quantity"),r.getString("status"),r.getString("product_name"),r.getBigDecimal("total"),r.getString("invoice_id"),r.getString("message"),r.getTimestamp("created_at").toLocalDateTime());}
 @GetMapping("/products") public ResponseEntity<?> products(){try{var r=http.send(HttpRequest.newBuilder(URI.create(esb+"/api/esb/products")).GET().build(),HttpResponse.BodyHandlers.ofString());return ResponseEntity.status(r.statusCode()).body(json.readTree(r.body()));}catch(Exception e){return ResponseEntity.status(503).body(Map.of("message","Catálogo temporalmente no disponible"));}}
 @GetMapping("/orders") public List<OrderView> orders(){return db.query("SELECT * FROM orders ORDER BY created_at DESC",this::map);}
 @GetMapping("/orders/{id}") public ResponseEntity<?> order(@PathVariable String id){var rows=db.query("SELECT * FROM orders WHERE id=?",this::map,id);return rows.isEmpty()?ResponseEntity.notFound().build():ResponseEntity.ok(rows.get(0));}
 @PostMapping("/orders") public ResponseEntity<?> create(@RequestBody OrderRequest request){
  if(request.productId()==null||request.productId()<=0||request.quantity()==null||request.quantity()<=0)return ResponseEntity.badRequest().body(Map.of("message","Producto y cantidad positiva son obligatorios"));
  String id=request.orderId()==null||request.orderId().isBlank()?UUID.randomUUID().toString():request.orderId();
  if(id.length()>80)return ResponseEntity.badRequest().body(Map.of("message","Identificador demasiado largo"));
  var previous=db.query("SELECT * FROM orders WHERE id=?",this::map,id);
  if(!previous.isEmpty()){OrderView old=previous.get(0);if(old.productId()!=request.productId()||old.quantity()!=request.quantity())return ResponseEntity.status(409).body(Map.of("message","Identificador reutilizado con otros datos"));if(!old.status().equals("PENDIENTE_FACTURACION")&&!old.status().equals("ERROR_COMUNICACION"))return ResponseEntity.ok(old);}
  else db.update("INSERT INTO orders(id,product_id,quantity,status,message) VALUES(?,?,?,'RECIBIDO','Pedido registrado')",id,request.productId(),request.quantity());
  try{
   String body=json.writeValueAsString(Map.of("orderId",id,"productId",request.productId(),"quantity",request.quantity()));
   var r=http.send(HttpRequest.newBuilder(URI.create(esb+"/api/esb/orders")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
   JsonNode result=json.readTree(r.body());String status=result.path("status").asText("ERROR_COMUNICACION");
   db.update("UPDATE orders SET status=?,product_name=?,total=?,invoice_id=?,message=? WHERE id=?",status,result.path("productName").asText(null),result.hasNonNull("total")?result.get("total").decimalValue():null,result.path("invoiceId").asText(null),result.path("message").asText(),id);
  }catch(Exception e){db.update("UPDATE orders SET status='ERROR_COMUNICACION',message='ESB temporalmente no disponible' WHERE id=?",id);}
  return ResponseEntity.ok(db.query("SELECT * FROM orders WHERE id=?",this::map,id).get(0));
 }
 @PostMapping("/orders/{id}/retry") public ResponseEntity<?> retry(@PathVariable String id){var rows=db.query("SELECT * FROM orders WHERE id=?",this::map,id);if(rows.isEmpty())return ResponseEntity.notFound().build();OrderView o=rows.get(0);if(!o.status().equals("PENDIENTE_FACTURACION")&&!o.status().equals("ERROR_COMUNICACION"))return ResponseEntity.status(409).body(Map.of("message","El pedido no admite reintento"));return create(new OrderRequest(o.productId(),o.quantity(),id));}
}

