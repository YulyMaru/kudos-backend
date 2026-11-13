# 03 · Historias de Usuario — UCO Kudos

**Formato de la tarjeta:** *Como [rol], necesito [funcionalidad], con el fin de [beneficio].*

**Formato de los criterios de aceptación:** *Dado [contexto], cuando [acción], entonces [resultado esperado].*

---

## Épica 1 · Identidad, registro y activación

### HU-01 · Solicitar registro en la plataforma
**Como** persona de la comunidad UCO, **necesito** registrarme con mi correo institucional y mi número celular, **con el fin de** solicitar acceso a la plataforma.

**Criterios de aceptación:**
1. Dado que ingreso un correo con dominio institucional, un número celular válido y el rol solicitado, cuando envío el formulario, entonces se crea una solicitud en estado *Pendiente* y se envían los códigos de verificación al correo y al celular.
2. Dado que ingreso un correo que no pertenece al dominio institucional, cuando envío el formulario, entonces el sistema rechaza el registro e indica que se requiere correo institucional.
3. Dado que el correo o el celular ya están asociados a una cuenta, cuando envío el formulario, entonces el sistema informa que ya existe una cuenta sin revelar datos de terceros.
4. Dado que no acepto la política de tratamiento de datos personales, cuando intento enviar el formulario, entonces el sistema no permite continuar.

### HU-02 · Verificar correo institucional
**Como** solicitante, **necesito** verificar mi correo institucional mediante un código o enlace, **con el fin de** demostrar que el correo me pertenece.

**Criterios de aceptación:**
1. Dado que recibí un código vigente, cuando lo ingreso correctamente, entonces el correo queda marcado como *Verificado*.
2. Dado que el código expiró o es incorrecto, cuando lo ingreso, entonces el sistema lo rechaza y permite solicitar uno nuevo.
3. Dado que fallo 5 intentos consecutivos, cuando intento de nuevo, entonces el sistema bloquea temporalmente la verificación.

### HU-03 · Verificar número celular
**Como** solicitante, **necesito** verificar mi número celular mediante un código, **con el fin de** demostrar que el celular me pertenece.

**Criterios de aceptación:**
1. Dado que recibí un código vigente en mi celular, cuando lo ingreso correctamente, entonces el celular queda marcado como *Verificado*.
2. Dado que el código expiró o es incorrecto, cuando lo ingreso, entonces el sistema lo rechaza y permite solicitar uno nuevo.
3. Dado que solicito reenvíos repetidos, cuando supero el límite permitido, entonces el sistema impide nuevos envíos durante un tiempo.

### HU-04 · Aprobar o rechazar solicitudes de registro
**Como** aprobador (según la tabla de aprobaciones), **necesito** aprobar o rechazar las solicitudes de registro pendientes, **con el fin de** que solo personas legítimas accedan a la plataforma.

**Criterios de aceptación:**
1. Dado que soy Coordinador de Programa, cuando consulto las solicitudes pendientes, entonces veo únicamente las solicitudes de estudiantes de mi programa.
2. Dado que una solicitud corresponde a un rol que no me compete aprobar, cuando intento aprobarla, entonces el sistema lo impide (por ejemplo, un Coordinador de Programa no aprueba a un Coordinador de Facultad).
3. Dado que rechazo una solicitud, cuando confirmo el rechazo, entonces la solicitud queda en estado *Rechazada* con el motivo y se notifica al solicitante.
4. Dado que apruebo o rechazo una solicitud, cuando se guarda la decisión, entonces queda registrada en la auditoría con aprobador, fecha y decisión.

### HU-05 · Activar la cuenta automáticamente
**Como** sistema, **necesito** activar la cuenta únicamente cuando la solicitud esté aprobada y el correo y el celular estén verificados, **con el fin de** garantizar la identidad del usuario.

**Criterios de aceptación:**
1. Dado que la solicitud está aprobada y el correo y el celular están verificados, cuando se cumple la última de las tres condiciones, entonces la cuenta pasa a estado *Activa* y se notifica al usuario.
2. Dado que falta al menos una de las tres condiciones, cuando el usuario intenta iniciar sesión, entonces el sistema le indica qué condición está pendiente.
3. Dado que las condiciones se cumplen en cualquier orden, cuando se completa la última, entonces el resultado es el mismo.

