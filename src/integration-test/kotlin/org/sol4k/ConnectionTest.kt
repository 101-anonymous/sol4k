package org.sol4k

import org.junit.jupiter.api.Test
import org.sol4k.api.TransactionSimulationError
import org.sol4k.api.TransactionSimulationSuccess
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction
import org.sol4k.instruction.SplTransferInstruction
import org.sol4k.instruction.TransferInstruction
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ConnectionTest {
    private val rpcUrl = getRpcUrl()
    private val secretKey = getSecretKey()

    @Test
    fun shouldGetBalance() {
        val connection = Connection(rpcUrl)
        val wallet = PublicKey("DxPv2QMA5cWR5Xfg7tXr5YtJ1EEStg5Kiag9HhkY1mSx")

        val balance = connection.getBalance(wallet)

        println("shouldGetBalance: Balance: $balance")
    }

    @Test
    fun shouldGetLatestBlockhash() {
        val connection = Connection(rpcUrl)

        val hash = connection.getLatestBlockhash()

        println("shouldGetLatestBlockhash: hash: $hash")
    }

    @Test
    fun shouldGetLatestBlockhashExtended() {
        val connection = Connection(rpcUrl)

        val blockhash = connection.getLatestBlockhashExtended()

        println("shouldGetLatestBlockhashExtended: hash: $blockhash")
    }

    @Test
    fun shouldSendTransaction() {
        val connection = Connection(rpcUrl)
        val blockhash = connection.getLatestBlockhash()
        val sender = Keypair.fromSecretKey(Base58.decode(secretKey))
        val receiver = PublicKey("DxPv2QMA5cWR5Xfg7tXr5YtJ1EEStg5Kiag9HhkY1mSx")
        val instruction = TransferInstruction(sender.publicKey, receiver, 1000)
        val transaction = Transaction(
            blockhash,
            instruction,
            sender.publicKey
        )
        transaction.sign(sender)

        val signature = connection.sendTransaction(transaction)

        println("shouldSendTransaction: signature: $signature")
    }

    @Test
    fun shouldSimulateTransaction() {
        val connection = Connection(rpcUrl)
        val blockhash = connection.getLatestBlockhash()
        val sender = Keypair.fromSecretKey(Base58.decode(secretKey))
        val receiver = PublicKey("DxPv2QMA5cWR5Xfg7tXr5YtJ1EEStg5Kiag9HhkY1mSx")
        val instruction = TransferInstruction(sender.publicKey, receiver, 1000)
        val transaction = Transaction(
            blockhash,
            instruction,
            sender.publicKey
        )
        transaction.sign(sender)

        val simulation = connection.simulateTransaction(transaction)

        assertTrue("Simulation must be successful") {
            simulation is TransactionSimulationSuccess
        }
        assertEquals(2, (simulation as TransactionSimulationSuccess).logs.size)
    }

    @Test
    fun shouldSimulateTransactionWithAnError() {
        val connection = Connection(rpcUrl)
        val blockhash = connection.getLatestBlockhash()
        val senderWithNoSol = Keypair.generate()
        val receiver = PublicKey("DxPv2QMA5cWR5Xfg7tXr5YtJ1EEStg5Kiag9HhkY1mSx")
        val instruction = TransferInstruction(senderWithNoSol.publicKey, receiver, 1000)
        val transaction = Transaction(
            blockhash,
            instruction,
            senderWithNoSol.publicKey
        )
        transaction.sign(senderWithNoSol)

        val simulation = connection.simulateTransaction(transaction)

        assertTrue("Simulation must produce an error") { simulation is TransactionSimulationError }
        assertEquals("AccountNotFound", (simulation as TransactionSimulationError).error)
    }

    @Test
    fun shouldSendTwoInstructionsInOneTransaction() {
        val connection = Connection(rpcUrl)
        val blockhash = connection.getLatestBlockhash()
        val sender = Keypair.fromSecretKey(
            Base58.decode(secretKey)
        )
        val firstReceiver = PublicKey("DxPv2QMA5cWR5Xfg7tXr5YtJ1EEStg5Kiag9HhkY1mSx")
        val secondReceiver = PublicKey("Hb2zfRfn5RwBq2DNWhee2iTVprfGHgiuK7KsiDA4HfMW")
        val transaction = Transaction(
            blockhash,
            listOf(
                TransferInstruction(sender.publicKey, firstReceiver, 1000),
                TransferInstruction(sender.publicKey, secondReceiver, 1000),
            ),
            sender.publicKey
        )
        transaction.sign(sender)

        val signature = connection.sendTransaction(transaction)

        println("shouldSendTowInstructionsInOneTransaction: signature: $signature")
    }

    @Test
    fun shouldSendCreateAssociatedTokenTransaction() {
        val connection = Connection(rpcUrl)
        val blockhash = connection.getLatestBlockhash()
        val payerWallet = Keypair.fromSecretKey(Base58.decode(secretKey))
        val usdcMintAddress = PublicKey("Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr")
        val destinationWallet = Keypair.generate().publicKey
        val (associatedAccount) = PublicKey.findProgramDerivedAddress(destinationWallet, usdcMintAddress)
        val instruction = CreateAssociatedTokenAccountInstruction(
            payer = payerWallet.publicKey,
            associatedToken = associatedAccount,
            owner = destinationWallet,
            mint = usdcMintAddress,
        )
        val transaction = Transaction(
            blockhash,
            instruction,
            feePayer = payerWallet.publicKey,
        )
        transaction.sign(payerWallet)
        val signature = connection.sendTransaction(transaction)

        println("shouldSendCreateAssociatedTokenTransaction: signature: $signature")
    }

    @Test
    fun shouldGetAccountInfo() {
        val usdc = PublicKey("Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr")
        val connection = Connection(rpcUrl)

        val accountInfo = connection.getAccountInfo(usdc)

        println("shouldGetAccountInfo: accountInfo: $accountInfo")
    }

    @Test
    fun shouldGetAccountInfoWhenAccountDoesNotExist() {
        val connection = Connection(rpcUrl)
        val publicKey = Keypair.generate().publicKey

        val accountInfo = connection.getAccountInfo(publicKey)

        println("shouldGetAccountInfoWhenAccountDoesNotExist: accountInfo: $accountInfo")
    }

    @Test
    fun shouldSendSpl() {
        val connection = Connection(rpcUrl)
        val blockhash = connection.getLatestBlockhash()
        val holder = Keypair.fromSecretKey(Base58.decode(secretKey))
        val usdc = PublicKey("Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr")
        val receiverAssociatedAccount = PublicKey("8r2iVNBQgJi59YCdj2YXipguirWZhdysWpL4cEGorN1v")
        val (holderAssociatedAccount) = PublicKey.findProgramDerivedAddress(holder.publicKey, usdc)
        val splTransferInstruction = SplTransferInstruction(
            holderAssociatedAccount,
            receiverAssociatedAccount,
            holder.publicKey,
            100,
        )
        val transaction = Transaction(
            blockhash,
            splTransferInstruction,
            holder.publicKey
        )
        transaction.sign(holder)

        val signature = connection.sendTransaction(transaction)

        println("shouldSendSpl: signature: $signature")
    }

    @Test
    fun shouldGetHealth() {
        val connection = Connection(rpcUrl)

        val health = connection.getHealth()

        println("shouldGetHealth: health: $health")
    }

    // TODO: find a way to fight rate limiting issue for airdrops
    // @Test
    fun shouldRequestAirdrop() {
        val connection = Connection(rpcUrl)
        val receiver = Keypair.fromSecretKey(Base58.decode(secretKey)).publicKey

        val signature = connection.requestAirdrop(receiver, 1000000000)

        println("shouldRequestAirdrop: signature: $signature")
    }

    @Test
    fun shouldGetIdentity() {
        val connection = Connection(rpcUrl)

        val identity = connection.getIdentity()

        println("shouldGetIdentity: identity: $identity")
    }

    @Test
    fun shouldGetTokenAccountBalance() {
        val connection = Connection(rpcUrl)
        val receiverAssociatedAccount = PublicKey("73d3sqQPLsiwKvdJt2XnnLEzNiEjfn2nreqLujM7zXiT")

        val (amount, decimals, uiAmount) = connection.getTokenAccountBalance(receiverAssociatedAccount)

        assertEquals(BigInteger("123"), amount)
        assertEquals(6, decimals)
        assertEquals("0.000123", uiAmount)
    }

    @Test
    fun shouldVerifyIfBlockhashValid() {
        val connection = Connection(rpcUrl)
        val blockhash = connection.getLatestBlockhash()

        val result = connection.isBlockhashValid(blockhash)

        assertTrue("blockhash must be valid") { result }
    }

    @Test
    fun shouldVerifyIfBlockhashValidGivenInvalidBlockhash() {
        val connection = Connection(rpcUrl)
        val anOutdatedBlockhash = "3dseDCjWBhwFxuukMuiRofHSZaNozXYQKAYFj9vDSoca"

        val result = connection.isBlockhashValid(anOutdatedBlockhash)

        assertFalse("blockhash must be invalid") { result }
    }

    @Test
    fun shouldGetEpochInfo() {
        val connection = Connection(rpcUrl)

        val result = connection.getEpochInfo()

        println("shouldGetEpochInfo: result: $result")
    }

    @Test
    fun shouldGetTransactionCount() {
        val connection = Connection(rpcUrl)

        val count = connection.getTransactionCount()

        println("shouldGetTransactionCount: count: $count")
    }

    @Test
    fun shouldGetSignaturesForAddress() {
        val connection = Connection(rpcUrl)

        // val result = connection.getSignaturesForAddress(PublicKey("BSzG62Khqw5pbbWPmoe8iZekExekFQBJmjYhiXhcVvtS"), 1)
        val result = connection.getSignaturesForAddress(PublicKey("BSzG62Khqw5pbbWPmoe8iZekExekFQBJmjYhiXhcVvtS"), 10, PublicKey("5MvXGPaqLix4eGv3iCou9JTQCYeCfctdtDfTxyQGFYS3EQChWT2u7zrtfcseHi2eDpfVEFf1zD6xtimgZcmhkRF2"))

        println("shouldGetSignaturesForAddress: result:")
        println(result.joinToString("\n  ", "  "))
    }

    @Test
    fun shouldGetTransaction() {
        val connection = Connection(rpcUrl)

        // val result = connection.getTransaction(PublicKey("5MvXGPaqLix4eGv3iCou9JTQCYeCfctdtDfTxyQGFYS3EQChWT2u7zrtfcseHi2eDpfVEFf1zD6xtimgZcmhkRF2")) // SOL
        val result = connection.getTransaction(PublicKey("2YJNNm9sP6gYWhP1BSjNeonUpQXs2pvwtEzcnLD9WQo7P6uQHVhi5dnGtw22vWZfsGvcxusM8MraHBZNAZnKZciM")) // Create token account, postTokenBalances[0].uiTokenAmount.uiAmount: null

        println("shouldGetTransaction: result:")
        println(result)
    }

    @Test
    fun shouldGetTransactionV0() {
        val connection = Connection("https://api.mainnet-beta.solana.com")

        val result = connection.getTransaction(PublicKey("5MdATd8GqakZQDdj5Wy124kNt4KBw64uZsubKNHA1RfRY4RhuKxLf8uYKThHee1BiiYgSoJajGR63eWMxu2C6781"), 1) // Version: V0, not LEGACY

        println("shouldGetTransactionV0: result:")
        println(result)
    }

    @Test
    fun shouldGetTokenTransaction() {
        val connection = Connection("https://api.mainnet-beta.solana.com")

        val result = connection.getTransaction(PublicKey("3rouks2VHBXDf5HhcnWr4k8ai6HGRyqmm8QZCJAMWdq3oUowrsgBAm2NBAwBE1ch5e89WyvLXVrvVugdyL29xEWj"))

        println("shouldGetTokenTransaction: result:")
        println(result)
    }

    private fun getRpcUrl(): String {
        val rpcUrl = System.getProperty("E2E_RPC_URL")
        return if (rpcUrl.isNullOrEmpty()) "https://api.devnet.solana.com" else rpcUrl
    }

    private fun getSecretKey(): String {
        val secretKey = System.getProperty("E2E_SECRET_KEY")
        return if (secretKey.isNullOrEmpty()) {
            // Public Key: EwtJVgZQGHe9MXmrNWmujwcc6JoVESU2pmq7wTDBvReF
            // Make sure it has Devnet SOL & Devnet USDC if you rely on it
            "28bMpVHJQjuxo3fWw4cBa6Gz7QELgYkx4cjMxU87aPx9Hn6amZZQwH2J5UNCzSYM1jDjcj7TndiK4gpGSiYyLcPy"
        } else {
            secretKey
        }
    }
}
