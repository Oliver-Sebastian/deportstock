# DEPORTSTOCK

App Android para gestionar el inventario y los préstamos de implementos deportivos del área de
Deportes de Bienestar Universitario (Universidad de Nariño, sede Ipiales).

Kotlin · Jetpack Compose (Material 3) · MVVM con repositorios · Firebase Authentication · Cloud Firestore.

## Puesta en marcha

1. **Firebase.** El archivo app/google-services.json del proyecto de Firebase ya está incluido.
2. **Reglas de seguridad.** Copia el contenido de [`firestore.rules`](firestore.rules) en
   Firebase Console > Firestore Database > Reglas y publícalo. Sin estas reglas el registro de
   estudiantes, el horario y la configuración responden "permiso denegado".
3. **Monitores.** Todas las cuentas nacen como estudiante. Para habilitar un monitor, el
   estudiante se registra en la app, verifica el correo e inicia sesión una vez; luego, en
   Firebase Console > Firestore, crea en la colección `monitores_autorizados` un documento cuyo ID
   sea su código estudiantil (o su uid) con los campos `activo` (booleano, `true`) y `periodo`
   (texto, por ejemplo `2026-2`). En Ajustes le aparece "Modo de uso" para pasar al modo monitor;
   no hace falta cerrar sesión. Ver "Monitores autorizados y modo de uso".
4. **Fin de semestre y horario.** Como monitor, entra a "Horario y Semestre" y define la fecha de
   fin de semestre: sin ella no se pueden registrar préstamos.

## Roles

| Rol | Qué puede hacer |
|---|---|
| Estudiante (toda cuenta) | Ver el catálogo con disponibilidad, el horario y su propio historial. |
| Monitor (estudiante con un documento activo en `monitores_autorizados`) | En modo monitor: inventario, préstamos, devoluciones, novedades, horario y fin de semestre (de otros estudiantes, no los suyos). En modo estudiante, lo mismo que cualquier estudiante. |

## Arquitectura

```
data/         Modelos y repositorios (único punto de acceso a Firebase)
viewmodel/    Un ViewModel por pantalla; exponen StateFlow con el estado de la UI
ui/screens/   Pantallas en Compose, agrupadas por módulo
ui/navigation Navegación por rol y rutas protegidas
ui/components Componentes compartidos
ui/theme/     Temas: paletas (Colores.kt), esquemas de Material 3 (Theme.kt), adornos neón (Neon.kt)
              e idioma de la interfaz (ProveedorIdioma.kt)
utils/        Fechas, idioma activo (Idioma.kt) y sesión en memoria
```

- Firestore es la única fuente de verdad. Su SDK mantiene la caché sin conexión; la app no guarda
  copias propias en memoria.
- Préstamo y devolución son transacciones: se actualizan juntos el préstamo, el inventario y el
  estudiante, o no se actualiza nada.

## Colecciones de Firestore

| Colección | Contenido |
|---|---|
| `usuarios/{uid}` | nombre, codigo, programa, telefono (10 dígitos, sin +57), correo, esMonitor (copia del permiso; no da permisos), modoActivo (`estudiante` o `monitor`), bloqueado, motivoBloqueo, prestamoActivoId, verificado. El campo `rol` de las cuentas antiguas ya no se usa. |
| `monitores_autorizados/{código o uid}` | activo (booleano), periodo (texto, por ejemplo `2026-2`). Lista blanca de monitores: solo se edita en Firebase Console. |
| `fotosPerfil/{uid}` | imagen: foto de perfil en JPEG de 256 × 256 px codificada en Base64 (unos 10 a 30 KB). Va aparte de `usuarios` para que la búsqueda de estudiantes no descargue todas las fotos. |
| `codigosEstudiante/{codigo}` | uid. Reserva el código para que no se repita; se crea en el primer ingreso con el correo verificado. |
| `inventario/{codigo}` | Artículo. `tipoControl` = `UNIDAD` (una pieza con código propio) o `CANTIDAD` (N piezas). `activo = false` es baja lógica. |
| `prestamos/{id}` | Estudiante, monitor, `items` (estado de cada artículo al devolver), fechas, carnet retenido, novedad pendiente (`danoPendiente`). `telefono` guarda el celular del estudiante al prestar, para el reporte. |
| `horarios/{id}` | diaSemana (1 = lunes), horaInicio, horaFin (en 24 horas, "14:30"; la app los muestra en 12 horas). |
| `configuracion/general` | fechaFinSemestre. |

