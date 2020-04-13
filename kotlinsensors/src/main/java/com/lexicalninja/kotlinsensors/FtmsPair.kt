package com.lexicalninja.kotlinsensors

data class FtmsPair<out A, out B>(val machine: A, val targetSettings: B) {

    override fun toString(): String = "($machine, $targetSettings)"
}

//infix fun <A, B> A.to(that: B): FtmsPair<A, B> = FtmsPair(this, that)
//fun <T> FtmsPair<T, T>.toList(): List<T> = listOf(machine, targetSettings)