package com.ds.test

import android.Manifest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import com.ds.test.databinding.ActivityMainBinding
import com.dstracker.enums.DSInternalMotionActivities
import com.dstracker.enums.DSMotionEvents
import com.dstracker.enums.DSNotification
import com.dstracker.enums.Outcome
import com.dstracker.interfaces.ManagerInterface
import com.dstracker.singleton.Tracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivityKotlin : AppCompatActivity(), ManagerInterface {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dsTracker: Tracker
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
        binding.anonymousUserButton.setOnClickListener {
            getAnonymousUser()
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

    private fun getOrAddUser(user: String) {
        GlobalScope.launch(Dispatchers.Main) {
            dsTracker.getOrAddUserIdBy(user){result ->
                if (result is Outcome.Success) {
                    userSession = result.toString()
                    addLog("User id created: $result")
                } else {
                    val error: String = (result as Outcome.Error).toString()
                    addLog("getOrAddUser error: $error")
                }
            }
        }
    }

    private fun getAnonymousUser() {
        GlobalScope.launch(Dispatchers.Main) {
            dsTracker.getAnonymousUser{result ->
                if (result is Outcome.Success) {
                    userSession = result.toString()
                    addLog("User id created: $result")
                } else {
                    val error: String = (result as Outcome.Error).toString()
                    addLog("getOrAddUser error: $error")
                }
            }
        }
    }

    private fun prepareEnvironment() {
        dsTracker = Tracker.getInstance(this)
        dsTracker.configure(apkID) { dsResult: Outcome ->
            if (dsResult is Outcome.Success) {
                addLog("DSTracker configured")
                identifyEnvironmet(userID)
            } else {
                val error: String = dsResult.toString()
                addLog("Configure DSTracker: $error")
            }
        }
    }

    private fun identifyEnvironmet(uid: String) {
        dsTracker.setUserId(uid) { result: Outcome? ->
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
    override fun startService(result: Outcome) {
        addLog("Evaluating service: $result")
        showTripInfo(result)
    }

    private fun showTripInfo(result: Outcome) {
        if (result is Outcome.Success) {
            val notification = result.data as DSNotification
            if (notification.ordinal == DSNotification.DS_RECORDING_TRIP.ordinal) {
                handlerTrip.postDelayed(updateTimerThread, 500)
            }
        }
    }

    override fun statusEventService(dsResult: Outcome) {}
    override fun stopService(result: Outcome) {
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