Diferencias con el documento de definición: el inventario usa una sola colección (`inventario`) con
`tipoControl` en lugar de `articulos` + `unidades`, y en `prestamos` los campos `fechaLimite`,
`fechaDevolucion` y `estado` se llaman `fechaEntregaEsperada`, `fechaEntregaReal` y
`estadoDevolucion`. Se conservaron los nombres que ya tenía la app para no perder los datos guardados.

## Monitores autorizados y modo de uso

Toda cuenta es de estudiante. Un estudiante es monitor mientras tenga un documento activo en la
colección `monitores_autorizados`, que Bienestar crea y edita en Firebase Console (desde la app
nadie la puede escribir).

**Documento de autorización:** ID = código estudiantil (lo más práctico: está en el carnet) o
uid del estudiante (Firebase Console > Authentication). Campos: `activo` (booleano) y `periodo`
(texto, por ejemplo `2026-2`, solo informativo). Si se usa el código, la autorización vale para
la cuenta que tiene ese código reservado (`codigosEstudiante/{código}`): antes de activarla,
conviene confirmar en `codigosEstudiante/{código}` que el `uid` es el del estudiante (su documento
en `usuarios` tiene su nombre y su correo). No se usa el correo porque las cuentas se registran
con el correo personal, no con el institucional.

**En la app:** la sesión escucha en tiempo real el documento del usuario (por uid y por código).

- Con `activo == true`, la sesión lo trata como monitor y en Ajustes aparece **Modo de uso**, con
  dos opciones: Monitor (gestionar préstamos, devoluciones e inventario) y Estudiante (pedir
  implementos para sí mismo). Al elegir, la app pasa al inicio de ese modo sin cerrar sesión. El
  modo se guarda en `usuarios/{uid}.modoActivo`.
- Si el documento no existe o `activo` pasa a `false` (fin de la monitoría), la sesión vuelve al
  instante a estudiante: si estaba en el panel de monitor, la app lo lleva al inicio del
  estudiante y se lo avisa. A un estudiante sin permiso la sección Modo de uso no le aparece.
- La app copia el permiso en `usuarios/{uid}.esMonitor` y, sin permiso, deja `modoActivo` en
  `estudiante`. Es solo una copia: las reglas de Firestore leen `monitores_autorizados` en cada
  petición, así que un permiso desactivado deja de servir en el servidor aunque la app esté
  cerrada o desactualizada.
- Los préstamos guardan quién los entregó (`monitorId`, `monitorNombre`) y quién recibió la
  devolución; las reglas no dejan cambiar el monitor de un préstamo, así que el historial se
  conserva cuando termina la monitoría.
- Un monitor no puede registrarse un préstamo a sí mismo ni registrar la devolución o resolver
  la novedad de sus propios préstamos: lo hace otro monitor (lo impiden las reglas y la app).

**Para terminar una monitoría:** en `monitores_autorizados/{código}`, poner `activo` en `false`
(o borrar el documento). Para el semestre siguiente basta volver a ponerlo en `true` y cambiar
`periodo`.

**Cuentas de monitor anteriores** (con `rol = MONITOR` o `ADMIN` en `usuarios`): el campo `rol`
ya no da permisos. Hay que crearles su documento en `monitores_autorizados` (por código o, si su
perfil no tiene código, por uid).

**Reglas:** `firestore.rules`, funciones `esMonitor()` (permiso vigente por uid o por código
propio) y `cambiosDeModoValidos()` (el modo monitor solo con permiso vigente). La lógica del modo
está en `ModoUso` (`data/Usuario.kt`, con su prueba `ModoUsoTest`) y en `SesionViewModel`.

## Reglas de negocio implementadas

- La fecha límite de un préstamo no supera el fin de semestre.
- Un estudiante con un préstamo sin devolver (vencido o no) o con una novedad sin resolver no
  recibe otro préstamo.
