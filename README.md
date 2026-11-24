# cloudsumativa2

Sistema de reservas de eventos desarrollado con Spring Boot que permite registrar usuarios y eventos, reservar cupos y generar tickets en formato TXT. Incluye consulta, modificación y eliminación de tickets, además de estadísticas por evento. Utiliza H2 para los datos, almacenamiento local para los archivos y S3 como opción de almacenamiento y descarga.

Requiere Java 21. Ejecutar desde esta carpeta.

**H2 (por defecto)**

```sh
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`. Las tablas se crean automáticamente. La base de datos se guarda en `./data/cloudsumativa2.mv.db` y los tickets en `./storage/tickets`.

**Flujo básico de reserva**

1. Crear un usuario con `POST /usuarios` enviando `nombre` y `correo`.
2. Crear un evento con `POST /eventos` enviando `nombre`, `fecha` (por ejemplo, `2030-01-01T18:00:00`), `precio` y `cuposDisponibles`.
3. Reservar un cupo con `POST /eventos/{eventoId}/tickets` enviando `{"usuarioId": 1}`. Usar los identificadores devueltos en los pasos anteriores.
4. Descargar el archivo TXT con `GET /tickets/{id}/archivo`, usando el identificador del ticket reservado.

Los tickets se pueden listar con `GET /usuarios/{id}/tickets` o `GET /eventos/{id}/tickets`. Para modificar la observación, usar `PATCH /tickets/{id}` con `{"observacion": "Texto"}`. Al eliminar un ticket con `DELETE /tickets/{id}`, se libera el cupo. Las estadísticas se consultan en `GET /eventos/{id}/estadisticas`.

**S3 (opcional)**

S3 está deshabilitado por defecto. Para activarlo con un bucket existente y credenciales temporales de AWS Academy, ejecutar:

```sh
S3_ENABLED=true \
AWS_REGION=us-east-1 \
S3_BUCKET='nombre-del-bucket' \
AWS_ACCESS_KEY_ID='tu-access-key' \
AWS_SECRET_ACCESS_KEY='tu-secret-key' \
AWS_SESSION_TOKEN='tu-session-token' \
./mvnw spring-boot:run
```

Con S3 habilitado, la reserva sube el ticket automáticamente. También se puede subir con `POST /tickets/{id}/s3` y descargar con `GET /tickets/{id}/s3`.

**Prueba de arranque**

```sh
./mvnw test 
```

