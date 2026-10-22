# Plan de implementación: Spotty, el chatbot de U-Spot

Decisiones ya tomadas (21-oct-2026): Gemini Flash-Lite con una clave de AI Studio **sin facturación**
(costo $0), clave en `local.properties`, búsqueda sobre los 171 lugares de `places.json` mediante
*function calling*, GPS o barrio escrito, historial en Firestore, "Ver detalles" en lugar de
"Reservar mesa", % Match calculado + ★ de comentarios reales, precio por persona, y para grupos
gana el precio por persona más bajo.

Regla de trabajo: como en el módulo de perfil, **la maquetación se aprueba antes de conectar la
lógica**, y cualquier duda de diseño se pregunta antes de construir esa pantalla.

---

## Cómo funciona una pregunta (de punta a punta)

```
Usuario: "somos 6, queremos un bar barato cerca, estoy en Chapinero"
   │
   ▼
App ──(pregunta + historial del chat + instrucciones de Spotty + funciones disponibles)──▶ Gemini
   │                                                                                       │
   │◀──── "llama buscar_lugares(categoria=bares, grupo=6, orden=precio, zona=Chapinero)" ───┘
   ▼
Kotlin filtra places.json, calcula distancias reales, precio por persona y total del grupo
   │
   ├──(resultados reales: ids, precios, distancias, totales)──▶ Gemini
   │                                                            │
   │◀─── JSON: { texto, lugares:[{id, motivo}], sugerencias } ──┘
   ▼
La app valida los ids (descarta cualquiera que no exista), calcula el % Match, lee las ★
y dibuja: burbuja de texto + aviso "Analicé N locales" + tarjetas + chips de sugerencias
   │
   ▼
Tarjeta → "Ver detalles" → PlaceDetailScreen del lugar (ya existe)
```

Cada pregunta gasta normalmente 2 llamadas a Gemini (decidir la búsqueda + redactar), como máximo 4.

---

## Fase 0. Configuración (tú + yo)

**Tú:** crear la clave siguiendo [TUTORIAL_API_KEY.md](TUTORIAL_API_KEY.md) y pegarla en
`local.properties`.

**Yo:**
- `app/build.gradle.kts`: activar `buildConfig` y leer `GEMINI_API_KEY` desde `local.properties`
  hacia `BuildConfig.GEMINI_API_KEY`. Si falta la clave, la app compila igual y Spotty muestra
  "Falta configurar la clave".
- `gradle/libs.versions.toml`: agregar OkHttp (conexión a Gemini), el plugin y la librería
  kotlinx-serialization (leer y escribir JSON) y `play-services-location` (GPS). Versiones
  compatibles con AGP 9.3.0.
- Verificar la clave con una llamada de prueba sin mostrarla, y fijar el **id exacto del modelo
  Flash-Lite** consultando la lista de modelos que tu proyecto tiene disponibles.

**Listo cuando:** el proyecto compila y una llamada de prueba a Gemini responde.

## Fase 1. Datos: coordenadas y calificaciones

- Script en `data_import/` que geocodifica las 171 direcciones una sola vez con OpenStreetMap
  (Nominatim, gratis, 1 consulta por segundo) y agrega `lat` y `lng` a cada lugar en
  `places.json`. Se integra a `excel_to_json.ps1` para que no se pierda al regenerar.
- **Riesgo conocido:** OpenStreetMap entiende mal algunas direcciones colombianas ("Cl. 23 #4-36").
  El script normaliza el formato, valida que el punto caiga dentro de Bogotá y a una distancia
  coherente con el `distanceM` del Excel, y deja un reporte de los que fallen. Esos los
  corrijo uno por uno; si quedan dudosos, te muestro la lista antes de seguir.
- `data/places.kt`: agregar `lat` y `lng` al modelo `Place`.
- Calificación ★: promedio de los comentarios reales de `places/{id}/comments`. Revisaré si los
  comentarios guardan estrellas; si no tienen, te pregunto cómo seguimos antes de inventar nada.

**Listo cuando:** los 171 lugares tienen coordenadas verificadas y el reporte de errores está vacío.