- El carnet queda retenido hasta la devolución en buen estado. Si algo regresa dañado, incompleto
  o no regresa (extraviado), sigue retenido y el estudiante queda bloqueado hasta que un monitor
  resuelve el caso.
- El estudiante solo puede leer sus propios préstamos (reglas de Firestore).
- El horario de atención va de lunes a sábado.

## Estados de los artículos

| Estado | Cuándo | ¿Se presta? |
|---|---|---|
| Disponible | En Bienestar y en buen estado | Sí |
| Prestado | Lo tiene un estudiante (lo asigna el préstamo) | No |
| Dañado | Roto o deteriorado | No |
| Incompleto | Le faltan piezas (por ejemplo, un juego de ajedrez). No existe para balones ni petos, que son de una sola pieza | Sí, con aviso al monitor y al estudiante |
| Extraviado | Perdido | No |
| En reparación | Lo están arreglando | No |

Una unidad incompleta se presta igual. El préstamo anota que salió incompleta: si vuelve igual
("Como se prestó"), regresa a Incompleto sin bloquear al estudiante; si le faltan más piezas, es
una novedad.

En artículos por cantidad (conos, petos) se cuentan las piezas dañadas y extraviadas; "incompleto"
no aplica a piezas sueltas. Las categorías de una sola pieza están en
`EstadoItem.CATEGORIAS_DE_UNA_PIEZA` (hoy Balones y Petos): ahí la app no ofrece "Incompleto" en el
formulario, en las novedades ni en la devolución, y los repositorios lo rechazan si llega igual.
Las piezas extraviadas se pueden marcar como encontradas o darse por perdidas, lo que las
descuenta del total.

## Registro y verificación de cuentas

- El programa académico se elige de una lista (`data/ProgramasAcademicos.kt`), con la opción
  "Otro programa" para escribirlo a mano.
- El celular es obligatorio: 10 dígitos que empiezan por 3 (celular) o por 60 (fijo), con el +57
  ya puesto. Los estudiantes registrados antes ven un aviso en su inicio para agregarlo.
- Contraseña: mínimo 8 caracteres, con mayúscula, minúscula, número y carácter especial, sin
  espacios. Solo se exige al crear la cuenta; las cuentas existentes entran con su contraseña.
- El correo se escribe dos veces, y los correos de Firebase (verificación y recuperación de
  contraseña) llegan en el idioma elegido en Ajustes.
- Si alguien intenta entrar sin verificar, la app lo explica y ofrece un botón para reenviar el
  enlace (antes se reenviaba solo en cada intento, lo que agotaba el límite de envíos).
- El código estudiantil se reserva en el primer ingreso con el correo verificado. Así, un correo
  mal escrito no deja el código bloqueado, y en la búsqueda del monitor solo salen cuentas
  verificadas. Las cuentas creadas con versiones anteriores aparecen en esa búsqueda después de
  que el estudiante vuelva a iniciar sesión una vez.

## Reportes (RF-17, RF-18, RF-20)

"Reportes e Historial", en el inicio del monitor, tiene tres secciones:

- **Indicadores:** préstamos del día, activos, vencidos, piezas dañadas y artículos más prestados.
- **Exportar:** reportes de préstamos y de inventario en CSV, Excel y PDF, con rango de fechas.
  Al exportar, Android pregunta dónde guardar el archivo (Descargas, Drive...).
- **Historial:** préstamos de un estudiante o de un artículo.

Detalles de los formatos:

- CSV: separador punto y coma y UTF-8 con BOM, que es lo que Excel en español abre sin configurar nada.
- Excel: el `.xlsx` se genera directamente (un ZIP con XML), sin Apache POI ni otra biblioteca.
- PDF: `PdfDocument` de Android, A4 horizontal; el texto que no cabe en una columna se recorta.

## Idioma (español e inglés)

Ajustes > Idioma ofrece **Español** y **English**, cada uno escrito en su propio idioma. El
cambio se ve al instante en toda la app, sin reiniciarla, y queda guardado. Si nunca se ha
elegido, la app abre en español.

