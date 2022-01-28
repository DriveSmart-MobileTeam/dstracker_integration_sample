`Ver esta guía en otros idiomas:`  [![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-English-success)](https://github.com/DriveSmart-MobileTeam/dstracker_integration_sample/blob/main/README-JAVA.md)
[![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-Kotlin-success)](https://github.com/DriveSmart-MobileTeam/dstracker_integration_sample/blob/main/README-ES.md)

# Integración de Tracker (Java)

En esta guía de inicio rápido, se describe cómo configurar el Tracker de Drive-Smart en tu app para que puedas evaluar los viajes realizados a través de dispositivos Android.

La configuración del Tracker de Drive-Smart requiere realizar tareas en el IDE. Para finalizar la configuración, deberás realizar un viaje de prueba a fin de confirmar el funcionamiento correcto del entorno.

# Tabla de contenidos
1. [Requisitos](#requisitos)
2. [Instalación](#instalacin)
3. [Permisos](#permisos)
4. [Interfaz pública](#interfaz-pblica)
5. [Configuración](#configuracin)
5. [Vinculación de usuarios](#vinculacin-de-usuarios)
6. [Registro de viajes](#registro-de-viajes)
  1. [Modos del Tracker](#modos-del-tracker)
  2. [Control de viajes en modo manual](#control-de-viajes-en-modo-manual)
  3. [Información del viaje](#informacin-del-viaje)

## Requisitos

Si aún no lo has hecho, descarga e instala el entorno de desarrollo y las librerias de Android. La integración se realizará sobre la siguiente versión:
* Android Studio Artic Fox | 2020.3.1
* Runtime version: 11.0.10+
* Gradle 7.0+
* En tu IDE aseguraté de tener configurado JAVA 11

![java11.jpg](https://i.imgur.com/2IcZ1Tv.jpeg)

## Instalación

* En el archivo `settings.gradle` de **nivel de proyecto**, agrega el complemento de Maven con la licencia de Drive-Smart para Gradle como dependencia.

  ```yaml
  dependencyResolutionManagement {
      repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
      repositories {
          google()
          mavenCentral()
          maven {
              url 'https://tfsdrivesmart.pkgs.visualstudio.com/5243836b-8777-4cb6-aded-44ab518bc748/_packaging/Android_Libraries/maven/v1'
              name 'Android_Libraries'
              credentials {
                  username "user"
                  password "password"
              }
          }
      }
  }
  ```
* En tu archivo `build.gradle` de **nivel de app**, aplica el complemento del Tracker para Gradle:

```
dependencies {
	// ......
	implementation 'DriveSmart:DS-Tracker:1.1.4'
  	// ......
}
```


## Permisos

Es necesario definir los permisos correspondientes, en caso contrario el Tracker responderá distintos mensajes de error.

Permisos en `Manifest` del proyecto:

```
<!-- ... -->
<!-- Servicio para creación/consulta usuario -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

<!-- Evaluación de viajes -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- Evaluación automática de viajes -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<!-- ... -->
```
Permisos de ubicación que deben consultarse y estar activos en las clases del proyecto.
```
// ...
Manifest.permission.ACCESS_COARSE_LOCATION
Manifest.permission.ACCESS_FINE_LOCATION
// ...
```

Además de los permisos básicos para la evaluación de viajes indicados previamente, es necesario el siguiente permiso para poder activar la grabación automática de viajes:

```
// ...
Manifest.permission.ACCESS_BACKGROUND_LOCATION
// ...
```

Por último, se debe confirmar que la aplicación no está incluida como aplicación optimizada. Para ello se puede consultar el estado con el siguiente extracto de código:

```javascript
// ...
PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
boolean isbatteryOptimized = powerManager.isDeviceIdleMode() && !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
// ...
```

Si todos los permisos indicados están correctamente configurados, el entorno estará configurado y se podrán realizar viajes.


## Interfaz pública

* En el archivo `Java o Kotlin` del **proyecto**, agrega la interfaz `DSManagerInterface` e implementa los métodos indicados. Dicha interfaz será la encargada de recibir los eventos que el Tracker genera. El programador decidirá que clase es la encargada.

  ```java
  @Override
  public void startService(@NonNull DSResult result) {}
  
  @Override
  public void stopService(@NonNull DSResult result) {}
  
  @Override
  public void statusEventService(@NonNull DSResult dsResult) {}
  ```
  
## Configuración

* En el archivo `Java o Kotlin` del **proyecto**, agrega el objeto principal del Tracker e inicializa:

  ```java
    // ...
    private Tracker tracker;
    private String apkID;
    private String userID;
    // ...
  
    private void defineConstants() {
        // TODO
        apkID = "";
        userID = "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding.setLifecycleOwner(this);
        setContentView(binding.getRoot());

        defineConstants();
        prepareEnvironment();

        prepareView();
    }
  
    private void prepareEnvironment() {
        tracker = Tracker.getInstance(this);
        tracker.configure(apkID, dsResult -> {
            if (dsResult instanceof DSResult.Success) {
                addLog("SDk configured");
                identifyEnvironmet(userID);
            }else{
                String error = ((DSResult.Error) dsResult).getError().getDescription();
                addLog("Configure SDK: "+error);
            }
            return null;
        });
    }
  
    // ...
  ```

## Vinculación de usuarios

Para que el Tracker de Drive-Smart pueda crear viajes se necesita un identificador de usuario *único.*

```javascript
// ... 
dsManager.setUserID(USERID, result -> {
    Log.e("DRIVE-SMART", "Defining USER ID: " + USERID);          
    return null;
});
// ... 
```

Para obtener un identificador de usuario válido, se puede consultar el siguiente servicio, el cual creará un nuevo usuario en el sistema de Drive-Smart o devolverá el usuario en caso de existir.

```javascript
private void getOrAddUser(String user) {
    dsManager.getOrAddUserIdBy(user, new Continuation<DSDKUserSession>() {
        @Override
        public void resumeWith(@NonNull Object o) {
            if(o instanceof DSDKUserSession){
                userSession = (DSDKUserSession)o;
                Log.e("DRIVE-SMART", "User id: " + ((DSDKUserSession)o).getDsUserId());
            }
        }
        @Override
        public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }
    });
}
```

Si el objeto recibido es valido, a continuación, se debe definir el userID en el método del Tracker ya comentado


## Registro de viajes

### Modos del tracker

El tracker ofrece tres modos de funcionamiento:
- Manual: se debe iniciar y finalizar los viajes de forma manual
- Por manos libres: Los viajes comienzan cuando se conectan a un dispositivo bluetooth seleccionado, y finalizan cuando se desconectan de este.
- Por movimiento: Según el movimiento detectado por los sensores del dispositivo, el tracker decide cuando se inicia o finaliza un viaje.

### Control de viajes en modo manual

Para controlar el Tracker se muestran las siguientes acciones:
```
// ...
// Iniciar un viaje:
dsManager.start()
// Parar un viaje:
dsManager.stop()
// Enviar los datos de trackeo pendientes de envío:
dsManager.sendPendingTrackingData()
// ...
```

### Información del viaje:
Una vez iniciado un viaje, el Tracker ofrece una serie de métodos para poder obtener información del viaje. *DSCheckStatus* se obtiene a través del método *checkService()* con la información:
+ Distancia total
+ Tiempo de viaje.
+ Trip ID
+ Estado del GPS.
+ Estado del viaje.
```
// ...
DSCheckStatus beanStatus = dsManager.checkService();
// ...
```
A su vez, el método *tripInfo()* ofrece otros datos:
+ Posición inicial del viaje.
+ Última posición obtenida.
```
// ...
DSInfoTrip info = dsManager.recordingTripInfo();
// ...
```
Si quieres saber si el servicio del tracker está funcionando en ese momento:
```
// ...
dsManager.isRunningService()
// ...
```
