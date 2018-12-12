package com.sxtanna.aspiriamc.store.base

import com.sxtanna.aspiriamc.base.Unique

interface Store<T : Unique<I>, I : Any> {

    fun save(data: T)

    fun load(uuid: I): T?

}