**Dónde están los textos:** `res/values/strings.xml` (español, el idioma base) y
`res/values-en/strings.xml` (inglés), con las mismas claves. Las claves empiezan por la pantalla
o el módulo (`comun_`, `ingreso_`, `registro_`, `inventario_`, `prestamo_`, `devolucion_`,
`estudiante_`, `reportes_`, `ajustes_`...). Los conteos ("1 préstamo" / "3 préstamos") usan
`<plurals>`. Si a values-en le faltara un texto, Android muestra el de español.

**Cómo se usan en el código:**

- En pantallas (`@Composable`): `stringResource(R.string.clave)` y
  `pluralStringResource(R.plurals.clave, cantidad, cantidad)`.
- En ViewModels, repositorios, reportes y notificaciones: `Idioma.texto(R.string.clave)` y
  `Idioma.plural(...)`.
- Un `remember` que calcula un texto traducido lleva `Idioma.actual` entre sus claves, para
  recalcularse al cambiar el idioma.

**Cómo funciona:** `Idioma.actual` (en `utils/Idioma.kt`) es un estado de Compose, como
`Colores.tema`. `ProveedorIdioma`, que envuelve la app en `MainActivity`, entrega a las pantallas
un contexto y una configuración con ese idioma, así que también cambian los textos propios de
Material. Los diálogos, los menús desplegables y el calendario se dibujan en ventanas propias que
vuelven a tomar los textos de la actividad: por eso `MainActivity` sobrescribe `getResources()`
con `RecursosConIdioma` (en `utils/Idioma.kt`), y un diálogo nuevo sale traducido sin hacer nada
más. `Idioma.iniciar` lee el idioma guardado al abrir la app y al empezar la revisión diaria de
recordatorios, que puede correr con la app cerrada.

**Datos de Firestore:** no cambian. Estados (`DISPONIBLE`...), categorías, roles, programas y
días se siguen guardando en español y se muestran traducidos con `EstadoItem.etiqueta`,
`Categorias.etiqueta`, `ProgramasAcademicos.etiqueta` y `DiasSemana.nombre`. Lo que escriben las
personas (nombres y descripciones de artículos, observaciones) se muestra tal cual. Tres textos
que la app guarda quedan en español: el motivo de bloqueo del estudiante (las pantallas muestran
en su lugar un aviso traducido), la nota "Novedad resuelta: …" y el sufijo "(Unidad N)" de los
artículos creados por lote.

**Reportes:** títulos, columnas, estados, categorías y programas salen en el idioma activo al
exportar; los datos escritos por las personas, tal cual.

**Al agregar un texto:** ponerlo en los dos archivos con la misma clave y los mismos marcadores
(`%1$s`, `%2$d`...), nunca escrito directamente en el código. `TraduccionesTest` revisa que cada
clave tenga su traducción y los mismos marcadores (`./gradlew test`).

**Si se actualiza Compose:** desde Compose UI 1.9 (`LocalResources`), `stringResource` deja de
leer el contexto. Al subir el BOM hay que agregar `LocalResources provides
contextoConIdioma.resources` en `ProveedorIdioma`.

## Ícono y pantalla de carga

- **Ícono:** el logo de DEPORTSTOCK sobre negro. Es un ícono adaptable
  (`mipmap-anydpi-v26/ic_launcher.xml`): fondo negro (`drawable/ic_launcher_background.xml`) y el
  logo en `mipmap-*/ic_launcher_foreground.png`, con el contenido dentro del círculo seguro de
  66 dp para que el lanzador no corte el nombre con ninguna forma. `mipmap-*/ic_launcher.webp` e
  `ic_launcher_round.webp` son las versiones fijas del mismo logo.
- **Pantalla de carga:** el logo sobre negro (`drawable-xxxhdpi/logo_carga.png`) se ve solo con
  sesión: al abrir la app con la sesión guardada y justo después de ingresar, hasta saber a qué
  inicio ir (y si es monitor). Sin sesión (por ejemplo, para crear una cuenta) la app abre directo
  en el ingreso, sin logo. No hay círculo de carga.
  - Android dibuja su propia pantalla de carga (biblioteca `core-splashscreen`) antes de que la
    app sepa si hay sesión: es solo el fondo negro (`Theme.MyApplication.Carga`) y se quita de
    golpe, sin animación, con el primer cuadro de la app.
  - El logo lo dibuja la app (`PantallaLogo` en `AppNavigation`), del mismo tamaño y en la misma
    posición, sin cortes hasta que aparece el inicio.
  - En Android 13 o superior, con sesión iniciada, `MainActivity` deja elegida para la próxima
    apertura la pantalla del sistema con el logo (`Theme.MyApplication.CargaConLogo`,
    `setSplashScreenTheme`): el logo sale desde que se toca el ícono. Al cerrar sesión vuelve a
    la de fondo negro. En versiones anteriores, primero se ve un instante el fondo negro.
