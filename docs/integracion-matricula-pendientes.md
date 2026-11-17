# Integración Trámites ↔ Matrícula · Pendientes

> Estado al 2 nov 2026. Épica TP-138 (SC-03).

## Actualización del 16/11/2026 (leer primero)

El resto del documento describe el estado del 2 de noviembre. Lo que cambió desde entonces:

| Tema | Estado actual |
|---|---|
| Base de datos de producción | **Railway quedó fuera de servicio** (venció la prueba gratuita). Producción está caída hasta el corte a la **BD compartida en el Aiven de Matrícula** (`docs/CORTE_RAILWAY_A_AIVEN.md` §0). Los datos eran de prueba y se arranca en limpio |
| Despliegue en el servidor UFPS | No se hará (acta CCB N.º 2). Producción sigue en Render |
| Producción de Matrícula | Su API corre en **code.run** (`https://p01--posgrados-project--kqqwl9rwqwbv.code.run/posgrados-project`, health UP el 16/11/2026). Falta la cuenta técnica `Servicio Tramites` en su producción (TP-205). Cuando exista, `MATRICULA_URL` pasa a esa URL y la copia en Render deja de ser necesaria |
| Punto 1 (sincronización en producción, TP-203) | Se verifica después del corte (casos R-30 y R-31 de `docs/REGRESION_CONJUNTA.md`). La contraseña de `svc-tramites` de la copia se rota según la sección 1, punto 2 |
| Punto 1b (monitor, TP-204) | Solo aplica mientras se use la copia en Render |
| Punto 2 (estilo visual) | Hecho (TP-207) |
| Punto 3 (Jira) | TP-140, TP-144, TP-145, TP-147 y TP-148 cerrados. TP-138 sigue abierta por TP-203 y TP-205 |
| Punto 4 (ADD) | Hecho: ADD v3.1 con diagrama, portada y decisiones de SC-02 (TP-206) |
| Punto 5 (con Matrícula) | Se sigue en TP-205 y en la entrega de `docs/entrega-matricula/` |
| Punto 6 (proceso) | Formalizado en `docs/CONTROL_DE_CAMBIOS.md` y en las actas del CCB | Detalle del trabajo hecho en `integracion-matricula-plan.md` y `integracion-matricula-documentacion.md`.

## Estado actual

