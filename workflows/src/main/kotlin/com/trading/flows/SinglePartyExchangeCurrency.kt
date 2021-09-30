package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.transactions.SignedTransaction
import com.trading.contracts.TradeContract
import com.trading.states.TradeState
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.contracts.Command
import net.corda.core.identity.Party


@InitiatingFlow
@StartableByRPC
class SinglePartyExchangeCurrency(
    private val sellAmount: Double,
    private val sellCurrency: String,
    private val buyCurrency: String
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        println("Obtaining notary reference")
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val tradeState = TradeState(
            sellAmount,
            0.0,
            sellCurrency,
            buyCurrency,
            "Pending",
            ourIdentity,
            null,
            0.0
        )

        println("Collecting keys - oracle , own")
        val cmdRequiredSigners = listOf(ourIdentity.owningKey)

        println("Building transaction")
        var txCommand =
            Command(TradeContract.Commands.CreateTradeWithOracle("",0.0), cmdRequiredSigners)
        var txBuilder = TransactionBuilder(notary)
            .addOutputState(tradeState, TradeContract.ID)
            .addCommand(txCommand)


        println("Verify if transaction is valid")
        txBuilder.verify(serviceHub)

        println("Sign the transaction")
        val ptx = serviceHub.signInitialTransaction(txBuilder)

        println("Transaction complete")
        return subFlow(FinalityFlow(ptx, listOf()))

    }
}

