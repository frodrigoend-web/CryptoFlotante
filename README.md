# Crypto Flotante v2

Aplicación Android nativa en Kotlin. Este proyecto incluye un flujo de compilación en la nube mediante GitHub Actions; no requiere instalar Android Studio en la computadora del usuario.

## Generar el APK sin Android Studio

1. Crea un repositorio en GitHub y carga el contenido de esta carpeta (incluida la carpeta oculta `.github`). También puedes subir el ZIP y extraerlo en el repositorio desde GitHub si la interfaz lo permite.
2. En GitHub, abre la pestaña **Actions**.
3. Selecciona **Compilar APK Android** y pulsa **Run workflow**.
4. Espera a que el trabajo termine. Si la compilación es correcta, entra al resultado de esa ejecución.
5. En **Artifacts**, descarga `CryptoFlotante-debug-apk` y extrae `app-debug.apk`.
6. Transfiere el APK al teléfono Android y ábrelo para instalarlo. Si Android lo solicita, permite instalar aplicaciones desde esa fuente.

También se compila automáticamente al hacer push a las ramas `main` o `master`.

## Entorno de compilación

- JDK 17
- Gradle 8.2
- Android Gradle Plugin 8.2.2
- Android SDK / compileSdk 34
- minSdk 26 (Android 8.0)

El flujo produce un APK **debug** instalable para pruebas. No es un APK firmado para publicación en Google Play.

## Funcionalidad documentada

- Barra flotante de precios.
- Consulta de datos de mercado de Binance.
- Alarmas e indicadores configurados en la aplicación.

## Importante

El APK solo se genera si GitHub Actions termina correctamente. La configuración de compilación no certifica por sí sola la exactitud de las señales, el funcionamiento continuo en segundo plano ni la compatibilidad con todos los fabricantes de teléfonos. Es necesario probar la aplicación en el dispositivo de destino.
