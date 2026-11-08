package com.franz.cryptoflotante
import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.gson.JsonParser
import okhttp3.*
import java.util.concurrent.TimeUnit
class CryptoOverlayService : Service() {
    companion object { const val ACTION_START="START"; const val ACTION_STOP="STOP"; const val CHANNEL_ID="crypto_flotante_channel" }
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var containerPrices: LinearLayout? = null
    private var tvStatus: TextView? = null
    private var tvAlarm: TextView? = null
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private val priceViews = mutableMapOf<String, TextView>()
    private val lastPrice = mutableMapOf<String, Double>()
    private val alarmReceiver = object: BroadcastReceiver(){ override fun onReceive(c: Context?, i: Intent?){ val t=i?.getStringExtra("title") ?: return; val isBuy=i.getBooleanExtra("isBuy", true); android.os.Handler(mainLooper).post{ tvAlarm?.text="ALARMA $t"; tvAlarm?.setTextColor(if(isBuy) android.graphics.Color.parseColor("#00E676") else android.graphics.Color.parseColor("#FF5252")); tvAlarm?.visibility=View.VISIBLE } } }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate(){ super.onCreate(); windowManager=getSystemService(Context.WINDOW_SERVICE) as WindowManager; createChannel(); registerReceiver(alarmReceiver, IntentFilter("com.franz.cryptoflotante.ALARM"), RECEIVER_NOT_EXPORTED) }
    private fun createChannel(){ if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){ val ch=NotificationChannel(CHANNEL_ID,"Crypto Flotante",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager::class.java).createNotificationChannel(ch) } }
    override fun onStartCommand(intent: Intent?, flags:Int, startId:Int): Int { when(intent?.action){ ACTION_STOP->{ stopOverlay(); stopSelf(); return START_NOT_STICKY } else->{ startForeground(1, NotificationCompat.Builder(this,CHANNEL_ID).setContentTitle("Crypto Flotante activo").setContentText("Barra flotante + Alarmas Binance").setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true).build()); showOverlay(); connectBinance(); return START_STICKY } } }
    private fun showOverlay(){
        if(overlayView!=null) return
        overlayView=LayoutInflater.from(this).inflate(R.layout.overlay_bar, null)
        containerPrices=overlayView?.findViewById(R.id.containerPrices); tvStatus=overlayView?.findViewById(R.id.tvStatus); tvAlarm=overlayView?.findViewById(R.id.tvAlarm)
        val prefs=getSharedPreferences("crypto_flotante", MODE_PRIVATE)
        val symbols=prefs.getStringSet("symbols", setOf("BTCUSDT","ETHUSDT","SOLUSDT","BNBUSDT")) ?: setOf("BTCUSDT","ETHUSDT","SOLUSDT","BNBUSDT")
        containerPrices?.removeAllViews(); priceViews.clear()
        for(sym in symbols){ val tv=TextView(this).apply{ text="${sym.replace("USDT","")} --"; setTextColor(android.graphics.Color.WHITE); textSize=13f; setPadding(18,6,18,6) }; priceViews[sym]=tv; containerPrices?.addView(tv) }
        val type=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params=WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, PixelFormat.TRANSLUCENT).apply{ gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL; y=0 }
        var initialY=0; var initialTouchY=0f
        overlayView?.setOnTouchListener{ _, ev-> when(ev.action){ MotionEvent.ACTION_DOWN->{initialY=params.y; initialTouchY=ev.rawY; true} MotionEvent.ACTION_MOVE->{params.y=initialY+(ev.rawY-initialTouchY).toInt(); windowManager.updateViewLayout(overlayView, params); true} else->false } }
        try{ windowManager.addView(overlayView, params) }catch(e:Exception){ e.printStackTrace() }
    }
    private fun stopOverlay(){ try{ overlayView?.let{windowManager.removeView(it)} }catch(_:Exception){}; overlayView=null; webSocket?.close(1000,"stop"); client?.dispatcher?.executorService?.shutdown() }
    private fun connectBinance(){
        val prefs=getSharedPreferences("crypto_flotante", MODE_PRIVATE)
        val symbols=prefs.getStringSet("symbols", setOf("BTCUSDT","ETHUSDT","SOLUSDT","BNBUSDT")) ?: setOf("BTCUSDT","ETHUSDT","SOLUSDT","BNBUSDT")
        val streams=symbols.joinToString("/") { it.lowercase()+"@miniTicker" }
        val url="wss://stream.binance.com:9443/stream?streams=$streams"
        client=OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
        webSocket=client?.newWebSocket(Request.Builder().url(url).build(), object: WebSocketListener(){
            override fun onMessage(ws: WebSocket, text: String){ try{ val json=JsonParser.parseString(text).asJsonObject; val data=json.getAsJsonObject("data"); val sym=data.get("s").asString; val price=data.get("c").asString.toDoubleOrNull() ?: return; val prev=lastPrice[sym] ?: price; lastPrice[sym]=price; android.os.Handler(mainLooper).post{ val tv=priceViews[sym]; tv?.text="${sym.replace("USDT","")} $${formatPrice(price)}"; tv?.setTextColor(if(price>=prev) android.graphics.Color.parseColor("#00E676") else android.graphics.Color.parseColor("#FF5252")); tvStatus?.text="EN VIVO Binance" } }catch(_:Exception){} }
            override fun onFailure(ws: WebSocket, t: Throwable, r: Response?){ android.os.Handler(mainLooper).post{ tvStatus?.text="Reconectando..." }; android.os.Handler(mainLooper).postDelayed({connectBinance()},3000) }
        })
    }
    private fun formatPrice(p:Double): String = when{ p>=1000->String.format("%,.2f",p); p>=1->String.format("%.2f",p); p>=0.01->String.format("%.4f",p); else->String.format("%.6f",p) }
    override fun onDestroy(){ try{unregisterReceiver(alarmReceiver)}catch(_:Exception){}; stopOverlay(); super.onDestroy() }
}
