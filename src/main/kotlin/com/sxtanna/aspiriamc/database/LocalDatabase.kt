package com.sxtanna.aspiriamc.database

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.config.Configs.COMPANY_COMMAND_TOP_MAX
import com.sxtanna.aspiriamc.database.base.CompanyDatabase
import com.sxtanna.aspiriamc.reports.Format
import com.sxtanna.aspiriamc.reports.Reports
import com.sxtanna.aspiriamc.store.FileStore
import java.util.UUID

class LocalDatabase(override val plugin: Companies) : CompanyDatabase {

    override val name = "Local"

    private val root = pluginFolder.resolve("companies-database")

    private val companyStore = FileStore(root.resolve("company"), Company::class)
    private val stafferStore = FileStore(root.resolve("staffer"), Staffer::class)
    private val reportsStore = FileStore(root.resolve("reports"), Reports::class)


    override fun load() {
        root.mkdirs()
    }

    override fun kill() {
        // nothing, omega-lul
    }


    override fun loadCompany(uuid: UUID, returnSync: Boolean, onLoad: (company: Company?) -> Unit) {
        companyStore.load(uuid).let(onLoad)
    }

    override fun killCompany(uuid: UUID) {
        companyStore.kill(uuid)
    }

    override fun saveCompany(data: Company, async: Boolean) {
        companyStore.save(data)
    }

    override fun saveCompany(data: Collection<Company>) {
        data.forEach { saveCompany(it, false) }
    }


    override fun allCompanies(returnSync: Boolean, onRetrieve: (companies: List<Company>) -> Unit) {
        async {
            val values = companyStore.loadAll()

            if (returnSync.not()) values.apply(onRetrieve)
            else sync {
                values.apply(onRetrieve)
            }
        }
    }

    override fun topCompanies(returnSync: Boolean, onRetrieve: (companies: List<Company>) -> Unit) {
        async {
            val values = companyStore.loadAll().take(plugin.configsManager.get(COMPANY_COMMAND_TOP_MAX).toInt())

            if (returnSync.not()) values.apply(onRetrieve)
            else sync {
                values.apply(onRetrieve)
            }
        }
    }


    override fun loadStaffer(uuid: UUID, returnSync: Boolean, onLoad: (staffer: Staffer?) -> Unit) {
        stafferStore.load(uuid).let(onLoad)
    }

    override fun saveStaffer(data: Staffer) {
        stafferStore.save(data)
    }

    override fun saveReports(data: Reports) {
        reportsStore.save(data)
    }

    override fun loadReports(format: Format, before: Long, returnSync: Boolean, onLoad: (List<Reports>) -> Unit) {
        async {
            val values = reportsStore.loadAll().filter { it.format == format }.filter { it.occurredAt >= before }

            if (returnSync.not()) values.apply(onLoad)
            else sync {
                values.apply(onLoad)
            }
        }
    }

}