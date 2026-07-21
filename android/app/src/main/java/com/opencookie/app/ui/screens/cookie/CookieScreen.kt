package com.opencookie.app.ui.screens.cookie

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencookie.app.R
import com.opencookie.app.domain.model.TransactionState
import com.opencookie.app.ui.components.BreakCookieButton
import com.opencookie.app.ui.components.CookieMessageCard
import com.opencookie.app.ui.components.StatCounter

@Composable
fun CookieScreen(
    viewModel: CookieViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val status = when (uiState.transactionState) {
        TransactionState.Building -> stringResource(R.string.tx_status_building)
        TransactionState.AwaitingSignature -> stringResource(R.string.tx_status_wallet)
        is TransactionState.Confirming -> stringResource(R.string.tx_status_confirming)
        TransactionState.Retrying -> stringResource(R.string.tx_status_retrying)
        else -> if (!uiState.configLoaded) stringResource(R.string.chain_data_loading) else null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF21140F),
                            Color(0xFF120F0D),
                            Color(0xFF090807),
                        ),
                    ),
                ),
        ) {
            CookieCrumbField(Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Header()

                CookieHero(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(190.dp),
                )

                StatCounter(
                    callsToday = uiState.callsToday,
                    maxCallsPerDay = uiState.maxCallsPerDay,
                    totalCalls = uiState.totalCalls,
                )

                BreakCookieButton(
                    text = stringResource(R.string.break_cookie),
                    onClick = { viewModel.breakCookie() },
                    enabled = uiState.buttonEnabled,
                    modifier = Modifier.padding(top = 4.dp),
                )

                status?.let {
                    StatusPill(text = it)
                }

                uiState.cookieMessage?.let { message ->
                    CookieMessageCard(
                        message = message,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    TextButton(onClick = { viewModel.dismissMessage() }) {
                        Text(stringResource(R.string.dismiss_message))
                    }
                }

                uiState.error?.let { error ->
                    AssistChip(
                        onClick = { viewModel.dismissError() },
                        label = {
                            Text(
                                text = error,
                                textAlign = TextAlign.Center,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                            labelColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                    TextButton(onClick = { viewModel.dismissError() }) {
                        Text(stringResource(R.string.retry))
                    }
                }

                if (uiState.isOffline) {
                    StatusPill(
                        text = stringResource(R.string.offline_banner),
                        isError = true,
                    )
                }

                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "🥠",
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = stringResource(R.string.cookie_title),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    isError: Boolean = false,
) {
    val background = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    }
    val foreground = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = foreground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CookieHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.34f
            drawCircle(
                color = Color(0xFFFFA928).copy(alpha = 0.24f),
                radius = radius * 1.55f,
                center = center,
            )
            drawCircle(
                color = Color(0xFFFFB84D),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = Color(0xFF8F4B1F),
                radius = radius * 0.1f,
                center = center + Offset(-radius * 0.36f, -radius * 0.2f),
            )
            drawCircle(
                color = Color(0xFF8F4B1F),
                radius = radius * 0.09f,
                center = center + Offset(radius * 0.22f, -radius * 0.28f),
            )
            drawCircle(
                color = Color(0xFF8F4B1F),
                radius = radius * 0.08f,
                center = center + Offset(-radius * 0.02f, radius * 0.25f),
            )
            drawCircle(
                color = Color(0xFF120F0D),
                radius = radius * 0.3f,
                center = center + Offset(radius * 0.56f, -radius * 0.42f),
            )
        }
    }
}

@Composable
private fun CookieCrumbField(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val crumbs = listOf(
            Offset(size.width * 0.13f, size.height * 0.14f) to 3.5f,
            Offset(size.width * 0.84f, size.height * 0.18f) to 5f,
            Offset(size.width * 0.2f, size.height * 0.47f) to 4f,
            Offset(size.width * 0.78f, size.height * 0.58f) to 3f,
            Offset(size.width * 0.33f, size.height * 0.83f) to 5.5f,
        )
        crumbs.forEach { (offset, radius) ->
            drawCircle(
                color = Color(0xFFFFB347).copy(alpha = 0.22f),
                radius = radius,
                center = offset,
            )
        }
    }
}