### HU-06 · Iniciar sesión
**Como** usuario activo, **necesito** iniciar sesión de forma segura, **con el fin de** acceder a las funcionalidades según mi rol.

**Criterios de aceptación:**
1. Dado que mis credenciales son correctas y mi cuenta está activa, cuando inicio sesión, entonces recibo un token de acceso con mi rol y accedo a la aplicación.
2. Dado que mis credenciales son incorrectas, cuando inicio sesión, entonces el sistema muestra un mensaje genérico sin indicar qué dato falló.
3. Dado que fallo 5 intentos consecutivos, cuando intento de nuevo, entonces la cuenta se bloquea temporalmente y el intento queda auditado.
4. Dado que mi token expiró, cuando consumo un recurso protegido, entonces el sistema responde *401* y la aplicación me solicita iniciar sesión de nuevo.

### HU-07 · Restablecer contraseña
**Como** usuario, **necesito** restablecer mi contraseña, **con el fin de** recuperar el acceso a mi cuenta.

**Criterios de aceptación:**
1. Dado que solicito restablecer la contraseña con mi correo institucional, cuando envío la solicitud, entonces recibo un enlace de un solo uso con vigencia limitada.
2. Dado que el correo no existe, cuando envío la solicitud, entonces el sistema muestra el mismo mensaje que para un correo existente.
3. Dado que defino una nueva contraseña que cumple la política de seguridad, cuando la confirmo, entonces se invalidan las sesiones anteriores.

---

## Épica 2 · Estructura organizacional

### HU-08 · Gestionar administradores y rector
**Como** administrador, **necesito** gestionar a los administradores y al rector, **con el fin de** mantener el gobierno de la plataforma.

**Criterios de aceptación:**
1. Dado que soy administrador, cuando asigno un nuevo rector, entonces el rector anterior queda inactivo, de modo que solo exista un rector activo.
2. Dado que no soy administrador, cuando intento acceder a esta gestión, entonces el sistema responde *403*.
3. Dado que realizo cualquier cambio, cuando se guarda, entonces queda auditado.

### HU-09 · Gestionar al coordinador académico
**Como** rector, **necesito** gestionar al coordinador académico, **con el fin de** mantener la estructura directiva.

**Criterios de aceptación:**
1. Dado que asigno un nuevo coordinador académico, cuando se guarda, entonces el anterior queda inactivo, de modo que solo exista uno activo.
2. Dado que no soy rector, cuando intento esta gestión, entonces el sistema responde *403*.

### HU-10 · Gestionar coordinadores de facultad
**Como** coordinador académico, **necesito** gestionar a los coordinadores de facultad, **con el fin de** mantener actualizada la estructura de facultades.

**Criterios de aceptación:**
1. Dado que asigno un coordinador a una facultad, cuando se guarda, entonces esa facultad tiene un único coordinador activo.
2. Dado que consulto un coordinador de facultad, cuando veo su detalle, entonces puedo identificar qué facultad acompaña.

### HU-11 · Gestionar coordinadores de programa
**Como** coordinador de facultad, **necesito** gestionar a los coordinadores de programa de mi facultad, **con el fin de** mantener actualizada la estructura de programas.

**Criterios de aceptación:**
1. Dado que asigno un coordinador a un programa de mi facultad, cuando se guarda, entonces ese programa tiene un único coordinador activo.
2. Dado que el programa pertenece a otra facultad, cuando intento asignarle un coordinador, entonces el sistema lo impide.

### HU-12 · Gestionar estudiantes y docentes del programa
**Como** coordinador de programa, **necesito** gestionar a los estudiantes y docentes de mi programa, **con el fin de** administrar mi comunidad académica.

**Criterios de aceptación:**
1. Dado que vinculo un estudiante o docente a mi programa, cuando se guarda, entonces aparece en el listado de mi programa.
2. Dado que un estudiante o docente pertenece a otro programa, cuando intento modificarlo, entonces el sistema solo me permite gestionar su vínculo con mi programa.

