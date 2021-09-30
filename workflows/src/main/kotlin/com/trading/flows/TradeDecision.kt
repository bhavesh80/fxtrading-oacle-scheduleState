package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import com.sun.istack.NotNull
import com.trading.contracts.TradeContract
import com.trading.states.TradeState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*


@InitiatingFlow
@StartableByRPC
class TradeDecision(
    val linearId: String,
    val tradeStatus: String
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // Step 1. Get a reference to the notary service on our network and our key pair.
        println("Obtaining notary reference")
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        // Step 2: Using the linear ID get the Trade State on ledger
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null, listOf(UUID.fromString(linearId)))
        val stateAndRefList: List<StateAndRef<TradeState>> =
            serviceHub.vaultService.queryBy(TradeState::class.java, queryCriteria).states
        if (stateAndRefList.isEmpty()) throw FlowException("State does not exist on Ledger.")
        val stateAndRef = stateAndRefList.first()

        // Step 3: Get the counter party the trade has to be assigned to after processing
        println("Looking up for party ")
        val state: TradeState = stateAndRef.state.data

        val tradeCounterParty: Party = state.requester
        println("Found Party ${tradeCounterParty.name}")

        val currencyPair = "$state.sellCurrency/$state.buyCurrency"
        val currencyRates = state.currencyRate
        //init
        var tradeState = TradeState(
            state.sellAmount,
            state.receivedAmount,
            state.sellCurrency,
            state.buyCurrency,
            tradeStatus,
            tradeCounterParty,
            ourIdentity,
            state.currencyRate
        )

        println("Collecting keys - oracle , own")
        val cmdRequiredSigners = listOf(ourIdentity.owningKey,tradeCounterParty.owningKey)

        println("Building transaction")
        var txCommand =
            Command(TradeContract.Commands.CreateTradeWithOracle(currencyPair, currencyRates), cmdRequiredSigners)
        var txBuilder = TransactionBuilder(notary)
            .addInputState(stateAndRef)
            .addOutputState(tradeState, TradeContract.ID)
            .addCommand(txCommand)


        println("Verify if transaction is valid")
        txBuilder.verify(serviceHub)

        println("Sign the transaction")
        val selfSignedTransaction = serviceHub.signInitialTransaction(txBuilder)

//        send transaction to other party and receive it back with their signature
        val otherPartySession = initiateFlow(tradeCounterParty)
        val signedTransaction = subFlow(CollectSignaturesFlow(selfSignedTransaction, setOf(otherPartySession),  listOf(ourIdentity.owningKey)))


        println("Transaction complete")
        return subFlow(FinalityFlow(signedTransaction, (otherPartySession)))

    }
}


@InitiatedBy(TradeDecision::class)
class TradeDecisionResponder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction?>() {
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