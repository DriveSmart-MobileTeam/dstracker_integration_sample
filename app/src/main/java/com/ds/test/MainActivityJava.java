package com.ds.test;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.drivesmart.tracker.enums.DSInternalMotionActivities;
import com.drivesmart.tracker.enums.DSMotionEvents;
import com.drivesmart.tracker.enums.DSNotification;
import com.drivesmart.tracker.enums.DSResult;
import com.drivesmart.tracker.interfaces.DSManagerInterface;
import com.drivesmart.tracker.models.TrackingStatus;
import com.drivesmart.tracker.singleton.DSTracker;
import com.ds.test.databinding.ActivityMainBinding;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivityJava extends AppCompatActivity implements DSManagerInterface{

    private ActivityMainBinding binding;
    private DSTracker dsTracker;

    private String apkID;
    private String userID;
    private Handler handlerTrip;
    private boolean initialInfo=true;
    private String userSession="";


    public static final String[] PERMISSIONS_GPS = { Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};
    public static final String[] PERMISSIONS_GPS_AUTO = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};

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

    private void prepareView() {
        handlerTrip = new Handler(getMainLooper());
        binding.logText.setMovementMethod(new ScrollingMovementMethod());

        checkPerms();
        binding.checkPermButton.setOnClickListener(view -> checkPerms());

        binding.startTripButton.setOnClickListener(view -> dsTracker.start());
        binding.stopTripButton.setOnClickListener(view -> dsTracker.stop());
        binding.setUserButton.setOnClickListener(view -> {
            if(userSession!=null) {
                identifyEnvironmet(userSession);
            }else{
                addLog("no user-session info");
            }
        });
        binding.getUserButton.setOnClickListener(view -> {
            if(!binding.userId.getText().toString().isEmpty()) {
                getOrAddUser(binding.userId.getText().toString());
            }
        });
    }

    private void checkPerms() {
        if(UtilsMethods.isDozing(this)){
            binding.permBatteryStatus.setText(getString(R.string.optimized));
        }else{
            binding.permBatteryStatus.setText(getString(R.string.no_optimized));
        }

        if(UtilsMethods.isGPSEnabled(this)){
            binding.permGpsStatus.setText(getString(R.string.enabled));
        }else{
            binding.permGpsStatus.setText(getString(R.string.disabled));
        }

        if(UtilsMethods.checkPermissions(this, PERMISSIONS_GPS)){
            binding.permGpsSemiStatus.setText(getString(R.string.ok));
        }else{
            binding.permGpsSemiStatus.setText(getString(R.string.nok));
        }

        if(UtilsMethods.checkPermissions(this, PERMISSIONS_GPS_AUTO)){
            binding.permGpsAutoStatus.setText(getString(R.string.ok));
        }else{
            binding.permGpsAutoStatus.setText(getString(R.string.nok));
        }
    }


    private void getOrAddUser(String user) {
        dsTracker.getOrAddUserIdBy(user, new Continuation<String>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                if(o instanceof String){
                    userSession = o.toString();
                    addLog("User id created: " + o.toString());
                }
            }
        });
    }

    private void prepareEnvironment() {
        dsTracker = DSTracker.getInstance(this);
        dsTracker.configure(apkID, dsResult -> {
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

    private void identifyEnvironmet(String uid) {
        dsTracker.setUserId(uid, result -> {
            addLog("Defining USER ID: " + uid);
            return null;
        });
    }

    // ****************************************v****************************************
    // ******************************* Client Stuff ************************************
    // ****************************************v****************************************

    private void addLog(String text) {
        binding.logText.append("\n" + text);
    }

    private final Runnable updateTimerThread = new Runnable() {
        @Override
        public void run() {
            TrackingStatus beanStatus = dsTracker.getStatus();

            if(initialInfo){
                addLog("Trip ID: " + beanStatus.getTripID());
                initialInfo=false;
            }

            addLog("Timer: " + convertMillisecondsToHMmSs(beanStatus.getServiceTime()));
            addLog("Distance: " + beanStatus.getTotalDistance());

            handlerTrip.postDelayed(this, 2000);
        }
    };
    // ****************************************v****************************************
    // ******************************* Client Stuff ************************************
    // ****************************************v****************************************



    private String convertMillisecondsToHMmSs(Long millisenconds) {
        Long seconds = millisenconds / 1000;
        Long s = seconds % 60;
        Long m = seconds / 60 % 60;
        Long h = seconds / (60 * 60) % 24;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    // ****************************************v****************************************
    // ******************** interface DSManagerInterface *******************************
    // ****************************************v****************************************
    @Override
    public void startService(@NonNull DSResult result) {
        addLog("Evaluating service: " + result.toString());

        showTripInfo(result);
    }

    private void showTripInfo(DSResult result) {
        if (result instanceof DSResult.Success) {
            DSNotification notification = (DSNotification)((DSResult.Success) result).getData();
            if (notification.ordinal() == DSNotification.DS_RECORDING_TRIP.ordinal()){
                handlerTrip.postDelayed(updateTimerThread, 500);
            }
        }
    }

    @Override
    public void statusEventService(@NonNull DSResult dsResult) { }

    @Override
    public void stopService(@NonNull DSResult result) {
        addLog("Stopping service: " + result.toString());
        handlerTrip.removeCallbacks(updateTimerThread);
    }

    @Override
    public void motionDetectedActivity(@NonNull DSInternalMotionActivities dsInternalMotionActivities, int i) { }

    @Override
    public void motionStatus(@NonNull DSMotionEvents dsMotionEvents) { }
    // ****************************************v****************************************
    // ******************** interface DSManagerInterface *******************************
    // ****************************************v****************************************
}