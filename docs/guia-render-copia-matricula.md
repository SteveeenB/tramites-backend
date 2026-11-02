# Guía · Copia del Módulo de Matrícula en Render (SC-03)

> Acordado con el PO de Matrícula: Johan despliega en su Render una copia del backend de Matrícula, desde la rama `SC-03-integracion-tramites`. Trámites se conecta a esa copia.
> Probado en local el 2 nov 2026 con los mismos scripts y valores (ver `docs/integracion-matricula-documentacion.md`, sección 5).

Tiempo estimado: unos 45 minutos. Necesitas:
- `DDL_Final.sql` y `DML_Final.sql`, que entregó el equipo de Matrícula. **No los subas a ningún repositorio**: traen contraseñas y datos de personas.
- MySQL Workbench, que ya tienes instalado.

---

## 1. Base de datos MySQL (Aiven, gratis)

Render no ofrece MySQL gratis. Aiven sí, y es lo que usa el `render.yaml` de Matrícula (esquema `defaultdb`).

1. Crea una cuenta en <https://console.aiven.io> y un servicio **MySQL**, plan **Free**.
2. Cuando esté *Running*, copia de **Connection information**: `Host`, `Port`, `User` (normalmente `avnadmin`) y `Password`. La base se llama `defaultdb`.
3. En MySQL Workbench, crea una conexión con esos datos. En la pestaña **SSL**, elige *Use SSL: Require*.

## 2. Cargar el esquema y los datos (en este orden)

| # | Script | Antes de correrlo |
|---|---|---|
| 1 | `DDL_Final.sql` | **Borra la primera línea `USE posgrados;`** y todo desde la línea `-- 2026-10-D00-separar-plazos-cohorte.sql` (y la línea `-- ====` justo encima) hasta el final del archivo |
| 2 | `DML_Final.sql` | **Borra la primera línea `USE posgrados;`** |
| 3 | `2026-10-D00-separar-plazos-cohorte.sql` | Nada (están en `db/scripts/` de su repo y son los que te mandaron aparte) |
| 4 | `2026-10-D00-ultimocodigoprograma.sql` | Nada |
| 5 | `2026-11-SC03-rol-servicio-tramites.sql` | Nada (está en `db/scripts/` de la rama SC-03) |

En Workbench: abre cada archivo y ejecútalo con el rayo, con `defaultdb` como esquema por defecto (doble clic sobre él en el panel izquierdo).

**Por qué el orden:** los scripts 3 y 4 necesitan los catálogos que trae el DML. Si se corren dentro del DDL, el primero aborta con «tipoplazo no tiene los tipos INSCRIPCION y/o PAGO» y el segundo nunca crea la tabla `ultimocodigoprograma`.

**Verifica:**
```sql
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'defaultdb'; -- 53
SELECT nombre FROM roles;            -- incluye «Servicio Tramites»
SELECT COUNT(*) FROM programas;      -- 20
```

## 3. Estudiantes de prueba y cuenta técnica

El DML no trae estudiantes. Sin ellos, cualquier consulta de Trámites da «no figura en Matrícula». Estos son ficticios y usan las cédulas del seed de Trámites, todos en el programa 2203 (Maestría en Gerencia de Empresas). Pedro (1098765436) queda fuera a propósito, para probar el bloqueo:

