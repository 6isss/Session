package yos.music.player.ui.widgets.basic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.navigationBarsHeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.code.utils.others.Vibrator
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.artistsName
import yos.music.player.data.libraries.defaultArtistsName
import yos.music.player.data.libraries.defaultTitle

@Stable
data class NavItem(val label: String, val iconResId: Int)

@Composable
fun BottomNavigator(
    nowLabel: () -> String,
    onLabelChange: (String) -> Unit,
    items: List<NavItem>,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val results = remember(query, MusicLibrary.songs) {
        if (query.isBlank()) emptyList() else MusicLibrary.songs.filter { song ->
            song.title.orEmpty().contains(query, true) || song.artistsName.orEmpty().contains(query, true)
        }.take(4)
    }

    Box(modifier.fillMaxWidth().navigationBarsHeight(72.dp)) {
        if (searchExpanded && results.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(bottom = 64.dp, top = 8.dp)
            ) {
                results.forEach { song ->
                    Column(
                        Modifier.fillMaxWidth().clickable {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(song, MusicLibrary.songs)
                            }
                            query = ""
                            searchExpanded = false
                        }.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(song.title ?: defaultTitle, maxLines = 1, fontWeight = FontWeight.Medium)
                        Text(
                            song.artistsName ?: defaultArtistsName,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedContent(
                targetState = searchExpanded,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "bottom-search"
            ) { expanded ->
                if (expanded) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(26.dp))
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search music", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().height(52.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), RoundedCornerShape(26.dp)),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        items.forEach { item -> NavigatorItem(item, nowLabel, onLabelChange) }
                    }
                }
            }
            Box(
                Modifier.size(52.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f), CircleShape)
                    .clickable {
                        Vibrator.click(context)
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) query = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(if (searchExpanded) R.drawable.ic_action_cancel else R.drawable.ic_uitabbar_search),
                    contentDescription = if (searchExpanded) "Close search" else "Search",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavigatorItem(
    item: NavItem,
    nowLabel: () -> String,
    onLabelChange: (String) -> Unit
) {
    val context = LocalContext.current
    val isSelected = remember(item) { derivedStateOf { nowLabel() == item.label } }
    val color = animateColorAsState(if (isSelected.value) MaterialTheme.colorScheme.primary else Color.Gray)
    val indicatorWidth = animateDpAsState(if (isSelected.value) 54.dp else 0.dp)
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight().weight(1f).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            Vibrator.click(context)
            onLabelChange(item.label)
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(indicatorWidth.value, 28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape))
            Icon(painterResource(item.iconResId), item.label, tint = color.value, modifier = Modifier.size(23.dp))
        }
        Text(item.label, color = color.value, fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Medium)
    }
}