package com.sxtanna.aspirianmc.base

import java.io.PrintWriter
import java.io.StringWriter

sealed class Result<T : Any> {


    fun <O : Any> with(block: () -> Result<O>): Result<O> = Result.of {
        when(this@Result) {
            is Some -> {
                when(val result = block.invoke()) {
                    is Some -> {
                        result.data
                    }
                    is None -> {
                        result.rethrow()
                    }
                }
            }
            is None -> {
                rethrow()
            }
        }
    }


    data class Some<T : Any>(val data: T) :  Result<T>()

    data class None<T : Any>(val info: String) : Result<T>() {

        fun rethrow(): Nothing {
            throw IllegalStateException(info)
        }

    }


    enum class Status {

        PASS,
        FAIL,

    }


    companion object {

        inline fun <T : Any> of(block: ResultContext.() -> T?): Result<T> = try {
            val data = checkNotNull(block(ResultContext))
            Some(data)
        }
        catch (ex: Exception) {
            val info = ex.message ?: StringWriter().apply { ex.printStackTrace(PrintWriter(this)) }.toString()
            None(info)
        }


        fun passing(): Result<Status> {
            return of { Status.PASS }
        }

        fun failing(): Result<Status> {
            return of { Status.FAIL }
        }

    }

    object ResultContext {

        fun fail(reason: String): Nothing {
            throw IllegalStateException(reason)
        }

    }

}