```sql
USE defaultdb;
SET @prog := (SELECT id FROM programas WHERE codigo = '2203');
INSERT INTO plazo (fechainicio, fechafin, id_tipoplazo) VALUES
  ('2024-11-01','2024-11-30',(SELECT id FROM tipoplazo WHERE tipo='DOCUMENTACION')),
  ('2024-10-01','2024-10-31',(SELECT id FROM tipoplazo WHERE tipo='INSCRIPCION')),
  ('2024-12-01','2024-12-31',(SELECT id FROM tipoplazo WHERE tipo='PAGO'));
SET @pdoc := LAST_INSERT_ID();
INSERT INTO cohorte (nombre, cupos, id_estado, id_semestre, id_modalidad,
                     id_plazodocumentacion, id_plazoinscripcion, id_plazopago, id_programa)
VALUES ('Cohorte 2025-1 (pruebas SC-03)', 30,
        (SELECT id FROM estado WHERE entidad='cohorte' AND tipo='CERRADA'),
        (SELECT MIN(id) FROM semestre),
        (SELECT id_modalidad FROM programas WHERE id=@prog),
        @pdoc, @pdoc+1, @pdoc+2, @prog);
SET @coh := LAST_INSERT_ID();
SET @activo := (SELECT id FROM estado WHERE tipo='ACTIVO' LIMIT 1);
INSERT INTO estudiantes (nombre, apellido, apellido2, cedula, codigo, email, esPosgrado,
                         fechalngreso, cohorte_id, estado_estudiante_id, programa_id) VALUES
  ('Juan',   'Perez',  NULL,    '1098765432', '22030001', 'juan.perez@example.com', TRUE, '2025-02-03', @coh, @activo, @prog),
  ('Laura',  'Gomez',  NULL,    '1098765435', '22030002', 'laura@example.com',      TRUE, '2025-02-03', @coh, @activo, @prog),
  ('Carlos', 'Rueda',  NULL,    '1098765437', '22030003', 'carlos@example.com',     TRUE, '2025-02-03', @coh, @activo, @prog),
  ('Andrea', 'Prueba', 'Grado', '2000000010', '22030004', 'andrea.grado@test.com',  TRUE, '2025-02-03', @coh, @activo, @prog);
```

**Cuenta técnica.** El frontend de Matrícula no se despliega, así que se crea por SQL. Cambia `<CONTRASEÑA_LARGA_ALEATORIA>` por una tuya (al menos 20 caracteres) y guárdala: es la que va en `MATRICULA_PASSWORD`.

```sql
INSERT INTO persona (nombres, apellidos, celular, correo)
VALUES ('Servicio', 'Tramites', '0000000000', 'svc-tramites@pruebas.local');
INSERT INTO clave (valor) VALUES ('<CONTRASEÑA_LARGA_ALEATORIA>');
INSERT INTO usuarios (nombreusuario, id_persona, id_rol, id_clave) VALUES ('svc-tramites',
  (SELECT id FROM persona WHERE correo = 'svc-tramites@pruebas.local'),
  (SELECT id FROM roles WHERE nombre = 'Servicio Tramites'),
  (SELECT id FROM clave WHERE valor = '<CONTRASEÑA_LARGA_ALEATORIA>'));
```

Matrícula acepta la clave en texto plano (`app.auth.password.allow-plain-text-legacy=true`) y la valida con BCrypt cuando ya está cifrada.

## 4. Servicio en Render

1. Haz un fork de `a-cubica-r/ACA-0001-POSGRADOS_PROJECT-BND` en tu cuenta de GitHub, con todas las ramas.
2. En Render: **New → Blueprint**, elige tu fork y la rama **`SC-03-integracion-tramites`**. Render lee el `render.yaml`: servicio `posgrados-api`, Docker, plan free, health check `/posgrados-project/actuator/health`.
3. Llena las variables que el `render.yaml` deja en blanco. `JWT_SECRET` lo genera Render solo, y las de valor fijo ya vienen puestas.

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:mysql://<HOST>:<PORT>/defaultdb?sslMode=REQUIRED` |
| `DB_USER` / `DB_PASSWORD` | Los de Aiven |
| `APP_BASE_URL` | `https://<tu-servicio>.onrender.com/posgrados-project` |
| `APP_FRONTEND_LOGIN_URL` / `APP_FRONTEND_RESET_PASSWORD_URL` | Cualquier URL (no hay frontend), p. ej. `https://example.com/login` |
| `SPRING_SECURITY_USER_NAME` / `SPRING_SECURITY_USER_PASSWORD` | Cualesquiera |
| `WOMPI_SIMULATED` | `true` |
| `WOMPI_API_BASE_URL` | `https://sandbox.wompi.co/v1` |
| `WOMPI_PUBLIC_KEY`, `WOMPI_PRIVATE_KEY`, `WOMPI_INTEGRITY_SECRET` | `no-usado` |
| `WOMPI_RETURN_URL`, `WOMPI_WEBHOOK_URL` | `https://example.com` |
| `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_KEY_SES`, `AWS_BUCKET_NAME` | `no-usado` |
| `AWS_SES_FROM_EMAIL`, `MAIL_FROM` | `no-responder@example.com` |
| `RESEND_API_KEY` | `no-usado` |