### HU-13 · Consultar vinculación académica
**Como** usuario, **necesito** consultar en qué programas estudia un estudiante o enseña un docente, **con el fin de** conocer su vinculación académica.

**Criterios de aceptación:**
1. Dado que un estudiante estudia varios programas, cuando consulto su perfil, entonces veo todos los programas en los que está vinculado.
2. Dado que un docente enseña en varios programas, cuando consulto su perfil, entonces veo todos los programas en los que enseña.

### HU-14 · Garantizar exclusividad y unicidad de roles
**Como** sistema, **necesito** garantizar que cada persona tenga un único rol activo y que exista un único rector, coordinador académico, coordinador por facultad y coordinador por programa activos, **con el fin de** cumplir las reglas organizacionales.

**Criterios de aceptación:**
1. Dado que una persona ya tiene un rol activo, cuando se le intenta asignar otro, entonces el sistema lo rechaza.
2. Dado que llegan dos asignaciones simultáneas para el mismo cargo único, cuando se procesan, entonces solo una queda activa.

---

## Épica 3 · Categorías de Kudos

### HU-15 · Administrar categorías
**Como** administrador, rector o coordinador académico, **necesito** crear, editar y desactivar categorías de Kudos, **con el fin de** alinear los reconocimientos con los valores institucionales.

**Criterios de aceptación:**
1. Dado que tengo uno de los tres roles autorizados, cuando creo una categoría con un nombre que no existe, entonces queda activa y disponible para publicar Kudos.
2. Dado que el nombre de la categoría ya existe, cuando intento crearla, entonces el sistema lo rechaza.
3. Dado que desactivo una categoría, cuando se guarda, entonces deja de ofrecerse al publicar, pero los Kudos existentes la conservan.
4. Dado que tengo cualquier otro rol, cuando intento administrar categorías, entonces el sistema responde *403*.

### HU-16 · Consultar categorías activas
**Como** usuario, **necesito** consultar las categorías activas, **con el fin de** seleccionar una al publicar un Kudo.

**Criterios de aceptación:**
1. Dado que estoy autenticado, cuando consulto las categorías, entonces veo solo las activas, entre ellas las 15 categorías mínimas definidas por la universidad.

---

## Épica 4 · Kudos

### HU-17 · Publicar Kudo
**Como** usuario, **necesito** publicar un Kudo indicando destinatario, categoría y contenido descriptivo sin límite de longitud, **con el fin de** reconocer públicamente a un miembro de la comunidad.

**Criterios de aceptación:**
1. Dado que selecciono un destinatario activo, una categoría activa e ingreso un contenido, cuando publico, entonces el Kudo se almacena con autor, destinatario, categoría, fecha y contenido, y recibo una confirmación.
2. Dado que el contenido es muy extenso, cuando publico, entonces se almacena completo, sin truncarse.
3. Dado que falta el destinatario, la categoría o el contenido, cuando intento publicar, entonces el sistema indica qué campo falta.
4. Dado que intento reconocerme a mí mismo, cuando publico, entonces el sistema lo impide.
5. Dado que el Kudo se almacenó, cuando termina la operación, entonces se dispara el proceso de notificación (HU-19, HU-20 y HU-21) sin que el autor tenga que esperar a que terminen los envíos.

### HU-18 · Adjuntar archivos a un Kudo
**Como** usuario, **necesito** adjuntar archivos a un Kudo, **con el fin de** soportar el reconocimiento con evidencias.

**Criterios de aceptación:**
1. Dado que adjunto archivos de un tipo y tamaño permitidos, cuando publico, entonces quedan almacenados y asociados al Kudo.
2. Dado que el archivo tiene un tipo no permitido (validado por contenido, no solo por extensión) o supera el tamaño máximo, cuando lo adjunto, entonces el sistema lo rechaza con un mensaje claro.
3. Dado que un usuario consulta un adjunto, cuando lo descarga, entonces lo hace mediante un enlace temporal autorizado y la descarga queda registrada.

### HU-19 · Notificar por correo electrónico
**Como** destinatario, **necesito** recibir un correo electrónico cuando me publiquen un Kudo, **con el fin de** enterarme del reconocimiento.

