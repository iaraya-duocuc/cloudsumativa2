# cloudsumativa2

API Spring Boot de reservas de eventos. Genera tickets TXT, permite consultarlos, modificarlos y cancelarlos, y calcula estadísticas por evento.

## Arranque local

Requiere Java 21. Desde la raíz del proyecto:

```sh
ISSUER_URI='iss-del-jwt' \
AUDIENCE='aud-del-jwt' \
JWK_SET_URI='jwks_uri-del-flujo-b2c' \
./mvnw spring-boot:run
```

La API escucha en `http://localhost:8080`. Usa H2 en `./data/cloudsumativa2.mv.db` y guarda los TXT en `./storage/tickets`. Ejecuta las pruebas con `./mvnw test`.

## Autenticación y pruebas

Spring valida el JWT de Azure AD B2C y lee `extension_CONSULTA_ROLE_CLAIM`. `ADMIN` crea eventos, consulta estadísticas, lista usuarios, gestiona cualquier ticket y puede subir TXT manualmente a S3. `CLIENT` consulta eventos y gestiona su perfil y sus propios tickets. `POST /usuarios` vincula el perfil del cliente al emisor y sujeto del JWT. `GET /actuator/health` admite solicitudes sin token.

1. Importa la [colección Postman](postman/cloudsumativa2-autorizacion.postman_collection.json). Completa `adminToken` y `clientToken` con los JWT obtenidos manualmente, sin el prefijo `Bearer`, y `clientEmail` con un correo de prueba. Usa los valores locales de la colección.
2. Ejecuta la colección completa. Si usas **Send**, sigue los pasos **01–15** entre **Admin** y **Client**. Los pasos **01–04** guardan los IDs automáticamente. Con S3 desactivado, pasa del **08** al **11**.
3. En **Éxito** espera `2xx`. En **Rechazo** espera el `401` o `403` indicado en cada solicitud.

## Despliegue

El [workflow](.github/workflows/main.yml) prueba el código y la imagen con H2. Al hacer push a `main`, publica `ignarayaa/cloudsumativa2` con `latest` y el SHA del commit, y despliega por SSH en EC2. Verifica `GET /actuator/health` y el `401` de `GET /eventos` sin token. Si falla el contenedor nuevo, restaura el anterior cuando existe.

- **EC2.** Instala Docker y permite TCP `22` desde el runner de despliegue. Para pruebas directas, permite TCP `8080` desde tu IP.
- **RDS MySQL.** Usa la misma VPC que EC2. Permite TCP `3306` desde el Security Group de EC2. Crea la base `cloudsumativa2` y da al usuario de aplicación permisos de lectura, escritura y creación o actualización de tablas. Hibernate prepara las tablas al arrancar.
- **EFS.** Permite TCP `2049` en el destino de montaje desde el Security Group de EC2. Monta EFS en `/mnt/efs` antes del despliegue. Docker monta `/mnt/efs/cloudsumativa2/tickets` en `/app/storage/tickets`.
- **S3.** Es opcional y usa `us-east-1` por defecto. Al activarlo, la reserva sube el TXT al bucket. Las credenciales de AWS Academy necesitan permisos para leer, escribir y eliminar objetos. Activa `ejecutarS3=true` en Postman para probarlo.

Agrega estos secretos en **GitHub → Settings → Secrets and variables → Actions**:

| Uso | Secretos |
| --- | --- |
| Docker Hub | `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` con permiso de escritura. |
| SSH | `EC2_HOST` con IP o DNS público, `USER_SERVER` y `EC2_SSH_KEY` con la clave privada completa. |
| B2C | `ISSUER_URI`, `AUDIENCE`, `JWK_SET_URI`. La URL de claves se puede omitir si Spring la descubre desde el emisor. |
| RDS | `MYSQL_JDBC_URL` con `jdbc:mysql://ENDPOINT_RDS:3306/cloudsumativa2`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`. |
| S3 | `S3_ENABLED=true`, `S3_BUCKET` con el nombre del bucket, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`. |

Para activar S3 en local, pasa esas mismas variables al arrancar. Usa `AWS_REGION` si el bucket está fuera de `us-east-1`. Para desplegar en EC2 con S3 activo, actualiza los tres secretos de AWS cuando caduquen.

## API Gateway

Registra los **16 pares método y ruta de negocio**, incluidas las rutas con ID. Asocia la integración con EC2 y el autorizador JWT a cada ruta. Usa el mismo emisor y audiencia que en Spring y conserva el encabezado `Authorization`.

Después de desplegar el stage, configura `baseUrl` con la **Invoke URL**, sin barra final. Ejecuta la colección con ambos roles para verificar el acceso con JWT y los rechazos esperados..x
