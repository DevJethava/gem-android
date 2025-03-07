package com.gemwallet.android.features.swap.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemwallet.android.features.asset_select.views.SelectSwapScreen
import com.gemwallet.android.features.confirm.views.ConfirmScreen
import com.gemwallet.android.features.swap.models.PriceImpactType
import com.gemwallet.android.features.swap.models.SwapError
import com.gemwallet.android.features.swap.models.SwapItemModel
import com.gemwallet.android.features.swap.models.SwapItemType
import com.gemwallet.android.features.swap.models.SwapPairSelect
import com.gemwallet.android.features.swap.models.SwapProviderItem
import com.gemwallet.android.features.swap.models.SwapState
import com.gemwallet.android.features.swap.viewmodels.SwapViewModel
import com.gemwallet.android.model.ConfirmParams
import com.gemwallet.android.ui.R
import com.gemwallet.android.ui.components.ListItem
import com.gemwallet.android.ui.components.designsystem.Spacer16
import com.gemwallet.android.ui.components.designsystem.Spacer2
import com.gemwallet.android.ui.components.designsystem.Spacer4
import com.gemwallet.android.ui.components.designsystem.Spacer8
import com.gemwallet.android.ui.components.designsystem.padding16
import com.gemwallet.android.ui.components.designsystem.trailingIcon20
import com.gemwallet.android.ui.components.image.AsyncImage
import com.gemwallet.android.ui.components.list_item.ListItemSupportText
import com.gemwallet.android.ui.components.list_item.ListItemTitleText
import com.gemwallet.android.ui.components.screen.Scene
import com.gemwallet.android.ui.theme.pendingColor
import com.wallet.core.primitives.Asset
import com.wallet.core.primitives.AssetId
import com.wallet.core.primitives.AssetType
import com.wallet.core.primitives.Chain

@Composable
fun SwapScreen(
    viewModel: SwapViewModel = hiltViewModel(),
    onConfirm: (ConfirmParams) -> Unit,
    onCancel: () -> Unit,
) {
    val selectState by viewModel.selectPair.collectAsStateWithLifecycle()
    val pairState by viewModel.swapPairUIModel.collectAsStateWithLifecycle()
    val fromEquivalent by viewModel.fromEquivalentFormatted.collectAsStateWithLifecycle()
    val toEquivalent by viewModel.toEquivalentFormatted.collectAsStateWithLifecycle()
    val swapState by viewModel.swapScreenState.collectAsStateWithLifecycle()
    val currentProvider by viewModel.currentProvider.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val priceImpact by viewModel.priceImpact.collectAsStateWithLifecycle()

    val isShowProviderSelect = remember { mutableStateOf(false) }
    var approveParams by rememberSaveable { mutableStateOf<ConfirmParams?>(null) }
    val pair = pairState
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(selectState != null) {
        if (approveParams != null) {
            approveParams = null
            return@BackHandler
        }

        val fromId = pair?.from?.asset?.id
        val toId = pair?.to?.asset?.id
        if (fromId == null && toId == null) {
            onCancel()
        } else {
            viewModel.onSelect(SwapPairSelect.request(fromId, toId))
        }
    }

    if (pair != null) {
        Scene(
            title = stringResource(id = R.string.wallet_swap),
            mainAction = {
                SwapAction(swapState, pair) {
                    viewModel.swap(
                        when (swapState) {
                            SwapState.Ready -> onConfirm
                            SwapState.RequestApprove -> { { approveParams = it } }
                            else -> { {} }
                        }
                    )
                }
            },
            onClose = onCancel,
        ) {
            SwapItem(
                type = SwapItemType.Pay,
                item = pair.from,
                equivalent = fromEquivalent,
                state = viewModel.fromValue,
                onAssetSelect = {
                    keyboardController?.hide()
                    viewModel.changePair(it)
                }
            )
            IconButton(onClick = viewModel::switchSwap) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "swap_switch"
                )
            }
            SwapItem(
                type = SwapItemType.Receive,
                item = pair.to,
                equivalent = toEquivalent,
                state = viewModel.toValue,
                calculating = swapState == SwapState.GetQuote,
                onAssetSelect = {
                    keyboardController?.hide()
                    viewModel.changePair(it)
                }
            )
            currentProvider?.let { provider ->
                CurrentSwapProvider(provider, providers.size > 1, isShowProviderSelect)
            }
            priceImpact?.let {
                ListItem(
                    modifier = Modifier.height(72.dp),
                    title = {
                        ListItemTitleText(stringResource(R.string.swap_price_impact))
                    },
                    trailing = {
                        ListItemTitleText(
                            it.percentageFormatted,
                            color = when (it.type) {
                                PriceImpactType.Positive,
                                PriceImpactType.Low -> MaterialTheme.colorScheme.tertiary
                                PriceImpactType.Medium -> pendingColor
                                PriceImpactType.High -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                )
            }
            Spacer16()
            SwapError(swapState)
        }
    }

    AnimatedVisibility(
        visible = approveParams != null,
        enter = slideIn { IntOffset(it.width, 0) },
        exit = slideOut { IntOffset(it.width, 0) },
    ) {
        LocalSoftwareKeyboardController.current?.hide()
        ConfirmScreen(
            approveParams ?: return@AnimatedVisibility,
            finishAction = { assetId, hash, route ->
                approveParams = null
                viewModel.onTxHash(hash)
            },
            cancelAction = {
                approveParams = null
            },
        )
    }

    AnimatedVisibility(
        visible = selectState != null,
        enter = slideIn { IntOffset(it.width, 0) },
        exit = slideOut { IntOffset(it.width, 0) },
    ) {
        SelectSwapScreen(
            select = selectState ?: return@AnimatedVisibility,
            onCancel = {
                val fromId = pair?.from?.asset?.id
                val toId = pair?.to?.asset?.id
                if (fromId == null || toId == null) {
                    onCancel()
                } else {
                    viewModel.onSelect(SwapPairSelect.request(fromId, toId))
                }
            },
            onSelect = {select -> viewModel.onSelect(select) },
        )
    }
    ProviderList(isShowProviderSelect, currentProvider?.swapProvider?.id, providers, viewModel::setProvider)
}

@Composable
private fun CurrentSwapProvider(
    provider: SwapProviderItem,
    isAvailableChoose: Boolean,
    isShowProviderSelect: MutableState<Boolean>,
) {
    Spacer16()
    val modifier = if (isAvailableChoose) {
        Modifier.clickable { isShowProviderSelect.value = true }
    } else {
        Modifier
    }
    ListItem(
        modifier = Modifier
            .height(72.dp)
            .then(modifier),
        title = {
            ListItemTitleText(stringResource(R.string.swap_provider))
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListItemSupportText(provider.swapProvider.name)
                Spacer4()
                AsyncImage(provider.icon, size = trailingIcon20)
                if (isAvailableChoose) {
                    Icon(Icons.Default.ChevronRight, "")
                }
            }
        }
    )
}

