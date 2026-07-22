package com.opencookie.app.data.wallet

import android.util.Base64
import android.util.Log
import com.opencookie.app.BuildConfig
import com.opencookie.app.data.rpc.RpcException
import com.opencookie.app.data.rpc.SimulateTransactionValue
import com.opencookie.app.data.rpc.SolanaRpcClient
import com.opencookie.app.domain.model.AppError
import com.opencookie.app.util.PublicKey

/**
 * RPC preflight simulation and MWA sign diagnostics (Nudge-style, Open Cookie program codes).
 */
internal object WalletFailureDiagnostics {
    private const val TAG = "WalletTxDiagnostics"
    private val anchorCustomCode = Regex(""""Custom"\s*:\s*(\d+)""")
    private val anchorCustomParen = Regex("""Custom\s*\(\s*(\d+)\s*\)""")

    private val programErrorNames = mapOf(
        6001 to "DailyLimitReached",
        6002 to "Unauthorized",
        6003 to "InvalidPda",
    )

    fun logSignAndSendAttempt(
        txBytes: ByteArray,
        feePayerBase58: String,
        mwaChain: String,
        rpcEndpoint: String,
    ) {
        Log.d(
            TAG,
            "signTransaction request | chain=$mwaChain | rpc=$rpcEndpoint | feePayer=$feePayerBase58 | txBytes=${txBytes.size}",
        )
    }

    fun logSignAndSendSuccess(signatureBase58: String) {
        Log.i(TAG, "sendTransaction success | signature=$signatureBase58")
    }

    fun logSignAndSendFailure(walletMessage: String?, error: Throwable?, txBytes: ByteArray? = null) {
        Log.e(TAG, "signTransaction failure | walletMessage=${walletMessage ?: "(null)"} | error=${error?.message}")
        if (txBytes != null) {
            Log.e(TAG, "signTransaction failure | txBytes=${txBytes.size}")
        }
    }

    /**
     * Runs RPC simulation before opening the wallet to catch stale blockhash / program errors early.
     */
    suspend fun preflightSimulation(
        rpcClient: SolanaRpcClient,
        txBytes: ByteArray,
        feePayer: PublicKey,
    ): Result<Unit> {
        val txBase64 = Base64.encodeToString(txBytes, Base64.NO_WRAP)
        Log.d(TAG, "preflight simulateTransaction | rpc=${rpcClient.rpcEndpoint} | txBytes=${txBytes.size}")
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "preflight | feePayer=${feePayer.toBase58()}")
        }
        return rpcClient.simulateTransactionDiagnostics(txBase64).fold(
            onSuccess = { diag ->
                logPreflightResult(diag.value)
                mapSimulationValueToResult(diag.value)
            },
            onFailure = { error ->
                val appError = when (error) {
                    is AppError -> error
                    is RpcException -> {
                        if (error.message?.contains("BlockhashNotFound") == true) {
                            AppError.BlockhashExpired
                        } else {
                            AppError.RpcError(error.code, error.message ?: "RPC error")
                        }
                    }
                    else -> SolanaRpcClient.mapToAppError(error)
                }
                Log.e(TAG, "preflight simulateTransaction | failed | ${appError.userMessage}")
                Result.failure(appError)
            },
        )
    }

    private fun logPreflightResult(value: SimulateTransactionValue) {
        value.err?.let { err ->
            Log.e(TAG, "preflight simulateTransaction | err=$err")
        } ?: Log.i(TAG, "preflight simulateTransaction | err=null (simulation ok)")
        value.unitsConsumed?.let { Log.d(TAG, "preflight simulateTransaction | unitsConsumed=$it") }
        value.logs?.forEachIndexed { index, line ->
            Log.e(TAG, "preflight simulation log[$index] | $line")
        }
    }

    private fun mapSimulationValueToResult(value: SimulateTransactionValue): Result<Unit> {
        val err = value.err ?: return Result.success(Unit)
        val errText = err.toString()
        if (errText.contains("BlockhashNotFound", ignoreCase = true)) {
            return Result.failure(AppError.BlockhashExpired)
        }
        val logText = collectText(errText, value.logs?.joinToString("\n"))
        extractAnchorCodes(logText).firstOrNull()?.let { code ->
            val name = programErrorNames[code] ?: "Custom($code)"
            return Result.failure(AppError.ProgramError(code, name))
        }
        if (errText.contains("ConstraintMut", ignoreCase = true)) {
            return Result.failure(AppError.ProgramError(2006, "ConstraintMut"))
        }
        return Result.failure(AppError.TransactionSimulationFailed)
    }

    private fun collectText(vararg parts: String?): String =
        parts.filterNot { it.isNullOrBlank() }.joinToString("\n")

    private fun extractAnchorCodes(text: String): Set<Int> {
        val codes = linkedSetOf<Int>()
        anchorCustomCode.findAll(text).forEach { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { codes.add(it) }
        }
        anchorCustomParen.findAll(text).forEach { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { codes.add(it) }
        }
        return codes
    }
}