- **Cambiar el logo:** reemplazar esos PNG manteniendo los tamaños (108 dp para el primer plano,
  288 dp para la pantalla de carga) y el contenido dentro del círculo central (66 dp de 108 y
  192 dp de 288).

## Fotos de los implementos

Los artículos se muestran con una foto en lugar del emoji: tarjetas del inventario y del catálogo
del estudiante, filtros y formulario del inventario, artículos elegidos en Registrar Préstamo,
diálogos de novedades y de dar de baja, y novedades en Reportes.

- Fotos en `res/drawable-nodpi/articulo_*.webp` (fondo transparente, 288 × 288 px, el objeto
  centrado y del mismo tamaño visual): balón de fútbol, baloncesto y voleibol, raqueta de tenis de
  mesa y de campo, peto, cono, ajedrez, cuerda, cronómetro y colchoneta.
- `data/TipoArticulo.kt` decide la foto: primero por el nombre ("Balón de Baloncesto Molten #7"),
  luego por el emoji guardado en `icono` y por último por la categoría. Si no reconoce el artículo,
  muestra su emoji, como antes. Los datos de Firestore no cambian.
- `ui/components/ImagenArticulo.kt` (`ImagenArticulo`, `ImagenCategoria`) dibuja la foto.
- Para agregar un tipo: la foto en `drawable-nodpi` con el nombre `articulo_<tipo>.webp`, el tipo
  en `TipoArticulo` (con sus palabras clave) y su línea en `TipoArticulo.imagen()`.
  `TipoArticuloTest` comprueba que cada tipo tenga su foto.

## Horas

Todas las horas se muestran en formato de 12 horas con AM y PM, en los dos idiomas
(`Fechas.conHora`: "29/11/2026 - 3:45 PM"; `Fechas.hora`: "2:30 PM"): préstamos, devoluciones,
reportes e historial, y el horario de atención. El campo de hora de una franja nueva también es
de 12 horas. El horario se sigue guardando en 24 horas ("14:30"), así que los datos no cambian.

## Recordatorios de devolución (RF-19)

El estudiante ve un aviso en la app y recibe una notificación diaria cuando faltan 3 días o menos
para la fecha límite, o cuando el préstamo ya venció. El monitor recibe una notificación diaria
si hay préstamos vencidos sin devolver. La notificación es local (WorkManager revisa una vez al
día en el teléfono); no usa Firebase Cloud Messaging porque enviar mensajes programados desde FCM
exige un servidor o Cloud Functions. Requiere haber abierto la app al menos una vez y, en
Android 13 o superior, aceptar el permiso. Se puede apagar en Ajustes.

## Ajustes y temas

El ícono de engranaje de la barra superior abre Ajustes, igual para monitor y estudiante:

- **Foto de perfil:** se cambia tocando el círculo de la tarjeta de la cuenta (también en el
  inicio del monitor): galería, cámara o quitar. No pide permisos: el selector de fotos y la
  cámara del sistema solo le entregan a la app la imagen elegida. Se guarda en Firestore
  (`fotosPerfil`) porque Cloud Storage for Firebase exige el plan de pago Blaze.
- **Opciones de usuario:** datos de la cuenta (nombre, correo, rol, ID, programa y celular),
  actualizar información (nombre, programa y celular; el correo y el código no se cambian),
  cambiar contraseña (pide la actual y aplica las reglas de contraseña segura) y cerrar sesión.
- **Modo de uso** (solo monitores con permiso vigente): elegir entre usar la app como monitor o
  como estudiante.
- **Opciones de la aplicación:** apariencia, idioma (Español o English), notificaciones de
  préstamos y vencimientos, reglamento y acerca de.

