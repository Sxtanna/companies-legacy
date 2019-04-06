package com.sxtanna.aspiriamc.database

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.config.Configs.COMPANY_COMMAND_TOP_MAX
import com.sxtanna.aspiriamc.database.KueryDatabase.Consts.DEF_KORM
import com.sxtanna.aspiriamc.database.KueryDatabase.Consts.EXECUTOR
import com.sxtanna.aspiriamc.database.base.CompanyDatabase
import com.sxtanna.aspiriamc.exts.ensureUsable
import com.sxtanna.aspiriamc.metrics.Metrics
import com.sxtanna.aspiriamc.reports.Format
import com.sxtanna.aspiriamc.reports.Reports
import com.sxtanna.db.Kuery
import com.sxtanna.db.KueryTask
import com.sxtanna.db.config.KueryConfig
import com.sxtanna.db.ext.Big
import com.sxtanna.db.ext.PrimaryKey
import com.sxtanna.db.ext.Size
import com.sxtanna.db.struct.Database
import com.sxtanna.db.struct.Resolver
import com.sxtanna.db.struct.base.Duplicate.Update
import com.sxtanna.korm.Korm
import org.bukkit.Material
import java.util.UUID
import java.util.concurrent.Executors

class KueryDatabase(override val plugin: Companies) : CompanyDatabase {

    override val name = "Kuery"

    private val base by lazy { DataDB() }

    private lateinit var sql: Kuery
    private lateinit var con: KueryConfig

    init {
        Resolver.SqlI.resolve<UUID> {
            try {
                UUID.fromString(getString(it.name))
            } catch (ex: Exception) {
                Consts.DEF_UUID
            }
        }
    }


    override fun load() {
        val file = pluginFolder.resolve("sql_config.korm")

        con = try {
            require(file.exists())
            plugin.korm.pull(file).to() ?: KueryConfig.DEFAULT
        } catch (ex: Exception) {
            plugin.korm.push(KueryConfig.DEFAULT, file.ensureUsable())

            KueryConfig.DEFAULT
        }

        sql = Kuery(con)

        sql.load()

        sql {
            use(base)
            create(base.COMPANY)
            create(base.STAFFER)
            create(base.REPORTS)
        }
    }

    override fun kill() {
        if (::sql.isInitialized) {
            sql.unload()
        }
    }


    override fun loadCompany(uuid: UUID, returnSync: Boolean, onLoad: (company: Company?) -> Unit) = accessDB {
        val (company) = select(base.COMPANY).where(InDBCompany::uuid) {
            it equals uuid
        }

        val value = company.singleOrNull()?.let(InDBCompany::toData)

        if (returnSync.not()) value.apply(onLoad)
        else sync {
            value.apply(onLoad)
        }
    }

    override fun killCompany(uuid: UUID) = accessDB {
        delete(base.COMPANY).where(InDBCompany::uuid) {
            it equals uuid
        }.execute()
    }

    override fun saveCompany(data: Company, async: Boolean) {
        fun insert() = sql {
            insert(base.COMPANY,
                   Update(InDBCompany::name, InDBCompany::icon, InDBCompany::tariffs, InDBCompany::balance, InDBCompany::account, InDBCompany::staffer, InDBCompany::product),
                   InDBCompany(data))
        }

        if (async.not()) {
            return insert()
        }

        EXECUTOR.execute {
            insert()
        }
    }

    override fun saveCompany(data: Collection<Company>) {
        accessDB {
            plugin.logger.warning("saving ${data.size} companies to database started")

            insert(base.COMPANY,
                   Update(InDBCompany::name, InDBCompany::icon, InDBCompany::tariffs, InDBCompany::balance, InDBCompany::account, InDBCompany::staffer, InDBCompany::product),
                   data.map { InDBCompany(it) })

            plugin.logger.warning("saving ${data.size} companies to database finished")

        }
    }


    override fun allCompanies(returnSync: Boolean, onRetrieve: (companies: List<Company>) -> Unit) = accessDB {
        val (companies) = select(base.COMPANY)

        val values = companies.map(InDBCompany::toData)

        onRetrieve.invoke(values)
    }

    override fun topCompanies(returnSync: Boolean, onRetrieve: (companies: List<Company>) -> Unit) = accessDB {
        val (companies) = select(base.COMPANY).descend(InDBCompany::balance).limit(plugin.configsManager.get(COMPANY_COMMAND_TOP_MAX).toLong())

        val values = companies.map(InDBCompany::toData)

        if (returnSync.not()) values.apply(onRetrieve)
        else sync {
            values.apply(onRetrieve)
        }
    }


