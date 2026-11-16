# Kamel Store ESB con Docker Compose

## Requisitos previos

- Docker Desktop en Windows, o Docker Engine en Linux/macOS.
- Docker Compose v2 (`docker compose version`).
- Aproximadamente 4 GB de memoria disponibles para Docker durante la primera compilación y demostración.

No se requiere instalar Java, Maven, Node.js ni npm en el equipo que ejecuta los contenedores.

## Primera ejecución

Desde la raíz del repositorio:

```powershell
docker compose up -d --build
```

La primera construcción descarga las imágenes base y dependencias Maven/npm. Después abre la aplicación en <http://localhost:5173>.

## Servicios y registros

```powershell
docker compose ps
docker compose logs -f
docker compose logs -f ventas esb
```

Puertos publicados para la demostración:

| Componente | URL |
|---|---|
| Frontend | <http://localhost:5173> |
| Ventas | <http://localhost:8081> |
| ESB | <http://localhost:8082> |
| Inventario | <http://localhost:8083> |
| Facturación | <http://localhost:8084> |

El navegador usa `/api` y Nginx lo envía internamente a ventas. Las aplicaciones backend usan los nombres DNS internos `ventas`, `esb`, `inventario` y `facturacion`; no usan `localhost` entre contenedores.

## Detener y reiniciar

```powershell
docker compose down
docker compose up -d
```

No use `docker compose down -v` salvo que quiera eliminar deliberadamente los datos de demostración.

## Persistencia

Compose crea tres volúmenes separados:

- `kamel-store_ventas-data`
- `kamel-store_inventario-data`
- `kamel-store_facturacion-data`

Cada H2 escribe bajo `/data` en su contenedor. `docker compose down` conserva estos volúmenes; reiniciar o reconstruir los contenedores no borra pedidos, inventario ni facturas.

Para revisar los volúmenes:

```powershell
docker volume ls
```

## Demostración sin internet

Prepare las imágenes en un equipo con internet:

```powershell
docker compose build
docker save -o kamel-store-images.tar kamel-store-frontend:latest kamel-store-ventas:latest kamel-store-esb:latest kamel-store-inventario:latest kamel-store-facturacion:latest
```

En el equipo de demostración, copie el repositorio completo y el archivo `.tar`. Con Docker operativo, ejecute:

```powershell
docker load -i .\kamel-store-images.tar
docker compose up -d
```

La transferencia de imágenes no reemplaza `compose.yaml`, los Dockerfiles ni la configuración del repositorio. El primer equipo también debe transferir las imágenes base incluidas por `docker save` cuando sean necesarias. Si se modifica una imagen o se ejecuta `--build`, Docker puede requerir acceso a internet otra vez.

## Solución de problemas

### Docker Desktop no iniciado

Abra Docker Desktop y espere a que indique que el motor está ejecutándose. Compruebe con:

```powershell
docker info
```

### Puertos ocupados

Detenga la ejecución local de Kamel Store antes de iniciar Compose, o identifique el proceso que usa 5173, 8081, 8082, 8083 o 8084:

```powershell
Get-NetTCPConnection -State Listen | Where-Object LocalPort -in 5173,8081,8082,8083,8084
```

### Los servicios Java todavía arrancan

Espere y compruebe el estado:

```powershell
docker compose ps
docker compose logs -f inventario facturacion esb ventas
```

Compose espera los healthchecks antes de iniciar los servicios dependientes, pero Spring Boot puede tardar algunos segundos la primera vez.

### Fallos de comunicación entre contenedores

Revise los logs de ESB y ventas. No configure URLs backend con `localhost`: use los nombres definidos por Compose.

### Problemas con volúmenes

Verifique que Docker conserve los volúmenes con `docker volume ls`. No ejecute `docker compose down -v` si necesita conservar los pedidos de la demostración.

### Error al construir imágenes

Revise conectividad, espacio libre y los logs completos:

```powershell
docker compose build --no-cache
```

Evite usar `--no-cache` durante una demostración sin internet.
