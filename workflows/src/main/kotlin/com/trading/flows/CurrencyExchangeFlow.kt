package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import com.trading.contracts.TradeContract
import com.trading.states.TradeState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.function.Predicate


@InitiatingFlow
@SchedulableFlow
class CurrencyExchangeFlow(private val stateRef: StateRef) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {


        println("Obtaining notary reference")
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val oracleName = CordaX500Name("Oracle", "New York", "US")

        println("Obtaining Oracle reference")
        val oracle = serviceHub.networkMapCache.getNodeByLegalName(oracleName)?.legalIdentities?.first()
            ?: throw IllegalArgumentException("Requested oracle $oracleName not found on network.")


        val input = serviceHub.toStateAndRef<TradeState>(stateRef)
        val stateData = input.state.data
        val currencyPair = stateData.sellCurrency +"/"+ stateData.buyCurrency

        val sellAmount = stateData.sellAmount;

        println("Querying for Currency Pair - $currencyPair")
        //query oracle for currency rate
        val currencyRates = subFlow(QueryRates(oracle, currencyPair))
        val convertedCurrency = "%.2f".format( sellAmount* currencyRates)

        println("$currencyPair Rate is : $currencyRates")
        println("Given Currency Pair $currencyPair -  Currency Rates - $currencyRates - ConvertCurrency/Receiving Amount $convertedCurrency")


        val output = TradeState(
            stateData.sellAmount,
            convertedCurrency.toDouble(),
            stateData.sellCurrency,
            stateData.buyCurrency,
            "Completed",
            ourIdentity,
            currencyRates
        )

        println("Collecting keys - oracle , own")
        val cmdRequiredSigners = listOf(oracle.owningKey, ourIdentity.owningKey)


        val txCommand = Command(TradeContract.Commands.CreateTrade(currencyPair, currencyRates), cmdRequiredSigners)


        val txBuilder = TransactionBuilder(notary)
            .addInputState(input)
            .addOutputState(output, TradeContract.ID)
            .addCommand(txCommand)


        println("Verify if transaction is valid")
        txBuilder.verify(serviceHub)

        println("Sign the transaction")
        val ptx = serviceHub.signInitialTransaction(txBuilder)

        println("preparing data for sending to oracle and signing")
        val ftx = ptx.buildFilteredTransaction(Predicate {
            when (it) {
                is Command<*> -> oracle.owningKey in it.signers && it.value is TradeContract.Commands.CreateTrade
                else -> false
            }
        })

        println("Oracle verify data and collect signature")
        val oracleSignature = subFlow(SignRates(oracle, ftx))

        val stx = ptx.withAdditionalSignature(oracleSignature)

        println("Transaction complete")
        return subFlow(FinalityFlow(stx, listOf()))

    }
}