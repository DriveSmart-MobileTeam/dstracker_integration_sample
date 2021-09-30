package com.ds.test

import android.Manifest

import androidx.appcompat.app.AppCompatActivity
import com.drivesmart.tracker.interfaces.DSManagerInterface
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import com.drivesmart.tracker.enums.DSNotification
import com.drivesmart.tracker.enums.DSInternalMotionActivities
import com.drivesmart.tracker.enums.DSMotionEvents
import com.drivesmart.tracker.enums.DSResult
import com.drivesmart.tracker.singleton.DSTracker
import com.ds.test.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityKotlin : AppCompatActivity(), DSManagerInterface {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dsTracker: DSTracker
    private lateinit var apkID: String
    private lateinit var userID: String
    private lateinit var handlerTrip: Handler
    private var userSession: String? = null

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
        binding.startTripButton.setOnClickListener { dsTracker.start() }
        binding.stopTripButton.setOnClickListener { dsTracker.stop() }
        binding.setUserButton.setOnClickListener {
            if (userSession != null) {
                identifyEnvironmet(userSession!!)
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

    private suspend fun  getUserSession(user: String): String =
        withContext(Dispatchers.IO) {
            return@withContext  dsTracker.getOrAddUserIdBy(user)
        }

    private fun getOrAddUser(user: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val session = getUserSession(user)

            userSession = session
            addLog("User id created: $session")
        }
    }

    private fun prepareEnvironment() {
        dsTracker = DSTracker.getInstance(this)
        dsTracker.configure(apkID) { dsResult: DSResult ->
            if (dsResult is DSResult.Success) {
                addLog("DSTracker configured")
                identifyEnvironmet(userID)
            } else {
                val error: String = dsResult.toString()
                addLog("Configure DSTracker: $error")
            }
        }
    }

    private fun identifyEnvironmet(uid: String) {
        dsTracker.setUserId(uid) { result: DSResult? ->
            addLog("Defining USER ID: $uid")
        }
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
            val beanStatus = dsTracker.getStatus()

            addLog("Timer: " + convertMillisecondsToHMmSs(beanStatus.serviceTime))
            addLog("Distance: " + beanStatus.totalDistance)
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