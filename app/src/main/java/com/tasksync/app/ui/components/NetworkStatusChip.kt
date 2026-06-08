package com.tasksync.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun NetworkStatusChip(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    var previousOnline by remember { mutableStateOf<Boolean?>(null) }
    var showOnlineChip by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        if (previousOnline == null) {
            previousOnline = isOnline
            return@LaunchedEffect
        }
        if (!isOnline) {
            showOnlineChip = false
        } else if (previousOnline == false) {
            showOnlineChip = true
            delay(3000)
            showOnlineChip = false
        }
        previousOnline = isOnline
    }

    AnimatedVisibility(
        visible = !isOnline,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, bottom = 8.dp)
    ) {
        AssistChip(
            onClick = {},
            label = { Text("Offline", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        )
    }

    AnimatedVisibility(
        visible = showOnlineChip,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, bottom = 8.dp)
    ) {
        AssistChip(
            onClick = {},
            label = { Text("Online", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}