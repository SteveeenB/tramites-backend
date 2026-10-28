# Configuración de Pagos con Wompi

## Requisitos previos

- Cuenta activa en [comercios.wompi.co](https://comercios.wompi.co)
- Checkout habilitado por Wompi (solicitarlo al soporte si no está activo)
- Backend desplegado o corriendo localmente

---

## Paso 1 — Obtener las llaves

1. Ingresar a `comercios.wompi.co`
2. Ir a **Desarrollo → Programadores**
3. Copiar las 4 llaves según el ambiente:

| Llave | Sandbox (pruebas) | Producción |
|---|---|---|
| Pública | `pub_test_...` | `pub_prod_...` |
| Privada | `prv_test_...` | `prv_prod_...` |
| Integridad | `test_integrity_...` | `prod_integrity_...` |
| Eventos | `test_events_...` | `prod_events_...` |

---

## Paso 2 — Configurar el backend

### En local

Abrir `src/main/resources/application-local.properties` y reemplazar
los valores de Wompi con las llaves copiadas:

```properties
wompi.public-key=pub_test_XXXXXXXXXXXXXXXXXXXX
wompi.private-key=prv_test_XXXXXXXXXXXXXXXXXXXX
wompi.integrity-key=test_integrity_XXXXXXXXXXXX
wompi.events-secret=test_events_XXXXXXXXXXXX
wompi.redirect-url=http://localhost:3000/pago/resultado
```

### En producción (Render / Railway)

Agregar estas variables de entorno en el panel del servidor:

| Variable | Valor |
|---|---|
| `WOMPI_PUBLIC_KEY` | Llave pública de producción |
| `WOMPI_PRIVATE_KEY` | Llave privada de producción |
| `WOMPI_INTEGRITY_KEY` | Llave de integridad de producción |
| `WOMPI_EVENTS_SECRET` | Secreto de eventos de producción |
| `WOMPI_REDIRECT_URL` | `https://tu-dominio.com/pago/resultado` |

---

## Paso 3 — Configurar el webhook

El webhook permite que Wompi notifique al sistema cuando un pago es
aprobado o rechazado.

1. En el panel de Wompi ir a **Desarrollo → Programadores**
2. En el campo **URL de Eventos** ingresar:
   ```
   https://tu-backend.com/api/pagos/webhook
   ```
3. Hacer clic en **Guardar**

> Para pruebas en local el webhook no es obligatorio. El sistema
> actualiza el estado del pago a través de la URL de retorno
> automáticamente.

---

## Tarjetas de prueba

Para probar pagos en el ambiente sandbox usar estas tarjetas:

| Resultado | Número de tarjeta | Vencimiento | CVV |
|---|---|---|---|
| ✅ Pago aprobado | `4242 4242 4242 4242` | Cualquier fecha futura | Cualquier 3 dígitos |
| ❌ Pago rechazado | `4111 1111 1111 1111` | Cualquier fecha futura | Cualquier 3 dígitos |

El nombre del titular y demás datos pueden ser cualquier valor en sandbox.

---

## Pagos disponibles en el sistema

| Concepto | Monto | Cuándo aplica |
|---|---|---|
| Terminación de Materias | $150.000 COP | Al crear la solicitud de terminación |
| Derechos de Grado | $250.000 COP | Al ser aprobada la solicitud de grado |
| Derechos de Ceremonia | $40.000 COP | Al elegir fecha de ceremonia de grado |

---

## Flujo del pago

```
Estudiante hace clic en "Pagar con PSE"
  → Sistema crea referencia única y redirige a Wompi
  → Estudiante completa el pago en la página de Wompi
  → Wompi redirige de vuelta al sistema con el resultado
  → Sistema muestra confirmación y actualiza el estado del trámite
```