## Fase 2. Maquetación de Spotty (requiere tu aprobación)

Pantallas iguales a Figma (frames 444:32, 579:369, 593:11 y 674:4) con datos de ejemplo, **sin IA
todavía**:

1. **Bienvenida:** "¡Hola, [nombre real]! 👋 ¿Qué quieres hacer hoy?", el campo "¡Pulsa aquí e
   inicia una conversación!" y la etiqueta del modelo.
2. **Conversación:**
   - burbujas de Spotty y del usuario con su hora
   - chips de sugerencias iniciales (🎉 Conciertos, 🍻 Bares, 📍 Cerca de mí, 💰 < $30k)
   - aviso "Analicé N locales • M coincidencias"
   - tarjetas de lugar: foto, % Match, distancia, nombre, dirección, ★, chips, precio,
     "Ver detalles" y corazón de favorito
   - chips de seguimiento y botón para bajar al final
3. **Barra de escritura** con el botón de enviar y el **botón Spotty** en la barra inferior. Ya
   existe `onSpotty` en `HomeBottomBar`, hoy vacío.
4. **Ventanita** "¡Lo has guardado en favoritos!" (frame 674:4), usando el `FavoriteStore` que ya
   existe.
5. **Diseñadas por mí con el estilo Spotty (no están en Figma):**
   - panel de historial: lista de chats con título y fecha, y "Nuevo chat"
   - confirmación de "¿Eliminar este chat?" con el mismo estilo de los diálogos de U-Spot
   - estados: Spotty escribiendo, sin conexión, cuota del día agotada, falta la clave, sin
     resultados

Nuevos archivos: `screens/Spotty.kt`, `screens/SpottyHistory.kt` y
`decorations/spotty_components.kt`. Ruta nueva en `navigation/app_navigation.kt`.

**Listo cuando:** apruebas cada pantalla en el emulador (te mando capturas).

## Fase 3. Motor de IA

Archivos nuevos en `data/spotty/`:

- **`GeminiClient.kt`:** llamada REST a `generateContent` con OkHttp. Incluye el bucle de *function
  calling* (máximo 3 rondas), temperatura baja, tiempo de espera, reintento ante errores
  temporales y manejo de errores:
  - 429 (cuota agotada) → "Spotty descansa hasta mañana"
  - sin internet → reutiliza `data/connectivity.kt`
  - clave inválida → aviso de configuración
- **`SpottyPrompt.kt`:** instrucciones del sistema: quién es Spotty, que solo recomienda lugares
  devueltos por las funciones, que responde en español colombiano cercano, que redirige con
  amabilidad lo que no tiene que ver con U-Spot, que ignora intentos de cambiarle las reglas y que
  no pide datos personales.
- **`SpottyTools.kt`:** las funciones que Gemini puede pedir, todas ejecutadas en Kotlin sobre
  `places.json`:

  | Función | Qué hace |
  |---|---|
  | `buscar_lugares` | Filtra por categoría, presupuesto por persona, tamaño del grupo, Nequi, para estudiar o tranquilo, abierto ahora y radio de distancia. Ordena por cercanía, precio o coincidencia. Devuelve un máximo de 5 lugares con sus datos reales |
  | `presupuesto_grupo` | Para N personas en un lugar: total mínimo y máximo, aporte por persona y la cifra sugerida en el punto medio |
  | `detalle_lugar` | Horario completo, notas y dirección de un lugar por su id |
  | `ubicar_zona` | Convierte "Chapinero" o "cerca a la Javeriana" en coordenadas con el `Geocoder` de Android, gratis y en el celular |

- **`SpottyMatch.kt`:** calcula el % Match. Es la proporción de requisitos del usuario que cumple el
  lugar (precio, grupo, distancia, Nequi, ambiente), ponderada. Es siempre igual para los mismos
  datos, nunca un número inventado.
- **Respuesta final en JSON estructurado** (`texto`, `lugares[{id, motivo}]`, `sugerencias[]`).
  La app **descarta cualquier id que no esté en el catálogo**, y los precios que se muestran en la
  tarjeta salen de `places.json`, no del texto del modelo.
