package com.nmorrione.divemeter.ui.videocalc

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nmorrione.divemeter.R
import kotlinx.coroutines.delay
import java.util.Locale

/** Manual per-tap nudge when the exact frame boundary matters more than smooth scrubbing. */
private const val FRAME_STEP_MS = 33L

fun formatTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000
    return String.format(Locale.US, "%d:%02d.%03d", minutes, seconds, millis)
}

@Composable
fun VideoTimelinePlayer(
    videoUri: Uri,
    apexMs: Long?,
    entryMs: Long?,
    onApexMark: (Long) -> Unit,
    onEntryMark: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0)
            if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
            delay(50)
        }
    }

    Column(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f)
        )

        Slider(
            value = currentPositionMs.toFloat(),
            onValueChange = {
                exoPlayer.pause()
                exoPlayer.seekTo(it.toLong())
            },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "${formatTimestamp(currentPositionMs)} / ${formatTimestamp(durationMs)}",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            IconButton(onClick = {
                exoPlayer.pause()
                exoPlayer.seekTo((currentPositionMs - FRAME_STEP_MS).coerceAtLeast(0))
            }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.video_calc_step_back))
            }
            IconButton(onClick = {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.video_calc_play_pause)
                )
            }
            IconButton(onClick = {
                exoPlayer.pause()
                exoPlayer.seekTo((currentPositionMs + FRAME_STEP_MS).coerceAtMost(durationMs))
            }) {
                Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.video_calc_step_forward))
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            OutlinedButton(
                onClick = { onApexMark(currentPositionMs) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Flag, contentDescription = null)
                Text(
                    text = apexMs?.let { stringResource(R.string.video_calc_apex_marked, formatTimestamp(it)) }
                        ?: stringResource(R.string.video_calc_mark_apex),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            OutlinedButton(
                onClick = { onEntryMark(currentPositionMs) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Waves, contentDescription = null)
                Text(
                    text = entryMs?.let { stringResource(R.string.video_calc_entry_marked, formatTimestamp(it)) }
                        ?: stringResource(R.string.video_calc_mark_entry),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
