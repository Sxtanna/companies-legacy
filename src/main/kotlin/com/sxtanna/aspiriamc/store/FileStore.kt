package com.sxtanna.aspiriamc.store

import com.sxtanna.aspiriamc.base.Unique
import com.sxtanna.aspiriamc.exts.ensureUsable
import com.sxtanna.aspiriamc.store.base.Store
import com.sxtanna.korm.Korm
import com.sxtanna.korm.writer.KormWriter
import com.sxtanna.korm.writer.base.Options
import com.sxtanna.korm.writer.base.Options.LIST_ENTRY_ON_NEW_LINE
import java.io.File
import kotlin.reflect.KClass

open class FileStore<T : Unique<I>, I : Any>(private val folder: File, private val clazz: KClass<T>) : Store<T, I> {

    private val korm = Korm(writer = KormWriter(2, Options.min(LIST_ENTRY_ON_NEW_LINE)))


    override fun save(data: T) {
        korm.push(data, getFileForData(data.uuid, true))
    }

    override fun load(uuid: I): T? {
        return try {
            val file = getFileForData(uuid, false)

            if (file.exists().not()) {
                return null
            }

            korm.pull(file).to(clazz)
        } catch (ignored: Exception) {
            null
        }
    }


    fun kill(uuid: I) {
        val file = getFileForData(uuid, false)
        file.delete()
    }

    fun loadAll(): List<T> {
        if (folder.exists().not()) {
            return emptyList()
        }

        return folder.listFiles().filter { it.isFile }.filter { it.extension.equals("korm", true) }.mapNotNull {
            try {
                korm.pull(it).to(clazz)
            } catch (ignored: Exception) {
                null
            }
        }
    }


    private fun getFileForData(uuid: I, createIfNotFound: Boolean): File {
        val file = folder.resolve("$uuid.korm")

        if (createIfNotFound) {
            file.ensureUsable()
        }

        return file
    }

}