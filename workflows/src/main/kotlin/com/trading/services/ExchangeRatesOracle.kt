package com.trading.services

import com.trading.contracts.TradeContract
import net.corda.core.contracts.Command
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction
import java.util.LinkedHashMap


class MaxSizeHashMap<K, V>(private val maxSize: Int = 1024) : LinkedHashMap<K, V>() {
    override fun removeEldestEntry(eldest: Map.Entry<K, V>?) = size > maxSize
}

@CordaService
class ExchangeRatesOracle(val services: ServiceHub) : SingletonSerializeAsToken() {

    private val myKey = services.myInfo.legalIdentities.first().owningKey

    private val currencyRates = mapOf("USD/INR" to 73.81 , "EUR/USD" to 1.17)

    fun query(n: String): Double {
        require(n.isNotBlank() or (n.length == 7)) { "Invalid value of N" } // URL param is n not N.
        return currencyRates[n] ?: 0.0
    }

    fun sign(ftx: FilteredTransaction): TransactionSignature {
        ftx.verify()

        fun isCommandWithCorrectCurrencyRateAndIAmSigner(elem: Any) = when {
            elem is Command<*> && elem.value is TradeContract.Commands.CreateTrade -> {
                val cmdData = elem.value as TradeContract.Commands.CreateTrade
                myKey in elem.signers && query(cmdData.currencyPair) == cmdData.currencyRate
            }
            else -> false
        }
        val isValidMerkleTree = ftx.checkWithFun(::isCommandWithCorrectCurrencyRateAndIAmSigner)

        if (isValidMerkleTree) {
            return services.createSignature(ftx, myKey)
        } else {
            throw IllegalArgumentException("Oracle signature requested over invalid transaction.")
        }
    }
}