El tema, el idioma y el interruptor de notificaciones se guardan en el teléfono
(SharedPreferences), no en Firestore. La app abre directamente con el tema y el idioma guardados.

| Tema | Uso |
|---|---|
| Oscuro | El aspecto original (predeterminado) |
| Claro | Fondo blanco y texto de alto contraste (el texto principal supera 4,5:1) |
| Udenar (institucional) | Colores de la Universidad de Nariño, tomados de SAPIENS: verde `#00923F`, verde oscuro `#14572A`, fondo `#F4FAF7`, texto `#333333` |
| Cyberpunk (en *Ver más diseños*) | Neón sobre azul noche, paneles HUD con esquinas cortadas, letra condensada y animaciones |
| Retro (en *Ver más diseños*) | Ropa deportiva vintage: papel arena `#F3EAD8`, tarjetas marfil `#FFFDF9` con borde carbón `#231F20` de 1,5 dp, barra verde petróleo `#0F5C6E` con íconos mostaza `#E9A825`, acentos coral `#E05A47` |
| Varsity (en *Ver más diseños*) | Chaqueta universitaria: fondo hueso `#FAF6F0`, tarjetas blancas con borde suave `#E5DEC9`, barra borgoña `#8B1E3F` con íconos dorados `#D4A373`, texto azul marino `#14213D` |
| Solar (en *Ver más diseños*) | Para usar al sol en las canchas: blanco puro, texto negro, tarjetas con borde negro de 2 dp, azul cobalto `#0051FF` y etiquetas de estado de color sólido con texto blanco |

**Ver más diseños:** Ajustes muestra los tres temas principales (`TemaApp.PRINCIPALES`). El
botón *Ver más diseños* abre la lista de todos los diseños (`AparienciaScreen`), cada uno con su
vista previa. Para agregar un diseño:

1. Agregar el valor a `TemaApp` (en `PreferenciasRepository.kt`).
2. Agregar su paleta en `Colores.kt` y su rama en `Colores.paletaDe` y en `DeportStockTheme`.
   Los tres últimos campos de `PaletaApp` son opcionales: `rellenoNeutro` (relleno de botones
   secundarios y filtros; por defecto, el color del borde), `grosorBorde` (borde de todas las
   tarjetas, sin sombra) y `estadosSolidos` (etiquetas de estado de color lleno).
3. Agregar la paleta a `ContrasteTemasTest` y correr `./gradlew test`.
4. Listo: aparece solo en la lista, sin tocar Ajustes.

**Retro, Varsity y Solar:** las tarjetas son planas y llevan el borde del tema
(`TarjetaApp` lo pone solo). El coral, la mostaza y el dorado puros no llegan a 4,5:1 como texto
sobre fondo claro (3,7:1, 2,0:1 y 2,3:1): van puros en íconos, barra y fondos, y en textos y
etiquetas se usa el mismo tono más oscuro. En Solar las etiquetas de estado (DISPONIBLE,
PRESTADO, DAÑADO, VENCIDO, el código del artículo...) son de color sólido con texto blanco, que
se distingue mejor al sol que un tinte claro.

**Contraste:** `app/src/test/.../ContrasteTemasTest.kt` mide, con la fórmula de WCAG 2.1, los
pares de colores que la app pone juntos (texto sobre fondos y tarjetas, etiquetas sobre su
tinte, botones, barras, bordes de campos). Retro, Varsity y Solar pasan todos: ningún texto
baja de 4,69:1. De los cuatro temas anteriores revisa el texto principal; algunas de sus
etiquetas de estado quedan entre 3,3:1 y 4,4:1.

**Tema Cyberpunk:** las piezas están en `ui/theme/Neon.kt` (colores, reloj de las animaciones y
trazo con brillo) y en `ui/components/NeonKit.kt` (`PanelNeon`, `MarcoIconoNeon`,
`TituloSeccionNeon`, `ChipNeon`, `MarcoPieNeon` y `FondoApp`). El inicio del monitor y Ajustes
son una copia de los diseños del equipo; el resto de la app usa las mismas piezas en versión
más liviana:

- Tarjetas de sección (préstamo, horario, reportes): panel HUD con el ícono enmarcado.
- Elementos de listas (inventario, préstamos, catálogo): borde neón fino, sin animación, con una
  barra a la izquierda del color del estado (disponible, prestado, vencido, novedad...).
