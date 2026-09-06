# Configuración SMTP (correo) — QA y otros entornos

El backend envía correo vía `JavaMailSender` (Spring Mail). La configuración vive en
`application.properties` y se resuelve **solo por variables de entorno** — no hay
credenciales en el repositorio (ver fix S-06).

## Variables de entorno

| Variable        | Requerida | Default (si falta)  | Descripción                                            |
|-----------------|-----------|---------------------|--------------------------------------------------------|
| `MAIL_HOST`     | No        | `smtp.gmail.com`    | Host del servidor SMTP.                                |
| `MAIL_PORT`     | No        | `587`               | Puerto SMTP (STARTTLS).                                |
| `MAIL_USERNAME` | **Sí**    | — (sin default)     | Usuario/cuenta remitente. Sin valor la app no arranca. |
| `MAIL_PASSWORD` | **Sí**    | — (sin default)     | Contraseña o **app password** de la cuenta.            |

> `MAIL_USERNAME` y `MAIL_PASSWORD` **no tienen default a propósito** (fail-fast): si
> faltan en QA/producción, la app falla al arrancar en lugar de enviar con una cuenta
> equivocada. Mismo criterio que las llaves de Wompi (riesgo R7).

## QA

Definir en el panel de variables de entorno del servicio de QA (p. ej. Render):

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<cuenta-remitente-qa@dominio>
MAIL_PASSWORD=<app-password-de-la-cuenta>
```

Con Gmail hay que generar un **App Password** (Cuenta Google → Seguridad →
Verificación en 2 pasos → Contraseñas de aplicaciones); la contraseña normal no funciona
con SMTP.

## Local

Para desarrollo local, `application-local.properties` (gitignored) ya define
`MAIL_USERNAME=` / `MAIL_PASSWORD=` vacíos: la app arranca y, si intenta enviar, el fallo
se registra en el log sin tumbar el proceso. Para envío real en local, rellénalos ahí.

## Seguridad

El app password de Gmail `jcro qiou zyob bgzb` estuvo hardcodeado en `application.properties`
y quedó expuesto en el historial de git. **Debe rotarse** en la cuenta de Gmail
correspondiente (revocar esa app password y generar una nueva).
