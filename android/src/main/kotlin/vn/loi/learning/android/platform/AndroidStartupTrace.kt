package vn.loi.learning.android.platform

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

object AndroidStartupTrace {
    const val TAG="LearningEngineStartup"
    @Volatile var enabled: Boolean = false
    private val flows = ConcurrentHashMap<String, Long>()

    fun mark(phase:String,count:Int?=null){
        if (!enabled) return
        write(false,"phase=$phase thread=${Thread.currentThread().name} elapsedMs=0 success=true${count?.let{" count=$it"}.orEmpty()}")
    }

    fun begin(phase: String) {
        if (!enabled) return
        flows[phase] = System.nanoTime()
        write(false, "phase=$phase event=START thread=${Thread.currentThread().name}")
    }

    fun end(phase: String, detail: String = "") {
        if (!enabled) return
        val started = flows.remove(phase) ?: return
        write(false, "phase=$phase event=END thread=${Thread.currentThread().name} elapsedMs=${(System.nanoTime()-started)/1_000_000} success=true${if(detail.isBlank()) "" else " $detail"}")
    }

    inline fun <T> measured(phase:String,block:()->T):T{
        if (!enabled) return block()
        write(false,"phase=$phase event=START thread=${Thread.currentThread().name}")
        val started=System.nanoTime()
        return try{block().also{write(false,"phase=$phase event=END thread=${Thread.currentThread().name} elapsedMs=${(System.nanoTime()-started)/1_000_000} success=true")}}catch(error:Throwable){write(true,"phase=$phase event=FAILURE thread=${Thread.currentThread().name} elapsedMs=${(System.nanoTime()-started)/1_000_000} success=false type=${error.javaClass.simpleName}");throw error}
    }

    fun write(error:Boolean,message:String){
        if (!enabled) return
        runCatching{if(error)Log.e(TAG,message)else Log.i(TAG,message)}
    }
}
