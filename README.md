# cloudsumativa2

Sistema de reservas de eventos desarrollado con Spring Boot que permite registrar usuarios y eventos, reservar cupos y generar tickets en formato TXT. Incluye consulta, modificación y eliminación de tickets, además de estadísticas por evento. En local utiliza H2 y guarda los archivos en disco. En EC2 utiliza MySQL en RDS y EFS para los tickets, con S3 opcional. GitHub Actions construye y publica la imagen Docker y automatiza el despliegue.

Requiere Java 21. Ejecutar desde esta carpeta.

**H2 (por defecto)**

```sh
ISSUER_URI='issuer-de-b2c' \
AUDIENCE='id-del-registro-de-la-api' \
JWK_SET_URI='jwks-uri-del-flujo-b2c' \
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`. Las tablas se crean automáticamente. La base de datos se guarda en `./data/cloudsumativa2.mv.db` y los tickets en `./storage/tickets`.

**Autenticación y reservas**

La API valida el JWT de Azure AD B2C y exige el claim `extension_CONSULTA_ROLE_CLAIM` con `ADMIN` o `CLIENT` en las rutas de negocio. `GET /actuator/health` está disponible sin token.

El rol `ADMIN` puede crear eventos, consultar estadísticas, listar usuarios, subir tickets manualmente a S3 y gestionar tickets de cualquier usuario. Ambos roles pueden consultar eventos. Un `CLIENT` registra su perfil con `POST /usuarios` y los campos `nombre` y `correo`. Spring vincula el perfil con el emisor y sujeto del token. Si ya existe para esa identidad, el mismo endpoint devuelve el perfil con `200`. Los perfiles anteriores o creados por un administrador quedan disponibles para administración. La API no los vincula a un cliente por coincidencia de correo.

Para reservar, usa `POST /eventos/{eventoId}/tickets` con `{"usuarioId": 1}`, reemplazando el ID por el del perfil. Spring rechaza con `403` si un cliente usa un ID ajeno. Los clientes solo pueden consultar, descargar, modificar y cancelar sus propios tickets. Sus listas por evento también se filtran por titular. Al cancelar una reserva, se recupera el cupo.

**Pruebas en Postman**

Importa la [colección de autorización](postman/cloudsumativa2-autorizacion.postman_collection.json) y abre su pestaña **Variables**. Pega los JWT de Azure AD B2C en los valores locales de `adminToken` y `clientToken`, sin el prefijo `Bearer`. Completa `clientEmail` con un correo de prueba y ejecuta la colección completa. **Éxito** y **Rechazo** tienen subcarpetas **Admin** y **Client**. Si usas **Send**, sigue los números del **01 al 15**, alternando entre roles. Los pasos **01 a 04** guardan automáticamente `eventoId`, `otroUsuarioId`, `clienteId` y `ticketId`, en ese orden. Si S3 está desactivado, pasa del **08** al **11**. En **Rechazo**, el 401 o 403 indicado en cada solicitud cuenta como prueba aprobada. Deja los valores compartidos de los tokens vacíos y evita exportar la colección con credenciales. Asigna el rol `ADMIN` mediante administración en B2C y mantén ese atributo fuera de los campos editables de autoservicio.

**S3 (opcional)**

S3 está deshabilitado por defecto. Para activarlo con un bucket existente y credenciales temporales de AWS Academy, ejecutar:

```sh
S3_ENABLED=true \
AWS_REGION=us-east-1 \
S3_BUCKET='nombre-del-bucket' \
AWS_ACCESS_KEY_ID='tu-access-key' \
AWS_SECRET_ACCESS_KEY='tu-secret-key' \
AWS_SESSION_TOKEN='tu-session-token' \
ISSUER_URI='issuer-de-b2c' \
AUDIENCE='id-del-registro-de-la-api' \
JWK_SET_URI='jwks-uri-del-flujo-b2c' \
./mvnw spring-boot:run
```

Con S3 habilitado, la reserva sube el ticket automáticamente. También se puede subir con `POST /tickets/{id}/s3` y descargar con `GET /tickets/{id}/s3`.

**Prueba de arranque**

```sh
./mvnw test
```

**Despliegue en EC2**

GitHub Actions ejecuta las pruebas y valida la imagen Docker con H2 en los pull requests hacia `main` y en los pushes a `main`. En un push a `main`, también publica en Docker Hub (`ignarayaa/cloudsumativa2`) con las etiquetas `latest` y el SHA completo del commit. Luego se conecta por SSH a EC2, despliega la imagen etiquetada con ese SHA y comprueba que `GET /actuator/health` responde correctamente y que `GET /eventos` exige autenticación.

En EC2, el usuario SSH necesita poder ejecutar Docker. El Security Group debe permitir TCP `22` desde el runner que despliega. Para probar desde Postman, permite TCP `8080` desde tu IP pública y configura `baseUrl` como `http://IP_PUBLICA_EC2:8080` en la [colección Postman](postman/cloudsumativa2-autorizacion.postman_collection.json).

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
| `ISSUER_URI` | Emisor exacto del JWT de B2C. |
| `AUDIENCE` | Audiencia del registro de la API en B2C. |
| `JWK_SET_URI` | URL de claves públicas de los metadatos del flujo B2C. Se puede omitir si Spring descubre las claves a partir del emisor. |
| `MYSQL_JDBC_URL` | `jdbc:mysql://ENDPOINT_RDS:3306/cloudsumativa2`. |
| `MYSQL_USERNAME` | Usuario de la aplicación en MySQL. |
| `MYSQL_PASSWORD` | Contraseña de ese usuario. |
| `S3_ENABLED` | `true` para activar S3 en el despliegue. |
| `S3_BUCKET` | Nombre del bucket, sin el prefijo `s3://`. |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN` | Credenciales temporales de AWS Academy con permisos para leer, escribir y eliminar los objetos del bucket. El workflow exige las tres cuando S3 está activo. |

El despliegue actual usa la región predeterminada `us-east-1`. Cuando caduquen las credenciales del laboratorio, actualiza los tres secretos de AWS y vuelve a desplegar. Para incluir las solicitudes de S3 en Postman, configura `ejecutarS3=true` en la colección.
