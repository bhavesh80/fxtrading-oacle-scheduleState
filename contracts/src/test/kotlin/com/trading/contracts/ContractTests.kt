package com.trading.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import com.trading.states.TradeState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.security.PublicKey

class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("com.trading"))
    private val PartyA = TestIdentity(CordaX500Name("PartyA", "NY", "US"))
    private val PartyB = TestIdentity(CordaX500Name("PartyB", "NY", "US"))
    private val orderPlaced = "OrderPlaced"
    private val orderFilled = "OrderFilled"

    private var state = TradeState(
        100,
        150,
        "EUR",
        "USD",
        orderPlaced,
        PartyA.party,
        UniqueIdentifier()
    )

    @Test
    fun `Basic Test cases`() {
        ledgerServices.ledger {
            transaction {
                output(TradeContract.ID, state)
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
                verifies()
            }
        }
    }

    @Test
    fun `Trade initiator and Receiver cannot be same`() {
        ledgerServices.ledger {
            transaction {
                output(TradeContract.ID, state.copy(tradeInitiator = PartyA.party, tradeReceiver = PartyA.party))
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
                fails()
            }
            transaction {
                output(TradeContract.ID, state.copy(tradeInitiator = PartyA.party, tradeReceiver = PartyB.party))
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
                verifies()
            }

        }
    }

    @Test
    fun `Trade sell-Value value should be greater than 0 & Exchange currency size to be equal to 3 Character`() {
        ledgerServices.ledger {
            transaction {
                output(TradeContract.ID, state.copy(sellValue = -1))
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
                fails()
            }
            transaction {
                output(TradeContract.ID, state.copy(buyValue = 0))
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
                fails()
            }
            transaction {
                output(TradeContract.ID, state.copy(sellCurrency = "ABCD"))
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
                fails()
            }
            transaction {
                output(TradeContract.ID, state.copy(sellCurrency = "USD" ,buyCurrency = "USD"))
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
                fails()
            }
            transaction {
                output(TradeContract.ID, state.copy(sellCurrency = ""))
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
                fails()
            }
        }
    }

    @Test
    fun `Transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(TradeContract.ID, state)
                output(TradeContract.ID, state)
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
//                `fails with`("No inputs should be consumed while initiating trade")
                fails()
            }
        }
    }

    @Test
    fun `Transaction must have one output state`() {
        ledgerServices.ledger {
            transaction {
                output(TradeContract.ID, state)
                output(TradeContract.ID, state)
                command(PartyA.publicKey, TradeContract.Commands.CreateTrade())
//                `fails with`("Only one output state should be created")
                fails()
            }
        }
    }
}
