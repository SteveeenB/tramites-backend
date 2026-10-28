# Plan de implementación — Módulo de Certificados (HU11)

> Constancia técnica para el equipo. Documenta el alcance, las decisiones tomadas y los puntos de extensión del módulo. Quien tome esto debe poder continuar sin tener que reconstruir el contexto.

## 1. Historias de usuario que cubre

- **HU-Certificados-1 (estudiante / graduado):** solicitar certificado académico, elegir modalidad, pagar y recibirlo (digital al correo, físico para retiro en la dependencia correspondiente).
- **HU-Certificados-2 (estudiante + dependencia):** consultar el historial de certificados emitidos; la dependencia puede consultar los suyos por cédula del estudiante; cualquier certificado generado se puede descargar de nuevo.
- **HU-Certificados-3 (admin / coordinador):** crear, editar, activar/desactivar tipos de certificado, ajustar precios y asignar la dependencia encargada.

## 2. Punto de partida — qué ya existía

Implementado en el PR #19 (autor: Steven) y mergeado a `main`:

- Entidades `SolicitudCertificado` y `TipoCertificado`.
- Repositorios JPA básicos.
- `CertificadoService.solicitarCertificado()` y `simularPago()` (solo cambia estado a `PAGADO`).
- `CertificadoController` con endpoints `/solicitar`, `/{id}/pagar`, `/tipos`, listar por cédula.
- Frontend `Certificados.jsx` con UI completa de solicitud + historial y pago simulado.
- Seed con 3 tipos iniciales: `CONSTANCIA_REGISTRO_CALIFICADO`, `CONSTANCIA_MATRICULA`, `CONSTANCIA_BUENA_CONDUCTA`.

Lo que **no estaba** y este plan cubre: generación real del PDF tras pago, envío por correo, descarga desde historial, flujo de retiro físico, bandeja de dependencia, administración de tipos.

## 3. Decisiones de diseño

### 3.1 No existe la modalidad "AMBAS"
El estudiante elige `DIGITAL` o `FÍSICA`. La modalidad solo determina la máquina de estados y el costo. **El PDF se genera siempre** y está disponible para descarga en ambas modalidades.

### 3.2 Equivalencia funcional digital ↔ física (Ley 527/1999)
El PDF generado tiene plena validez jurídica. **No** se aplica marca de agua "copia de referencia" al físico, porque eso degrada injustificadamente el documento digital y rompe la promesa de generación inmediata.

Implicaciones concretas:
- Un mismo PDF, una sola fila en BD, una sola URL de descarga.
- En `FÍSICA`, el funcionario de la dependencia descarga el mismo PDF, lo imprime en papel membretado y le aplica el sello físico (seco o automático). El estudiante recibe el papel con sello; el sistema sigue ofreciéndole el PDF para descarga futura.

### 3.3 Sin requisitos académicos para certificados
Eliminada la validación `creditosAprobados > 0` que tenía el `CertificadoService` inicial. Los certificados son trámites administrativos básicos y no deben bloquearse por créditos. Esto los separa funcionalmente de "Terminación de Materias" y "Solicitud de Grado", que sí tienen prerrequisitos.

### 3.4 Tabla unificada en frontend (no hay pestaña "Historial")
Una sola tabla en la página de Certificados que crece con cada solicitud. Las **acciones disponibles dependen del estado de la fila**, siguiendo el patrón visual del sistema legado de UFPS ("Histórico Recibos" con columna OPCIÓN).

### 3.5 Modelo de precios
`tipo_certificado.precio_digital` es el **precio base** del documento. `tipo_certificado.costo_logistica_fisica` es el **delta** que se suma si la modalidad es física (cubre impresión, sello, manejo). El precio final físico se calcula: `precio_digital + costo_logistica_fisica`.

Esto permite al admin ajustar de forma independiente "qué cuesta el documento" vs "qué cuesta la gestión física", y el frontend muestra el delta como `+$ 3.700` junto al radio de modalidad física.

