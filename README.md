`See this guide in other languages:`  [![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-Spanish-success)](https://github.com/DriveSmart-MobileTeam/dstracker_lite_integration_sample/blob/main/README-ES.md)
[![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-Java-success)](https://github.com/DriveSmart-MobileTeam/dstracker_lite_integration_sample/blob/main/README-JAVA.md)

# Tracker Integration (Kotlin)

This quick start guide describes how to configure the DriveSmart Tracker library in your app so that you can evaluate the driving activity tracked by Android devices.

The configuration of DriveSmart Tracker library requires IDE tasks. To finish the setup, you will need to perform a driving test to confirm the correct operation of the environment.

# Table of contents
1. [Requirements](#requirements)
2. [Installation](#installation)
3. [Permissions](#permissions)
4. [Public interface](#public-interface)
5. [Configuration](#configuration)
5. [User linking](#user-linking)
6. [Trip analysis](#trip-analysis)
1. [Tracker modes](#tracker-modes)
2. [Trip analysis in manual mode](#trip-analysis-in-manual-mode)
3. [Trip info](#trip-info)

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

<!-- Automatic trip evaluation -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
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


## Public interface

* In the ** project ** file, add the interface `DSManagerInterface` and implement the indicated methods. This interface will be in charge of receiving the events that the Tracker generates. The programmer will decide which class is in charge.
  
  ```kotlin
  override fun startService(dsResult: DSResult) {}
  
  override fun stopService(dsResult: DSResult) {}
  
  override fun statusEventService(dsResult: DSResult) {}
  ```

## Configuration
* In the **project** file, add the library main object and initialize it:

  ```kotlin
    // ...
    private Tracker tracker
    private lateinit var apkID: String
    private lateinit var userID: String
    // ...
  
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ...
        defineConstants()
        prepareEnvironment() 
        // ...
    }
  
    private fun defineConstants() {
        apkID = ""
        userID = ""
    }
  
    private fun prepareEnvironment() {
        tracker = Tracker.getInstance(this)
        tracker.configure(apkID) { dsResult: DSResult ->
            if (dsResult is DSResult.Success) {
                addLog("Tracker configured")
                identifyEnvironmet(userID)
            } else {
                val error: String = dsResult.toString()
                addLog("Configure Tracker: $error")
            }
        }
    }
  
    // ...
  ```


## User linking
A unique user identifier is required for the DriveSmart Library to create trips.

```kotlin
// ... 
tracker.setUserId(uid) {
  addLog("Defining USER ID: $uid")
}
// ... 
```

To obtain a valid user identifier, the following service can be consulted, whitch will create a new user in the DriveSmart System or return the user if it exist.

```kotlin
private fun getOrAddUser(user: String) {
  GlobalScope.launch(Dispatchers.Main) {
    val session = getUserSession(user)

    userSession = session
    addLog("User id created: $session")
  }
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
dsManager.startService();
// Pause a trip:
dsManager.pauseService();
// Stop a trip:
dsManager.stopService();
// ...
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
DSInfoTrip info = dsManager.tripInfo();
// ...
```
