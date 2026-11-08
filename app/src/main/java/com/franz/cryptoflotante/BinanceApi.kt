package com.franz.cryptoflotante
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit
data class Kline(val openTime:Long, val open:Double, val high:Double, val low:Double, val close:Double, val volume:Double)
object BinanceApi {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    suspend fun getKlines(symbol:String, interval:String, limit:Int=250): List<Kline> = withContext(Dispatchers.IO) {
        val url="https://api.binance.com/api/v3/klines?symbol=${symbol.uppercase()}&interval=$interval&limit=$limit"
        val req=Request.Builder().url(url).build()
        val resp=client.newCall(req).execute()
        val body=resp.body?.string() ?: return@withContext emptyList()
        val arr=JsonParser.parseString(body).asJsonArray
        val out=mutableListOf<Kline>()
        for(el in arr){ val a=el.asJsonArray; out.add(Kline(a[0].asLong, a[1].asString.toDouble(), a[2].asString.toDouble(), a[3].asString.toDouble(), a[4].asString.toDouble(), a[5].asString.toDouble())) }
        out
    }
    suspend fun getPrice(symbol:String): Double = withContext(Dispatchers.IO) {
        try{
            val req=Request.Builder().url("https://api.binance.com/api/v3/ticker/price?symbol=${symbol.uppercase()}").build()
            val resp=client.newCall(req).execute()
            val j=JsonParser.parseString(resp.body?.string()).asJsonObject
            j.get("price").asString.toDouble()
        }catch(e:Exception){ 0.0 }
    }
}
