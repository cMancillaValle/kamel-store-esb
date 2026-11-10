package com.kamelstore.facturacion;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:billingtest;DB_CLOSE_DELAY=-1")
class InvoiceControllerTest {
 @Autowired InvoiceController billing;
 @Test void repeatedOrderKeepsOneInvoice(){
  var input=new InvoiceRequest("invoice-test-1","Bolso Ruta",2,new BigDecimal("178000"));
  assertEquals(201,billing.create(input).getStatusCode().value());
  assertEquals(200,billing.create(input).getStatusCode().value());
  assertEquals(1,billing.list().size());
 }
}