    override fun loadStaffer(uuid: UUID, returnSync: Boolean, onLoad: (staffer: Staffer?) -> Unit) = accessDB {
        val (staffer) = select(base.STAFFER).where(InDBStaffer::uuid) {
            it equals uuid
        }

        val value = staffer.singleOrNull()?.let(InDBStaffer::toData)

        if (returnSync.not()) value.apply(onLoad)
        else sync {
            value.apply(onLoad)
        }
    }

    override fun saveStaffer(data: Staffer) = accessDB {
        insert(base.STAFFER,
               Update(InDBStaffer::companyUUID, InDBStaffer::voidedItems),
               InDBStaffer(data))
    }

    override fun saveReports(data: Reports) = accessDB {
        insert(base.REPORTS, InDBReports(data))
    }

    override fun loadReports(format: Format, before: Long, returnSync: Boolean, onLoad: (List<Reports>) -> Unit) = accessDB {
        val (reports) = if (before == Long.MIN_VALUE) { // all of this format
            select(base.REPORTS).where(InDBReports::format) {
                it equals format
            }
        } else {
            select(base.REPORTS)
                    .where(InDBReports::format) {
                        it equals format
                    }
                    .where(InDBReports::occurred) {
                        it moreThanOrEquals before
                    }
        }

        val value = reports.map(InDBReports::toData)

        if (returnSync.not()) value.apply(onLoad)
        else sync {
            value.apply(onLoad)
        }
    }


    private fun accessDB(block: KueryTask.() -> Unit) {
        EXECUTOR.execute {
            try {
                sql.invoke(block)
            }
            catch (ex: Exception) {
                // ignored
            }
        }
    }


    private inner class DataDB : Database() {

        override val name = con.data.database


        val COMPANY = table<InDBCompany>()
        val STAFFER = table<InDBStaffer>()
        val REPORTS = table<InDBReports>()

    }


    private interface InDBData<T : Any> {

        fun toData(): T

    }

    internal data class InDBCompany(@PrimaryKey
                                    val uuid: UUID,
                                    val name: String,

                                    val icon: Material,

                                    val tariffs: Int,
                                    @Size(30, 2)
                                    val balance: Double,

                                    @Big
                                    val account: String,
                                    @Big
                                    val staffer: String,
                                    @Big
                                    val product: String)
        : InDBData<Company> {
        constructor(company: Company) : this(
            company.uuid,
            company.name,
            company.icon,
            company.finance.tariffs,
            company.finance.balance,
            DEF_KORM.push(company.finance.account),
            DEF_KORM.push(company.staffer),
            DEF_KORM.push(company.product))


        override fun toData(): Company {
            return Company().updateData(
                uuid,
                name,
                icon,
                tariffs,
                balance,
                DEF_KORM.pull(account).toHash(),
                DEF_KORM.pull(staffer).toList(),
                DEF_KORM.pull(product).toList())
        }

    }

    internal data class InDBStaffer(@PrimaryKey
                                    val uuid: UUID,
                                    val companyUUID: UUID?,

                                    @Big
                                    val voidedItems: String)
        : InDBData<Staffer> {
        constructor(staffer: Staffer) : this(
            staffer.uuid,
            staffer.companyUUID,
            DEF_KORM.push(staffer.voidedItems))


        override fun toData(): Staffer {
            return Staffer().updateData(
                uuid,
                if (companyUUID == Consts.DEF_UUID) null else companyUUID,
                DEF_KORM.pull(voidedItems).toList())
        }

    }

    internal data class InDBReports(@PrimaryKey
                                    val uuid: UUID,
                                    val format: Format,
                                    val occurred: Long,
                                    @Big
                                    val data: String)
        : InDBData<Reports> {
        constructor(report: Reports) : this(
            report.uuid,
            report.format,
            report.occurredAt,
            DEF_KORM.push(report))


        override fun toData(): Reports {
            return Reports.Maker.make(DEF_KORM, data)
        }

    }

    internal data class InDBMetrics(@PrimaryKey
                                    val uuid: UUID)
        : InDBData<Metrics> {


        override fun toData(): Metrics {
            return Metrics()
        }

    }


    private object Consts {

        val DEF_KORM = Korm()
        val DEF_UUID = UUID.randomUUID()

        val EXECUTOR = Executors.newCachedThreadPool()

    }

}