package com.sxtanna.aspirianmc.store.base

import com.sxtanna.aspirianmc.base.Identified

interface Store<T : Identified<I>, I : Any> {

    fun save(data: T)

    fun load(uuid: I): T?

}