### 3.6 Modularidad — data-driven, NO patrones de diseño aún
La diferencia entre tipos de certificado vive **en filas de la BD**, no en `if/else` por tipo en código. Si en el futuro se necesita comportamiento radicalmente distinto por tipo (p. ej. plantillas PDF diferentes), ahí sí introducir Strategy. Hoy un solo `CertificadoConstanciaPdfService` parametrizable cubre los 3 tipos.

### 3.7 Dependencias reutilizan la tabla `usuario`
Las dependencias (Biblioteca, Tesorería, Admisiones y Registro) ya existen como usuarios con `rol='DEPENDENCIA'`. `tipo_certificado.dependencia_cedula` es FK a `usuario(cedula)`. No se crea tabla `dependencia` aparte para no duplicar.

### 3.8 Gancho para firma digital futura
El responsable de la firma digital criptográfica trabajará sobre el PDF generado. Para que su trabajo no implique reescribir esta lógica:

- El PDF se guarda en `solicitud_certificado.url_pdf` (Supabase Storage) y se conserva su `hash_pdf` (SHA-256) en BD.
- La generación del PDF está aislada en `CertificadoConstanciaPdfService.generar(...)`. El proceso de firma puede envolver/decorar esta clase o reemplazar el byte[] resultante antes de subirlo a storage.
- La transición a `GENERADO` queda como un solo método (`CertificadoService.generarYNotificar`) — el punto natural donde aplicar la firma antes de guardar/enviar.

### 3.9 Re-solicitud del mismo tipo: cada solicitud es independiente (Opción A)
Cuando un estudiante ya tiene un certificado en estado `GENERADO` y vuelve a solicitar el mismo tipo, **se crea una nueva fila** en `solicitud_certificado` con su propio id, su propio pago y su propio PDF. No hay descuento por "ya tienes uno digital, paga solo el delta físico".

**Por qué se eligió esta opción:**
- Cada certificado es un "snapshot" en el tiempo (fecha de expedición, datos del estudiante en ese momento). Tratarlos como entidades independientes preserva la trazabilidad.
- La validación de duplicados existente (`tieneVigente` sobre `PENDIENTE_PAGO`) ya impide el caso peor: doble recibo sin pagar.
- El estudiante se asume informado de su necesidad: si pide físico tras digital, sabe que está pagando un nuevo documento, no un upgrade.

**No se muestra ningún cartel de advertencia** en el formulario sobre re-solicitar el mismo tipo. Es una decisión consciente para no inflar la UI con avisos paternalistas.

**Deuda explícita (ver §7):** la opción B ("upgrade digital → físico cobrando solo el delta") se discutirá en sprint review con reglas de negocio claras (ventana de validez, si crea fila nueva con `solicitud_padre_id` o muta la existente, etc.). Por ahora, NO está implementada.

### 3.10 Cliente HTTP centralizado para el frontend
Todas las páginas del módulo (`Certificados.jsx`, `BandejaCertificadosDependencia.jsx`, `ConfiguracionAdmin.jsx`) usan `BASE_URL` de `api/apiClient.js` en lugar de URLs hardcoded.

- En local: por defecto apunta a `http://localhost:8080/api` (sin configuración).
- En producción: se setea `REACT_APP_API_URL` antes del build.
- Backend en local: la CORS por defecto (`cors.allowed-origin=${CORS_ORIGIN:http://localhost:3000}`) ya permite localhost. En producción se debe setear `CORS_ORIGIN`.

> **Antipatrón a evitar:** los archivos heredados del PR #19 hardcodeaban `https://tramites-backend.onrender.com/api`, lo que rompía el desarrollo local con un error CORS confuso (el síntoma era CORS, la causa real era apuntar al backend equivocado y a endpoints inexistentes en producción). Los archivos del módulo de certificados se corrigieron; conviene migrar el resto del codebase en una limpieza posterior.