**Criterios de aceptación:**
1. Dado que se publicó un Kudo para mí, cuando se procesa la notificación, entonces recibo en mi correo institucional un mensaje con el autor, la categoría y un enlace al Kudo.
2. Dado que el proveedor de correo falla temporalmente, cuando se procesa el envío, entonces el sistema reintenta automáticamente sin afectar la publicación ni los otros canales.
3. Dado que el envío termina (con éxito o tras agotar los reintentos), cuando finaliza, entonces el resultado queda auditado.

### HU-20 · Notificar por WhatsApp
**Como** destinatario, **necesito** recibir una notificación de WhatsApp cuando me publiquen un Kudo, **con el fin de** enterarme desde mi celular.

**Criterios de aceptación:**
1. Dado que se publicó un Kudo para mí y mi celular está verificado, cuando se procesa la notificación, entonces recibo un mensaje de WhatsApp con el autor y la categoría.
2. Dado que el proveedor de WhatsApp falla temporalmente, cuando se procesa el envío, entonces el sistema reintenta sin afectar los otros canales.
3. Dado que el envío termina, cuando finaliza, entonces el resultado queda auditado.

> **Nota de implementación (13-11-2026):** el canal "WhatsApp" de esta historia se entrega por **SMS** con Traccar SMS Gateway, porque Meta desactivó la cuenta de WhatsApp Business (ADR-006 rechazado, ADR-015 aceptado). El objetivo de la historia, notificar en el celular del destinatario, se mantiene; el adaptador de WhatsApp Cloud API se retiró del código (queda en el historial de Git) y el canal se renombró a `Canal.SMS` con la migración V6.

### HU-21 · Notificar por Push en tiempo real
**Como** destinatario conectado, **necesito** recibir una notificación Push en tiempo real cuando me publiquen un Kudo, **con el fin de** enterarme de inmediato sin recargar la aplicación.

**Criterios de aceptación:**
1. Dado que tengo la aplicación abierta y con sesión iniciada, cuando me publican un Kudo, entonces veo la notificación en pantalla en pocos segundos, sin recargar.
2. Dado que no estoy conectado, cuando me publican un Kudo, entonces no se genera error y el Kudo aparece en mi feed la próxima vez que ingrese.
3. Dado que otro usuario está conectado, cuando me publican un Kudo, entonces él no recibe mi notificación.

### HU-22 · Auditar el proceso de publicación
**Como** administrador, **necesito** que todo el proceso de publicación y notificación quede auditado, **con el fin de** tener trazabilidad completa.

**Criterios de aceptación:**
1. Dado que se publica un Kudo, cuando termina cada paso (almacenamiento, correo, WhatsApp y Push), entonces se registra un evento de auditoría con fecha, actor, acción, identificador del Kudo, canal y resultado.
2. Dado que todos los eventos de un mismo Kudo comparten un identificador de correlación, cuando consulto la auditoría, entonces puedo reconstruir el proceso completo.
3. Dado que un registro de auditoría ya fue creado, cuando alguien intenta modificarlo o eliminarlo, entonces el sistema no lo permite.
4. Dado que se registra un evento, cuando se guarda, entonces no contiene contraseñas, tokens ni el contenido completo de los adjuntos.

### HU-23 · Consultar el feed de Kudos
**Como** usuario, **necesito** consultar el feed de Kudos de forma rápida y paginada, **con el fin de** conocer los reconocimientos de la comunidad.

**Criterios de aceptación:**
1. Dado que ingreso al feed, cuando carga, entonces veo los Kudos más recientes primero, en páginas de tamaño fijo.
2. Dado que me desplazo al final de la página, cuando solicito la siguiente, entonces se cargan más Kudos sin repetir los anteriores.
3. Dado que un Kudo fue vetado, cuando cargo el feed, entonces no aparece.
4. Dado que uso un celular, cuando veo el feed, entonces se adapta a la pantalla sin desplazamiento horizontal.

### HU-24 · Consultar el detalle de un Kudo
**Como** usuario, **necesito** consultar el detalle de un Kudo con sus adjuntos, **con el fin de** ver el reconocimiento completo.

