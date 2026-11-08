package com.franz.cryptoflotante
import kotlin.math.*
object Indicators {
    fun ema(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        val k = 2.0 / (period + 1)
        val out = MutableList(values.size) { 0.0 }
        var prev = values[0]; out[0] = prev
        for (i in 1 until values.size) { prev = values[i]*k + prev*(1-k); out[i]=prev }
        return out
    }
    fun rsi(closes: List<Double>, period: Int = 14): Double {
        if (closes.size < period+1) return 50.0
        var gain=0.0; var loss=0.0
        for (i in 1..period) { val d=closes[i]-closes[i-1]; if(d>0) gain+=d else loss+=-d }
        var avgGain=gain/period; var avgLoss=loss/period
        for (i in period+1 until closes.size) { val d=closes[i]-closes[i-1]; val g=if(d>0) d else 0.0; val l=if(d<0) -d else 0.0; avgGain=(avgGain*(period-1)+g)/period; avgLoss=(avgLoss*(period-1)+l)/period }
        if (avgLoss==0.0) return 100.0
        val rs=avgGain/avgLoss
        return 100 - (100/(1+rs))
    }
    fun isMacdCrossUp(closes: List<Double>): Boolean {
        if (closes.size<30) return false
        val f=ema(closes,12); val s=ema(closes,26)
        val m=f.zip(s){a,b->a-b}; val sig=ema(m,9)
        return m[m.size-2] < sig[sig.size-2] && m.last() > sig.last()
    }
    fun isMacdCrossDown(closes: List<Double>): Boolean {
        if (closes.size<30) return false
        val f=ema(closes,12); val s=ema(closes,26)
        val m=f.zip(s){a,b->a-b}; val sig=ema(m,9)
        return m[m.size-2] > sig[sig.size-2] && m.last() < sig.last()
    }
    fun atr(highs:List<Double>, lows:List<Double>, closes:List<Double>, period:Int=14): Double {
        if (closes.size<2) return 0.0
        val trs=mutableListOf<Double>()
        for(i in 1 until closes.size){ val tr=maxOf(highs[i]-lows[i], abs(highs[i]-closes[i-1]), abs(lows[i]-closes[i-1])); trs.add(tr) }
        if(trs.isEmpty()) return 0.0
        return trs.takeLast(period).average()
    }
    fun vwma(closes:List<Double>, volumes:List<Double>, period:Int=20): Double {
        if(closes.size<period) return closes.lastOrNull()?:0.0
        val c=closes.takeLast(period); val v=volumes.takeLast(period)
        val num=c.zip(v){a,b->a*b}.sum(); val den=v.sum()
        return if(den==0.0) c.average() else num/den
    }
    fun sma(values:List<Double>, period:Int): Double { if(values.size<period) return values.average(); return values.takeLast(period).average() }
    fun stdev(values:List<Double>, period:Int): Double { val s=values.takeLast(period); val m=s.average(); return sqrt(s.map{(it-m)*(it-m)}.average()) }
    fun bollingerLower(closes:List<Double>, period:Int=20, mult:Double=2.0): Double { return sma(closes,period) - mult*stdev(closes,period) }
    fun wvf(lows:List<Double>, closes:List<Double>, pd:Int=22): Double {
        if(closes.size<pd) return 0.0
        val highest=closes.takeLast(pd).max()
        val low= lows.last()
        if(highest==0.0) return 0.0
        return (highest-low)/highest*100
    }
    fun wvfUpper(closes:List<Double>, lows:List<Double>, pd:Int=22, bbl:Int=20, mult:Double=2.0): Double {
        val wvfs=mutableListOf<Double>()
        for(i in pd until closes.size){ val h=closes.subList(i-pd,i).max(); val l=lows[i]; wvfs.add((h-l)/h*100) }
        if(wvfs.size<bbl) return 100.0
        return sma(wvfs,bbl) + mult*stdev(wvfs,bbl)
    }
}