- Diálogos: panel liviano, ícono en un marco del color de la acción (rojo para cerrar sesión o
  dar de baja) y una línea neón bajo el título.
- Botones, campos, etiquetas, chips, menús y calendario: esquinas cortadas.
- Ingreso y registro: la foto de fondo en blanco y negro con un velo cian y magenta.
- Animaciones: brillo que "respira", luz que recorre los bordes, arcos que se mecen alrededor de
  la foto y una banda de escaneo en el fondo. Se dibujan sin recomponer la pantalla: cada panel
  tiene su propia capa y lee el reloj solo al dibujar.
- Si el teléfono tiene las animaciones desactivadas (Accesibilidad > Quitar animaciones), el tema
  se ve igual pero quieto.
- Con Cyberpunk activo y la app en pantalla, la animación se dibuja en cada cuadro: gasta algo
  más de batería que los otros temas.

**Al agregar una pantalla**, para que se vea bien en todos los temas, usa las piezas que se
adaptan solas (en Oscuro, Claro y Udenar dibujan exactamente lo mismo que la pieza de Material):

| En lugar de | Usa |
|---|---|
| `Card` / `ElevatedCard` | `TarjetaApp` / `TarjetaElevadaApp` (mismos parámetros, más `estiloNeon`) |
| `AlertDialog` | `DialogoApp` (mismos parámetros, más `tonoNeon`) |
| `RoundedCornerShape(12.dp)` | `formaApp(12.dp)` |
| Forma de `Button` y `OutlinedButton` | `shape = formaBoton()` |
| Círculo de color detrás de un ícono | `Modifier.fondoIconoApp(color)` |
| Búsqueda o filtros fijos con una lista debajo | `ListaConEncabezado` |
| Etiqueta de estado: `color.copy(alpha = 0.18f)` de fondo y `color` de texto | `Colores.rellenoEtiqueta(color, 0.18f)` y `Colores.textoEtiqueta(color)` |
| `Colores.borde` como relleno de un botón o un filtro | `Colores.rellenoNeutro` (en Retro y Solar el borde es oscuro) |
| `BorderStroke(1.dp, Colores.borde)` en una tarjeta hecha con `Surface` | `BorderStroke(Colores.anchoBorde(1.dp), Colores.borde)` |

**Teléfono girado:** con poca altura (menos de 480 dp, `alturaCompacta()`) los diálogos no
muestran el ícono, en inventario, devolución, catálogo e historial la búsqueda y los filtros se
desplazan junto con la lista (`ListaConEncabezado`), el ingreso se desplaza y el calendario se
abre en modo de escribir la fecha. **El contenido de un diálogo nuevo debe ir en una columna con
`verticalScroll`**, o con el teléfono girado no se alcanza a ver.

Las pantallas no usan colores fijos: usan nombres de uso de `Colores` (`Colores.texto`,
`Colores.superficie`, `Colores.error`...). **Al agregar una pantalla, usa esos nombres en vez de
`Color.White` o `Color(0xFF...)`**, o se verá mal en los temas claros.

**Logo de la universidad:** está en `res/drawable-nodpi/`: `logo_udenar.png` (solo el sello) y
`logo_udenar_completo.png` (sello y nombre). Son grises sobre fondo transparente; en el tema
oscuro el logo completo se pinta del color del texto. Dónde aparece:

- Tema Udenar: sello en la franja verde de los inicios y de Ajustes, y logo completo
  debajo de la tarjeta de ingreso.
- Todos los temas: logo completo en Ajustes > Acerca de la aplicación.

## Contactar al estudiante

El monitor ve el celular del estudiante, con botones **Llamar** (abre el marcador, no necesita
permisos) y **WhatsApp**, en:

- Registrar Préstamo, al elegir al estudiante (junto con su foto, para confirmar la identidad).
- Registrar Devolución, en cada préstamo pendiente, vencido o con novedad. Se usa el número
  actual del perfil, así que si el estudiante lo cambia, el monitor ve el nuevo.
- El reporte de préstamos (columna "Celular").

Si el estudiante no registró celular, la app lo indica en esos mismos lugares.
