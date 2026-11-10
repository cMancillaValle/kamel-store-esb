package com.kamelstore.facturacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
record InvoiceRequest(String orderId,String productName,int quantity,BigDecimal total){}
record Invoice(String id,String orderId,String productName,int quantity,BigDecimal total,LocalDateTime createdAt){}
@RestController @RequestMapping("/api/invoices") public class InvoiceController {
 private final JdbcTemplate db; InvoiceController(JdbcTemplate db){this.db=db;}
 private Invoice map(java.sql.ResultSet r,int n)throws java.sql.SQLException{return new Invoice(r.getString("id"),r.getString("order_id"),r.getString("product_name"),r.getInt("quantity"),r.getBigDecimal("total"),r.getTimestamp("created_at").toLocalDateTime());}
 @GetMapping public List<Invoice> list(){return db.query("SELECT * FROM invoices ORDER BY created_at DESC",this::map);}
 @GetMapping("/order/{orderId}") public ResponseEntity<?> byOrder(@PathVariable String orderId){var rows=db.query("SELECT * FROM invoices WHERE order_id=?",this::map,orderId);return rows.isEmpty()?ResponseEntity.notFound().build():ResponseEntity.ok(rows.get(0));}
 @PostMapping @Transactional public ResponseEntity<?> create(@RequestBody InvoiceRequest request){
  if(request.orderId()==null||request.orderId().isBlank()||request.productName()==null||request.quantity()<=0||request.total()==null||request.total().signum()<0)return ResponseEntity.badRequest().body(Map.of("message","Datos inválidos"));
  var old=db.query("SELECT * FROM invoices WHERE order_id=?",this::map,request.orderId());if(!old.isEmpty())return ResponseEntity.ok(old.get(0));
  String id="FAC-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();db.update("INSERT INTO invoices(id,order_id,product_name,quantity,total) VALUES(?,?,?,?,?)",id,request.orderId(),request.productName(),request.quantity(),request.total());
  return ResponseEntity.status(201).body(db.query("SELECT * FROM invoices WHERE order_id=?",this::map,request.orderId()).get(0));
 }
}