@Composable
private fun SwapError(state: SwapState) {
    if (state !is SwapState.Error || state.error == SwapError.None) {
        return
    }
    Column(
        modifier = Modifier
            .padding(padding16)
            .background(
                MaterialTheme.colorScheme.errorContainer.copy(0.2f),
                shape = MaterialTheme.shapes.medium
            )
            .fillMaxWidth()
            .padding(padding16),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(trailingIcon20),
                imageVector = Icons.Outlined.Warning,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = ""
            )
            Spacer8()
            Text(
                text = stringResource(R.string.errors_error_occured),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W400),
            )
        }
        Spacer2()
        Text(
            text = when (state.error) {
                SwapError.None -> ""
                SwapError.IncorrectInput -> stringResource(R.string.common_required_field, stringResource(R.string.swap_you_pay))
                SwapError.NoQuote -> stringResource(R.string.errors_swap_no_quote_available)
                SwapError.NotSupportedAsset -> stringResource(R.string.errors_swap_not_supported_asset)
                SwapError.NotSupportedChain -> stringResource(R.string.errors_swap_not_supported_chain)
                SwapError.NotImplemented,
                SwapError.NotSupportedPair -> stringResource(R.string.errors_swap_not_supported_pair)
                SwapError.NetworkError -> "Node not available. Check internet connection."
                is SwapError.Unknown -> "${stringResource(R.string.errors_unknown_try_again)}: ${state.error.message}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
fun PreviewSwapItem() {
    MaterialTheme {
        SwapItem(
            type = SwapItemType.Pay,
            item = SwapItemModel(
                Asset(
                    id = AssetId(Chain.Ethereum),
                    symbol = "ETH",
                    name = "Ethereum",
                    type = AssetType.NATIVE,
                    decimals = 18,
                ),
                assetBalanceValue = "10.0",
                assetBalanceLabel = "10.0 ETH",
            ),
            equivalent = "0.0$",
            calculating = true,
            onAssetSelect = {},
        )
    }
}