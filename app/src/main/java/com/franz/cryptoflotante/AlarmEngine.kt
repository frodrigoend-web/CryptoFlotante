package com.franz.cryptoflotante
data class AlarmResult(val symbol:String, val timeframe:String, val type:String, val title:String, val price:Double, val detail:String, val isBuy:Boolean)
object AlarmEngine {
    var rsiPeriod=14; var rsiOS=25; var rsiOB=75
    var ema1Len=100; var ema2Len=50; var ema3Len=20
    var volLen=20; var volMult=1.8
    var vixActive=true
    suspend fun evaluate(symbol:String, tf:String, enabled:Set<String>): List<AlarmResult> {
        val klines=BinanceApi.getKlines(symbol, tf, 250)
        if(klines.size<100) return emptyList()
        val closes=klines.map{it.close}; val highs=klines.map{it.high}; val lows=klines.map{it.low}; val vols=klines.map{it.volume}
        val price=closes.last()
        val rsi=Indicators.rsi(closes, rsiPeriod)
        val ema1=Indicators.ema(closes, ema1Len).last()
        val ema2=Indicators.ema(closes, ema2Len).last()
        val ema3=Indicators.ema(closes, ema3Len).last()
        val macdUp=Indicators.isMacdCrossUp(closes)
        val macdDown=Indicators.isMacdCrossDown(closes)
        val atr=Indicators.atr(highs,lows,closes,14)
        val vwma=Indicators.vwma(closes, vols, volLen)
        val franjaSup=vwma + atr*volMult; val franjaInf=vwma - atr*volMult
        val recentHigh=highs.takeLast(50).max(); val recentLow=lows.takeLast(50).min()
        val nearSupport = price <= recentLow*1.002
        val nearResistance = price >= recentHigh*0.998
        val fvgBull = lows.last() > highs[highs.size-3]
        val fvgBear = highs.last() < lows[lows.size-3]
        val wvf=Indicators.wvf(lows, closes, 22)
        val wvfUp=Indicators.wvfUpper(closes, lows)
        val volSma=vols.takeLast(20).average()
        val volSpike=vols.last() > volSma*1.5
        val lastK=klines.last()
        val wickRatio = run { val range=lastK.high-lastK.low; if(range==0.0) 0.0 else (minOf(lastK.open,lastK.close)-lastK.low)/range }
        val vixCapitulation = wvf >= wvfUp
        val vixBottom = vixCapitulation && volSpike && wickRatio>=0.4 && rsi<=30
        val bbLower=Indicators.bollingerLower(closes,20,2.0)
        val vixTrigger = vixBottom && price <= bbLower*1.01
        val out=mutableListOf<AlarmResult>()
        val condRsiBull = rsi < rsiOS
        val condRsiBear = rsi > rsiOB
        val condEmaBull = price > ema1 && price > ema2 && price > ema3
        val condEmaBear = price < ema1 && price < ema2 && price < ema3
        val inBullZone = nearSupport || fvgBull
        val inBearZone = nearResistance || fvgBear
        if("COMPRA" in enabled && inBullZone && condRsiBull && macdUp && condEmaBull && (!vixActive || vixTrigger)){
            out.add(AlarmResult(symbol,tf,"COMPRA","COMPRA $symbol $tf",price,"Soporte/FVG + RSI ${rsi.toInt()} + MACD cruce + EMAs OK + VIX OK",true))
        }
        if("VENTA" in enabled && inBearZone && condRsiBear && macdDown && condEmaBear){
            out.add(AlarmResult(symbol,tf,"VENTA","VENTA $symbol $tf",price,"Resistencia/FVG + RSI ${rsi.toInt()} + MACD cruce + EMAs OK",false))
        }
        if("SOPORTE" in enabled && nearSupport) out.add(AlarmResult(symbol,tf,"SOPORTE","Toco Soporte $symbol $tf",price,"Precio cerca de soporte $recentLow",true))
        if("RESISTENCIA" in enabled && nearResistance) out.add(AlarmResult(symbol,tf,"RESISTENCIA","Toco Resistencia $symbol $tf",price,"Precio cerca de resistencia $recentHigh",false))
        if("FVG" in enabled && (fvgBull||fvgBear)) out.add(AlarmResult(symbol,tf,"FVG","FVG ${if(fvgBull) "Alcista" else "Bajista"} $symbol $tf",price,"Gap detectado",fvgBull))
        if("RSI" in enabled && (condRsiBull||condRsiBear)) out.add(AlarmResult(symbol,tf,"RSI","RSI ${if(condRsiBull) "Sobreventa" else "Sobrecompra"} $symbol $tf",price,"RSI ${String.format("%.1f",rsi)}",condRsiBull))
        if("MACD" in enabled && (macdUp||macdDown)) out.add(AlarmResult(symbol,tf,"MACD","MACD Cruce ${if(macdUp) "Alcista" else "Bajista"} $symbol $tf",price,"Cruce MACD",macdUp))
        if("EMA" in enabled && (price>ema1 && price<ema1*1.001 || price<ema1 && price>ema1*0.999)) out.add(AlarmResult(symbol,tf,"EMA","Precio cruza EMA $symbol $tf",price,"Precio cerca EMA100 $ema1",price>ema1))
        if("VIX" in enabled && vixBottom) out.add(AlarmResult(symbol,tf,"VIX","VIX Pico - Posible suelo $symbol $tf",price,"WVF ${String.format("%.1f",wvf)} + Vol + Mecha + RSI",true))
        if("FRANJA" in enabled && (price>=franjaSup*0.999 || price<=franjaInf*1.001)) out.add(AlarmResult(symbol,tf,"FRANJA","Toco Franja VWMA $symbol $tf",price,"VWMA $vwma Franja [$franjaInf - $franjaSup]",price>vwma))
        return out
    }
}
