package com.sxtanna.aspirianmc.database.base

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.database.KueryDatabase
import com.sxtanna.aspirianmc.database.LocalDatabase

enum class DatabaseType(private val create: (Companies) -> CompanyDatabase) {

    LOCAL(::LocalDatabase),
    KUERY(::KueryDatabase);


    fun createDatabase(plugin: Companies): CompanyDatabase {
        return create.invoke(plugin)
    }


    companion object {

        fun get(name: String) = try {
            valueOf(name.toUpperCase())
        }
        catch (ignored: Exception) {
            LOCAL
        }

    }

}