package org.sol4k

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.sol4k.api.*
import org.sol4k.api.Commitment.FINALIZED
import org.sol4k.api.IsBlockhashValidResult
import org.sol4k.exception.RpcException
import org.sol4k.rpc.*
import org.sol4k.rpc.Balance
import org.sol4k.rpc.BlockhashResponse
import org.sol4k.rpc.EpochInfoResult
import org.sol4k.rpc.GetAccountInfoResponse
import org.sol4k.rpc.GetSignaturesForAddressResponse
import org.sol4k.rpc.Identity
import org.sol4k.rpc.RpcErrorResponse
import org.sol4k.rpc.RpcRequest
import org.sol4k.rpc.RpcResponse
import org.sol4k.rpc.SimulateTransactionResponse
import org.sol4k.rpc.TokenBalanceResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

class Connection @JvmOverloads constructor(
    private val rpcUrl: String,
    private val commitment: Commitment = FINALIZED,
) {
    @JvmOverloads
    constructor(
        rpcUrl: RpcUrl,
        commitment: Commitment = FINALIZED,
    ) : this(rpcUrl.value, commitment)

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun getBalance(walletAddress: PublicKey): BigInteger {
        val balance: Balance = rpcCall("getBalance", listOf(walletAddress.toBase58()))
        return balance.value
    }

    @JvmOverloads
    fun getTokenAccountBalance(
        accountAddress: PublicKey,
        commitment: Commitment = this.commitment,
    ): TokenAccountBalance {
        val result: TokenBalanceResult = rpcCall(
            "getTokenAccountBalance",
            listOf(
                Json.encodeToJsonElement(accountAddress.toBase58()),
                Json.encodeToJsonElement(mapOf("commitment" to commitment.toString())),
            ),
        )
        val (amount, decimals, uiAmountString) = result.value
        return TokenAccountBalance(
            amount = BigInteger(amount),
            decimals = decimals,
            uiAmount = uiAmountString,
        )
    }

    @JvmOverloads
    fun getLatestBlockhash(commitment: Commitment = this.commitment): String =
        this.getLatestBlockhashExtended(commitment).blockhash

    @JvmOverloads
    fun getLatestBlockhashExtended(commitment: Commitment = this.commitment): Blockhash {
        val result: BlockhashResponse = rpcCall(
            "getLatestBlockhash",
            listOf(mapOf("commitment" to commitment.toString())),
        )
        return Blockhash(
            blockhash = result.value.blockhash,
            slot = result.context.slot,
            lastValidBlockHeight = result.value.lastValidBlockHeight,
        )
    }

    @JvmOverloads
    fun isBlockhashValid(blockhash: String, commitment: Commitment = this.commitment): Boolean {
        val result: IsBlockhashValidResult = rpcCall(
            "isBlockhashValid",
            listOf(
                Json.encodeToJsonElement(blockhash),
                Json.encodeToJsonElement(mapOf("commitment" to commitment.toString())),
            ),
        )
        return result.value
    }

    fun getHealth(): Health {
        val result: String = rpcCall("getHealth", listOf<String>())
        return if (result == "ok") Health.OK else Health.ERROR
    }

    fun getEpochInfo(): EpochInfo {
        val result: EpochInfoResult = rpcCall("getEpochInfo", listOf<String>())
        return EpochInfo(
            absoluteSlot = result.absoluteSlot,
            blockHeight = result.blockHeight,
            epoch = result.epoch,
            slotIndex = result.slotIndex,
            slotsInEpoch = result.slotsInEpoch,
            transactionCount = result.transactionCount,
        )
    }

    fun getIdentity(): PublicKey {
        val (identity) = rpcCall<Identity, String>("getIdentity", listOf())
        return PublicKey(identity)
    }

    fun getTransactionCount(): Long = rpcCall<Long, String>("getTransactionCount", listOf())

    fun getAccountInfo(accountAddress: PublicKey): AccountInfo? {
        val (value) = rpcCall<GetAccountInfoResponse, JsonElement>(
            "getAccountInfo",
            listOf(
                Json.encodeToJsonElement(accountAddress.toBase58()),
                Json.encodeToJsonElement(mapOf("encoding" to "base64")),
            ),
        )
        return value?.let {
            val data = Base64.getDecoder().decode(value.data[0])
            AccountInfo(
                data,
                executable = value.executable,
                lamports = value.lamports,
                owner = PublicKey(value.owner),
                rentEpoch = value.rentEpoch,
                space = value.space ?: data.size,
            )
        }
    }

    fun requestAirdrop(accountAddress: PublicKey, amount: Long): String {
        return rpcCall(
            "requestAirdrop",
            listOf(
                Json.encodeToJsonElement(accountAddress.toBase58()),
                Json.encodeToJsonElement(amount),
            ),
        )
    }

    fun sendTransaction(transaction: Transaction): String {
        val encodedTransaction = Base64.getEncoder().encodeToString(transaction.serialize())
        return rpcCall(
            "sendTransaction",
            listOf(
                Json.encodeToJsonElement(encodedTransaction),
                Json.encodeToJsonElement(mapOf("encoding" to "base64")),
            )
        )
    }

    fun simulateTransaction(transaction: Transaction): TransactionSimulation {
        val encodedTransaction = Base64.getEncoder().encodeToString(transaction.serialize())
        val result: SimulateTransactionResponse = rpcCall(
            "simulateTransaction",
            listOf(
                Json.encodeToJsonElement(encodedTransaction),
                Json.encodeToJsonElement(mapOf("encoding" to "base64")),
            )
        )
        val (err, logs) = result.value
        if (err != null) {
            when (err) {
                is JsonPrimitive -> return TransactionSimulationError(err.content)
                else -> throw IllegalArgumentException("Failed to parse the error")
            }
        } else if (logs != null) {
            return TransactionSimulationSuccess(logs)
        }
        throw IllegalArgumentException("Unable to parse simulation response")
    }

    fun getSignaturesForAddress(accountAddress: PublicKey, limit: Int, before: PublicKey? = null): List<SignaturesForAddress> {
        val result = rpcCall<List<GetSignaturesForAddressResponse>, JsonElement>(
            "getSignaturesForAddress",
            listOf(
                Json.encodeToJsonElement(accountAddress.toBase58()),
                Json.encodeToJsonElement(
                    mapOf(
                        // commitment string optional
                        // minContextSlot number optional -- The minimum slot that the request can be evaluated at
                        // limit number optional, Default: 1000 -- maximum transaction signatures to return (between 1 and 1,000).
                        "limit" to Json.encodeToJsonElement(limit),
                        // before string optional -- start searching backwards from this transaction signature. If not provided the search starts from the top of the highest max confirmed block.
                        "before" to before ?. let {
                            Json.encodeToJsonElement(before.toBase58())
                        },
                        // until string optional -- search until this transaction signature, if found before limit reached
                    )
                ),
            ),
        )
        return result.map {
            // Resolve: [length] memo
            val memo = it.memo ?. let { memo ->
                val plainMemo = memo.replaceFirst(Regex("""\[\d+] """), "")
                plainMemo
            }
            SignaturesForAddress(
                signature = it.signature,
                slot = it.slot,
                err = it.err,
                memo = memo,
                blockTime = it.blockTime,
                confirmationStatus = it.confirmationStatus,
            )
        }
    }

    fun getTransaction(signature: PublicKey, maxSupportedTransactionVersion: Int? = null): org.sol4k.api.Transaction {
        val result = rpcCall<GetTransactionResponse, JsonElement>(
            "getTransaction",
            listOf(
                Json.encodeToJsonElement(signature.toBase58()),
                Json.encodeToJsonElement(
                    mapOf(
                        // commitment string optional
                        // maxSupportedTransactionVersion number optional -- Set the max transaction version to return in responses. If the requested transaction is a higher version, an error will be returned. If this parameter is omitted, only legacy transactions will be returned, and any versioned transaction will prompt the error.
                        "maxSupportedTransactionVersion" to maxSupportedTransactionVersion ?. let {
                            Json.encodeToJsonElement(maxSupportedTransactionVersion)
                        },
                        // encoding string optional, Default: json, Values: json jsonParsed base64 base58 -- Encoding for the returned Transaction
                        "encoding" to Json.encodeToJsonElement("json"),
                    )
                ),
            ),
        )
        return org.sol4k.api.Transaction.fromGetTransactionResponse(result)
    }

    private inline fun <reified T, reified I : Any> rpcCall(method: String, params: List<I>): T {
        val connection = URL(rpcUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.use {
            val body = Json.encodeToString(
                RpcRequest(method, params)
            )
            it.write(body.toByteArray())
        }
        val responseBody = connection.inputStream.use {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }
        connection.disconnect()
        try {
            val (result) = jsonParser.decodeFromString<RpcResponse<T>>(responseBody)
            return result
        } catch (_: SerializationException) {
            val (error) = jsonParser.decodeFromString<RpcErrorResponse>(responseBody)
            throw RpcException(error.code, error.message, responseBody)
        }
    }
}
