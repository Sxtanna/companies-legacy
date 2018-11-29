package com.sxtanna.aspirianmc.database

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.company.Company
import com.sxtanna.aspirianmc.company.Staffer
import com.sxtanna.aspirianmc.config.Configs.COMPANY_COMMAND_TOP_MAX
import com.sxtanna.aspirianmc.database.base.CompanyDatabase
import com.sxtanna.aspirianmc.exts.korm
import com.sxtanna.db.Kuery
import com.sxtanna.db.KueryTask
import com.sxtanna.db.config.KueryConfig
import com.sxtanna.db.ext.Big
import com.sxtanna.db.ext.PrimaryKey
import com.sxtanna.db.struct.Database
import com.sxtanna.db.struct.Resolver
import com.sxtanna.db.struct.base.Duplicate.Update
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bukkit.Material
import java.util.*

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
            korm.pull(file).to() ?: KueryConfig.DEFAULT
        } catch (ex: Exception) {
            if (file.exists().not()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }

            korm.push(KueryConfig.DEFAULT, file)

            KueryConfig.DEFAULT
        }

        sql = Kuery(con)

        sql.load()

        accessDB {
            use(base)
            create(base.COMPANY)
            create(base.STAFFER)
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

    override fun saveCompany(data: Company) = accessDB {
        insert(base.COMPANY, Update(
                InDBCompany::name,
                InDBCompany::icon,
                InDBCompany::balance,
                InDBCompany::account,
                InDBCompany::staffer,
                InDBCompany::product),
                InDBCompany(data))
    }


    override fun allCompanies(returnSync: Boolean, onRetrieve: (companies: List<Company>) -> Unit) = accessDB {
        val (companies) = select(base.COMPANY)

        val values = companies.map(InDBCompany::toData)

        if (returnSync.not()) values.apply(onRetrieve)
        else sync {
            values.apply(onRetrieve)
        }
    }

    override fun topCompanies(returnSync: Boolean, onRetrieve: (companies: List<Company>) -> Unit) = accessDB {
        val (companies) = select(base.COMPANY).descend(InDBCompany::balance).limit(plugin.configsManager.get(COMPANY_COMMAND_TOP_MAX))

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
        insert(base.STAFFER, Update(
                InDBStaffer::companyUUID,
                InDBStaffer::voidedItems),
                InDBStaffer(data))
    }


    private fun accessDB(block: KueryTask.() -> Unit) {
        GlobalScope.launch {
            sql.invoke(block)
        }
    }


    private inner class DataDB : Database() {

        override val name = con.data.database


        val COMPANY = table<InDBCompany>()
        val STAFFER = table<InDBStaffer>()

    }


    private interface InDBData<T : Any> {

        fun toData(): T

    }

    internal data class InDBCompany(@PrimaryKey
                                    val uuid: UUID,
                                    val name: String,

                                    val icon: Material,

                                    val tariffs: Int,
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
                korm.push(company.finance.account),
                korm.push(company.staffer),
                korm.push(company.product))


        override fun toData(): Company {
            return Company().updateData(
                    uuid,
                    name,
                    icon,
                    tariffs,
                    balance,
                    korm.pull(account).toHash(),
                    korm.pull(staffer).toList(),
                    korm.pull(product).toList())
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
                korm.push(staffer.voidedItems))


        override fun toData(): Staffer {
            return Staffer().updateData(
                    uuid,
                    if (companyUUID == Consts.DEF_UUID) null else companyUUID,
                    korm.pull(voidedItems).toList())
        }

    }


    private object Consts {

        val DEF_UUID = UUID.randomUUID()

    }

}