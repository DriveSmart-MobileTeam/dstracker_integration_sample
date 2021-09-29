`See this guide in other languages:`  [![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-Spanish-success)](https://github.com/DriveSmart-MobileTeam/dstracker_integration_sample/blob/main/README-JAVA-ES.md)
[![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-Kotlin-success)](https://github.com/DriveSmart-MobileTeam/dstracker_integration_sample/blob/main/README.md)

# DSTracker Integration (Java)

This quick start guide describes how to configure the DriveSmart Tracker library in your app so that you can evaluate the driving activity tracked by Android devices.

The configuration of DriveSmart Tracker library requires IDE tasks. To finish the setup, you will need to perform a driving test to confirm the correct operation of the environment.

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

`See this guide in other languages:`  [![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-Spanish-success)](https://github.com/DriveSmart-MobileTeam/dstracker_lite_integration_sample/blob/main/README-ES.md)
[![DSTracker](https://img.shields.io/badge/DSTracker%20Integration-Java-success)](https://github.com/DriveSmart-MobileTeam/dstracker_lite_integration_sample/blob/main/README-JAVA.md)

# DSTracker Integration (Kotlin)

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

## Public interface

* In the ** project ** file, add the interface `DSManagerInterface` and implement the indicated methods. This interface will be in charge of receiving the events that the Tracker generates. The programmer will decide which class is in charge.

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

## Step 4: Trip analysis

### Basic trip control:

To control the DSTracker we can use this actions:

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

### Automatic or semi automatic trip control

The Drive-Smart Tracker must be configured to allow automatic or semi-automatic evaluation of trips. If automatic evaluation is configured, it is not necessary to have the application open.

In the case of configuring semi-automatic travel evaluation, it is essential to keep the application open.

Initially, we deactivate the automatic trip evaluation function, at the end of the operation, we define how we want to evaluate trips:

* **Automatic:**
  Set the first parameter to `TRUE` and the second to` TRUE`.

* **Semi-Automatic:**
  Set the first parameter to `TRUE` and the second to` FALSE`.

```javascript

// ... 
// AUTOMATIC:
dsManager.setMotionStart(false, dsResult -> {
    Handler handler = new Handler(Looper.getMainLooper());
    handler.postDelayed(() -> dsManager.setMotionStart(true, true, dsResult2 -> {
	    return null;
    }), 2000);
    return null;
});

// ... 

// SEMI-AUTOMATIC:
dsManager.setMotionStart(false, dsResult -> {
    Handler handler = new Handler(Looper.getMainLooper());
    handler.postDelayed(() -> dsManager.setMotionStart(true, false, dsResult2 -> {
	    return null;
    }), 2000);
    return null;
});

// ... 

```

You can check if the automatic mode is active:

```
// ... 
dsManager.isMotionServiceAlive();
// ... 
```
### Trip info:
Once a trip has started, DSTracker offers a method for obtaining trip information. *TrackingStatus* is obtained throught the *getStatus()* method with the info:
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
