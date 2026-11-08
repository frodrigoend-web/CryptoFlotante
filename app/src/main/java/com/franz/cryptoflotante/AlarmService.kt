package com.franz.cryptoflotante
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
class AlarmService : Service() {
    companion object { const val ACTION_START="START_ALARMS"; const val ACTION_STOP="STOP_ALARMS"; const val CHANNEL="alarm_channel" }
    private val scope=CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running=false
    private val lastAlertTime=mutableMapOf<String,Long>()
    override fun onBind(i: Intent?): IBinder? = null
    override fun onCreate(){ super.onCreate(); createChannel() }
    private fun createChannel(){ if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){ val ch=NotificationChannel(CHANNEL,"Alarmas Crypto",NotificationManager.IMPORTANCE_HIGH); getSystemService(NotificationManager::class.java).createNotificationChannel(ch) } }
    override fun onStartCommand(intent: Intent?, flags:Int, startId:Int): Int {
        if(intent?.action==ACTION_STOP){ running=false; scope.cancel(); stopSelf(); return START_NOT_STICKY }
        startForeground(2, NotificationCompat.Builder(this,CHANNEL).setContentTitle("Alarmas Crypto activas").setContentText("Evaluando Binance cada 30 seg - repite cada 5 min").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setOngoing(true).build())
        if(!running){ running=true; startLoop() }
        return START_STICKY
    }
    private fun startLoop(){
        scope.launch{
            while(running){
                try{ checkAll() }catch(e:Exception){ e.printStackTrace() }
                delay(30_000)
            }
        }
    }
    private suspend fun checkAll(){
        val prefs=getSharedPreferences("crypto_flotante", MODE_PRIVATE)
        val symbols=prefs.getStringSet("symbols", setOf("BTCUSDT","ETHUSDT","SOLUSDT","BNBUSDT")) ?: setOf("BTCUSDT")
        val tfs=prefs.getStringSet("timeframes", setOf("3m","15m","1h","4h","1d")) ?: setOf("3m")
        val enabled=prefs.getStringSet("alarms_enabled", setOf("COMPRA","VENTA","SOPORTE","RESISTENCIA","RSI","MACD","VIX")) ?: setOf("COMPRA","VENTA")
        val soundOn=prefs.getBoolean("sound_on", true)
        val vibOn=prefs.getBoolean("vib_on", true)
        val repeatMin=prefs.getInt("repeat_min", 5)
        for(sym in symbols){
            for(tf in tfs){
                val results=AlarmEngine.evaluate(sym, tf, enabled)
                for(r in results){
                    val key="${r.symbol}-${r.timeframe}-${r.type}"
                    val now=System.currentTimeMillis()
                    val last=lastAlertTime[key] ?: 0L
                    if(now-last >= repeatMin*60*1000L){
                        lastAlertTime[key]=now
                        withContext(Dispatchers.Main){ fireAlarm(r, soundOn, vibOn) }
                    }
                }
            }
        }
    }
    private fun fireAlarm(r: AlarmResult, soundOn:Boolean, vibOn:Boolean){
        val nm=getSystemService(NotificationManager::class.java)
        val notif=NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle(r.title)
            .setContentText("${r.detail} - Precio ${String.format("%.2f", r.price)}")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .build()
        nm.notify((r.symbol+r.timeframe+r.type).hashCode(), notif)
        if(soundOn){ try{ val uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); val rt=RingtoneManager.getRingtone(this, uri); rt.play() }catch(e:Exception){} }
        if(vibOn){ val vib=getSystemService(Context.VIBRATOR_SERVICE) as Vibrator; if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){ vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0,300,200,300), -1)) } else { @Suppress("DEPRECATION") vib.vibrate(longArrayOf(0,300,200,300), -1) } }
        val intent=Intent("com.franz.cryptoflotante.ALARM").apply{ putExtra("title", r.title); putExtra("price", r.price); putExtra("isBuy", r.isBuy) }
        sendBroadcast(intent)
        Toast.makeText(this, r.title, Toast.LENGTH_LONG).show()
    }
    override fun onDestroy(){ running=false; super.onDestroy() }
}
