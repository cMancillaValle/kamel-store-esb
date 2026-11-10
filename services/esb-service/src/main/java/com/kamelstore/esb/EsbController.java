package com.kamelstore.esb;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/esb") public class EsbController {
 private final ProducerTemplate camel; EsbController(ProducerTemplate camel){this.camel=camel;}
 @GetMapping("/products") public Object products(){return camel.requestBody("direct:products",null,String.class);}
 @PostMapping("/orders") public Object order(@RequestBody String body){return camel.requestBody("direct:processOrder",body,Object.class);}
}
