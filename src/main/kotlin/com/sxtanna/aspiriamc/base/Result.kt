package com.sxtanna.aspiriamc.base

import java.io.PrintWriter
import java.io.StringWriter

sealed class Result<T : Any> {


    fun <O : Any> with(block: () -> Result<O>): Result<O> = Result.of {
        when (this@Result) {
            is Some -> {
                when (val result = block.invoke()) {
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


    data class Some<T : Any>(val data: T) : Result<T>()

    data class None<T : Any>(val info: String) : Result<T>() {

        fun rethrow(): Nothing {
            throw IllegalStateException(info)
        }

    }


    companion object {

        inline fun <T : Any> of(block: ResultContext.() -> T?): Result<T> = try {
            val data = checkNotNull(block(ResultContext))
            Some(data)
        } catch (ex: Exception) {
            val info = ex.message ?: StringWriter().apply { ex.printStackTrace(PrintWriter(this)) }.toString()
            None(info)
        }

    }



    object ResultContext {

        private data class ResultException(override val message: String) : Exception(message)

        fun fail(reason: String): Nothing {
            throw ResultException(reason)
        }

    }

}
