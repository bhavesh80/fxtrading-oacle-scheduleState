package com.trading.contracts

import com.trading.states.TradeState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat

class TradeContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands

        when (command.first().value) {
            is Commands.CreateTrade -> {
                requireThat {
                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<TradeState>().single()

                    "Output State should be TradeState class" using (out::class == TradeState::class)

                    "Sell value must not be negative" using (out.sellAmount > 0)

                    "Trade buy & sell currency must not be same" using(out.sellCurrency.toLowerCase() != out.buyCurrency.toLowerCase())

                    "Sell Currency must not be null & less than 3 Character" using (out.sellCurrency.length == 3)
                    "Buy Currency must not be null & less than 3 Character" using (out.buyCurrency.length == 3)
                }
            }
            is Commands.CreateTradeWithOracle -> {
                requireThat {
                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<TradeState>().single()

                    "Output State123 should be TradeState class" using (out::class == TradeState::class)

                    "Sell value must not be negative" using (out.sellAmount > 0)

                    "Trade buy & sell currency must not be same" using(out.sellCurrency.toLowerCase() != out.buyCurrency.toLowerCase())

                    "Sell Currency must not be null & less than 3 Character" using (out.sellCurrency.length == 3)
                    "Buy Currency must not be null & less than 3 Character" using (out.buyCurrency.length == 3)
                }
            }

        }
    }

    companion object {
        const val ID = "com.trading.contracts.TradeContract"
    }

    interface Commands : CommandData {
        class CreateTrade() : Commands
        class CreateTradeWithOracle(val currencyPair: String, val currencyRate: Double) : Commands

    }
}
