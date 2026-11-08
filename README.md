# 📍 U-Spot (USpot)

<p align="center">
  <img src="Logo U-Spot.png" alt="Logo U-Spot" width="220" />
</p>

> *"Tu entorno también es parte de la U"*  
> **Proyecto de Desarrollo Móvil — Universidad de Bogotá Jorge Tadeo Lozano (UTADEO)**  
> **Asignatura:** Aplicaciones Móviles (6to Semestre)  
> **Autor principal:** Samuel Giraldo ([@samuelgiraldo1703-oss](https://github.com/samuelgiraldo1703-oss))

---

## 📖 1. Concepto y Visión del Proyecto

**U-Spot** es una aplicación móvil nativa desarrollada para el ecosistema Android con **Jetpack Compose**. Su objetivo principal es conectar a la comunidad universitaria con su campus y su entorno urbano en Bogotá, facilitando el descubrimiento, consulta, valoración y recomendación de lugares clave ("spots") para la vida universitaria.

### 🎯 Objetivos de la Aplicación:
- **Descubrimiento de Spots:** Encontrar espacios óptimos de estudio, bibliotecas, cafeterías, zonas verdes, puntos de descanso, restaurantes y sitios culturales cercanos a la universidad.
- **Identidad Tadeísta:** Interfaz gráfica moderna inspirada en la vibra universitaria, con paletas de colores llamativas (azul marino, cian, amarillo, blanco) y formas orgánicas distintivas.
- **Gestión de Usuarios y Perfiles:** Autenticación completa con Firebase Auth, Google Sign-In y sincronización en tiempo real con Cloud Firestore.
- **Navegación Intuitiva:** Transición fluida entre pantallas mediante `Navigation Compose` con diseño responsivo, manejo de conectividad y modo *Edge-to-Edge* para una experiencia inmersiva.

---

## 📱 2. Módulos y Pantallas Actuales

El proyecto se encuentra organizado modularmente bajo paquetes limpios y arquitectura desacoplada:

```
app/src/main/java/
├── com.example.u_spot/
│   ├── MainActivity.kt        # Entrada principal con Edge-to-Edge y observación de conectividad
│   ├── Icons.kt               # Catálogo de iconos vectoriales personalizados
│   └── ui/theme/              # Material 3 Theme (Color.kt, Theme.kt, Type.kt)
├── data/
│   ├── auth_repository.kt     # Repositorio de Firebase Auth, Firestore y validaciones de contraseña
│   ├── google_auth_helper.kt  # Integración nativa con Credential Manager para inicio con Google
│   └── connectivity.kt        # Monitoreo reactivo de red en tiempo real
├── decorations/
│   ├── app_colors.kt          # Paleta de colores oficial de la app (Azul, Cian, Amarillo, etc.)
│   └── corner_shapes.kt       # Elementos gráficos, curvaturas y tarjetas decorativas
├── navigation/
│   └── app_navigation.kt      # Rutas (splash, login, register, email_verification, change_password, home, offline)
└── screens/
    ├── Splash.kt              # Pantalla de bienvenida con animación de carga y verificación de sesión
    ├── Login.kt               # Inicio de sesión con correo/contraseña, toggle de visibilidad y Google Sign-In
    ├── Register.kt            # Registro con paso a paso, validación estricta y sincronización en Firestore
    ├── EmailVerification.kt   # Verificación en 2 pasos (2FA/Email) con accesos directos a Gmail y Outlook
    ├── ChangePassword.kt      # Restablecimiento y actualización segura de contraseñas
    ├── Home.kt                # Pantalla principal con carrusel de categorías, spots recomendados y navbar
    └── Offline.kt             # Pantalla de contingencia cuando se pierde la conexión a internet
```

### Funcionalidades implementadas:
1. **Splash (`SplashScreen`)**:
   - Presentación de marca animada y verificación automática de estado de autenticación.
2. **Login (`LoginScreen`)**:
   - Encabezado con imagotipo oficial de U-Spot.
   - Campos estilizados para correo institucional y contraseña (con botón para mostrar/ocultar).
   - Acceso con botones redondeados y autenticación directa mediante Google Credential Manager.
   - Enlace directo a registro y recuperación de contraseña.
3. **Registro (`RegisterScreen`)**:
   - Captura y validación de Nombre de usuario, Correo institucional y Contraseña (mínimo 6 caracteres).
   - Registro en Firebase Auth y guardado de perfil en Cloud Firestore.
4. **Verificación de Correo (`EmailVerificationScreen`)**:
   - Réplica exacta de diseño Figma con envío de enlace de verificación.
   - Botones con enlace directo a las apps/web de Gmail y Outlook.
5. **Recuperación de Contraseña (`ChangePasswordScreen`)**:
   - Envío de correo de recuperación y validación de coincidencia de nuevas credenciales.
6. **Inicio / Explorador (`HomeScreen`)**:
   - Categorías interactivas (Cafés, Bares, Ocio, Restaurantes, Teatros) y listado de spots recomendados (Juan Valdez, Mono Bandido, La Embajada).
   - Barra de navegación inferior flotante con acceso a perfil y cierre de sesión.
7. **Modo Offline (`OfflineScreen`)**:
   - Detección reactiva de desconexión con opción de reintentar conexión al instante.

---

## ⚙️ 3. Ficha Técnica y Especificaciones del Entorno

> [!IMPORTANT]
> **Para evitar errores de compilación (`build failed`) y sincronización de Gradle, TODOS los miembros del equipo deben trabajar sobre las mismas especificaciones técnicas descritas a continuación.**

| Componente | Versión Exacta | Notas / Ubicación de Configuración |
| :--- | :--- | :--- |
| **IDE Recomendado** | **Android Studio (2024.3+ / 2026.1+)** | Ladybug Feature Drop / Meerkat o superior con soporte para AGP 9.x |
| **Android Gradle Plugin (AGP)** | **`9.3.0`** | Configurado en `gradle/libs.versions.toml` (`agp = "9.3.0"`) |
| **Gradle** | **`9.6.0`** | Configurado en `gradle/wrapper/gradle-wrapper.properties` |
| **Kotlin** | **`2.2.10`** | `kotlin = "2.2.10"` con Compose Compiler Plugin oficial |
| **Compose Compiler Plugin** | **`2.2.10`** | `org.jetbrains.kotlin.plugin.compose` |
| **Compose BOM** | **`2026.02.01`** | Manejo unificado de versiones de Compose |
| **JDK para Gradle Daemon** | **Java 21 o Java 25 (JBR)** | `toolchainVersion=25` en `gradle/gradle-daemon-jvm.properties` |
| **Java Compatibility** | **Java 11 (`VERSION_11`)** | `compileOptions` en `app/build.gradle.kts` |
| **Compile SDK** | **`37`** (`release(37)`) | Android SDK API 37 |
| **Target SDK** | **`37`** | Última versión de destino para Android moderno |
| **Min SDK** | **`28`** | Compatible con Android 9.0 (Pie) en adelante |
| **Material 3** | **`1.4.0`** | `androidx.compose.material3` |
| **Navigation Compose** | **`2.10.2`** | `androidx.navigation:navigation-compose` |

---

## 🚀 4. Guía de Instalación y Puesta en Marcha (Paso a Paso)

Sigue estos pasos con atención la primera vez que descargues el proyecto:

### Paso 1: Clonar el Repositorio
Abre tu terminal en la carpeta donde deseas guardar tus proyectos de la universidad y ejecuta:
```bash
git clone https://github.com/samuelgiraldo1703-oss/U-Spot.git
cd U-Spot
```

### Paso 2: Verificar el Android SDK en Android Studio
1. Abre **Android Studio**.
2. Ve a **Tools** > **SDK Manager** (o desde la pantalla de bienvenida en *More Actions* > *SDK Manager*).
3. En la pestaña **SDK Platforms**:
   - Asegúrate de tener instalado el SDK correspondiente al **API Level 37**. Si no lo tienes, márcalo y haz clic en **Apply** para descargarlo.
4. En la pestaña **SDK Tools**:
   - Verifica tener instalado **Android SDK Build-Tools**, **Android SDK Command-line Tools** y **Android Emulator**.

### Paso 3: Abrir el Proyecto en Android Studio
1. Selecciona **File > Open...** (o *Open* en la pantalla inicial).
2. Selecciona la carpeta raíz del proyecto `U-Spot`.
3. Espera a que Android Studio indexe el proyecto y descargue el Gradle Wrapper 9.6.0 automáticamente.

### Paso 4: Configurar el JDK de Gradle
Si experimentas errores de JVM al sincronizar:
1. Ve a **File > Settings** (en Windows/Linux) o **Android Studio > Settings** (en macOS).
2. Navega a: **Build, Execution, Deployment > Build Tools > Gradle**.
3. En la opción **Gradle JDK**, selecciona **Java 21** o el JDK embebido de Android Studio (**Embedded JDK / JBR 21 o superior**).
4. Haz clic en **Apply** y luego en **OK**.

### Paso 5: Sincronizar Gradle
Presiona el icono de elefante con la flecha azul (**Sync Project with Gradle Files**) en la barra de herramientas superior derecha.
La sincronización debe concluir con: `BUILD SUCCESSFUL`.

### Paso 6: Ejecutar la Aplicación
1. Selecciona o crea un dispositivo virtual (AVD) en el **Device Manager** (recomendado: Pixel 7 o Pixel 8 con API 34 o 37).
2. Haz clic en el botón verde de **Run** (`Shift + F10` o el botón ▶️).

---

## 🛠️ 5. Manejo del archivo `local.properties`

> [!WARNING]
> El archivo `local.properties` **NUNCA** se debe subir a GitHub porque contiene la ruta local del SDK en la computadora de cada persona y está incluido en el `.gitignore`.

Cuando abras el proyecto por primera vez, Android Studio creará automáticamente este archivo. Si por alguna razón requieres compilar desde la terminal y no existe, créalo en la raíz del proyecto con la ruta de tu SDK:

**En Windows:**
```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
```

**En macOS:**
```properties
sdk.dir=/Users/TU_USUARIO/Library/Android/sdk
```

**En Linux:**
```properties
sdk.dir=/home/TU_USUARIO/Android/Sdk
```

---

## 🤝 6. Guía de Trabajo en Equipo con Git

Para evitar conflictos de fusión (*merge conflicts*) y mantener el código ordenado, todo el equipo debe seguir este flujo de trabajo:

### 1. No trabajar directamente sobre `main`
La rama `main` siempre debe ser funcional y ejecutable sin errores.

### 2. Crear una rama para cada nueva funcionalidad o corrección
Antes de comenzar a programar una función, actualiza tu repositorio local y crea una rama:
```bash
git checkout main
git pull origin main
git checkout -b feature/nombre-de-tu-pantalla-o-modulo
```

*Ejemplos de nombres de ramas:*
- `feature/pantalla-home`
- `feature/mapa-spots`
- `feature/detalle-spot`
- `fix/validacion-login`

### 3. Realizar commits claros y descriptivos
```bash
git add .
git commit -m "feat: implementar buscador de spots con filtro por categorias"
```

### 4. Subir la rama y abrir Pull Request (PR)
```bash
git push origin feature/nombre-de-tu-pantalla-o-modulo
```
Luego entra en GitHub y abre un **Pull Request** para que tus compañeros puedan revisar y aprobar tus cambios antes de unirlos a `main`.

---

## 📋 7. Solución de Problemas Frecuentes (FAQ)

### ❓ Error: `SDK location not found`
- **Causa:** No se encuentra la ruta del SDK de Android en tu sistema.
- **Solución:** Abre el proyecto con Android Studio para que genere `local.properties`, o créalo manualmente con la ruta a tu carpeta `Android/Sdk`.

### ❓ Error: `Incompatible Java version` o error al ejecutar Gradle Daemon
- **Causa:** Tu Android Studio está usando una versión antigua de Java (ej. Java 1.8 o Java 17).
- **Solución:** En `Settings > Build Tools > Gradle > Gradle JDK`, selecciona Java 21 o Java 25.

### ❓ Error: `Failed to find target with hash string 'android-37'`
- **Causa:** No tienes descargada la versión 37 de la plataforma de Android.
- **Solución:** Abre el **SDK Manager** en Android Studio, marca el SDK de nivel **37** y haz clic en **Download / Apply**.

---

## 📲 8. Instalación y Despliegue en Dispositivos Físicos y Emuladores

> [!IMPORTANT]
> **Rama Actual con la Versión Funcional:** La rama oficial del repositorio que contiene la última versión consolidada y 100% funcional de la aplicación es **`main`**. Asegúrate de clonar o posicionarte sobre esta rama (`git checkout main`) antes de compilar o desplegar.

A continuación se detalla el procedimiento completo para compilar, generar el paquete instalable y desplegar **U-Spot** en dispositivos reales y emuladores:

### 8.1 Requisitos Previos del Sistema
- **Android Studio** Ladybug (2024.3+) o superior.
- **Android SDK Platform 37** (API 37) y herramientas de compilación instaladas.
- **Java JDK 21 o 25 (JBR / Embedded JDK)** asignado como Gradle JDK.
- **Dispositivo físico o AVD (Android Virtual Device)** con Android 9.0 (API 28) o superior.
- **Clave API de Gemini** (gratuita, sin facturación) para habilitar el asistente inteligente Spotty.

### 8.2 Configuración de Credenciales y Servicios
1. **Archivo `local.properties`:**
   - Asegúrate de que en la raíz del proyecto exista el archivo `local.properties`.
   - Agrega tu clave de Gemini al final del archivo siguiendo la plantilla de `local.properties.example`:
     ```properties
     GEMINI_API_KEY=tu_clave_de_gemini_aqui
     ```
   - *(Para generar tu clave sin costo en Google AI Studio, consulta el paso a paso en [`docs/spotty/TUTORIAL_API_KEY.md`](docs/spotty/TUTORIAL_API_KEY.md)).*
2. **Servicios de Firebase y Autenticación de Google:**
   - El archivo de configuración `app/google-services.json` y el keystore de depuración compartido `app/debug.keystore` ya se encuentran integrados en el repositorio.
   - La huella digital SHA-1 del keystore compartido está pre-registrada en la consola de Firebase, permitiendo que el inicio de sesión con correo/contraseña y el botón **"Continuar con Google"** funcionen inmediatamente sin requerir configuración adicional de claves SHA en tu máquina local.

### 8.3 Generación del APK desde Línea de Comandos
Si deseas compilar el paquete instalable sin necesidad de abrir el entorno gráfico de Android Studio, ejecuta:
```bash
# En Windows (PowerShell / CMD)
.\gradlew assembleDebug

# En macOS / Linux
./gradlew assembleDebug
```
Una vez finalizada la tarea (`BUILD SUCCESSFUL`), el instalador APK estará disponible en:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 8.4 Instalación Directa mediante ADB o Gradle
- **Opción A (Mediante ADB):** Con el dispositivo conectado por USB (modo de depuración habilitado) o el emulador encendido:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- **Opción B (Compilación e Instalación Automática con Gradle):**
  ```bash
  .\gradlew installDebug
  ```

---

## ⚡ 9. Últimos Avances y Funcionalidades Incorporadas

A través de las iteraciones más recientes del proyecto (incorporadas en los Pull Requests #5, #6 y #7), la aplicación evolucionó significativamente en estabilidad, diseño y cobertura funcional:

### 1. Centro de Ayuda con Tarjetas Encajonadas y Animadas (PR #6)
- **Tarjetas de diseño unificado:** Las opciones de ayuda y soporte en `ProfileHelp.kt` se consolidaron en componentes de tarjeta completos (`RoundedCornerShape(20.dp)` con borde `#E9EEF5` y sombra suave).
- **Cajas delimitadas para respuestas:** Cada texto de solución desplegado ya no flota en el fondo de la pantalla, sino que queda elegantemente encajonado en un contenedor interior con fondo suave (`app_colors.profileStatsBg` / `#F0F6FD`), padding simétrico y tipografía legible Plus Jakarta Sans.
- **Microinteracciones y animaciones:** Rotación dinámica de 180° en el icono de chevron (`animateFloatAsState`) y transición de tamaño fluida con `Modifier.animateContentSize()`.

### 2. Flujo Completo de Reporte de Problemas con Validaciones Estrictas (PR #7)
- **Asistente por pasos:** Proceso guiado en 4 etapas: Hub de incidentes, Identificación del problema, Clasificación por áreas del campus y Captura detallada (`ProfileReport.kt`).
- **Validación rigurosa de correo:** Se implementó una verificación obligatoria de sintaxis de correo electrónico (presencia indispensable de `@` y dominio válido) antes de habilitar el envío del reporte.
- **Carga y previsualización de evidencias multimedia:** Integración directa con cámara y galería para adjuntar capturas de imagen o video, con pantalla de validación previa y opción de descarte antes de persistir en Cloud Firestore.

### 3. Asistente Conversacional Inteligente: Spotty AI
- **Chatbot tadeísta en tiempo real:** Integración de Google Gemini Flash-Lite con llamadas a funciones (*function calling*) sobre un catálogo local enriquecido de 171 lugares de Bogotá (`Spotty.kt`).
- **Estilo visual Glassmorphism:** Interfaz inmersiva con fondo translúcido, zonas seguras adaptadas a las barras de estado y navegación del sistema (*Edge-to-Edge*), y burbujas de diálogo con recomendaciones dinámicas.
- **Algoritmo de cálculo de afinidad:** Medición de porcentaje de coincidencia (`% Match`) según los gustos del estudiante, cálculo de gasto por persona para salidas en grupo y visualización directa de tarjetas de lugares con enlace a detalles.

### 4. Sistema Interactivo de Comentarios en Spots
- **Participación social comunitaria:** Panel interactivo en tiempo real (`comments.kt`) que permite a los usuarios compartir experiencias sobre cada spot, responder a comentarios en hilo (*replies*), reaccionar con "me gusta" y seleccionar emojis con el selector oficial de Android (`androidx.emoji2`).
- **Persistencia en la nube:** Toda la actividad se sincroniza en vivo con Cloud Firestore.

### 5. Refactorización Integral de UI y Preparación i18n (PR #5)
- **Centralización total de textos:** Extracción de más de 400 cadenas hardcodeadas hacia `res/values/strings.xml`, preparando la aplicación para soporte multi-idioma.
- **Tokens de diseño consistentes:** Centralización de colores en `decorations/app_colors.kt` y tipografías en `decorations/figma_components.kt`, alineados al sistema de diseño oficial de Figma.

---

## 🛠️ 10. Stack Tecnológico y Arquitectura Implementada

**U-Spot** adopta una arquitectura modular limpia orientada a componentes desacoplados, cumpliendo con los estándares modernos de desarrollo en Android:

| Capa / Módulo | Tecnología / Librería | Versión | Propósito en la Aplicación |
| :--- | :--- | :--- | :--- |
| **Lenguaje Base** | **Kotlin** | `2.2.10` | Tipado estricto, extensiones idiomáticas, Coroutines y StateFlow reactivo. |
| **Framework de UI** | **Jetpack Compose** | BOM `2026.02.01` | Interfaz 100% declarativa, animaciones fluidas y soporte responsivo. |
| **Diseño y Tema** | **Material 3** | `1.4.0` | Tematización moderna, accesibilidad, tipografía Plus Jakarta Sans y tokens de Figma. |
| **Navegación** | **Navigation Compose** | `2.10.2` | Enrutamiento desacoplado entre módulos (Splash, Auth, Home, Spotty, Perfil, Reporte). |
| **Autenticación** | **Firebase Auth** | BOM `33.9.0` | Inicio de sesión, registro institucional, verificación en 2 pasos y cambio de clave. |
| **Credenciales** | **Credential Manager** | `1.5.0` | Soporte nativo para Google Sign-In con la API moderna de Android Credentials. |
| **Base de Datos NoSQL** | **Cloud Firestore** | BOM `33.9.0` | Sincronización en tiempo real de perfiles, catálogo de spots, comentarios y reportes. |
| **Inteligencia Artificial** | **Google Gemini API** | 1.5 / 2.0 Flash-Lite | Motor conversacional de Spotty AI con function calling y prompts contextualizados. |
| **Cliente HTTP / Red** | **OkHttp** | `5.0.0-alpha.14` | Conexión directa y eficiente con los endpoints REST de Google AI Studio. |
| **Geolocalización** | **Play Services Location** | `21.3.0` | Obtención de coordenadas del usuario y cálculo de distancias reales a los lugares. |
| **Selector de Emojis** | **Emoji2 Picker** | `1.6.0` | Selector nativo de emojis para enriquecer los comentarios de la comunidad. |
| **Carga de Imágenes** | **Android Graphics / Painters** | Nativo | Gestión de avatares, fotos de lugares, evidencias de reporte y fondos vectoriales. |

---

## 🤖 11. Implementación de Agentes de IA y Flujo de Trabajo Asistido

Durante el ciclo de desarrollo del proyecto se integraron agentes especializados e infraestructura de Inteligencia Artificial para acelerar la entrega y elevar la calidad del código:

1. **Agente Antigravity (Google DeepMind):**
   - Utilizado como asistente agentico de programación en pareja (*pair programming*) para la orquestación y ejecución de tareas complejas.
   - Ejecución autónoma de refactorizaciones de código masivas (centralización de recursos XML, desacoplamiento de componentes y normalización de temas).
   - Diagnóstico proactivo y resolución de errores de compilación, dependencias de Gradle y configuración de SDK/JDK.
   - Gestión de ramas Git, sincronización con repositorios remotos y resolución limpia de conflictos.

2. **Servidor MCP de Figma (`Figma MCP Server`):**
   - Integrado mediante el protocolo abierto **Model Context Protocol (MCP)** configurado en `.agents/mcp_config.json`.
   - Permitió al agente conectarse directamente con el archivo de diseño oficial de Figma de U-Spot para inspeccionar dimensiones exactas, jerarquías de capas, estilos tipográficos, curvas de radio (`RoundedCornerShape`) y códigos hexadecimales de color, garantizando una fidelidad visual 1:1.

3. **Spotty AI (Agente Inteligente Conversacional dentro de la App):**
   - Agente autónomo embebido en la aplicación que actúa como guía para la comunidad tadeísta.
   - Implementa *Function Calling* estructurado para interactuar con la base de datos de 171 spots, evaluando distancias, rangos de precios y valoraciones para ofrecer recomendaciones hiper-personalizadas en lenguaje natural.

---

## 🏆 12. Producto Final: Estado y Alcance Alcanzado

El resultado de este proceso de desarrollo es **U-Spot en su versión 1.0 funcional**, una aplicación móvil nativa integral que cubre el ciclo de vida del estudiante universitario dentro de su entorno:

- **Autenticación e Identidad Confiable:** Sistema completo con validación de correos institucionales `@utadeo.edu.co`, recuperación de credenciales, autenticación en 1 toque con Google y verificación en dos pasos.
- **Explorador Urbano Tadeísta:** Catálogo geocodificado de 171 lugares emblemáticos alrededor del campus (cafeterías de estudio, restaurantes universitarios, teatros, bares y espacios de ocio) con filtros avanzados por categoría y ubicación.
- **Comunidad y Retroalimentación:** Módulo interactivo de opiniones y comentarios con likes, hilos de respuesta y avatares personalizados.
- **Asistencia Inteligente Embebida:** Spotty AI listo para responder preguntas y encontrar el lugar ideal para estudiar, comer o socializar en segundos.
- **Gestión de Perfil, Soporte y Seguridad:** Menú completo de configuración de cuenta, personalización de preferencias, centro de ayuda interactivo y módulo formal de reporte de problemas con soporte multimedia.
- **Experiencia de Usuario Moderna:** Navegación *Edge-to-Edge* respetando las zonas seguras del sistema, tiempos de respuesta optimizados y modo offline reactivo para garantizar operatividad continua.

> [!NOTE]
> Todo este desarrollo se encuentra totalmente integrado, verificado y disponible en la rama **`main`** del repositorio.

*Desarrollado con dedicación para el curso de Aplicaciones Móviles — UTADEO 2026.*
