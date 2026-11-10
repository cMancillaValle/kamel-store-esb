package com.kamelstore.inventario;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:inventorytest;DB_CLOSE_DELAY=-1")
class InventoryControllerTest {
 @Autowired InventoryController inventory;
 @Test void reservationIsIdempotentAndStockNeverNegative(){
  int before=inventory.product(1L).getBody() instanceof Product p?p.stock():-1;
  var first=inventory.reserve(new ReserveRequest("inventory-test-1",1,2));
  var duplicate=inventory.reserve(new ReserveRequest("inventory-test-1",1,2));
  assertEquals(200,first.getStatusCode().value());
  assertTrue(duplicate.getBody().duplicate());
  assertEquals(before-2,((Product)inventory.product(1L).getBody()).stock());
  assertEquals(409,inventory.reserve(new ReserveRequest("inventory-test-2",1,1000)).getStatusCode().value());
  assertEquals(before-2,((Product)inventory.product(1L).getBody()).stock());
 }
}
