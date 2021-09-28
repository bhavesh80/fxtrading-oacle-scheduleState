package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.transactions.SignedTransaction
import com.trading.contracts.TradeContract
import net.corda.core.transactions.TransactionBuilder
import com.trading.states.TradeState
import net.corda.core.contracts.Command
import net.corda.core.identity.CordaX500Name
import java.util.function.Predicate


@InitiatingFlow
@StartableByRPC
class TradeInitiator(
    private val sellAmount: Double,
    private val sellCurrency: String,
    private val buyCurrency: String
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

//        val currencyPair = "$sellCurrency/$buyCurrency"

        println("Obtaining notary reference")
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
//        val oracleName = CordaX500Name("Oracle", "New York", "US")
//
//        println("Obtaining Oracle reference")
//        val oracle = serviceHub.networkMapCache.getNodeByLegalName(oracleName)?.legalIdentities?.first()
//            ?: throw IllegalArgumentException("Requested oracle $oracleName not found on network.")
//
//
//        println("Querying for Currency Rates")
//        //query oracle for currency rate
//        val currencyRates = subFlow(QueryRates(oracle, currencyPair))
//        val convertedCurrency = "%.2f".format(sellAmount*currencyRates)
//
//        println("$currencyPair Rate is : $currencyRates")
//        println("Given $sellCurrency-$sellAmount - Receiving $buyCurrency-$convertedCurrency")

        //init
        var tradeState = TradeState(
            sellAmount,
            0.0,
            sellCurrency,
            buyCurrency,
            "Pending",
            ourIdentity,
            0.0
        )

        println("Collecting keys - oracle , own")
        val cmdRequiredSigners = listOf(ourIdentity.owningKey)

        println("Building transaction")
        var txCommand =
            Command(TradeContract.Commands.CreateTrade("",0.0), cmdRequiredSigners)
        var txBuilder = TransactionBuilder(notary)
            .addOutputState(tradeState, TradeContract.ID)
            .addCommand(txCommand)


        println("Verify if transaction is valid")
        txBuilder.verify(serviceHub)

        println("Sign the transaction")
        val ptx = serviceHub.signInitialTransaction(txBuilder)

   /*     println("preparing data for sending to oracle and signing")
        val ftx = ptx.buildFilteredTransaction(Predicate {
            when (it) {
                is Command<*> -> oracle.owningKey in it.signers && it.value is TradeContract.Commands.CreateTrade
                else -> false
            }
        })

        println("Oracle verify data and collect signature")
        val oracleSignature = subFlow(SignRates(oracle, ftx))

        val stx = ptx.withAdditionalSignature(oracleSignature) */

        println("Transaction complete")
        return subFlow(FinalityFlow(ptx, listOf()))

    }
}

