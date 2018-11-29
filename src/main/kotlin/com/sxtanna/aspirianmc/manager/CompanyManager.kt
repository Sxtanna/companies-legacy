package com.sxtanna.aspirianmc.manager

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.base.Result
import com.sxtanna.aspirianmc.base.Result.Companion.of
import com.sxtanna.aspirianmc.base.Result.None
import com.sxtanna.aspirianmc.base.Result.Some
import com.sxtanna.aspirianmc.company.Company
import com.sxtanna.aspirianmc.company.Staffer
import com.sxtanna.aspirianmc.config.Configs.*
import com.sxtanna.aspirianmc.exts.ensureUsable
import com.sxtanna.aspirianmc.manager.base.Manager
import org.bukkit.entity.Player
import java.io.File
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class CompanyManager(override val plugin: Companies) : Manager("Companies") {

    val topCache = TopCache()
    val sponsorManager = SponsorManager()

    internal val cache = mutableMapOf<Any, Company>()

    var memberMax = 0
        private set
    var createFee = 0.0
        private set


    override fun enable() {
        memberMax = plugin.configsManager.get(COMPANY_MEMBER_MAX)
        createFee = plugin.configsManager.get(COMPANY_CREATE_FEE)

        sponsorManager.enable()


        delay(60) {
            plugin.database.allCompanies {
                it.forEach(::push)
            }
        }
    }

    override fun disable() {
        sponsorManager.disable()

        cache.values.forEach { plugin.database.saveCompany(it) }
        cache.clear()
    }


    val companies: List<Company>
        get() = cache.values.distinctBy { it.uuid }

    val sponsored: List<Company>
        get() = companies.filter { it.uuid in sponsorManager.cached.keys }


    fun get(uuid: UUID, whenLoaded: (Company?) -> Unit = { }): Result<Company> {
        return of {
            cache[uuid].apply {
                if (this == null) load(uuid, whenLoaded)
            }
        }
    }

    fun get(name: String): Company? {
        return cache[name.toLowerCase()]
    }


    fun top(onRetrieve: (companies: List<Company>) -> Unit) {
        topCache.attempt(onRetrieve)
    }


    fun kill(company: Company) = Result.of {
        sponsorManager.cached.remove(company.uuid)

        cache.remove(company.uuid)
        cache.remove(company.name.toLowerCase())

        plugin.database.killCompany(company.uuid)


        fun fireStaffer(staffer: Staffer) {
            company.product.removeIf {
                val result = it.stafferUUID == staffer.uuid

                if (result) {
                    staffer.voidedItems += it.base
                }

                result
            }

            if (findPlayerByUUID(staffer.uuid) != null) {
                staffer.companyUUID = null
            }

            plugin.database.saveStaffer(staffer)
        }


        company.staffer.forEach {
            val staffer = plugin.stafferManager.get(it) { data, _ ->
                fireStaffer(data)
            }

            when (staffer) {
                is Some -> {
                    fireStaffer(staffer.data)
                }
            }
        }
    }


    fun attemptCreate(name: String, player: Player) = Result.of {
        val max = plugin.configsManager.get(COMPANY_COMMAND_NAME_MAX)

        if (name.length > max) {
            fail("company name $name is too long, must be at most  $max")
        }

        when (val existed = get(name)) {
            null -> when (val result = plugin.vaultHook.attemptTake(player, createFee)) {
                is Some -> when (result.data.transactionSuccess()) {
                    true -> {
                        Company(name).apply(::push)
                    }
                    else -> {
                        fail("failed to purchase company: ${result.data.errorMessage}")
                    }
                }
                is None -> {
                    result.rethrow()
                }
            }
            else -> {
                fail("company ${existed.name} already exists")
            }
        }
    }


    private fun load(uuid: UUID, whenLoaded: (Company?) -> Unit = { }) {
        plugin.database.loadCompany(uuid) {
            if (it != null) {
                push(it)
            }

            whenLoaded.invoke(it)
        }
    }

    internal fun save(uuid: UUID, remove: Boolean = false) {
        val data = if (remove) {
            cache.remove(uuid).apply {
                cache.remove(name.toLowerCase())
            }
        } else {
            cache[uuid]
        }

        plugin.database.saveCompany(data ?: return)
    }

    private fun push(data: Company) {
        cache[data.uuid] = data
        cache[data.name.toLowerCase()] = data

        data.plugin = plugin
        data.finance.plugin = plugin

        if (data.finance.tariffs == -1) {
            data.finance.tariffs = plugin.configsManager.get(PAYOUTS_DEF_TAXES)
        }

        data.product.forEach { it.plugin = plugin }
    }


    inner class TopCache {

        /*private var out = 0L
        private val max = 60_000L
        private val top = mutableListOf<Company>()*/


        fun attempt(onRetrieve: (companies: List<Company>) -> Unit) {
            companies
                    .sortedByDescending { it.finance.balance }
                    .take(plugin.configsManager.get(COMPANY_COMMAND_TOP_MAX).toInt())
                    .apply(onRetrieve)

            /*if (out == 0L || (currentTimeMillis() - out) >= max) {
               plugin.database.topCompanies {
                   out = currentTimeMillis()

                   top.clear()
                   top += it

                   top.apply(onRetrieve)
               }
            }
            else {
                top.apply(onRetrieve)
            }*/
        }

    }

    inner class SponsorManager(override val plugin: Companies = this@CompanyManager.plugin) : Manager("Sponsors") {

        internal val cached = mutableMapOf<UUID, Long>()

        var liveTime = 0L
            private set
        var slotCost = 0.0
            private set


        override fun enable() {
            liveTime = plugin.configsManager.get(SPONSOR_LIVE_TIME)
            slotCost = plugin.configsManager.get(SPONSOR_SLOT_COST)

            val sponsoredCompanies = try {
                plugin.korm().pull(File(pluginFolder, "sponsored-companies.korm")).toHashRef<UUID, Long>()
            } catch (ignored: Exception) {
                emptyMap<UUID, Long>()
            }

            cached.putAll(sponsoredCompanies)

            //println("Loaded $sponsoredCompanies")
            //println("Cached $cached")

            repeat(20 * 60) {
                val iter = cached.iterator()

                while (iter.hasNext()) {
                    val next = iter.next()

                    if ((liveTime * 1000L) - (currentTimeMillis() - next.value) <= 0) {
                        iter.remove()
                    }
                }
            }
        }

        override fun disable() {
            plugin.korm().push(cached, ensureUsable(File(pluginFolder, "sponsored-companies.korm")))
        }


        fun attemptPurchaseSponsorship(company: Company, player: Player) = of {
            when (val existed = cached[company.uuid]) {
                null -> when (val result = plugin.vaultHook.attemptTake(player, slotCost)) {
                    is Some -> when (result.data.transactionSuccess()) {
                        true -> {
                            cached[company.uuid] = currentTimeMillis()
                            Unit
                        }
                        else -> {
                            fail("failed to purchase sponsor slot: ${result.data.errorMessage}")
                        }
                    }
                    is None -> {
                        result.rethrow()
                    }
                }
                else -> {
                    fail("company is already sponsored, ${humanReadableTime(liveTime - SECONDS.convert((currentTimeMillis() - existed), MILLISECONDS))} left")
                }
            }
        }


        /**
         * Super verbose, but its quick and efficient
         */
        fun humanReadableTime(timeInSeconds: Long = liveTime): String {
            var unit = "second"

            if (timeInSeconds < 60) {
                if (timeInSeconds > 1) unit += 's'
                return "$timeInSeconds $unit"
            }

            unit = "minute"

            val minutes = timeInSeconds / 60
            var remains = timeInSeconds % 60

            if (minutes < 60) {
                if (minutes > 1) unit += 's'

                return "$minutes $unit${if (remains == 0L) "" else " and $remains seconds"}"
            }

            unit = "hour"

            val hours = minutes / 60
            remains = minutes % 60

            if (hours < 24) {
                if (hours > 1) unit += 's'

                return "$hours $unit${if (remains == 0L) "" else " and $remains minutes"}"
            }

            unit = "day"

            val days = hours / 24
            remains = hours % 24

            if (days > 1) unit += 's'

            return "$days $unit${if (remains == 0L) "" else " and $remains hours"}"
        }

    }

}