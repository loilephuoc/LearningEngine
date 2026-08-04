package vn.loi.learning.android.platform

import android.util.Log

object AndroidStartupTrace {
    const val TAG="LearningEngineStartup"
    fun mark(phase:String,count:Int?=null){write(false,"phase=$phase thread=${Thread.currentThread().name} elapsedMs=0 success=true${count?.let{" count=$it"}.orEmpty()}")}
    inline fun <T> measured(phase:String,block:()->T):T{val started=System.nanoTime();return try{block().also{write(false,"phase=$phase thread=${Thread.currentThread().name} elapsedMs=${(System.nanoTime()-started)/1_000_000} success=true")}}catch(error:Throwable){write(true,"phase=$phase thread=${Thread.currentThread().name} elapsedMs=${(System.nanoTime()-started)/1_000_000} success=false type=${error.javaClass.simpleName}");throw error}}
    fun write(error:Boolean,message:String){runCatching{if(error)Log.e(TAG,message)else Log.i(TAG,message)}}
}
