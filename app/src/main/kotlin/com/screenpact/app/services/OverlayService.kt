package com.screenpact.app.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.screenpact.app.data.crypto.TOTPManager
import com.screenpact.app.data.db.AppDatabase
import com.screenpact.app.data.db.UnlockGrant
import com.screenpact.app.ui.overlay.BlockedOverlayContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Servicio que dibuja una capa por encima de cualquier app cuando se excede el límite.
 * El usuario solo puede salir si introduce un TOTP válido generado por algún amigo emparejado.
 *
 * Diseño de ciclo de vida:
 *  - El servicio se inicia cuando hay que mostrar el overlay (ACTION_SHOW).
 *  - Cuando el overlay se oculta (ACTION_HIDE o tras unlock exitoso) el servicio llama stopSelf().
 *  - UsageMonitorService solo manda ACTION_HIDE si isShowing == true, evitando reiniciar el
 *    servicio innecesariamente (lo que causaba el destello rojo en la pantalla principal).
 */
class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var wm: WindowManager
    private var rootView: ComposeView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        when (intent.action) {
            ACTION_SHOW -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_NOT_STICKY
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: pkg
                val usedMs = intent.getLongExtra(EXTRA_USED_MS, 0L)
                val limitMin = intent.getIntExtra(EXTRA_LIMIT_MIN, 0)
                showOverlay(pkg, appName, usedMs, limitMin)
            }
            ACTION_HIDE -> {
                hideOverlayAndStop()
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(pkg: String, appName: String, usedMs: Long, limitMin: Int) {
        // Already showing — do not recreate the view (avoids flicker on repeated calls from UsageMonitorService).
        if (rootView != null) return

        val params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            // SOFT_INPUT_ADJUST_RESIZE allows the keyboard to push up the text field.
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            format = PixelFormat.OPAQUE
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }

        val errorState = mutableStateOf<String?>(null)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                BlockedOverlayContent(
                    appName = appName,
                    usedMinutes = (usedMs / 60_000L).toInt(),
                    limitMinutes = limitMin,
                    errorMessage = errorState.value,
                    onSubmit = { code -> attemptUnlock(pkg, code, errorState) }
                )
            }
        }

        rootView = view
        isShowing = true
        wm.addView(view, params)
    }

    private fun attemptUnlock(
        pkg: String,
        code: String,
        errorState: androidx.compose.runtime.MutableState<String?>
    ) {
        scope.launch {
            val db = AppDatabase.get(applicationContext)
            val friends = db.friendDao().getAll()
            val match = friends.firstOrNull { TOTPManager.verifyCode(it.secret, code) }
            if (match != null) {
                db.unlockGrantDao().upsert(
                    UnlockGrant(
                        packageName = pkg,
                        expiresAt = System.currentTimeMillis() + GRACE_PERIOD_MS
                    )
                )
                // Hide the view and stop the service.
                // Do NOT call stopSelf() before hideOverlay() — the service must remain alive
                // long enough to remove the window, otherwise the WindowManager view leaks.
                hideOverlayAndStop()
            } else {
                errorState.value = "Código inválido"
            }
        }
    }

    private fun hideOverlayAndStop() {
        hideOverlay()
        stopSelf()
    }

    private fun hideOverlay() {
        rootView?.let { runCatching { wm.removeView(it) } }
        rootView = null
        isShowing = false
    }

    override fun onDestroy() {
        hideOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "com.screenpact.app.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.screenpact.app.HIDE_OVERLAY"
        const val EXTRA_PACKAGE = "pkg"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_USED_MS = "used_ms"
        const val EXTRA_LIMIT_MIN = "limit_min"

        const val GRACE_PERIOD_MS = 5 * 60_000L

        /**
         * True while the blocking overlay is visible on screen.
         * Read by UsageMonitorService to avoid starting OverlayService just to hide nothing
         * (which was causing brief red flashes when the user navigated away).
         */
        @Volatile var isShowing: Boolean = false
            private set

        fun show(context: Context, pkg: String, appName: String, usedMs: Long, limitMin: Int) {
            val i = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_PACKAGE, pkg)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_USED_MS, usedMs)
                putExtra(EXTRA_LIMIT_MIN, limitMin)
            }
            context.startService(i)
        }

        fun hide(context: Context) {
            // Only send hide if the overlay is actually on screen — prevents restarting
            // the service unnecessarily and causing spurious overlay flashes.
            if (!isShowing) return
            val i = Intent(context, OverlayService::class.java).apply { action = ACTION_HIDE }
            context.startService(i)
        }
    }
}