Con esos valores el backend arranca: así se probó en local. No se envían correos ni se suben archivos, porque esta copia es solo para la integración.

4. **Create**. El primer build tarda varios minutos (compila los 11 módulos con Maven).

## 5. Verificar la copia

```bash
B=https://<tu-servicio>.onrender.com/posgrados-project
curl $B/actuator/health        # {"status":"UP"} (la primera vez puede tardar ~1 min: cold start)

TOKEN=$(curl -s -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"svc-tramites","password":"<CONTRASEÑA>","requestedRole":"SERVICIO_TRAMITES"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")

curl -H "Authorization: Bearer $TOKEN" $B/api/application/case/integracion/v1/estudiantes/1098765435   # 200, programa 2203
curl -H "Authorization: Bearer $TOKEN" $B/api/application/case/integracion/v1/estudiantes/1098765436   # 404
curl -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN" $B/api/application/case/estudiantes  # 403
```

## 6. Conectar Trámites

Requiere que el PR #58 (`develop` → `main`) esté mergeado. Trae el mapeo de programas (#55) y evita que se reemplacen nuestros créditos (#57).

1. En nuestra base de producción, corre una sola vez:
   - `.docs/sql/migracion_sc03_snapshot_matricula.sql`
   - `.docs/sql/migracion_sc03_codigos_programa.sql`
2. Variables del servicio `tramites-backend` en Render:

| Variable | Valor |
|---|---|
| `MATRICULA_MODO` | `http` |
| `MATRICULA_URL` | `https://<tu-servicio>.onrender.com/posgrados-project` |
| `MATRICULA_USUARIO` | `svc-tramites` |
| `MATRICULA_PASSWORD` | La de la cuenta técnica |
| `MATRICULA_SINCRONIZAR` | `true` |

3. Prueba de punta a punta:
   - Inicia sesión como estudiante; en el log aparece `[MATRICULA] GET /actuator/health` y luego la consulta.
   - Intenta una terminación con alguien que no esté en la copia: debe bloquear con «El estudiante no aparece como admitido y legalizado en el Módulo de Matrícula».

**Ojo:** con `MATRICULA_SINCRONIZAR=true`, un estudiante real de Trámites que no exista en la copia de Matrícula **no podrá iniciar trámites nuevos**. Antes de activarlo, carga en la copia a los estudiantes reales que lo necesiten (o pídele a Matrícula sus datos), o déjalo en `false` hasta tenerlos.

## Notas

- **Horas gratis:** los dos servicios (Trámites y la copia de Matrícula) comparten las horas gratis de tu cuenta de Render. Se duermen tras unos minutos sin uso; Trámites despierta a Matrícula al iniciar sesión un estudiante.
- **Swagger:** el `render.yaml` deja Swagger activo (`ENABLE_SWAGGER_UI=true`). Si no lo quieres público, ponlo en `false`.
- **Actualizar la copia:** cada commit en la rama `SC-03-integracion-tramites` de tu fork se despliega solo (`autoDeploy: true`). Para traer cambios de su repo: GitHub → *Sync fork*.