- **Código:** todo SC-03 está en `main` y `develop` (backend #48–#61, frontend #44). `develop` y `main` sincronizados (#60).
- **Copia de Matrícula en Render:** <https://posgrados-api-d4yv.onrender.com/posgrados-project>.
  - Base en Aiven, cargada con los scripts de `C:\AYD\copia-matricula-render\`, que quedan fuera de cualquier repo.
  - Rama `SC-03-integracion-tramites`.
  - Cuenta técnica `svc-tramites`.
  - Estudiantes: los 6 de nuestra base de producción, cargados con `7a` → `7b`. La cuenta de prueba `steven` recibió la cédula `9999999902` con el script `8`; si `7c` todavía la lista, falta cargarla con `7a` → `7b`.
- **Base de producción de Trámites (Railway):** ya tiene las migraciones `migracion_sc03_snapshot_matricula.sql` y `migracion_sc03_codigos_programa.sql`.
- **Render de Trámites:** configurado con `MATRICULA_MODO=http`, `MATRICULA_URL`, `MATRICULA_USUARIO`, `MATRICULA_PASSWORD` y **`MATRICULA_SINCRONIZAR=true`**, desplegado el 2 nov 2026. Falta confirmarlo en los logs (punto 1).
- **Pruebas:** la integración se probó de punta a punta por internet contra la copia en Render: login técnico, 200, 404 y 403, warm-up, bloqueo de un estudiante inexistente y conservación de nuestros créditos.

## 1. Confirmar la sincronización en producción

Con `MATRICULA_SINCRONIZAR=true`, Trámites consulta a Matrícula antes de crear una terminación, un grado o un certificado. Un estudiante que no exista en la copia de Matrícula queda bloqueado para iniciar trámites nuevos; los ya iniciados siguen su curso.

1. **Verificar en los logs de `tramites-backend` (Render):** al iniciar sesión un estudiante deben aparecer:
   ```
   [MATRICULA] GET /actuator/health status=200
   [MATRICULA] POST /auth/login status=200
   [MATRICULA] GET /api/application/case/integracion/v1/estudiantes/{cedula} cedula=... intento=1 status=200
   ```
2. **Cambiar la contraseña de la cuenta técnica**, si todavía es la que quedó en el chat del 2 nov. En Aiven:
   ```sql
   UPDATE clave c JOIN usuarios u ON u.id_clave = c.id
      SET c.valor = '<NUEVA>' WHERE u.nombreusuario = 'svc-tramites';
   ```
   Después, actualizar `MATRICULA_PASSWORD` en Render.
3. **Estudiantes nuevos en Trámites:** cada estudiante que se cree en nuestra base debe existir también en la copia de Matrícula. Si no, queda bloqueado. Para agregarlos, vuelve a correr `7a` (Railway) → `7b` (Aiven); solo inserta los que faltan. `7c` dice quién no se puede cargar y por qué.
4. **Para apagarla** si algo falla: `MATRICULA_SINCRONIZAR=false`. No hace falta revertir nada más.

## 1b. Mantener despierta la copia de Matrícula (recomendado)

La copia de Matrícula está en el plan gratuito de Render. Render la duerme a los 15 minutos sin uso, y **tarda unos 2 minutos en despertar**: el 2 nov se midieron 131 s. Nuestro cliente se rinde a los 15 s (RNF-09), así que, si Matrícula está dormida:
- un estudiante con copia de menos de 24 h sigue normal;
- un estudiante sin copia vigente (la primera vez o tras 24 h) ve «La validación académica no está disponible en este momento. Intenta más tarde». A los ~2 minutos ya le funciona, porque el login la despertó.

**Solución:** un monitor gratuito que la mantenga despierta, por ejemplo [UptimeRobot](https://uptimerobot.com) o [cron-job.org](https://cron-job.org). Configúralo con una petición GET cada **10 minutos** a:
```
https://posgrados-api-d4yv.onrender.com/posgrados-project/actuator/health
```
Una respuesta 200 con `{"status":"UP"}` significa que está bien.

**Costo en horas:** Render da 750 horas gratis al mes **por cuenta**, y un servicio despierto todo el mes gasta unas 730.
- Si en esa cuenta solo está la copia de Matrícula, alcanza.
- Si también está el backend de Trámites y se mantiene despierto, se pasa. En ese caso, programa el monitor solo en horario hábil (por ejemplo, lunes a viernes de 7:00 a 19:00), algo que cron-job.org permite.

## 2. Estilo visual del frontend (sesión aparte)

**Prompt listo para la sesión:** `tramites-frontend/docs/prompt-estilo-matricula.md` (frontend #46).

Alinear el estilo de Trámites con el de Matrícula.
- **Matrícula:** React 19 + Vite + Tailwind 4 + Heroicons (repo `matricula-frontend`, rama `prod`, `src/index.css`).
- **Trámites:** Create React App + Tailwind 3.

No hace falta migrar el build. El plan:
1. Inventario de sus tokens: colores, tipografía, espaciado y bordes.
2. Llevarlos a nuestro `tailwind.config.js`.
3. Ajustar los componentes compartidos: sidebar, encabezado, botones, tarjetas y tablas.

**Antes de empezar:** confirmar con su PO o con el ing. Matías si su frontend es la referencia de toda la plataforma, y si entra en SC-03 o es una solicitud de cambio o historia nueva (AD-CCA-07 no lo menciona).

## 3. Jira

- **Cerrar TP-147 y TP-148:** el trabajo está hecho y comentado. Los cierra el PO o los responsables, Bryan y Santiago.
- **TP-145:** Diego revisa la propuesta de conteo IFPUG (EIF 5 PF + EI 4 PF = 9 PF), comentada en el issue.
- **TP-144, TP-140 y TP-138:** cerrarlos cuando la sincronización esté verificada en los logs de producción (punto 1).

## 4. Documentación

- **ADD v3** (Google Doc en la carpeta de la v2):
  - insertar el diagrama de arquitectura, en el recuadro amarillo de la sección 2;
  - ajustar portada y logos;
  - confirmar los permisos del archivo, que quedó a nombre de `claudetposgrados@gmail.com`.

## 5. Con el equipo de Matrícula

- Confirmar que su `DDL_Final.sql` coincide con el esquema de su producción (rama `render`).
- Contarles lo que encontramos:
  - Su `DDL_Final` trae al final migraciones que fallan si corren antes del DML.
  - Su `DML_Final` usa comillas dobles, que fallan con el `sql_mode=ANSI` de Aiven.
  - Su catálogo pide 48 créditos en todas las maestrías y 24 en todas las especializaciones.
- Si algún día despliegan nuestra rama en su producción, la copia en Render deja de ser necesaria: basta con cambiar `MATRICULA_URL` y la cuenta técnica.

## 6. Proceso del equipo

- El #56 se mergeó directo a `main` desde una rama basada en `develop`, y arrastró SC-03 a producción antes de tiempo. El flujo acordado es rama → PR a `develop` → PR `develop` → `main`.

## Fuera de esta iteración

- **N7:** login con Google, que hoy crea usuarios sin cédula.
- **N11:** endpoint para que Matrícula consulte el estado del grado (solo si lo piden).
- **N12:** inicio de sesión único desde Matrícula con un token de paso.