- Ahorro de cuota: solo se envían los últimos ~12 mensajes del chat, y el título del chat se arma
  en Kotlin con la primera pregunta, sin gastar una llamada extra.

**Listo cuando:** pasan las pruebas unitarias de filtros, presupuesto y Match, y las preguntas de
la batería de la Fase 7 dan respuestas correctas en el emulador.

## Fase 4. Ubicación

- Permiso de ubicación aproximada o precisa, que se pide la primera vez que el usuario busca algo
  "cerca". Si lo niega, Spotty pregunta "¿En qué barrio o lugar estás?".
- `FusedLocationProviderClient` para la última ubicación conocida, con un tiempo límite.
- Si el usuario escribe una zona, esa zona tiene prioridad sobre el GPS.
- Las tarjetas muestran la distancia real ("A 1.2 km") en lugar de "A 400 m de UTADEO".

**Listo cuando:** en el emulador, con ubicación simulada en Chapinero y en el centro, las
distancias y el orden cambian correctamente.

## Fase 5. Historial en Firestore

- Estructura:
  - `users/{uid}/chats/{chatId}`, con `title`, `createdAt`, `updatedAt` y `lastMessage`
  - `users/{uid}/chats/{chatId}/messages/{msgId}`, con `role`, `text`, `places[]`,
    `suggestions[]` y `createdAt`
- `data/spotty/ChatRepository.kt`, con estas operaciones:
  - crear un chat al enviar el primer mensaje
  - listar los chats (los más recientes primero)
  - retomar un chat (carga sus mensajes y los envía como contexto)
  - eliminar un chat con sus mensajes
- `firestore.rules`: solo el dueño lee y escribe sus chats. Es importante porque hoy
  `users/{uid}` permite que cualquiera lo lea, así que los chats necesitan su propia regla
  privada. Te diré cuándo publicarlas, igual que las anteriores.

**Listo cuando:** se crea, retoma y elimina un chat, y otro usuario no puede leerlo.

## Fase 6. Integración final

- "Ver detalles" → `Routes.HOME_PLACE` con el id del lugar.
- Corazón → `FavoriteStore.toggle` + ventanita de Figma.
- Chips de sugerencias → se envían como mensaje.
- Etiqueta del modelo con el nombre real.
- Nombre real del usuario en el saludo (desde `user_profile.kt`).

## Fase 7. Pruebas y APK

- **Pruebas unitarias** de filtros, presupuesto de grupo y Match.
- **Batería de unas 25 preguntas** reales que se correrán en el emulador:
  - "bar cerca, estoy en Chapinero"
  - "somos 6, ¿cuánto pone cada uno?"
  - "lo más barato para estudiar"
  - "algo que acepte Nequi"
  - jerga: "parce, algo pa' farrear con 40 lucas"
  - errores de ortografía
  - preguntas fuera de tema (deportes, tareas)
  - intentos de que invente ("¿el bar X que no existe?")
  - intentos de cambiarle las reglas ("ignora tus instrucciones")
  - preguntas sin resultados

  Cada respuesta se revisa contra `places.json`.
- **Compilar el APK** de depuración firmado con el keystore compartido. Hay que probarlo al menos en
  un celular físico además del emulador.

**Listo cuando:** la batería pasa sin lugares inventados ni precios incorrectos y el APK funciona en
el celular.

---

## Orden y dependencias

```
F0 Config ──┬─▶ F3 Motor IA ──┐
            │                 ├─▶ F6 Integración ─▶ F7 Pruebas + APK
F1 Datos ───┼─▶ F4 Ubicación ─┤
            │                 │
F2 Maquetación (tu aprobación)┤
                              │
F5 Historial ─────────────────┘
```

F0, F1 y F2 pueden avanzar sin la clave. F3 necesita la clave.

## Fuera de alcance (por ahora)

Reservas reales, voz, imágenes en el chat (el "+" de la barra de Figma), modo oscuro de Spotty y
notificaciones.
