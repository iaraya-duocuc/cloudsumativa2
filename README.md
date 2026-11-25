# cloudsumativa2

Sistema de reservas de eventos desarrollado con Spring Boot que permite registrar usuarios y eventos, reservar cupos y generar tickets en formato TXT. Incluye consulta, modificación y eliminación de tickets, además de estadísticas por evento. En local utiliza H2 y guarda los archivos en disco. En EC2 utiliza MySQL en RDS y EFS para los tickets, con S3 opcional. GitHub Actions construye y publica la imagen Docker y automatiza el despliegue.

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

<<<<<<< Updated upstream
=======
**Despliegue en EC2**

GitHub Actions ejecuta las pruebas y valida la imagen Docker con H2 en los pull requests hacia `main` y en los pushes a `main`. En un push a `main`, también publica en Docker Hub (`ignarayaa/cloudsumativa2`) con las etiquetas `latest` y el SHA completo del commit. Luego se conecta por SSH a EC2, despliega la imagen etiquetada con ese SHA y comprueba `GET /eventos`.

En EC2, el usuario SSH necesita poder ejecutar Docker. El Security Group debe permitir TCP `22` desde el runner que despliega. Para probar desde Postman, permite TCP `8080` desde tu IP pública y configura `baseUrl` como `http://IP_PUBLICA_EC2:8080` en la [colección Postman](postman/cloudsumativa2.postman_collection.json).

**MySQL en RDS**

El despliegue activa el perfil `mysql`. Configura RDS MySQL en la misma VPC que EC2 y permite TCP `3306` en el Security Group de RDS desde el Security Group de EC2. Antes del primer despliegue, crea la base con un usuario autorizado en MySQL.

```sql
CREATE DATABASE IF NOT EXISTS cloudsumativa2;
```

El usuario de la aplicación necesita permisos para leer y escribir datos y para crear o actualizar las tablas de esa base. Hibernate prepara las tablas al arrancar.

**EFS para los tickets**

Monta EFS en `/mnt/efs` en EC2 antes de desplegar. El Security Group del destino de montaje de EFS debe permitir TCP `2049` desde el Security Group de EC2. Para comprobar el montaje después de reiniciar la instancia, ejecuta:

```sh
findmnt -rn -t nfs,nfs4 --mountpoint /mnt/efs
```

El workflow comprueba el montaje y que el contenedor pueda escribir en `/mnt/efs/cloudsumativa2/tickets`. Esa carpeta se monta en `/app/storage/tickets` dentro del contenedor. Si falta el montaje o la carpeta no es escribible, el despliegue se detiene antes de reemplazar el contenedor actual.

**Secretos de GitHub Actions**

Agrégalos en **Settings → Secrets and variables → Actions → Repository secrets**.

| Secreto | Valor esperado |
| --- | --- |
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub con acceso de escritura al repositorio de imágenes. |
| `DOCKERHUB_TOKEN` | Token de Docker Hub con permiso de escritura. |
| `EC2_HOST` | IPv4 pública o DNS público de EC2, sin esquema, usuario ni puerto. |
| `USER_SERVER` | Usuario SSH de EC2, por ejemplo `ec2-user` o `ubuntu`. |
| `EC2_SSH_KEY` | Contenido completo de la clave privada SSH de la instancia. |
| `MYSQL_JDBC_URL` | `jdbc:mysql://ENDPOINT_RDS:3306/cloudsumativa2`. |
| `MYSQL_USERNAME` | Usuario de la aplicación en MySQL. |
| `MYSQL_PASSWORD` | Contraseña de ese usuario. |
| `S3_ENABLED` | `true` para activar S3 en el despliegue. |
| `S3_BUCKET` | Nombre del bucket, sin el prefijo `s3://`. |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN` | Credenciales temporales de AWS Academy con permisos para leer, escribir y eliminar los objetos del bucket. El workflow exige las tres cuando S3 está activo. |

El despliegue actual usa la región predeterminada `us-east-1`. Cuando caduquen las credenciales del laboratorio, actualiza los tres secretos de AWS y vuelve a desplegar. Para incluir las solicitudes de S3 en Postman, configura `ejecutarS3=true` en la colección.
