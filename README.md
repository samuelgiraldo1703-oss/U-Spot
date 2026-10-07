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

*Desarrollado con dedicación para el curso de Aplicaciones Móviles — UTADEO 2026.*
