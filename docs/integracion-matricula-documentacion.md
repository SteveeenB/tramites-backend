# Integración Trámites ↔ Matrícula · Cambios a la documentación (N10)

> Épica TP-138 (SC-03) · Subtarea N10 del plan (`docs/integracion-matricula-plan.md`, sección 9) · 2 nov 2026
> **Estado:** borrador listo para pegar. Refleja lo que ya está implementado en `develop` (PR #49 a #52 del backend y #44 del frontend), no lo que se proponía antes.

## 0. Dónde va cada cosa

| Documento | Ubicación encontrada | Qué hay que hacer |
|---|---|---|
| ADD (EP4) | Drive: `EP4- ADD -Arquitectura de alto nivel -Modelo del Negocio - Tramites de Postgrados v2.docx` (junio 2026) | Pegar los textos de la sección 1 en las secciones indicadas |
| Casos de uso (EP3) | No existe para Trámites. El `EP3_Casos_de_Uso.docx` de la carpeta «4.3. Casos de uso» es del grupo jurídico, al que le hacemos auditoría | El caso de la sección 2 puede ser el primero de un EP3 de Trámites. Se mantiene el número CU-05 porque RF16 y TP-143 ya lo citan |
| SRS (EP2) | Probablemente no existe como documento aparte. Los requisitos viven en el informe final (Tabla 12) y en el informe TP-143 | Aplicar los cambios de la sección 3 donde estén los requisitos |
| TP-145 (EIF) | Jira | Usar la propuesta de la sección 4 como punto de partida |

**Corrección en el ADD:** el documento dice PostgreSQL en el glosario, en las tecnologías y en la capa de infraestructura. La base de datos es MySQL desde SC-01 (AD-CCA-07), y Supabase solo se usa como Storage. Hay que corregirlo en la misma edición.

**Numeración decidida por el PO (2 nov 2026):**
- El requisito no funcional de interoperabilidad es **RNF-09** (el informe final ya usa RNF-06 para Mantenibilidad). En Jira se sigue llamando RNF06; se anota «RNF06 (RNF-09 en el SRS)».
- El riesgo de dependencia de Matrícula es **R8**, como ya lo registra la versión corregida de AD-CCA-07 (SC-03). R7 sigue siendo el riesgo de las llaves de Wompi.

---

## 1. ADD (EP4)

### 1.1 «1. Introducción → Alcance» (reemplazar la última frase)

> Este documento tampoco cubre la arquitectura interna de la plataforma de posgrados. Sí describe la integración con el **Módulo de Matrícula** (SC-03): Trámites lo consulta en modo de solo lectura, mediante un contrato REST versionado, para validar la situación académica del estudiante antes de iniciar un trámite.

### 1.2 «Glosario» (agregar)

- **Módulo de Matrícula:** sistema de otro equipo que gestiona la admisión de posgrados hasta la legalización del admitido y la creación del estudiante. Es el proveedor de los datos de identidad, programa, cohorte y estado del estudiante.
- **Contrato de interfaz (v1):** acuerdo entre los dos equipos sobre las rutas, los campos, los formatos y la autenticación de la integración. Un cambio incompatible exige una versión nueva (`/v2`).
- **Cuenta técnica:** usuario de Matrícula que no corresponde a una persona sino al backend de Trámites. Tiene el rol `SERVICIO_TRAMITES`, que solo permite consultar la situación académica por cédula.
- **Copia del estudiante (snapshot):** datos del estudiante guardados en Trámites tras la última consulta exitosa a Matrícula, con la hora de esa consulta. Permite seguir operando por un tiempo limitado si Matrícula no responde.
- **Cold start:** tiempo que tarda en responder un servicio del plan gratuito de Render cuando estaba dormido (alrededor de 50 s).

### 1.3 «2. Visión General → Tecnologías Clave» (fila de Infraestructura)

| Capa | Tecnologías |
|---|---|
| Infraestructura | Spring Data JPA, **MySQL**, JavaMailSender, **Spring RestClient (Módulo de Matrícula)**, Supabase Storage, iText 7, ZXing 3.5.2 |

### 1.4 «Consideraciones de Calidad» (agregar)

> **Interoperabilidad y disponibilidad (RNF-09):** la integración con Matrícula es de backend a backend y la credencial nunca llega al navegador. Cada consulta tiene 2 s de conexión y 5 s de respuesta, hasta 2 reintentos solo ante 5xx o timeout, y un presupuesto total de 15 s. Si Matrícula no responde, se usa la copia del estudiante si tiene menos de 24 horas; si no, se bloquea solo el inicio de trámites nuevos, y los trámites ya iniciados siguen su curso. Cada llamada queda registrada con ruta, cédula, intento, código HTTP y duración.

### 1.5 «Riesgos Técnicos Iniciales» (actualizar y agregar)

- **Desconocimiento de la plataforma existente** → agregar al final: *«Para el Módulo de Matrícula el riesgo se mitigó: se analizó su código (TP-143), se acordó un contrato v1 y se implementó un adaptador que aísla sus nombres de campos (RNF06).»*
- **R8 · Dependencia de un servicio y un equipo externos (nuevo).** Trámites depende de que Matrícula esté disponible y mantenga el contrato. Matrícula corre en el plan gratuito de Render, con cold start de unos 50 s. *Mitigación:* adaptador con modo simulado (`matricula.modo=mock`), copia del estudiante con vigencia de 24 h, warm-up al iniciar sesión, contrato versionado `/v1` y bloqueo solo del inicio de trámites nuevos. (Numerado R8 en AD-CCA-07; R7 es el riesgo de las llaves de Wompi.)
- **Copia desactualizada del estudiante (nuevo).** Durante una caída, la copia puede tener hasta 24 h de antigüedad. *Mitigación:* la vigencia es configurable (`MATRICULA_SNAPSHOT_VIGENCIA_HORAS`) y un 404 de Matrícula bloquea aunque haya copia.
- **Créditos aprobados sin fuente oficial (nuevo).** Ni Matrícula ni Trámites registran materias ni notas, así que los créditos aprobados se precargan en la base de datos de Trámites. *Mitigación:* decisión documentada del PO (plan, 7.1.4); si en el futuro aparece una fuente institucional, se integra en `AvanceAcademicoService` sin tocar el resto del código.

### 1.6 «3. Descripción de las Capas → Capa de Aplicación» (agregar a los servicios)

- **AvanceAcademicoService:** único punto de cálculo del requisito de créditos (aprobados frente a requeridos). Lo usan la terminación de materias, el grado y la vista del proceso de grado.
- **SincronizacionMatriculaService:** antes de crear una terminación, un grado o un certificado, consulta a Matrícula y actualiza la copia del estudiante; aplica la contingencia ante caídas y hace el warm-up al iniciar sesión.

### 1.7 «3. Descripción de las Capas → Capa de Infraestructura» (agregar)

> **Adaptador del Módulo de Matrícula (`MatriculaClient`):** puerto con dos implementaciones, elegidas con `matricula.modo`. `MatriculaHttpClient` consume el API real: inicia sesión con la cuenta técnica (`POST /auth/login`), guarda el token en memoria hasta poco antes de que venza (120 min) y lo renueva ante un 401/403. Consulta `GET /api/application/case/integracion/v1/estudiantes/{cedula}` (C1) y `GET /actuator/health` (C2), y traduce la respuesta a `SituacionAcademica`, el modelo propio de Trámites. `MatriculaMockClient` devuelve datos fijos para desarrollo y pruebas. El resto del sistema solo conoce el puerto, nunca los nombres de campos de Matrícula.

### 1.8 Sección nueva: «Decisiones de arquitectura de SC-03»

| ID | Decisión | Alternativas descartadas y motivo |
|---|---|---|
| ADR-SC03-1 | Integración **REST síncrona y de solo lectura**, de Trámites hacia Matrícula, con una sola consulta por cédula (C1) que trae toda la situación del estudiante | Llamar a varias rutas existentes de Matrícula (más llamadas y rutas de otros roles). Replicar su base de datos (acoplamiento fuerte con su modelo) |
| ADR-SC03-2 | **Créditos aprobados precargados** en la base de datos de Trámites (`estudiante.creditos_aprobados`); los requeridos los define Matrícula por programa | Pedirlos a Matrícula (no tiene materias ni notas). Registro académico institucional (no existe una fuente disponible hoy) |
| ADR-SC03-3 | **Cuenta técnica con rol propio** `SERVICIO_TRAMITES`, que solo puede hacer `GET /integracion/v1/**`; login con el `/auth/login` de Matrícula | Cuenta con rol Super Administrador (privilegio excesivo). API key fija (Matrícula debía crear un filtro nuevo y la clave no caduca) |
| ADR-SC03-4 | **Copia del estudiante con vigencia de 24 h** como contingencia; sin copia vigente se bloquea solo el inicio de trámites nuevos | Bloquear todo ante cualquier caída (afecta trámites en curso). Usar datos simulados (prohibido por RF16) |

### 1.9 Sección nueva: «Despliegue»

> Trámites y Matrícula son dos servicios independientes en Render (plan gratuito), cada uno con su propia base de datos MySQL. Se comunican por HTTPS con el contrato v1. La integración se configura en el backend de Trámites con variables de entorno, nunca en el repositorio: `MATRICULA_MODO` (`mock` o `http`), `MATRICULA_URL` (incluye el prefijo `/posgrados-project`), `MATRICULA_USUARIO` y `MATRICULA_PASSWORD` (cuenta técnica), `MATRICULA_SINCRONIZAR` (activa la copia y la contingencia) y `MATRICULA_SNAPSHOT_VIGENCIA_HORAS` (24). C2 (`/actuator/health`) sirve como health check y para el warm-up.

---

## 2. Caso de uso (EP3), en el formato de la plantilla

### CU-05 — Consultar la situación académica en el Módulo de Matrícula

**1. Datos generales**

| | |
|---|---|
| CU-Id: | CU-05 |
| Nombre del caso de uso: | Consultar la situación académica en el Módulo de Matrícula |
| Módulo/Épica asociada: | SC-03 · Interoperabilidad con el Módulo de Matrícula (TP-138) |
| Actor(es) principal(es): | Sistema de Trámites (consumidor). Actor secundario: Módulo de Matrícula (proveedor) |
| Stakeholders interesados: | Estudiante, Director de Programa, Oficina de Posgrados, equipo de Matrícula |
| Prioridad: | Alta (RF16, prioridad 5 – Crítico) |
| Estado: | Implementado con Matrícula simulado; pendiente la prueba contra el servicio real |

**2. Objetivo y alcance**

| | |
|---|---|
| Objetivo del CU: | Validar con los datos oficiales de Matrícula que el estudiante está registrado antes de iniciar un trámite, y mantener una copia de sus datos para seguir operando si Matrícula no responde. |
| Alcance / Límites: | Consulta de solo lectura por cédula, actualización de la copia del estudiante y contingencia ante caídas. No incluye el cálculo de créditos aprobados (vienen precargados en Trámites) ni escribir en Matrícula. |

**3. Suposiciones y restricciones**

| | |
|---|---|
| Suposiciones: | La cédula es la llave común entre los dos módulos. Basta con que el estudiante exista en Matrícula; no se valida la matrícula de cada semestre. |
| Restricciones: | Solo lectura. La credencial de la cuenta técnica nunca llega al navegador. Tiempo máximo de 15 s por consulta. |

**4. Precondiciones y postcondiciones**

| | |
|---|---|
| Precondiciones: | Contrato v1 vigente. Cuenta técnica creada en Matrícula y configurada en Trámites. El estudiante inició sesión y tiene cédula. |
| Postcondiciones de éxito: | La copia del estudiante queda actualizada con la hora de la consulta, y el trámite continúa con las reglas de negocio de Trámites. |
| Postcondiciones de fallo: | No se inicia el trámite y el estudiante ve la causa. Los trámites ya iniciados no se afectan. |

**5. Flujo principal (alto nivel)**

1. El estudiante inicia una terminación de materias, un grado o un certificado.
2. Trámites consulta a Matrícula la situación académica de la cédula del estudiante.
3. Matrícula responde con identidad, programa, cohorte y estado.
4. Trámites valida que la respuesta cumpla el contrato y sea de la misma cédula.
5. Trámites actualiza la copia del estudiante y los créditos requeridos del programa.
6. El trámite continúa con sus validaciones propias (por ejemplo, el 100 % de créditos).

**6. Flujos alternativos y excepciones**

- **FA-1 · Inicio de sesión:** al iniciar sesión un estudiante, Trámites comprueba en segundo plano si Matrícula responde (C2) y, si responde, actualiza la copia (C1). El inicio de sesión no espera.
- **FA-2 · Token vencido (401/403):** Trámites vuelve a iniciar sesión con la cuenta técnica una sola vez y repite la consulta.
- **EX-1 · No figura en Matrícula (404):** no se inicia el trámite: «El estudiante no aparece como admitido y legalizado en el Módulo de Matrícula». Aplica aunque haya una copia vigente.
- **EX-2 · Matrícula no responde (timeout, 5xx o cold start):** hasta 2 reintentos dentro de 15 s. Si persiste y la copia tiene menos de 24 h, se usa la copia. Si no hay copia vigente: «La validación académica no está disponible en este momento. Intenta más tarde».
- **EX-3 · Respuesta fuera de contrato o de otra cédula:** se descarta sin guardar y se trata como EX-2.
- **EX-4 · Cédula con formato inválido:** no se consulta a Matrícula y no se inicia el trámite.

**7. Reglas de negocio**

- RB-1: Trámites nunca escribe en Matrícula.
- RB-2: Ningún trámite se habilita con datos simulados (RF16).
- RB-3: Los trámites ya iniciados no dependen de la disponibilidad de Matrícula.
- RB-4: Los créditos requeridos los define Matrícula; si difieren de los de Trámites, gana Matrícula.

**8. Datos y validaciones clave**

| | |
|---|---|
| Entradas principales: | Cédula del estudiante autenticado. |
| Validaciones: | Cédula numérica de 5 a 15 dígitos; campos obligatorios del contrato; fechas ISO 8601; la cédula de la respuesta coincide con la consultada. |

**9. Criterios de aceptación (alto nivel)**

- CA-1: Given un estudiante registrado en Matrícula, When inicia un trámite, Then su copia queda actualizada y el trámite sigue sus reglas.
- CA-2: Given una cédula que no existe en Matrícula, When el estudiante inicia un trámite, Then no se inicia y el mensaje lo explica.
- CA-3: Given Matrícula caída y una copia de menos de 24 h, When el estudiante inicia un trámite, Then se usa la copia y la respuesta llega en 15 s o menos.
- CA-4: Given Matrícula caída y sin copia vigente, When el estudiante inicia un trámite, Then no se inicia y recibe un mensaje claro.

**10. Referencias de interfaz**

| | |
|---|---|
| Boceto / Wireframe relacionado: | Sin pantalla propia: el resultado se ve en el error de las pantallas de terminación, grado y certificados. |
| Mensajes clave: | «El estudiante no aparece como admitido y legalizado en el Módulo de Matrícula.» «La validación académica no está disponible en este momento. Intenta más tarde.» |

**11. Consideraciones no funcionales**

- Seguridad: cuenta técnica con rol `SERVICIO_TRAMITES` (solo `GET /integracion/v1/**`), credenciales en variables de entorno.
- Disponibilidad: umbrales de RNF-09 (2 s, 5 s, 2 reintentos, 15 s) y copia con vigencia de 24 h.
- Trazabilidad: log `[MATRICULA]` por llamada con ruta, cédula, intento, código HTTP y duración.

**12. Dependencias y trazabilidad**

| | |
|---|---|
| Depende de: | CU de inicio de sesión; contrato v1 (TP-144). |
| Historias de usuario relacionadas: | HU-19 (TP-140). |
| Requisitos SRS asociados: | RF16, RNF-09 (RNF06 en Jira), riesgo R8. |

**13. Preguntas abiertas / decisiones pendientes**

- PO-1: Ninguna pendiente (numeración decidida el 2 nov 2026: RNF-09 y R8).

**14. Aprobaciones**

| | |
|---|---|
| Product Owner: | |
| Profesor/Coach: | |

---

## 3. Requisitos (informe final, Tabla 12, y TP-143)

1. **Numeración del RNF de interoperabilidad (decidido):** **RNF-09**. En Jira se anota «RNF06 (RNF-09 en el SRS)» para no renombrar issues.
2. **RF16 · ajustes a la versión de TP-143:**
   - *Descripción:* reemplazar «la identidad, el programa, la admisión, la matrícula y los créditos del estudiante» por «la identidad, el programa, la cohorte y el estado del estudiante, y los créditos requeridos del programa». Los créditos aprobados no vienen de Matrícula (ADR-SC03-2).
   - *Flujo normal, paso 4:* «Evalúa las reglas: el estudiante existe en Matrícula y sus créditos aprobados (precargados en Trámites) cubren los requeridos».
   - *Flujo alterno nuevo:* «Matrícula no responde y existe una copia de menos de 24 h → se usa la copia».
3. **RNF-09 (interoperabilidad):** agregar la fila *Contingencia con copia: vigencia de 24 h, configurable*. Los demás umbrales quedan como en TP-143.
4. **Riesgo R8 (decidido):** dependencia del Módulo de Matrícula, como en AD-CCA-07. Donde TP-143 dice «R7», leer «R8».

**Estado de los criterios de aceptación de RF16**

| Criterio (TP-143) | Estado | Evidencia |
|---|---|---|
| Habilita Terminación sin usar DEMO_USERS | Cumplido | Backend: `AvanceAcademicoService` (#49). Frontend: TP-147 (#44) |
| Con créditos incompletos indica cuántos faltan | Cumplido | «Te faltan N créditos (llevas X de Y)» (#49) |
| Cédula inexistente: no habilita y lo explica | Cumplido con mock | `IntegracionMatriculaHttpTest` (#52) |
| Matrícula caída: nada con datos simulados y mensaje claro | Cumplido con mock | `SincronizacionMatriculaServiceTest` (#51), `IntegracionMatriculaHttpTest` (#52) |
| Los trámites ya iniciados siguen | Cumplido | La validación solo está en la creación de terminación, grado y certificado (#51) |
| Cada consulta registrada con fecha, cédula, código HTTP y duración | Cumplido | Log `[MATRICULA]` de `MatriculaHttpClient` (#50) |
| Sin lecturas de DEMO_USERS en producción | Cumplido | Frontend #44 |
| Prueba contra el servicio real | **Pendiente** | Requiere la URL de Matrícula (o la copia en Render) y la cuenta técnica |

---

## 4. TP-145 · Propuesta de conteo del EIF (IFPUG, sin ajustar)

> Propuesta del equipo de desarrollo para revisar en TP-145. Cuenta solo los datos que Trámites usa, según la regla de IFPUG para los DET de un EIF.

| Función | Tipo | RET / FTR | DET | Complejidad | PF |
|---|---|---|---|---|---|
| Situación académica del estudiante (Matrícula) | EIF | 2 RET (estudiante con su admisión; programa) | 11 (cédula, código, nombres, apellidos, email, estado, código, nombre y créditos requeridos del programa, cohorte, fecha de ingreso) | Baja (2-5 RET, 1-19 DET) | 5 |
| Sincronizar la copia del estudiante | EI (los datos cruzan la frontera y mantienen ILF de Trámites) | 2 FTR (Estudiante, ProgramaAcademico) | 11 | Media (2 FTR, 5-15 DET) | 4 |
| **Total nuevo** | | | | | **9** |

Notas:
- Las solicitudes de terminación, grado y certificado (EI ya contadas) ahora referencian también el EIF de Matrícula. Si su complejidad sube por ese FTR adicional, hay que recalcularlas en TP-145.
- C2 (health check) no se cuenta: no entrega datos de negocio.
- El login técnico es un mecanismo de seguridad, no una función de usuario, así que tampoco se cuenta.
