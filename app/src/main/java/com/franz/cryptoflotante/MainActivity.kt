package com.franz.cryptoflotante
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.franz.cryptoflotante.databinding.ActivityMainBinding
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val allCoins=listOf("BTCUSDT","ETHUSDT","BNBUSDT","SOLUSDT","XRPUSDT","DOGEUSDT","ADAUSDT","AVAXUSDT","LINKUSDT","DOTUSDT")
    private val allTFs=listOf("3m","15m","1h","4h","1d")
    private val allAlarms=listOf("COMPRA" to "COMPRA COMPLETA","VENTA" to "VENTA COMPLETA","SOPORTE" to "Toca Soporte","RESISTENCIA" to "Toca Resistencia","FVG" to "Toca FVG","RSI" to "RSI Sobreventa/Sobrecompra","MACD" to "MACD Cruce","EMA" to "Cruza EMA","VIX" to "VIX Pico / Gatillo","FRANJA" to "Toca Franja VWMA")
    private var coinBoxes=mutableMapOf<String,CheckBox>()
    private var tfBoxes=mutableMapOf<String,CheckBox>()
    private var alarmBoxes=mutableMapOf<String,CheckBox>()
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState); binding=ActivityMainBinding.inflate(layoutInflater); setContentView(binding.root)
        val prefs=getSharedPreferences("crypto_flotante", MODE_PRIVATE)
        val savedCoins=prefs.getStringSet("symbols", setOf("BTCUSDT","ETHUSDT","SOLUSDT","BNBUSDT")) ?: setOf("BTCUSDT","ETHUSDT","SOLUSDT","BNBUSDT")
        val savedTFs=prefs.getStringSet("timeframes", setOf("3m","15m","1h","4h","1d")) ?: setOf("3m","15m","1h","4h","1d")
        val savedAlarms=prefs.getStringSet("alarms_enabled", setOf("COMPRA","VENTA","SOPORTE","RESISTENCIA","RSI","MACD","VIX")) ?: setOf("COMPRA","VENTA")
        renderCoins(savedCoins); renderTFs(savedTFs); renderAlarms(savedAlarms)
        binding.switchSonido.isChecked=prefs.getBoolean("sound_on", true)
        binding.switchVibracion.isChecked=prefs.getBoolean("vib_on", true)
        binding.etRepeat.setText(prefs.getInt("repeat_min",5).toString())
        binding.etRsiOS.setText("25"); binding.etRsiOB.setText("75")
        binding.btnPermiso.setOnClickListener{ if(!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) else Toast.makeText(this,"Permiso flotante ya activo",Toast.LENGTH_SHORT).show() }
        binding.btnBateria.setOnClickListener{ val pm=getSystemService(POWER_SERVICE) as PowerManager; if(!pm.isIgnoringBatteryOptimizations(packageName)) startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))) }
        binding.btnIniciar.setOnClickListener{
            if(!Settings.canDrawOverlays(this)){ Toast.makeText(this,"Primero activa permiso flotante",Toast.LENGTH_LONG).show(); return@setOnClickListener }
            val coins=coinBoxes.filter{it.value.isChecked}.keys; if(coins.isEmpty()){ Toast.makeText(this,"Elegi al menos 1 moneda",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val tfs=tfBoxes.filter{it.value.isChecked}.keys; if(tfs.isEmpty()){ Toast.makeText(this,"Elegi al menos 1 temporalidad",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val alarms=alarmBoxes.filter{it.value.isChecked}.keys
            val repeat=binding.etRepeat.text.toString().toIntOrNull() ?: 5
            prefs.edit().putStringSet("symbols", coins).putStringSet("timeframes", tfs).putStringSet("alarms_enabled", alarms).putBoolean("sound_on", binding.switchSonido.isChecked).putBoolean("vib_on", binding.switchVibracion.isChecked).putInt("repeat_min", repeat).apply()
            val custom=binding.etCustom.text.toString().trim().uppercase(); if(custom.isNotEmpty()){ val sym=if(custom.endsWith("USDT")) custom else custom+"USDT"; val cur=coins.toMutableSet(); cur.add(sym); prefs.edit().putStringSet("symbols", cur).apply() }
            val i1=Intent(this, CryptoOverlayService::class.java).apply{ action=CryptoOverlayService.ACTION_START }
            val i2=Intent(this, AlarmService::class.java).apply{ action=AlarmService.ACTION_START }
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){ startForegroundService(i1); startForegroundService(i2) } else { startService(i1); startService(i2) }
            Toast.makeText(this,"Listo! Barra + Alarmas activas. Repite cada $repeat min",Toast.LENGTH_LONG).show()
        }
        binding.btnDetener.setOnClickListener{ startService(Intent(this, CryptoOverlayService::class.java).apply{ action=CryptoOverlayService.ACTION_STOP }); startService(Intent(this, AlarmService::class.java).apply{ action=AlarmService.ACTION_STOP }) }
        binding.btnTestAlarma.setOnClickListener{ val i=Intent("com.franz.cryptoflotante.ALARM").apply{ putExtra("title","TEST COMPRA BTC 3m"); putExtra("price",67234.0); putExtra("isBuy", true) }; sendBroadcast(i); Toast.makeText(this,"Test enviado - mira la barra flotante",Toast.LENGTH_SHORT).show() }
    }
    private fun renderCoins(sel:Set<String>){ binding.containerCoins.removeAllViews(); coinBoxes.clear(); for(c in allCoins){ val cb=CheckBox(this).apply{ text=c.replace("USDT"," / USDT"); isChecked=sel.contains(c); textSize=15f; setPadding(8,10,8,10) }; coinBoxes[c]=cb; binding.containerCoins.addView(cb) }; for(e in sel) if(!allCoins.contains(e)){ val cb=CheckBox(this).apply{ text="$e - Personalizada"; isChecked=true; textSize=15f }; coinBoxes[e]=cb; binding.containerCoins.addView(cb) } }
    private fun renderTFs(sel:Set<String>){ binding.containerTF.removeAllViews(); tfBoxes.clear(); for(tf in allTFs){ val cb=CheckBox(this).apply{ text=tf; isChecked=sel.contains(tf); textSize=16f; setPadding(12,8,12,8) }; tfBoxes[tf]=cb; binding.containerTF.addView(cb) } }
    private fun renderAlarms(sel:Set<String>){ binding.containerAlarms.removeAllViews(); alarmBoxes.clear(); for((key,label) in allAlarms){ val cb=CheckBox(this).apply{ text=label; isChecked=sel.contains(key); textSize=15f; setPadding(8,10,8,10) }; alarmBoxes[key]=cb; binding.containerAlarms.addView(cb) } }
}
