package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import com.sun.istack.NotNull
import net.corda.core.flows.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.transactions.SignedTransaction
import com.trading.contracts.TradeContract
import net.corda.core.transactions.TransactionBuilder
import com.trading.states.TradeState
import net.corda.core.contracts.Command
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import org.intellij.lang.annotations.Flow
import java.util.function.Predicate


@InitiatingFlow
@StartableByRPC
class TwoPartyTradeFlow(
    private val sellAmount: Double,
    private val sellCurrency: String,
    private val buyCurrency: String,
    private val responder: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val currencyPair = "$sellCurrency/$buyCurrency"

        println("Obtaining notary reference")
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val oracleName = CordaX500Name("Oracle", "New York", "US")

        println("Obtaining Oracle reference")
        val oracle = serviceHub.networkMapCache.getNodeByLegalName(oracleName)?.legalIdentities?.first()
            ?: throw IllegalArgumentException("Requested oracle $oracleName not found on network.")


        println("Querying for Currency Rates")
        //query oracle for currency rate
        val currencyRates = subFlow(QueryRates(oracle, currencyPair))
        val convertedCurrency = "%.2f".format(sellAmount * currencyRates)

        println("$currencyPair Rate is : $currencyRates")
        println("Given $sellCurrency-$sellAmount - Receiving $buyCurrency-$convertedCurrency")


        //init
        var tradeState = TradeState(
            sellAmount,
            convertedCurrency.toDouble(),
            sellCurrency,
            buyCurrency,
            "OrderPlaced",
            ourIdentity,
            responder,
            currencyRates
        )

        println("Collecting keys - oracle , own")
        val cmdRequiredSigners = listOf(oracle.owningKey,ourIdentity.owningKey,responder.owningKey)

        println("Building transaction")
        var txCommand =
            Command(TradeContract.Commands.CreateTradeWithOracle(currencyPair, currencyRates), cmdRequiredSigners)
        var txBuilder = TransactionBuilder(notary)
            .addOutputState(tradeState, TradeContract.ID)
            .addCommand(txCommand)


        println("Verify if transaction is valid")
        txBuilder.verify(serviceHub)

        println("Sign the transaction")
        val selfSignedTransaction = serviceHub.signInitialTransaction(txBuilder)

        println("preparing data for sending to oracle and signing")
        val ftx = selfSignedTransaction.buildFilteredTransaction(Predicate {
            when (it) {
                is Command<*> -> {
                    oracle.owningKey in it.signers && it.value is TradeContract.Commands.CreateTradeWithOracle
                }
                else -> false
            }
        })

        println("Oracle verify data and collect signature")
        val oracleSignature = subFlow(SignRates(oracle, ftx))

        val selfAndOracleSignedTransaction = selfSignedTransaction.withAdditionalSignature(oracleSignature)



//        send transaction to other party and receive it back with their signature
        val otherPartySession = initiateFlow(responder)
        val signedTransaction = subFlow(CollectSignaturesFlow(selfAndOracleSignedTransaction, setOf(otherPartySession),  listOf(ourIdentity.owningKey)))




        println("Transaction complete")
        return subFlow(FinalityFlow(signedTransaction, (otherPartySession)))


    }
}

@InitiatedBy(TwoPartyTradeFlow::class)
class Responder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction? {
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(@NotNull stx: SignedTransaction) {
                val state = stx.tx.outputs.iterator().next().data as TradeState
                println("accept transaction with linear ${state.linearId}")

            }
        })
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}