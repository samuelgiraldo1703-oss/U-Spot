# Tutorial: crear la clave de Gemini (API key) y agregarla a U-Spot

Tiempo estimado: 10 minutos. Solo necesitas una cuenta de Google (puede ser tu Gmail) y Android Studio.

> **¿Qué es una API key?** Es una contraseña larga que identifica a U-Spot ante Google. Cada vez que
> alguien le escribe a Spotty, la app manda la pregunta a Gemini junto con esta clave, y Google
> responde. Quien tenga la clave puede gastar tu cuota, por eso se guarda como una contraseña.

---

## ⚠️ Antes de empezar: NO actives la facturación

No hace falta tarjeta para que Spotty funcione. Así queda U-Spot:

| | Sin facturación (lo que vamos a hacer) | Con facturación |
|---|---|---|
| Costo | **$0 siempre**. Google no tiene cómo cobrarte | Pago anticipado de mínimo **5 USD** y luego cobro por cada mensaje |
| Si se acaba la cuota del día | Spotty muestra "intenta más tarde" y al otro día vuelve a funcionar | Sigue funcionando y sigue cobrando |
| Si alguien roba la clave | Solo gasta tu cuota gratis | Puede generar cobros a tu tarjeta |
| Mensajes usados para mejorar Google | Sí (no enviamos datos personales) | No |

Si en algún momento AI Studio te muestra botones como **"Set up billing"**, **"Configurar
facturación"**, **"Upgrade"** o te pide una tarjeta, **no los toques**. Ninguno es necesario.

---

## Paso 1. Entrar a Google AI Studio

1. Abre Chrome y entra a **https://aistudio.google.com**
2. Si no has iniciado sesión, haz clic en **Sign in** e inicia sesión con tu cuenta de Google.
   - Usa una cuenta personal (Gmail). Las cuentas de la universidad a veces tienen bloqueado AI
     Studio o no dejan crear claves.
3. La primera vez aparece una ventana con los **Términos de servicio**:
   - Marca la casilla de aceptar los términos (es obligatoria).
   - Las casillas de recibir correos o participar en investigaciones son opcionales; puedes
     dejarlas sin marcar.
   - Haz clic en **Continue** / **Aceptar**.

> AI Studio puede verse en inglés aunque tu Chrome esté en español. En este tutorial pongo los
> nombres de los botones en inglés y, entre paréntesis, cómo podrían aparecer en español.

## Paso 2. Ir a la página de claves

1. En el panel de la izquierda busca **Get API key** (*Obtener clave de API*). También puedes
   entrar directo a **https://aistudio.google.com/apikey**
2. Verás una tabla de claves. Puede pasar una de dos cosas:
   - **La tabla ya tiene una clave**: Google crea un proyecto y una clave por defecto a las cuentas
     nuevas al aceptar los términos. Puedes usar esa (salta al Paso 4) o crear una nueva (Paso 3).
   - **La tabla está vacía**: sigue con el Paso 3.

## Paso 3. Crear la clave

1. Haz clic en el botón azul **Create API key** (*Crear clave de API*), arriba a la derecha.
2. Se abre una ventana que pregunta en qué **proyecto** crearla. Un proyecto es solo una carpeta de
   Google Cloud que agrupa la clave y su cuota.
   - Si te ofrece **Create API key in new project** (*Crear clave en un proyecto nuevo*), elígelo.
   - Si te pide escribir un nombre de proyecto, pon **U-Spot Spotty**.
   - Si te muestra un proyecto que ya existe (por ejemplo "Default Gemini Project"), puedes
     elegirlo.
   - **No elijas el proyecto de Firebase de U-Spot** aunque aparezca en la lista: ese proyecto
     podría tener otras configuraciones y es mejor mantener Spotty aparte.
3. Si te pide un nombre para la clave, escribe **Spotty Android**.
4. Haz clic en **Create key** (*Crear clave*). En unos segundos aparece la clave: una cadena larga
   de letras y números.
   - Si aparece el mensaje *"You do not have permission to create a key in this project"*, estás
     usando una cuenta de organización (universidad o empresa). Cierra sesión y repite con tu Gmail
     personal.

## Paso 4. Restringir la clave (recomendado, 30 segundos)

Esto hace que, si alguien obtiene la clave, solo pueda usarla para Gemini y no para otros
servicios de Google.

1. En la tabla de claves, busca tu clave. Si al lado dice **Unrestricted** (*Sin restricciones*),
   pasa el mouse encima de esa etiqueta.
2. Haz clic en **Add restrictions** (*Agregar restricciones*).
3. Elige **Restrict to Gemini API only** (*Restringir solo a la API de Gemini*).
4. Haz clic en **Restrict key** (*Restringir clave*).

