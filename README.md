# Bodega Líder

Aplicación Android para el **control de góndola y reposición** en sala de ventas.
Los reponedores escanean productos, ajustan el stock en góndola y reciben tareas;
los supervisores ven los quiebres de su sector, asignan prioridades a su equipo y
generan reportes PDF.

## Funcionalidades

### Reponedor
- **Catálogo**: búsqueda por nombre, marca, pasillo o código (sin importar tildes),
  filtros rápidos *Todos / Sin stock / Stock bajo* y orden por criticidad, stock, nombre o pasillo.
- **Ajuste de stock** con botones `-` / `+` grandes; cada unidad repuesta descuenta lo que falta por reponer.
- **Escáner** de códigos de barras (CameraX + ML Kit) con linterna, vibración al leer y
  búsqueda manual. Si el código no existe, permite registrarlo al instante.
- **Registro / edición de productos** con foto, ubicación (pasillo + detalle) y validaciones.
- **Avisos**: tareas del supervisor (pendientes / completadas) que se marcan al terminarlas.

### Supervisor de sector
- **Equipo**: reponedores de sus comunas, estado en línea, tareas pendientes y filtros por comuna.
- **Alertas de prioridad** a un reponedor (con espera de 1 hora por producto para no saturar).
- **Quiebres**: disponibilidad en góndola (%), productos sin stock, stock bajo y unidades a reponer.
- **Reporte PDF** listo para compartir (correo, WhatsApp, Drive…).

### General
- Sincronización en tiempo real con **Cloud Firestore** y funcionamiento sin señal
  (los cambios se envían al recuperar la conexión).
- Modo claro / oscuro (se recuerda la preferencia) y sesión persistente.

## Usuarios de demostración

| Usuario  | Rol                   | Local / Comuna   |
|----------|-----------------------|------------------|
| `admin`  | Supervisor de sector  | Maipú, Pudahuel  |
| `carlos` | Reponedor             | 046 · Maipú      |
| `ana`    | Reponedor             | 048 · Maipú      |
| `juan`   | Reponedor             | 052 · Pudahuel   |
| `maria`  | Reponedor             | 060 · Cerrillos  |
| `pedro`  | Reponedor             | 071 · Stgo Centro|
| `sofia`  | Reponedor             | 046 · Maipú      |

En la pantalla de inicio también hay accesos rápidos para entrar con cada usuario.

## Puesta en marcha

1. Abrir el proyecto en **Android Studio** (JDK 25 para Gradle; ver `gradle/gradle-daemon-jvm.properties`).
2. Crear un proyecto en [Firebase](https://console.firebase.google.com), registrar la app
   `com.example.bodegalider`, habilitar **Cloud Firestore** y reemplazar
   `app/google-services.json` por el archivo descargado (el incluido es solo de prueba para compilar).
3. Ejecutar en un dispositivo con Android 8.0 (API 26) o superior.

La primera vez que la colección `products` está vacía, la app carga productos de ejemplo.

## Estructura

```
app/src/main/java/com/example/bodegalider/
├── MainActivity.kt          # Punto de entrada, tema y barras del sistema
├── data/                    # InventoryRepository (Firestore + estado de red)
├── model/                   # Product, User, TaskItem, InventoryStats
├── ui/
│   ├── components/          # Componentes reutilizables (encabezados, badges, KPIs…)
│   ├── screens/             # Pantallas: Login, Catálogo, Quiebres, Escáner, Equipo, Avisos, Ajustes
│   ├── theme/               # Colores de marca, tipografía y colores de estado
│   └── viewmodel/           # InventoryViewModel
└── util/                    # Reporte PDF y utilidades de texto
```

## Tests

```bash
./gradlew testDebugUnitTest
```

Los tests unitarios cubren la lógica de estados de stock, el resumen de inventario y la búsqueda sin tildes.

## Actualizaciones automáticas en el celular

Cada push a `main` ejecuta `.github/workflows/distribuir.yml`. Ese workflow compila el APK firmado y lo sube a **Firebase App Distribution**, y la actualización llega al celular a través de la app **Firebase App Tester**.

Configuración (una sola vez):

1. **Testers:** en Firebase → App Distribution → *Verificadores y grupos*, crea un grupo con alias `testers` y agrega tu correo.
2. **Clave de firma:** genérala en tu PC (guárdala bien; si la pierdes, habrá que reinstalar la app):
   ```powershell
   & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore distribucion.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bodega
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("distribucion.jks")) | Set-Clipboard
   ```
3. **Cuenta de servicio:** en Google Cloud → IAM → Cuentas de servicio, crea una con el rol *Firebase App Distribution Admin* y descarga su clave JSON.
4. **Secretos en GitHub** (*Settings → Secrets and variables → Actions*):

   | Secreto | Valor |
   |---|---|
   | `KEYSTORE_BASE64` | El texto copiado en el paso 2 |
   | `KEYSTORE_PASSWORD` | Contraseña del keystore |
   | `KEY_ALIAS` | `bodega` |
   | `KEY_PASSWORD` | Contraseña de la clave (la misma si no pusiste otra) |
   | `FIREBASE_SERVICE_ACCOUNT` | Contenido completo del JSON del paso 3 |
   | `FIREBASE_APP_ID` | ID de la app Android (*Configuración del proyecto → Tus apps*, empieza con `1:`) |

La primera vez, desinstala la versión instalada desde Android Studio, porque su firma es distinta. Las versiones siguientes se instalan encima.