### 3.11 Roles ADMIN vs POSGRADOS — protección de rutas tolerante
En el demo el botón "Coordinador de Posgrados" mapea a `rol='POSGRADOS'`, pero en BD el usuario administrador todavía tiene `rol='ADMIN'`. Hay deuda histórica de unificación de roles que **escapa al alcance de esta HU**.

Para evitar el `no-autorizado` cuando el coordinador entra a Configuración, la ruta `/tramites/admin/configuracion` en `App.js` acepta ambos: `rolesPermitidos={['ADMIN', 'POSGRADOS']}`. Misma política para los endpoints admin del backend (no se enforza el rol en el controller — la HU asume que solo llega tráfico autenticado de la UI).

> **Decisión pendiente para sprint review:** unificar a un solo rol (más simple) o mantener separados con matriz de permisos por endpoint (más limpio).

## 4. Modelo de datos

### 4.1 Cambios a tablas existentes

```sql
-- solicitud_certificado
ALTER TABLE solicitud_certificado
  ADD COLUMN fecha_pago        timestamp without time zone,
  ADD COLUMN fecha_generacion  timestamp without time zone,
  ADD COLUMN fecha_vencimiento_pago date,
  ADD COLUMN fecha_entrega     timestamp without time zone,
  ADD COLUMN url_pdf           character varying,
  ADD COLUMN hash_pdf          character varying;

-- tipo_certificado
ALTER TABLE tipo_certificado
  DROP COLUMN precio_fisico;
ALTER TABLE tipo_certificado
  ADD COLUMN costo_logistica_fisica double precision NOT NULL DEFAULT 0,
  ADD COLUMN descripcion             character varying,
  ADD COLUMN dependencia_cedula      character varying,
  ADD COLUMN direccion_oficina       character varying,
  ADD COLUMN tiempo_entrega_dias     integer DEFAULT 0;
```

> Nota: el proyecto usa `spring.jpa.hibernate.ddl-auto=update`, que aplica `ADD COLUMN` automáticamente al cambiar las entidades. **No** aplica automáticamente `DROP COLUMN` ni `ADD CONSTRAINT FK`. Quien despliegue por primera vez en un entorno con datos previos debe ejecutar la migración manual de arriba.

### 4.2 Máquina de estados

```
Modalidad DIGITAL:
  PENDIENTE_PAGO ──(pago confirmado)──► PAGADO ──(PDF generado + correo enviado)──► GENERADO
                                                                                       │
                                                                                 (terminal)

Modalidad FÍSICA:
  PENDIENTE_PAGO ──(pago)──► PAGADO ──(PDF generado + correo aviso)──► GENERADO
                                                                          │
                            (funcionario marca listo en su bandeja) ─────►LISTO_RETIRO
                                                                                │
                            (funcionario marca entregado al recibir al alumno) ─►ENTREGADO

Estado adicional:
  VENCIDA   — alcanzado por job opcional cuando `fecha_vencimiento_pago < hoy` y estado sigue `PENDIENTE_PAGO`.
              No implementado en este sprint; el frontend lo muestra si llega del backend, pero hoy se queda en PENDIENTE_PAGO.
```

## 5. Backend — endpoints

Base path: `/api/certificados`

### 5.1 Existentes (no se cambian)
- `GET  /tipos` — lista tipos activos (para dropdown estudiante).
- `POST /solicitar?cedula=&tipo=&modalidad=&destinatario=` — crea solicitud `PENDIENTE_PAGO`.
- `GET  /?cedula=` — historial del estudiante.

### 5.2 Modificados
- `POST /{id}/pagar?cedula=` — además de cambiar estado a `PAGADO`, dispara la transición `PAGADO → GENERADO`: genera el PDF, lo sube a storage, calcula hash, envía correo (siempre, sin importar modalidad), persiste `fecha_pago` y `fecha_generacion`.

