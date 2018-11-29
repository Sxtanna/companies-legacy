package com.sxtanna.aspirianmc.database

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.company.Company
import com.sxtanna.aspirianmc.company.Staffer
import com.sxtanna.aspirianmc.config.Configs.COMPANY_COMMAND_TOP_MAX
import com.sxtanna.aspirianmc.database.base.CompanyDatabase
import com.sxtanna.aspirianmc.store.FileStore
import java.util.*

class LocalDatabase(override val plugin: Companies) : CompanyDatabase {

    override val name = "Local"

    private val root = pluginFolder.resolve("companies")

    private val companyStore = FileStore(root.resolve("company"), Company::class)
    private val stafferStore = FileStore(root.resolve("staffer"), Staffer::class)


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

    override fun saveCompany(data: Company) {
        companyStore.save(data)
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

}