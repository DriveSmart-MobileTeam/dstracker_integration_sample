package com.ds.test;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.drivesmartsdk.DSDKUserSession;
import com.drivesmartsdk.enums.DSInternalMotionActivities;
import com.drivesmartsdk.enums.DSMotionEvents;
import com.drivesmartsdk.enums.DSNotification;
import com.drivesmartsdk.enums.DSResult;
import com.drivesmartsdk.interfaces.DSManagerInterface;
import com.drivesmartsdk.models.DSCheckStatus;
import com.drivesmartsdk.models.DSInfoTrip;
import com.drivesmartsdk.singleton.DSManager;
import com.ds.test.databinding.ActivityMainBinding;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivityJava extends AppCompatActivity implements DSManagerInterface{

    private ActivityMainBinding binding;
    private DSManager dsManager;

    private String apkID;
    private String userID;
    private Handler handlerTrip;
    private boolean initialInfo=true;
    private DSDKUserSession userSession;


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

        binding.autoButton.setOnClickListener(view -> {
            if(dsManager.isRunningService()){
                dsManager.stopService();
            }

            if(dsManager.isMotionServiceAlive()){
                dsManager.setMotionStart(false, dsResult -> {
                    addLog("Motion - automatic (disabled): " + dsResult.toString());
                    return null;
                });
            }else {
                dsManager.setMotionStart(false, dsResult -> {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> dsManager.setMotionStart(true, true, dsResult2 -> {

                        addLog("Motion - automatic (enable): " + dsResult2.toString());
                        return null;
                    }), 2000);
                    return null;
                });
            }
        });
        binding.semiAutoButton.setOnClickListener(view -> {
            if(dsManager.isRunningService()){
                dsManager.stopService();
            }
            dsManager.setMotionStart(false, dsResult -> {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> dsManager.setMotionStart(true, false, dsResult2 -> {
                    addLog("Motion - semi_automatic: " + dsResult2.toString());
                    return null;
                }), 2000);
                return null;
            });

        });
        binding.startTripButton.setOnClickListener(view -> dsManager.startService());
        binding.stopTripButton.setOnClickListener(view -> dsManager.stopService());
        binding.setUserButton.setOnClickListener(view -> {
            if(userSession!=null) {
                identifyEnvironmet(userSession.getDsUserId());
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
        dsManager.getOrAddUserIdBy(user, new Continuation<DSDKUserSession>() {
            @Override
            public void resumeWith(@NonNull Object o) {
                if(o instanceof DSDKUserSession){
                    userSession = (DSDKUserSession)o;
                    addLog("User id created: " + ((DSDKUserSession)o).getDsUserId());
                }else{
                    addLog("getOrAddUserIdBy: " + o.toString());
                }
            }
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }
        });
    }

    private void prepareEnvironment() {
        dsManager = DSManager.getInstance(this);
        dsManager.configure(apkID, dsResult -> {
            if (dsResult instanceof DSResult.Success) {
                addLog("SDk configured");
                identifyEnvironmet(userID);
                configEnvironment();
            }else{
                String error = ((DSResult.Error) dsResult).getError().getDescription();
                addLog("Configure SDK: "+error);
            }
            return null;
        });
    }

    private void identifyEnvironmet(String uid) {
        dsManager.setUserID(uid, result -> {
            addLog("Defining USER ID: " + uid);
            return null;
        });
    }

    private void configEnvironment() {
        dsManager.setListener(this);

        dsManager.setModeOnline(true, dsResult -> null);
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
            DSCheckStatus beanStatus = dsManager.checkService();
            DSInfoTrip info = dsManager.tripInfo();

            if(initialInfo){
                addLog("Trip ID: " + beanStatus.getTripID());
                addLog("Initial marker: " + "la: " + info.getStartLocation().getLatitute() + " lo: " + info.getStartLocation().getLongitude());
                initialInfo=false;
            }

            addLog("Timer: " + convertMillisecondsToHMmSs(beanStatus.getServiceTime()));
            addLog("Distance: " + beanStatus.getTotalDistance());
            addLog("Marker: " + "la: " + info.getEndLocation().getLatitute() + " lo: " + info.getEndLocation().getLongitude());

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