### 5.3 Nuevos
- `GET  /{id}/pdf?cedula=` — descarga el PDF generado. Devuelve 404 si todavía no se ha generado, 403 si la cédula no corresponde al dueño ni a la dependencia encargada.
- `POST /{id}/marcar-listo?cedulaDependencia=` — la dependencia marca un físico como `LISTO_RETIRO`.
- `POST /{id}/marcar-entregado?cedulaDependencia=` — la dependencia marca un físico como `ENTREGADO`.
- `GET  /dependencia/{cedulaDependencia}?estado=` — bandeja de la dependencia (lista de certificados físicos en el estado dado).

### 5.4 Endpoints de administración
Base path: `/api/admin/tipos-certificado` (rol `ADMIN` o `POSGRADOS`).

- `GET    /` — lista todos los tipos (activos e inactivos).
- `POST   /` — crea un nuevo tipo.
- `PUT    /{id}` — edita (precio, dependencia, dirección, descripción, tiempo).
- `PATCH  /{id}/activo?valor=true|false` — soft delete / re-activación.

Adicional: `GET /api/dependencias` — lista usuarios con `rol='DEPENDENCIA'` para poblar el dropdown del formulario admin.

## 6. Frontend

### 6.1 Estudiante — `pages/Certificados.jsx`
Se reescribe la sección del historial:
- Columnas: `N° | TIPO | MODALIDAD | FECHA SOLICITUD | VENCIMIENTO PAGO | VALOR | ESTADO | ACCIONES`.
- `VENCIMIENTO PAGO` se muestra siempre (incluso pagado/vencido).
- `ESTADO` solo lleva el badge.
- `ACCIONES` lleva los botones contextuales:
  - `PENDIENTE_PAGO` → `[Descargar Recibo]` `[Pagar]`
  - `PAGADO` (transitorio) → spinner "Generando…"
  - `GENERADO` / `LISTO_RETIRO` / `ENTREGADO` → `[Descargar PDF]`
  - `VENCIDA` → sin acciones
- En el bloque de modalidad, el delta físico se muestra como `+$ 3.700` (calculado desde `costoLogisticaFisica`).

### 6.2 Dependencia — `pages/BandejaCertificadosDependencia.jsx` (nuevo)
- Tabs: `Por imprimir | Listos para entrega | Entregados | Todos`.
- Tabla con filas por solicitud, buscador opcional por cédula del estudiante.
- Acciones por estado:
  - `GENERADO` → `[Descargar PDF]` `[Marcar listo para retiro]`
  - `LISTO_RETIRO` → `[Marcar entregado]`
  - `ENTREGADO` → solo lectura.

Se añade la subtab "Certificados" en el sidebar de DEPENDENCIA junto a "Paz y Salvos" (`menuConfig.js` + `TramitesView.jsx`).

### 6.3 Admin — `pages/ConfiguracionAdmin.jsx`
Se extiende con una segunda sección "Tipos de certificado":
- Tabla de tipos existentes con columnas `Código | Label | Precio digital | Costo logística | Dependencia | Activo | Acciones`.
- Modal/inline form para crear y editar.
- Botón toggle de activo.

## 7. Lo que NO se incluye en este sprint (deuda explícita)

- **Firma digital criptográfica.** Solo se deja el QR cosmético existente y el gancho `hash_pdf` para que el responsable lo conecte después.
- **Pasarela de pagos real.** El endpoint `pagar` sigue simulando confirmación inmediata. Cuando se integre Wompi o PSE real, el dispatcher de generación del PDF debe pasar de ejecutarse en `pagar` a ejecutarse en el webhook de confirmación de pago.
- **Upgrade digital → físico cobrando solo el delta logístico (Opción B).** Discusión pendiente para sprint review: ¿ventana de validez? ¿fila nueva con `solicitud_padre_id` o mutación de la original? ¿el upgrade reusa el mismo PDF o regenera? Hoy cada solicitud es un cobro completo, ver §3.9.
- **Job de vencimiento de recibos.** El estado `VENCIDA` no se calcula automáticamente.
- **Notificaciones SSE/in-app** cuando el certificado pasa a `LISTO_RETIRO`. Solo se notifica por correo electrónico, según lo pactado con el director del proyecto.
- **Estadísticas / reportería** para coordinador de posgrados.
- **Unificación del rol ADMIN/POSGRADOS.** Hoy se permite ambos en las rutas/protecciones admin como mitigación tolerante. La normalización del modelo de roles es deuda transversal del proyecto.
- **Foreign keys formales en BD.** El `ALTER TABLE ... ADD CONSTRAINT FOREIGN KEY` para `solicitud_certificado.cedula`, `solicitud_certificado.tipo_certificado` y `tipo_certificado.dependencia_cedula` debe correrse manualmente — Hibernate con `ddl-auto=update` no las crea. Sin FKs el sistema funciona pero pierde integridad referencial. Ver script al inicio de §4.1.

