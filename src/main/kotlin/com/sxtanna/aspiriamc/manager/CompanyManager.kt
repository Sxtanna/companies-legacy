package com.sxtanna.aspiriamc.manager

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.Result
import com.sxtanna.aspiriamc.base.Result.Companion.of
import com.sxtanna.aspiriamc.base.Result.None
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.config.Configs.*
import com.sxtanna.aspiriamc.exts.ensureUsable
import com.sxtanna.aspiriamc.manager.base.Manager
import org.bukkit.entity.Player
import java.io.File
import java.lang.System.currentTimeMillis
import java.util.UUID
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class CompanyManager(override val plugin: Companies) : Manager("Companies") {

    val topCache = TopCache()
    val sponsorManager = SponsorManager()

    internal val cache = mutableMapOf<Any, Company>()

    val memberMax: Int
        get() = plugin.configsManager.get(COMPANY_MEMBER_MAX)
    val createFee: Double
        get() = plugin.configsManager.get(COMPANY_CREATE_FEE)
    val renameFee: Double
        get() = plugin.configsManager.get(COMPANY_RENAME_FEE)


    @Volatile
    private var loading = true


    override fun enable() {
        sponsorManager.enable()

        plugin.companyDatabase.allCompanies(true, ::push)

        while (loading) {
            continue
        }

        plugin.logger.info("FINISHED LOADING COMPANIES FROM DATABASE")

        repeatAsync((20 * 60) * 5) {
            plugin.companyDatabase.saveCompany(companies)
        }
    }

    override fun disable() {
        sponsorManager.disable()

        plugin.companyDatabase.saveCompany(cache.values)
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

        plugin.companyDatabase.killCompany(company.uuid)


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

            plugin.companyDatabase.saveStaffer(staffer)
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
        when(val available = checkNameAvailability(name)) {
            is None -> {
                available.rethrow()
            }
        }

        when (val create = plugin.economyHook.attemptTake(player, createFee)) {
            is Some -> {
                Company(name).apply(::push)
            }
            is None -> {
                create.rethrow()
            }
        }
    }

    fun attemptRename(company: Company, name: String, player: Player) = Result.of {
        when(val available = checkNameAvailability(name)) {
            is None -> {
                available.rethrow()
            }
        }

        when (val rename = plugin.economyHook.attemptTake(player, renameFee)) {
            is Some -> {
                cache.remove(company.name.toLowerCase())
                company.name = name
                cache[company.name.toLowerCase()] = company
            }
            is None -> {
                rename.rethrow()
            }
        }
    }

    fun checkNameAvailability(name: String) = Result.of {
        val max = plugin.configsManager.get(COMPANY_COMMAND_NAME_MAX)

        if (name.length > max) {
            fail("company name '$name' is too long, must be at most $max letters.")
        }

        if (get(name) != null) {
            fail("a company named '$name' already exists, please choose another name.")
        }
    }


    private fun load(uuid: UUID, whenLoaded: (Company?) -> Unit = { }) {
        plugin.companyDatabase.loadCompany(uuid) {
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

        plugin.companyDatabase.saveCompany(data ?: return)
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

    private fun push(data: List<Company>) {
        data.forEach(::push)
        loading = false
    }


    inner class TopCache {

        /*private var out = 0L
        private val max = 60_000L
        private val top = mutableListOf<Company>()*/


        fun attempt(onRetrieve: (companies: List<Company>) -> Unit) {
            companies.sortedByDescending { it.finance.balance }
                    .take(plugin.configsManager.get(COMPANY_COMMAND_TOP_MAX))
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

        val liveTime: Long
            get() = plugin.configsManager.get(SPONSOR_LIVE_TIME)
        val slotCost: Double
            get() = plugin.configsManager.get(SPONSOR_SLOT_COST)


        override fun enable() {
            val sponsoredCompanies = try {
                plugin.korm.pull(File(pluginFolder, "sponsored-companies.korm")).toHashRef<UUID, Long>()
            } catch (ignored: Exception) {
                emptyMap<UUID, Long>()
            }

            cached.putAll(sponsoredCompanies)

            repeatAsync(20 * 60) {
                if (cached.isEmpty()) return@repeatAsync

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
            plugin.korm.push(cached, pluginFolder.resolve("sponsored-companies.korm").ensureUsable())
        }


        fun isSponsored(company: Company): Boolean {
            return company.uuid in cached
        }

        fun attemptPurchaseSponsorship(company: Company, player: Player) = of {
            when {
                cached.size >= 9     -> {
                    fail("no available sponsor slots left")
                }
                isSponsored(company) -> {
                    fail("company is already sponsored, ${sponsorshipTimeLeft(company)} left")
                }
                else                 -> when (val result = plugin.economyHook.attemptTake(player, slotCost)) {
                    is Some -> {
                        cached[company.uuid] = currentTimeMillis()
                    }
                    is None -> {
                        result.rethrow()
                    }
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

        fun sponsorshipTimeLeft(company: Company): String {
            val existed = cached[company.uuid]
            if (existed == null || existed == 0L) {
                return "none"
            }

            return humanReadableTime(liveTime - SECONDS.convert((currentTimeMillis() - existed), MILLISECONDS))
        }

    }

}