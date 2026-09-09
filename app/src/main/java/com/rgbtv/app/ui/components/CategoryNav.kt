package com.rgbtv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rgbtv.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Vertical category rail for tablets, desktops and TV. */
@Composable
fun CategoryRail(
    groups: List<String>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxHeight().width(224.dp).padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.nav_browse),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 10.dp)
        )
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                CategoryRow(
                    label = stringResource(R.string.group_all),
                    selected = selectedGroup == null,
                    icon = Icons.Rounded.GridView,
                    onClick = { onSelectGroup(null) },
                    index = 0,
                    listState = listState,
                    scope = scope
                )
            }
            itemsIndexed(groups) { index, group ->
                CategoryRow(
                    label = group,
                    selected = selectedGroup == group,
                    icon = Icons.Rounded.Tv,
                    onClick = { onSelectGroup(group) },
                    index = index + 1,
                    listState = listState,
                    scope = scope
                )
            }
        }
    }
}

/** Horizontal category chips for phones and narrow windows. */
@Composable
fun CategoryChips(
    groups: List<String>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        item {
            CategoryChip(
                label = stringResource(R.string.group_all),
                selected = selectedGroup == null,
                onClick = { onSelectGroup(null) },
                index = 0,
                listState = listState,
                scope = scope
            )
        }
        itemsIndexed(groups) { index, group ->
            CategoryChip(
                label = group,
                selected = selectedGroup == group,
                onClick = { onSelectGroup(group) },
                index = index + 1,
                listState = listState,
                scope = scope
            )
        }
    }
}

@Composable
private fun CategoryRow(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    index: Int,
    listState: LazyListState,
    scope: CoroutineScope
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    val background = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        focused -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(shape)
            .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, shape) else Modifier)
            .background(background)
            // onFocusChanged must sit outside the clickable node it observes.
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) scope.launch { listState.animateScrollToItem(index) }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    listState: LazyListState,
    scope: CoroutineScope
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        shape = shape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier
            .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, shape) else Modifier)
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) scope.launch { listState.animateScrollToItem(index) }
            }
    )
}
