package com.mirage.auto

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val specs = listOf(
        Triple("atkMin", "Ataques por rodada (mínimo)", 6),
        Triple("atkMax", "Ataques por rodada (máximo)", 12),
        Triple("intMin", "Intervalo entre ataques, mínimo (ms)", 350),
        Triple("intMax", "Intervalo entre ataques, máximo (ms)", 700),
        Triple("walkMin", "Tempo andando, mínimo (ms)", 300),
        Triple("walkMax", "Tempo andando, máximo (ms)", 1200),
        Triple("joyRadius", "Raio do arrasto do joystick (px)", 120),
        Triple("skillEvery", "Skill a cada N ataques (0 = nunca)", 4),
        Triple("healEvery", "Cura a cada N segundos (0 = nunca)", 45),
        Triple("maxMin", "Parar sozinho após (minutos)", 120)
    )

    private val fields = linkedMapOf<String, EditText>()
    private lateinit var statusTv: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "Mirage Auto"
            textSize = 24f
        })

        statusTv = TextView(this).apply {
            textSize = 15f
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(statusTv)

        root.addView(TextView(this).apply {
            text = "Como usar:\n" +
                "1) Ative o serviço \"Mirage Auto\" em Acessibilidade.\n" +
                "2) Abra o jogo. Vai aparecer um painel flutuante (arraste pelo ≡).\n" +
                "3) Toque em Calibrar e toque, na ordem, no botão de ataque, skill, cura " +
                "e no centro do joystick (skill e cura podem ser puladas).\n" +
                "4) Toque em ▶ Iniciar. Para parar, toque em ⏸ Parar.\n\n" +
                "Se o Android 13+ bloquear a opção (acinzentada), abra Informações do app > " +
                "menu ⋮ > \"Permitir configurações restritas\" e tente de novo."
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        })

        root.addView(button("1. Abrir Acessibilidade") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(button("Informações do app (configurações restritas)") {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        })

        root.addView(TextView(this).apply {
            text = "Parâmetros do ciclo"
            textSize = 18f
            setPadding(0, dp(20), 0, dp(4))
        })

        for ((key, label, def) in specs) {
            root.addView(TextView(this).apply {
                text = label
                textSize = 13f
                setPadding(0, dp(8), 0, 0)
            })
            val et = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(Config.int(this@MainActivity, key, def).toString())
            }
            fields[key] = et
            root.addView(et, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        root.addView(button("Salvar parâmetros") { save() })

        root.addView(TextView(this).apply {
            text = "Aviso: automação pode ser proibida pelas regras do jogo e render " +
                "ban na conta. Use por sua conta e risco."
            textSize = 12f
            setPadding(0, dp(16), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(packageName) == true
        statusTv.text = if (enabled) {
            "✔ Serviço ativo. Abra o jogo e use o painel flutuante."
        } else {
            "✖ Serviço desativado. Toque em \"Abrir Acessibilidade\" e ative o Mirage Auto."
        }
    }

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun save() {
        for ((key, _, def) in specs) {
            val v = fields[key]?.text?.toString()?.trim()?.toIntOrNull() ?: def
            Config.setInt(this, key, v)
        }
        Toast.makeText(this, "Parâmetros salvos", Toast.LENGTH_SHORT).show()
    }
}
