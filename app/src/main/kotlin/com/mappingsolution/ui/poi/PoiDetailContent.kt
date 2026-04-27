package com.mappingsolution.ui.poi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.mappingsolution.data.model.Group
import com.mappingsolution.data.model.MediaItem
import com.mappingsolution.data.model.MediaType
import com.mappingsolution.data.model.Poi
import com.mappingsolution.ui.common.IconCatalog
import java.io.File
import kotlin.random.Random

/**
 * Full-width swipeable image/media pager occupying 65% of screen height.
 */
@Composable
fun PoiMediaPager(
    mediaItems: List<MediaItem>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val galleryHeight = LocalConfiguration.current.screenHeightDp.dp * 0.65f
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(galleryHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = mediaItems[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onItemClick(page) },
            ) {
                when (item.type) {
                    MediaType.AUDIO -> {
                        Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                            val bars = 20
                            val barWidth = size.width / (bars * 2 - 1)
                            for (i in 0 until bars) {
                                val h = Random.nextFloat() * size.height
                                drawRect(
                                    color = Color.Gray,
                                    topLeft = androidx.compose.ui.geometry.Offset(i * barWidth * 2, (size.height - h) / 2),
                                    size = androidx.compose.ui.geometry.Size(barWidth, h),
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center).size(48.dp),
                        )
                    }
                    else -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(if (item.path.startsWith("http")) item.path else File(item.path))
                                .decoderFactory(VideoFrameDecoder.Factory())
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        if (item.type == MediaType.VIDEO) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .size(28.dp),
                            )
                        }
                    }
                }
            }
        }

        if (mediaItems.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(mediaItems.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Name / group / description info block shared across POI detail screens.
 */
@Composable
fun PoiInfoBlock(
    poi: Poi,
    group: Group?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = poi.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        group?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = IconCatalog.iconVector(it.iconKey),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!poi.description.isNullOrBlank()) {
            Text(
                text = poi.description,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