## 8. Cómo extender este módulo

- **Agregar un tipo de certificado nuevo**: el admin lo crea desde la UI. Cero código.
- **Cambiar el formato del PDF**: editar `CertificadoConstanciaPdfService.generar(...)`. Si el cambio es solo cosmético (logos, colores, posiciones), no hay impacto aguas abajo.
- **Conectar firma digital real**: envolver `CertificadoConstanciaPdfService.generar(...)` o aplicar la firma en `CertificadoService.generarYNotificar(...)` antes de subir el byte[] a storage. Persistir `firmaAplicada=true` y opcionalmente la URL del PDF firmado en una nueva columna si conviene separarlos.
- **Conectar pasarela de pago real**: mover la llamada a `generarYNotificar(id)` desde `CertificadoController.simularPago` al handler del webhook de la pasarela.

## 9. Bitácora de correcciones aplicadas durante el sprint

### 9.1 Contadores incorrectos en las tabs de la bandeja de dependencia
**Síntoma:** En `BandejaCertificadosDependencia.jsx`, al estar en la pestaña "Por imprimir (2)", la pestaña "Listos para entrega" mostraba `(0)` incluso cuando había solicitudes en ese estado. Al cambiar de pestaña, los conteos cambiaban porque dependían del filtro activo.

**Causa:** la función `cargar()` enviaba el `estado=...` como query param al backend, así que `solicitudes` solo contenía las filas del tab activo. Los contadores de las otras pestañas se calculaban sobre un conjunto vacío para ellas.

**Fix:** se eliminó el filtrado server-side. La bandeja carga **todas** las solicitudes de la dependencia una sola vez, y el filtrado por tab pasó a ser cliente-side. Esto mantiene los conteos correctos para todas las pestañas siempre. El endpoint `GET /api/certificados/dependencia/{cedula}?estado=...` sigue soportando filtro server-side por si se necesita en el futuro, pero la UI no lo usa.

### 9.2 BASE_URL hardcoded a producción
**Síntoma:** al arrancar el backend en local, el frontend seguía pegándole al backend de Render y reportaba un error CORS confuso. Los endpoints nuevos creados en local no existían en producción, agravando la confusión.

**Fix:** los 3 archivos del módulo (`Certificados.jsx`, `BandejaCertificadosDependencia.jsx`, `ConfiguracionAdmin.jsx`) importan `BASE_URL` de `api/apiClient.js`, que ya respeta `REACT_APP_API_URL` con fallback a localhost. Ver §3.10.

### 9.3 Coordinador de Posgrados bloqueado de Configuración
**Síntoma:** al entrar al sub-menú "Configuración" como rol `POSGRADOS`, la ruta redirigía a `/no-autorizado`.

**Causa:** la ruta `/tramites/admin/configuracion` en `App.js` solo permitía `['ADMIN']`, pero el `menuConfig.js` exponía el ítem para `POSGRADOS`. Inconsistencia heredada.

**Fix:** `rolesPermitidos={['ADMIN', 'POSGRADOS']}`. Ver §3.11 para el debate de fondo sobre unificación de roles.
