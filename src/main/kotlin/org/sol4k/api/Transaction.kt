package org.sol4k.api

import org.sol4k.rpc.GetTransactionResponse

data class Transaction(
    val slot: Long,
    val blockTime: Long? = null,
    /** 手续费，通常由第一个账户支付 */
    val fee: Long,
    val feePayer: String,
    val balanceChange: List<Pair<String, Long>>,
    val tokenBalanceChange: List<Triple<String?, String, String>>,
) {
    companion object {
        @JvmStatic
        fun fromGetTransactionResponse(tx: GetTransactionResponse): Transaction {
            val fee = tx.meta?.fee ?: 0
            val accountKeys = tx.transaction.message.accountKeys

            val preBalances = tx.meta?.preBalances ?: emptyList()
            val postBalances = tx.meta?.postBalances ?: emptyList()
            val balanceChange = ArrayList<Pair<String, Long>>(postBalances.size)
            postBalances.forEachIndexed { index, it ->
                (it - preBalances[index]).let { change ->
                    if (change != 0L)
                        balanceChange.add(Pair(accountKeys[index], change))
                }
            }

            val preTokenBalances = tx.meta?.preTokenBalances ?: emptyList()
            val postTokenBalances = tx.meta?.postTokenBalances ?: emptyList()
            val tokenBalanceChange = ArrayList<Triple<String?, String, String>>(postTokenBalances.size)
            postTokenBalances.forEachIndexed { index, it ->
                val post = it.uiTokenAmount.uiAmountString.toDouble()
                val pre = preTokenBalances[index].uiTokenAmount.uiAmountString.toDouble()
                (post - pre).let { change ->
                    if (change != 0.0)
                        tokenBalanceChange.add(Triple(it.owner, it.mint, it.uiTokenAmount.uiAmountString))
                }
            }

            return Transaction(
                slot = tx.slot,
                blockTime = tx.blockTime,
                fee = fee,
                feePayer = accountKeys[0],
                balanceChange = balanceChange,
                tokenBalanceChange = tokenBalanceChange,
            )
        }
    }
}
