package com.kamelstore.inventario;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

record Product(long id,String name,String description,BigDecimal price,int stock,String image) {}
record ReserveRequest(String orderId,long productId,int quantity) {}
record ReserveResult(String orderId,long productId,int quantity,boolean reserved,boolean duplicate,String message) {}
@RestController @RequestMapping("/api/inventory") public class InventoryController {
 private final JdbcTemplate db;
 InventoryController(JdbcTemplate db){this.db=db;}
 private Product map(java.sql.ResultSet r,int n)throws java.sql.SQLException{return new Product(r.getLong("id"),r.getString("name"),r.getString("description"),r.getBigDecimal("price"),r.getInt("stock"),r.getString("image"));}
 @GetMapping("/products") public List<Product> products(){return db.query("SELECT * FROM products ORDER BY id",this::map);}
 @GetMapping("/products/{id}") public ResponseEntity<?> product(@PathVariable long id){var p=db.query("SELECT * FROM products WHERE id=?",this::map,id);return p.isEmpty()?ResponseEntity.notFound().build():ResponseEntity.ok(p.get(0));}
 @PostMapping("/reservations") @Transactional public ResponseEntity<ReserveResult> reserve(@RequestBody ReserveRequest request){
  if(request.orderId()==null||request.orderId().isBlank()||request.quantity()<=0||request.productId()<=0)return ResponseEntity.badRequest().body(new ReserveResult(request.orderId(),request.productId(),request.quantity(),false,false,"Datos inválidos"));
  var old=db.query("SELECT product_id,quantity FROM reservations WHERE order_id=?",(r,n)->new long[]{r.getLong(1),r.getLong(2)},request.orderId());
  if(!old.isEmpty()){boolean same=old.get(0)[0]==request.productId()&&old.get(0)[1]==request.quantity();return ResponseEntity.status(same?200:409).body(new ReserveResult(request.orderId(),request.productId(),request.quantity(),same,true,same?"Reserva existente":"Pedido reutilizado con otros datos"));}
  int changed=db.update("UPDATE products SET stock=stock-? WHERE id=? AND stock>=?",request.quantity(),request.productId(),request.quantity());
  if(changed==0)return ResponseEntity.status(409).body(new ReserveResult(request.orderId(),request.productId(),request.quantity(),false,false,"Inventario insuficiente o producto inexistente"));
  db.update("INSERT INTO reservations(order_id,product_id,quantity) VALUES(?,?,?)",request.orderId(),request.productId(),request.quantity());
  return ResponseEntity.ok(new ReserveResult(request.orderId(),request.productId(),request.quantity(),true,false,"Existencias descontadas"));
 }
}
