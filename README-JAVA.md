`See this guide in other languages:`  [![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-Spanish-success)](https://github.com/DriveSmart-MobileTeam/dstracker_integration_sample/blob/main/README-JAVA-ES.md)
[![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-Kotlin-success)](https://github.com/DriveSmart-MobileTeam/dstracker_integration_sample/blob/main/README.md)

# Tracker Integration (Java)

This quick start guide describes how to configure the DriveSmart Tracker library in your app so that you can evaluate the driving activity tracked by Android devices.

The configuration of DriveSmart Tracker library requires IDE tasks. To finish the setup, you will need to perform a driving test to confirm the correct operation of the environment.

# Table of contents
1. [Requirements](#requirements)
2. [Installation](#installation)
3. [Permissions](#permissions)
4. [Configuration](#configuration)
5. [User linking](#user-linking)
6. [Trip analysis](#trip-analysis)
  1. [Tracker modes](#tracker-modes)
  2. [Trip analysis in manual mode](#trip-analysis-in-manual-mode)
  3. [Public interface](#public-interface)
  4. [Trip info](#trip-info)

## Requirements
If you haven't already, download and install the Android Development Environment and libraries. The integration will be carried out on the next versions:
* Android Studio Artic Fox | 2020.3.1
* Runtime version: 11.0.10+
* Gradle 7.0+
* In your IDE make sure you have Java 11 configured

![java11.jpg](https://i.imgur.com/2IcZ1Tv.jpeg)

## Installation

* In the **project level** `settings.gradle` file, add the Maven plugin with the DriveSmart License for Gradle as a dependency.

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
* In the **app level** `build.gradle` file, apply the Tracker plugin for Gradle:

```
dependencies {
	// ......
	implementation 'DriveSmart:DS-Tracker:1.0'
  	// ......
}
```


## Permissions

It is necessary to define the corresponding permissions, otherwise the library will respond with different error messages.

Project Permissions in `Manifest`:

```
<!-- ... -->

<!-- Services for user creation/query -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

<!-- Trip evaluation -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<!-- ... -->
```
Location permissions to be queried and be active in project classes.
```
// ...
Manifest.permission.ACCESS_COARSE_LOCATION
Manifest.permission.ACCESS_FINE_LOCATION
// ...
```

In addition to the basic permissions for the evaluation of trips indicated previously, the following permission is mandatory to be able to activate the automatic recording of trips:

```
// ...
Manifest.permission.ACCESS_BACKGROUND_LOCATION
// ...
```

If all the permissions indicated are correctly configured, the environment will be configured and trips can be made.

## Configuration
* In the **project** file, add the library main object and initialize it:

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
              	Log.e("DRIVE-SMART", "Tracker configured");          
                
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

## Configuration
* In the **project** file, add the library main object and initialize it:

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

## User linking
A unique user identifier is required for the DriveSmart Library to create trips.

```javascript
// ... 
dsManager.setUserID(USERID, result -> {
    Log.e("DRIVE-SMART", "Defining USER ID: " + USERID);          
    return null;
});
// ... 
```

To obtain a valid user identifier, the following service can be consulted, whitch will create a new user in the DriveSmart System or return the user if it exist.

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

If the received object is valid, then the userId must be defined in the library method already commented.

## Trip analysis

### Tracker modes

El tracker ofrece tres modos de funcionamiento:
- Manual: se debe iniciar y finalizar los viajes de forma manual
- Por manos libres: Los viajes comienzan cuando se conectan a un dispositivo bluetooth seleccionado, y finalizan cuando se desconectan de este.
- Por movimiento: Según el movimiento detectado por los sensores del dispositivo, el tracker decide cuando se inicia o finaliza un viaje.

### Trip analysis in manual mode

To control the Tracker we can use this actions:

```
// ...
// Initiate a trip:
dsManager.start();
// Stop a trip:
dsManager.stop();
// Enviar los datos de trackeo pendientes de envío:
dsManager.sendPendingTrackingData();
// ...
```

### Public interface

* In the ** project ** file, add the interface `DSManagerInterface` and implement the indicated methods. This interface will be in charge of receiving the events that the Tracker generates. The programmer will decide which class is in charge.

  ```java
  @Override
  public void startService(@NonNull DSResult result) {}
  
  @Override
  public void stopService(@NonNull DSResult result) {}
  
  @Override
  public void statusEventService(@NonNull DSResult dsResult) {}
  ```

### Trip info:
Once a trip has started, Tracker offers a method for obtaining trip information. *TrackingStatus* is obtained throught the *getStatus()* method with the info:
+ Total distance
+ Trip time
+ Trio id
+ GPS Status
+ Trip Status

```
// ...
DSCheckStatus beanStatus = dsManager.checkService();
// ...
```
In turn, the * tripInfo () * method offers other data:
+ Initial position of the trip.
+ Last position obtained.
```
// ...
DSInfoTrip info = dsManager.recordingTripInfo();
// ...
```
If you want to know if the tracker service is working right now:
```
// ...
dsManager.isRunningService()
// ...
```