Si no ves la etiqueta "Unrestricted", la clave ya viene restringida y no tienes que hacer nada.

## Paso 5. Copiar la clave

1. En la fila de tu clave, haz clic en el ícono de **copiar** (dos cuadritos) o en la clave misma
   y luego en **Copy key** (*Copiar clave*).
2. Mientras la pegas en el proyecto (Paso 6), **no la pegues en ningún otro lugar**:
   - ❌ No me la mandes por este chat.
   - ❌ No la mandes por WhatsApp ni la pongas en capturas de pantalla.
   - ❌ No la escribas en ningún archivo `.kt`, ni en el README, ni en Git.

Si en algún momento sospechas que la clave se filtró: vuelve a https://aistudio.google.com/apikey,
bórrala con el ícono de la papelera y crea una nueva (Pasos 3 a 6).

## Paso 6. Agregar la clave al proyecto en Android Studio

La clave va en el archivo **`local.properties`**, que existe solo en tu computador: Git lo ignora,
así que nunca se sube a GitHub.

1. Abre el proyecto **U-Spot** en Android Studio.
2. En el panel izquierdo, con la vista **Android** seleccionada (arriba del panel), despliega
   **Gradle Scripts**.
3. Haz doble clic en **local.properties (SDK Location)**.
   - Si no lo ves: cambia la vista de **Android** a **Project** en el menú de arriba del panel,
     abre la carpeta raíz `U-Spot` y haz doble clic en `local.properties`.
4. El archivo ya tiene una línea que empieza por `sdk.dir=`. **No la borres ni la cambies.**
5. Ve al final del archivo, presiona **Enter** para crear una línea nueva y escribe exactamente
   esto, pegando tu clave después del signo igual (sin espacios y sin comillas):

   ```properties
   GEMINI_API_KEY=pega_aqui_tu_clave
   ```

   Debe quedar más o menos así (con tu clave real en vez de las X):

   ```properties
   sdk.dir=D\:\\Android\\Sdk
   GEMINI_API_KEY=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   ```

6. Guarda con **Ctrl + S**.
7. Arriba aparecerá una barra amarilla o azul que dice **Sync Now** (*Sincronizar ahora*); haz clic
   en ella. Si no aparece, usa el menú **File → Sync Project with Gradle Files**.

¡Listo! Avísame en el chat con un **"ya está la clave"** (sin la clave). Yo verifico que funcione
leyéndola desde el archivo sin mostrarla, y confirmo cuál es el modelo Flash-Lite exacto y la cuota
diaria que Google le asignó a tu proyecto.

## Paso 7 (opcional). Ver tu cuota gratis

1. Entra a **https://aistudio.google.com/rate-limit** (o en el panel izquierdo: **Dashboard →
   Usage / Rate limits**).
2. Busca la fila de **Gemini Flash-Lite**. Las columnas son:
   - **RPM**: preguntas por minuto.
   - **RPD**: preguntas por día (se reinicia a la medianoche del Pacífico: 2:00 a. m. hora
     Colombia hasta el 1 de noviembre y 3:00 a. m. desde el 2 de noviembre hasta marzo).
   - **TPM**: cantidad de texto por minuto.
3. Si quieres, dime el número de RPD que ves: con eso ajusto cuántas preguntas puede hacer cada
   usuario antes de que Spotty avise que se acabó la cuota del día.

---

## Para tus compañeros de equipo

Cada integrante que compile la app en su computador necesita la línea `GEMINI_API_KEY=` en **su
propio** `local.properties` (Git no lo copia). Dos opciones:

- **Compartir tu clave** por un canal privado (mensaje directo, no un grupo). Todos gastan la misma
  cuota diaria.
- **Que cada uno cree la suya** con este tutorial. Cada clave tiene su propia cuota, pero el APK
  usa la clave de quien lo compiló.

El APK que se instala en los celulares lleva la clave de quien lo compiló; las personas que solo
instalan el APK no tienen que hacer nada.

## Problemas frecuentes

| Problema | Solución |
|---|---|
| No aparece "Create API key" o sale un error de permisos | Usa tu Gmail personal, no la cuenta de la universidad |
| AI Studio dice que no está disponible en tu país | Colombia está soportado; revisa que no tengas una VPN activa |
| Android Studio no muestra "Sync Now" | Menú **File → Sync Project with Gradle Files** |
| Spotty responde "se alcanzó el límite de hoy" | Es la cuota gratis; vuelve a funcionar al día siguiente. **No actives facturación** |
| Borré `sdk.dir` sin querer | Ciérralo sin guardar (o usa Ctrl + Z) y avísame |
