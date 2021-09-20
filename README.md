En esta guía de inicio rápido, se describe cómo configurar el SDK de Drive-Smart en tu app para que puedas evaluar los viajes realizados a través de dispositivos Android.

La configuración del SDK de Drive-Smart requiere realizar tareas en el IDE. Para finalizar la configuración, deberás realizar un viaje de prueba a fin de confirmar el funcionamiento correcto del entorno.


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
* En tu archivo `build.gradle` de **nivel de app**, aplica el complemento del SDK para Gradle:

```
dependencies {
	// ......
	implementation 'DriveSmart:DS-SDK:5.20.28'
  	// ......
}
```


## Permisos

Es necesario definir los permisos correspondientes, en caso contrario el SDK responderá distintos mensajes de error.

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

* En el archivo `Java o Kotlin` del **proyecto**, agrega la interfaz `DSManagerInterface` e implementa los métodos indicados. Dicha interfaz será la encargada de recibir los eventos que el SDK genera. El programador decidirá que clase es la encargada.

  ```java
  @Override
  public void startService(@NonNull DSResult result) {}
  
  @Override
  public void statusEventService(@NonNull DSResult dsResult) {}
  
  @Override
  public void stopService(@NonNull DSResult result) {}
  
  @Override
  public void motionDetectedActivity(@NonNull DSInternalMotionActivities dsInternalMotionActivities, int i) { }
  
  @Override
  public void motionStatus(@NonNull DSMotionEvents dsMotionEvents) { }
  ```
  
## Configuración

* En el archivo `Java o Kotlin` del **proyecto**, agrega el objeto principal del sdk e inicializa:

  ```java
  // ...
  private DSManager dsManager;
  // ...
  
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
  	super.onViewCreated(view, savedInstanceState);
    	// ...
    	dsManager = DSManager.getInstance(requireActivity());
    		dsManager.configure(apkID, dsResult -> {
            	if (dsResult instanceof Success) {
              	Log.e("DRIVE-SMART", "SDk configured");          
                
                	// Interfaz previamente comentada.
                	dsManager.setListener(this);
              }else{
                	String error = ((DSResult.Error) dsResult).getError().getDescription();         
                	Log.e("DRIVE-SMART", error);
              }
            	return null;
              });
          }
  	// ...
  }
  ```

## Vinculación de usuarios

Para que el SDK de Drive-Smart pueda crear viajes se necesita un identificador de usuario *único.*

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

Si el objeto recibido es valido, a continuación, se debe definir el userID en el método del SDK ya comentado


## Paso 4: Evaluación de viajes

### Control de viajes básico:

Para controlar el SDK se muestran las siguientes acciones:
```
// ...
// Iniciar un viaje:
dsManager.startService()
// Pausar un viaje:
dsManager.pauseService()
// Parar un viaje:
dsManager.stopService()
// ...
```

### Control de viajes automático o semi automático

Es preciso configurar el SDK de Drive-Smart para que permita la evaluación automática o semi-automática de viajes. Si se configura la evaluación automática, no es necesario tener la aplicación abierta. 

En el caso de configurar la evaluación de viajes semi-automática, es imprescindible mantener la aplicación abierta.

Inicialmente, desactivamos la función automática de evaluación de viajes, al finalizar la operación, definimos como queremos evaluar viajes:

* **Automática:**
  Se define el primer parametro a `TRUE` y el segundo a `TRUE`.

* **Semi-Automática:**
  Se define el primer parametro a `TRUE` y el segundo a `FALSE`.

```javascript

// ... 
// AUTOMÁTICO:
dsManager.setMotionStart(false, dsResult -> {
    Handler handler = new Handler(Looper.getMainLooper());
    handler.postDelayed(() -> dsManager.setMotionStart(true, true, dsResult2 -> {
	    return null;
    }), 2000);
    return null;
});

// ... 

// SEMI-AUTOMÁTICO:
dsManager.setMotionStart(false, dsResult -> {
    Handler handler = new Handler(Looper.getMainLooper());
    handler.postDelayed(() -> dsManager.setMotionStart(true, false, dsResult2 -> {
	    return null;
    }), 2000);
    return null;
});

// ... 

```

Se puede consultar si el modo automático está activo:

```
// ... 
dsManager.isMotionServiceAlive()
// ... 
```

### Información del viaje:
Una vez iniciado un viaje, el SDK ofrece una serie de métodos para poder obtener información del viaje. *DSCheckStatus* se obtiene a través del método *checkService()* con la información:
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
DSInfoTrip info = dsManager.tripInfo();
// ...
```
