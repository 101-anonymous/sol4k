package org.sol4k.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// https://solana.com/docs/rpc/http/gettransaction

@Serializable
data class GetTransactionResponse(
    val slot: Long,
    val transaction: GetTransactionTransaction,
    val blockTime: Long? = null,
    val meta: GetTransactionMeta? = null,
    /** version: <"legacy"|number|undefined> - Transaction version */
    val version: String? = null,
)

// https://solana.com/docs/rpc/json-structures#transactions

@Serializable
data class GetTransactionTransaction(
    val signatures: List<String>,
    val message: GetTransactionTransactionMessage,
)

@Serializable
data class GetTransactionTransactionMessage(
    /**
     * 交易使用的 Base58 编码公钥列表。从第一个公钥开始的 N 个公钥必须签署交易，该数量由 message.header.numRequiredSignatures 定义
     * 签名在 transaction.signatures 中一一对应
     */
    val accountKeys: List<String>,
    /** 详细说明交易所需的账户类型和签名 */
    val header: JsonElement,
    /** 账本中最近一个区块的 Base58 编码哈希值，用于防止交易重复并提供交易生命周期 */
    val recentBlockhash: String,
    /** 按顺序执行并在一个原子事务中提交的程序指令列表 */
    val instructions: JsonElement,
    /** 交易使用的地址表查找列表，用于从链上地址查找表中动态加载地址。如果 maxSupportedTransactionVersion 未设置，则为未定义 */
    val addressTableLookups: List<JsonElement>? = null,
)

@Serializable
data class GetTransactionMeta(
    val err: JsonElement? = null,
    val fee: Long,
    val preBalances: List<Long>,
    val postBalances: List<Long>,
    // https://solana.com/docs/rpc/json-structures#inner-instructions
    val innerInstructions: List<JsonElement>? = null,
    val preTokenBalances: List<TokenBalance>,
    val postTokenBalances: List<TokenBalance>,
    val logMessages: List<String>? = null,
    val rewards: List<JsonElement>? = null,
    val loadedAddresses: JsonElement? = null,
    val returnData: JsonElement? = null,
    val computeUnitsConsumed: Long? = null,
)

// https://solana.com/docs/rpc/json-structures#token-balances

@Serializable
data class TokenBalance(
    val accountIndex: Int,
    val mint: String,
    val owner: String? = null,
    val programId: String? = null,
    val uiTokenAmount: UITokenAmount,
)

@Serializable
data class UITokenAmount(
    val amount: String,
    val decimals: Int,
    /** DEPRECATED */
    val uiAmount: Long,
    val uiAmountString: String,
)
