# Integración Trámites ↔ Matrícula · Pendientes

> Estado al 2 nov 2026. Épica TP-138 (SC-03). Detalle del trabajo hecho en `integracion-matricula-plan.md` y `integracion-matricula-documentacion.md`.

## Estado actual

- **Código:** todo SC-03 está en `main` y `develop` (backend #48–#59, frontend #44). `develop` y `main` sincronizados (#60).
- **Copia de Matrícula en Render:** <https://posgrados-api-d4yv.onrender.com/posgrados-project>.
  - Base en Aiven, cargada con los scripts de `C:\AYD\copia-matricula-render\`, que quedan fuera de cualquier repo.
  - Rama `SC-03-integracion-tramites`.
  - Cuenta técnica `svc-tramites`.
  - 4 estudiantes de prueba ficticios.
- **Base de producción de Trámites (Railway):** ya tiene las migraciones `migracion_sc03_snapshot_matricula.sql` y `migracion_sc03_codigos_programa.sql`.
- **Render de Trámites:** configurado con `MATRICULA_MODO=http`, `MATRICULA_URL`, `MATRICULA_USUARIO` y `MATRICULA_PASSWORD`. **`MATRICULA_SINCRONIZAR` sigue en `false`**: la integración está apagada y producción se comporta como antes.
- **Pruebas:** la integración se probó de punta a punta por internet contra la copia en Render: login técnico, 200, 404 y 403, warm-up, bloqueo de un estudiante inexistente y conservación de nuestros créditos.

## 1. Encender la sincronización (pendiente principal)

Con `MATRICULA_SINCRONIZAR=true`, Trámites consulta a Matrícula antes de crear una terminación, un grado o un certificado. **Un estudiante que no exista en la copia de Matrícula queda bloqueado** para iniciar trámites nuevos; los ya iniciados siguen su curso. Por eso, antes de encenderla:

1. **Cargar en la copia de Matrícula a los estudiantes reales** que vayan a hacer trámites. Opciones:
   - **a)** Un script que los copie desde nuestra BD (tablas `estudiante` y `usuario`) a la tabla `estudiantes` de la copia, con cédula, nombres, código, correo y programa por código, en una cohorte y con estado `ACTIVO`.
   - **b)** Pedirle al equipo de Matrícula sus estudiantes reales (solo los datos que usa la consulta C1).
2. **Cambiar la contraseña de la cuenta técnica** si todavía es la que quedó en el chat del 2 nov. En Aiven:
   ```sql
   UPDATE clave c JOIN usuarios u ON u.id_clave = c.id
      SET c.valor = '<NUEVA>' WHERE u.nombreusuario = 'svc-tramites';
   ```
   Después, actualizar `MATRICULA_PASSWORD` en Render.
3. Poner `MATRICULA_SINCRONIZAR=true` en el Render de Trámites.
4. **Verificar en producción:**
   - Un estudiante cargado inicia sesión y en el log aparece `[MATRICULA] GET /actuator/health`, `POST /auth/login` y la consulta C1 con 200.
   - Un estudiante que no está en la copia recibe «El estudiante no aparece como admitido y legalizado en el Módulo de Matrícula» al iniciar un trámite.
5. **Para apagarla** si algo falla: `MATRICULA_SINCRONIZAR=false`. No hace falta revertir nada más.

## 2. Estilo visual del frontend (sesión aparte)

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
- **TP-144, TP-140 y TP-138:** cerrarlos cuando la sincronización esté encendida y verificada en producción (punto 1).

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
