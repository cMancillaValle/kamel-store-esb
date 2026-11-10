# Integración ESB

```mermaid
sequenceDiagram
 participant U as React
 participant V as Ventas
 participant E as Camel ESB
 participant I as Inventario
 participant F as Facturación
 U->>V: POST /api/orders
 V->>E: POST /api/esb/orders
 E->>E: Validar y asignar correlación
 E->>I: GET /products/{id}
 I-->>E: Producto, precio
 E->>E: Transformar a ReserveRequest
 E->>I: POST /reservations
 I-->>E: Reserva o rechazo
 alt reserva exitosa
 E->>E: Transformar a InvoiceRequest
 E->>F: POST /invoices
 F-->>E: Factura simulada
 else inventario insuficiente
 E-->>V: RECHAZADO
 end
 E-->>V: Resultado
 V-->>U: Pedido y estado
```

`IntegrationRoutes` contiene las rutas Camel `direct:products` y `direct:processOrder`. Camel valida, transforma mensajes JSON, enruta por HTTP mediante `toD`, usa `choice` para el rechazo y `onException` para errores. Ventas no conoce las APIs de inventario ni facturación. El encabezado `X-Correlation-Id` y logs con `orderId` permiten seguir la operación.

La reserva se hace con un `UPDATE ... WHERE stock>=quantity` atómico. La tabla `reservations` impide duplicados por ID de pedido. Facturación también impide facturas duplicadas con `UNIQUE(order_id)`. Ante fallo de facturación, la reserva persiste y el resultado queda pendiente para un reintento. No se aplica compensación automática ni transacción distribuida.
