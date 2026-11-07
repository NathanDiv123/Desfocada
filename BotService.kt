package com.mirage.auto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Serviço de acessibilidade: desenha um painel flutuante (Iniciar/Parar/Calibrar)
 * e executa o ciclo andar -> atacar -> curar com toques e deslizes na tela.
 */
class BotService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager

    private var panel: LinearLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var calibView: View? = null
    private var toggleBtn: Button? = null
    private var statusTv: TextView? = null

    @Volatile private var running = false
    private var worker: Thread? = null

    private val steps = listOf(
        Triple("attack", "ATAQUE", false),
        Triple("skill", "SKILL (opcional)", true),
        Triple("heal", "CURA (opcional)", true),
        Triple("joy", "CENTRO DO JOYSTICK", false)
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ------------------------------------------------------------ ciclo de vida

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        stopBot()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopBot()
        removeCalib()
        panel?.let { runCatching { wm.removeView(it) } }
        panel = null
        return super.onUnbind(intent)
    }

    // ------------------------------------------------------------ painel flutuante

    private fun smallBtn(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 11f
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { onClick() }
        }

    private fun showPanel() {
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.argb(210, 20, 24, 40))
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(8)
            y = dp(8)
        }

        val handle = TextView(this).apply {
            text = "≡"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
        }
        var sx = 0f
        var sy = 0f
        var ox = 0
        var oy = 0
        handle.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = ev.rawX; sy = ev.rawY; ox = params.x; oy = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = ox + (ev.rawX - sx).toInt()
                    params.y = oy + (ev.rawY - sy).toInt()
                    runCatching { wm.updateViewLayout(p, params) }
                }
            }
            true
        }

        val toggle = smallBtn("▶ Iniciar") { toggle() }
        val calib = smallBtn("Calibrar") { startCalibration() }
        val status = TextView(this).apply {
            text = "parado"
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(dp(8), 0, dp(6), 0)
        }

        p.addView(handle)
        p.addView(toggle)
        p.addView(calib)
        p.addView(status)

        toggleBtn = toggle
        statusTv = status
        panel = p
        panelParams = params
        wm.addView(p, params)
    }

    private fun updateUi(msg: String? = null) {
        toggleBtn?.text = if (running) "⏸ Parar" else "▶ Iniciar"
        statusTv?.text = msg ?: if (running) "rodando" else "parado"
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    // ------------------------------------------------------------ calibração

    private fun startCalibration() {
        stopBot()
        panel?.visibility = View.GONE
        runStep(0)
    }

    private fun removeCalib() {
        calibView?.let { runCatching { wm.removeView(it) } }
        calibView = null
    }

    private fun runStep(i: Int) {
        removeCalib()
        if (i >= steps.size) {
            panel?.visibility = View.VISIBLE
            toast("Calibração salva ✔")
            return
        }
        val (key, label, skippable) = steps[i]

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(70, 0, 0, 0))
        }
        val tv = TextView(this).apply {
            text = "Toque exatamente no botão de $label\n(passo ${i + 1}/${steps.size})"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(220, 20, 24, 40))
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        root.addView(
            tv,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            )
        )

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        if (skippable) {
            row.addView(smallBtn("Pular") {
                Config.clearPt(this, key)
                main.post { runStep(i + 1) }
            })
        }
        row.addView(smallBtn("Cancelar") {
            main.post {
                removeCalib()
                panel?.visibility = View.VISIBLE
            }
        })
        root.addView(
            row,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = dp(24) }
        )

        root.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_UP) {
                val x = ev.rawX.toInt()
                val y = ev.rawY.toInt()
                Config.setPt(this, key, x, y)
                toast("$label salvo ($x, $y)")
                main.post { runStep(i + 1) }
            }
            true
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        calibView = root
        wm.addView(root, params)
    }

    // ------------------------------------------------------------ iniciar / parar

    private fun toggle() {
        if (running) stopBot() else startBot()
    }

    private fun startBot() {
        if (Config.pt(this, "attack") == null || Config.pt(this, "joy") == null) {
            toast("Calibre primeiro (ataque e joystick)")
            return
        }
        running = true
        updateUi()
        worker = Thread { loop() }.also {
            it.isDaemon = true
            it.start()
        }
    }

    private fun stopBot() {
        running = false
        worker?.interrupt()
        worker = null
        main.post { updateUi() }
    }

    // ------------------------------------------------------------ ciclo do bot

    private fun rnd(a: Int, b: Int): Int {
        val lo = minOf(a, b)
        val hi = maxOf(a, b)
        return Random.nextInt(lo, hi + 1)
    }

    private fun loop() {
        val atk = Config.pt(this, "attack") ?: return
        val joy = Config.pt(this, "joy") ?: return
        val skill = Config.pt(this, "skill")
        val heal = Config.pt(this, "heal")

        val atkMin = Config.int(this, "atkMin", 6)
        val atkMax = Config.int(this, "atkMax", 12)
        val intMin = Config.int(this, "intMin", 350)
        val intMax = Config.int(this, "intMax", 700)
        val walkMin = Config.int(this, "walkMin", 300)
        val walkMax = Config.int(this, "walkMax", 1200)
        val radius = Config.int(this, "joyRadius", 120)
        val skillEvery = Config.int(this, "skillEvery", 4)
        val healEvery = Config.int(this, "healEvery", 45)
        val maxMin = Config.int(this, "maxMin", 120)

        val start = System.currentTimeMillis()
        var lastHeal = start
        var rounds = 0

        try {
            while (running && System.currentTimeMillis() - start < maxMin * 60_000L) {
                walk(joy, radius, rnd(walkMin, walkMax).toLong())

                val n = rnd(atkMin, atkMax)
                for (i in 1..n) {
                    if (!running) break
                    tap(atk)
                    if (skill != null && skillEvery > 0 && i % skillEvery == 0) tap(skill)
                    Thread.sleep(rnd(intMin, intMax).toLong())
                }

                val now = System.currentTimeMillis()
                if (heal != null && healEvery > 0 && now - lastHeal >= healEvery * 1000L) {
                    tap(heal)
                    lastHeal = now
                }

                rounds++
                if (rounds % 15 == 0) Thread.sleep(rnd(2000, 6000).toLong())

                val minutos = (System.currentTimeMillis() - start) / 60_000
                main.post { updateUi("$rounds rod. | ${minutos}min") }
            }
        } catch (_: InterruptedException) {
            // parado pelo usuário
        } finally {
            running = false
            main.post { updateUi() }
        }
    }

    // ------------------------------------------------------------ gestos

    private fun tap(p: Pair<Int, Int>) {
        val path = Path().apply {
            moveTo((p.first + rnd(-6, 6)).toFloat(), (p.second + rnd(-6, 6)).toFloat())
        }
        gesture(path, 60)
    }

    private fun walk(c: Pair<Int, Int>, radius: Int, dur: Long) {
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        val (dx, dy) = dirs[Random.nextInt(dirs.size)]
        val path = Path().apply {
            moveTo(c.first.toFloat(), c.second.toFloat())
            lineTo((c.first + dx * radius).toFloat(), (c.second + dy * radius).toFloat())
        }
        gesture(path, dur.coerceAtLeast(50))
    }

    private fun gesture(path: Path, dur: Long) {
        val latch = CountDownLatch(1)
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, dur))
            .build()
        val cb = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                latch.countDown()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                latch.countDown()
            }
        }
        val ok = dispatchGesture(g, cb, main)
        if (ok) latch.await(dur + 1500, TimeUnit.MILLISECONDS) else Thread.sleep(100)
    }
}
