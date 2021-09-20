package com.ds.test

import android.Manifest

import androidx.appcompat.app.AppCompatActivity
import com.drivesmartsdk.interfaces.DSManagerInterface
import com.drivesmartsdk.singleton.DSManager
import com.drivesmartsdk.DSDKUserSession
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.os.Looper
import android.view.View
import com.drivesmartsdk.enums.DSNotification
import com.drivesmartsdk.enums.DSInternalMotionActivities
import com.drivesmartsdk.enums.DSMotionEvents
import com.drivesmartsdk.enums.DSResult
import com.ds.test.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityKotlin : AppCompatActivity(), DSManagerInterface {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dsManager: DSManager
    private lateinit var apkID: String
    private lateinit var userID: String
    private lateinit var handlerTrip: Handler
    private var initialInfo = true
    private var userSession: DSDKUserSession? = null

    companion object {
        val PERMISSIONS_GPS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val PERMISSIONS_GPS_AUTO = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    private fun defineConstants() {
        // TODO
        apkID = ""
        userID = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        setContentView(binding.root)
        defineConstants()
        prepareEnvironment()
        prepareView()
    }

    private fun prepareView() {
        handlerTrip = Handler(mainLooper)
        binding.logText.movementMethod = ScrollingMovementMethod()
        checkPerms()
        binding.checkPermButton.setOnClickListener { checkPerms() }
        binding.autoButton.setOnClickListener {
            if (dsManager.isRunningService()) {
                dsManager.stopService()
            }
            if (dsManager.isMotionServiceAlive()) {
                dsManager.setMotionStart(false) { dsResult: DSResult ->
                    addLog("Motion - automatic (disabled): $dsResult")
                }
            } else {
                dsManager.setMotionStart(false) { dsResult: DSResult? ->
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        dsManager.setMotionStart(
                            enable = true,
                            isForegroundService = true
                        ) { dsResult2: DSResult ->
                            addLog("Motion - automatic (enable): $dsResult2")
                        }
                    }, 2000)
                }
            }
        }
        binding.semiAutoButton.setOnClickListener { view: View? ->
            if (dsManager.isRunningService()) {
                dsManager.stopService()
            }
            dsManager.setMotionStart(false) { dsResult: DSResult? ->
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    dsManager.setMotionStart(
                        enable = true,
                        isForegroundService = false
                    ) { dsResult2: DSResult ->
                        addLog("Motion - semi_automatic: $dsResult2")
                    }
                }, 2000)
            }
        }
        binding.startTripButton.setOnClickListener { dsManager.startService() }
        binding.stopTripButton.setOnClickListener { dsManager.stopService() }
        binding.setUserButton.setOnClickListener {
            if (userSession != null) {
                identifyEnvironmet(userSession!!.dsUserId)
            } else {
                addLog("no user-session info")
            }
        }
        binding.getUserButton.setOnClickListener {
            if (!binding.userId.text.toString().isEmpty()) {
                getOrAddUser(binding.userId.text.toString())
            }
        }
    }

    private fun checkPerms() {
        if (UtilsMethods.isDozing(this)) {
            binding.permBatteryStatus.text = getString(R.string.optimized)
        } else {
            binding.permBatteryStatus.text = getString(R.string.no_optimized)
        }
        if (UtilsMethods.isGPSEnabled(this)) {
            binding.permGpsStatus.text = getString(R.string.enabled)
        } else {
            binding.permGpsStatus.text = getString(R.string.disabled)
        }
        if (UtilsMethods.checkPermissions(this, PERMISSIONS_GPS)) {
            binding.permGpsSemiStatus.text = getString(R.string.ok)
        } else {
            binding.permGpsSemiStatus.text = getString(R.string.nok)
        }
        if (UtilsMethods.checkPermissions(this, PERMISSIONS_GPS_AUTO)) {
            binding.permGpsAutoStatus.text = getString(R.string.ok)
        } else {
            binding.permGpsAutoStatus.text = getString(R.string.nok)
        }
    }

    private suspend fun  getUserSession(user: String): DSDKUserSession? =
        withContext(Dispatchers.IO) {
            return@withContext  dsManager.getOrAddUserIdBy(user)
        }

    private fun getOrAddUser(user: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val session = getUserSession(user)

            if (session is DSDKUserSession) {
                userSession = session
                addLog("User id created: " + session.dsUserId)
            } else {
                addLog("getOrAddUserIdBy: $session")
            }
        }
    }

    private fun prepareEnvironment() {
        dsManager = DSManager.getInstance(this)
        dsManager.configure(apkID) { dsResult: DSResult ->
            if (dsResult is DSResult.Success) {
                addLog("SDk configured")
                identifyEnvironmet(userID)
                configEnvironment()
            } else {
                val error: String = (dsResult as Error).message.toString()
                addLog("Configure SDK: $error")
            }
        }
    }

    private fun identifyEnvironmet(uid: String) {
        dsManager.setUserID(uid) { result: DSResult? ->
            addLog("Defining USER ID: $uid")
        }
    }

    private fun configEnvironment() {
        dsManager.setListener(this)
        dsManager.setModeOnline(true) { }
    }

    // ****************************************v****************************************
    // ******************************* Client Stuff ************************************
    // ****************************************v****************************************
    private fun addLog(text: String) {
        binding.logText.append(
            """
    
    $text
    """.trimIndent()
        )
    }

    private val updateTimerThread: Runnable = object : Runnable {
        override fun run() {
            val beanStatus = dsManager.checkService()
            val (startLocation, endLocation) = dsManager.tripInfo()
            if (initialInfo) {
                addLog("Trip ID: " + beanStatus.tripID)
                addLog("Initial marker: " + "la: " + startLocation.latitute + " lo: " + startLocation.longitude)
                initialInfo = false
            }
            addLog("Timer: " + convertMillisecondsToHMmSs(beanStatus.serviceTime))
            addLog("Distance: " + beanStatus.totalDistance)
            addLog("Marker: " + "la: " + endLocation.latitute + " lo: " + endLocation.longitude)
            handlerTrip.postDelayed(this, 2000)
        }
    }

    // ****************************************v****************************************
    // ******************************* Client Stuff ************************************
    // ****************************************v****************************************
    private fun convertMillisecondsToHMmSs(millisenconds: Long): String {
        val seconds = millisenconds / 1000
        val s = seconds % 60
        val m = seconds / 60 % 60
        val h = seconds / (60 * 60) % 24
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    // ****************************************v****************************************
    // ******************** interface DSManagerInterface *******************************
    // ****************************************v****************************************
    override fun startService(result: DSResult) {
        addLog("Evaluating service: $result")
        showTripInfo(result)
    }

    private fun showTripInfo(result: DSResult) {
        if (result is DSResult.Success) {
            val notification = result.data as DSNotification
            if (notification.ordinal == DSNotification.DS_RECORDING_TRIP.ordinal) {
                handlerTrip.postDelayed(updateTimerThread, 500)
            }
        }
    }

    override fun statusEventService(dsResult: DSResult) {}
    override fun stopService(result: DSResult) {
        addLog("Stopping service: $result")
        handlerTrip.removeCallbacks(updateTimerThread)
    }

    override fun motionDetectedActivity(
        activity: DSInternalMotionActivities,
        percentage: Int
    ) {
    }

    override fun motionStatus(dsMotionEvents: DSMotionEvents) {}
    // ****************************************v****************************************
    // ******************** interface DSManagerInterface *******************************
    // ****************************************v****************************************

}