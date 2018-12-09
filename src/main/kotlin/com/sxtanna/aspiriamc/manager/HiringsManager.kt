package com.sxtanna.aspiriamc.manager

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.Result
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.config.Configs.HIRINGS_RESPOND_TIME
import com.sxtanna.aspiriamc.manager.base.Manager
import org.bukkit.scheduler.BukkitTask
import java.util.*

class HiringsManager(override val plugin: Companies) : Manager("Hiring") {

    internal val cache = mutableMapOf<Company, MutableMap<UUID, Long>>()

    lateinit var clear: BukkitTask


    var respondTime = 0L
        private set


    override fun enable() {
        respondTime = plugin.configsManager.get(HIRINGS_RESPOND_TIME)

        clear = repeatAsync(20) {
            if (cache.isEmpty()) return@repeatAsync

            val iter0 = cache.iterator()

            while (iter0.hasNext()) {
                val (k, v) = iter0.next()

                val iter1 = v.iterator()

                while (iter1.hasNext()) {
                    val next1 = iter1.next()

                    if (next1.value == 1L) {
                        iter1.remove()
                    }
                    else {
                        next1.setValue(next1.value - 1)
                    }
                }

                if (v.isEmpty()) {
                    iter0.remove()
                }
            }
        }
    }

    override fun disable() {
        if (::clear.isInitialized) {
            clear.cancel()
        }

        cache.values.forEach { it.clear() }
        cache.clear()
    }


    fun attemptHire(company: Company, staffer: Staffer): Result<Unit> = Result.of {
        when(val hirings = hirings(staffer)) {
            is Some -> {
                if (hirings.data.size >= 5) fail("already being hired by 5 companies, please wait")
            }
        }

        val existed = cache.getOrPut(company) { mutableMapOf() }.putIfAbsent(staffer.uuid, respondTime)

        if (existed != null) {
            fail("already sent, $existed secs left to respond")
        }
    }

    fun attemptStop(company: Company, staffer: Staffer): Result<Unit> = Result.of {
        val hirings = cache[company] ?: fail("no hiring invitations have been sent")
        val remains = hirings.remove(staffer.uuid) ?: fail("is not being hired")
    }


    fun hirings(staffer: Staffer): Result<List<Company>> = Result.of {
        val hirings = mutableListOf<Company>()

        cache.forEach { company, cache ->
            if (staffer.uuid in cache) hirings += company
        }

        hirings
    }

}