**Criterios de aceptación:**
1. Dado que selecciono un Kudo, cuando abro su detalle, entonces veo autor, destinatario, categoría, fecha, contenido completo y adjuntos.
2. Dado que el Kudo no existe o fue vetado, cuando intento abrirlo, entonces el sistema responde *404*.

---

## Épica 5 · Réplicas y calificaciones

### HU-25 · Replicar a un Kudo o a otra réplica
**Como** usuario, **necesito** replicar a un Kudo o a otra réplica, sin límite de profundidad, cantidad ni longitud, **con el fin de** participar en la conversación.

**Criterios de aceptación:**
1. Dado que estoy viendo un Kudo, cuando publico una réplica, entonces queda asociada al Kudo con autor y fecha.
2. Dado que estoy viendo una réplica, cuando le respondo, entonces mi réplica queda anidada bajo ella, sin importar la profundidad.
3. Dado que un Kudo tiene muchas réplicas, cuando las consulto, entonces se cargan de forma paginada o por niveles para no degradar el rendimiento.

### HU-26 · Calificar un Kudo o una réplica
**Como** usuario, **necesito** calificar un Kudo o una réplica de 1 a 5 estrellas, **con el fin de** valorar su contenido.

**Criterios de aceptación:**
1. Dado que califico un contenido con un valor entre 1 y 5, cuando guardo, entonces la calificación queda registrada y se actualiza el promedio.
2. Dado que envío un valor fuera del rango 1–5, cuando guardo, entonces el sistema lo rechaza.

### HU-27 · Modificar mi calificación
**Como** usuario, **necesito** modificar mi calificación existente, **con el fin de** mantener una única calificación activa por contenido.

**Criterios de aceptación:**
1. Dado que ya califiqué un contenido, cuando lo califico de nuevo, entonces se reemplaza mi calificación anterior y no se crea una segunda.
2. Dado que envío dos calificaciones simultáneas sobre el mismo contenido, cuando se procesan, entonces solo queda una activa.

---

## Épica 6 · Moderación

### HU-28 · Reportar contenido
**Como** usuario, **necesito** reportar un Kudo o una réplica inapropiada, **con el fin de** que sea revisada.

**Criterios de aceptación:**
1. Dado que veo un contenido inapropiado, cuando lo reporto con un motivo, entonces el reporte queda registrado y visible para el Rector y el Coordinador Académico.
2. Dado que ya reporté ese contenido, cuando intento reportarlo de nuevo, entonces el sistema me informa que ya fue reportado por mí.

### HU-29 · Vetar contenido
**Como** rector o coordinador académico, **necesito** vetar un Kudo o una réplica, **con el fin de** retirar contenido que no respete los valores institucionales.

**Criterios de aceptación:**
1. Dado que soy Rector o Coordinador Académico, cuando veto un contenido indicando el motivo, entonces deja de mostrarse a los usuarios y el veto queda auditado.
2. Dado que tengo cualquier otro rol (incluido Administrador), cuando intento vetar, entonces el sistema responde *403*.
3. Dado que un contenido es vetado, cuando se aplica el veto, entonces el contenido no se elimina físicamente, para conservar la trazabilidad.

---

## Épica 7 · Operación y observabilidad

### HU-30 · Consultar la auditoría
**Como** administrador, **necesito** consultar el registro de auditoría, **con el fin de** investigar incidentes y verificar operaciones.

**Criterios de aceptación:**
1. Dado que soy administrador, cuando consulto la auditoría, entonces puedo filtrar por fecha, actor, acción y recurso.
2. Dado que tengo otro rol, cuando intento consultarla, entonces el sistema responde *403*.

### HU-31 · Monitorear la plataforma
**Como** administrador, **necesito** monitorear métricas, logs y trazas de la plataforma, **con el fin de** detectar y diagnosticar fallas.

**Criterios de aceptación:**
1. Dado que la plataforma está en ejecución, cuando consulto el endpoint de salud, entonces obtengo el estado de la aplicación y de sus dependencias.
2. Dado que ocurre una petición, cuando reviso los logs, entonces cada línea está estructurada e incluye un identificador de traza que permite seguir la petición de punta a punta.
3. Dado que se publican Kudos, cuando consulto las métricas, entonces veo los Kudos publicados y las notificaciones enviadas y fallidas por canal.
