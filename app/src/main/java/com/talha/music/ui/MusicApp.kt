@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.talha.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.talha.music.data.model.Playlist
import com.talha.music.data.model.Song
import com.talha.music.data.repository.MusicRepository
import com.talha.music.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import javax.inject.Inject

private val AppBackground = Color(0xFFF7F5F2)
private val AppInk = Color(0xFF1D1D21)
private val AppMuted = Color(0xFF77747C)
private val AppAccent = Color(0xFF2D6A5A)
private val AppSoft = Color(0xFFE4EEE8)

private enum class AppSection(val label: String) {
    DISCOVER("Discover"), SONGS("Songs"), SEARCH("Search"), LIBRARY("Library")
}

@HiltViewModel
class MusicAppViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerConnection: PlayerConnection
) : ViewModel() {
    val songs = repository.getAllSongs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repository.getFavoriteSongs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recent = repository.getRecentlyPlayed().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repository.getPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val player = playerConnection.player
    val playing = playerConnection.isPlaying
    val position = playerConnection.currentPosition
    val duration = playerConnection.duration
    val shuffle = playerConnection.shuffleEnabled
    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults = _searchResults.asStateFlow()
    private val _artistResults = MutableStateFlow<List<Song>>(emptyList())
    val artistResults = _artistResults.asStateFlow()
    private val _albumResults = MutableStateFlow<List<Song>>(emptyList())
    val albumResults = _albumResults.asStateFlow()
    private val _playlistResults = MutableStateFlow<List<com.talha.music.data.remote.InnerTubeService.CollectionResult>>(emptyList())
    val playlistResults = _playlistResults.asStateFlow()
    private val _searching = MutableStateFlow(false)
    val searching = _searching.asStateFlow()
    private val _downloads = MutableStateFlow<List<Song>>(emptyList())
    val downloads = _downloads.asStateFlow()

    init {
        viewModelScope.launch { playerConnection.pollPosition() }
        refreshDownloads()
    }

    fun refreshDownloads() {
        viewModelScope.launch { _downloads.value = repository.getDownloadedSongs() }
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            repository.deleteDownload(song.id)
            refreshDownloads()
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _artistResults.value = emptyList()
            _albumResults.value = emptyList()
            _playlistResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searching.value = true
            _searchResults.value = repository.searchSongs(query)
            _artistResults.value = repository.searchArtists(query)
            _albumResults.value = repository.searchAlbums(query)
            _playlistResults.value = repository.searchPlaylists(query)
            _searching.value = false
        }
    }

    fun play(song: Song, queue: List<Song> = listOf(song)) {
        viewModelScope.launch {
            val url = repository.getPlaybackUri(song.id) ?: return@launch
            val item = mediaItem(song, url)
            val player = playerConnection.player.value ?: return@launch
            player.setMediaItem(item)
            player.prepare()
            player.play()
            repository.recordPlay(song)
            launch { repository.downloadSong(song) }
            val queued = queue.dropWhile { it.id != song.id }.drop(1).mapNotNull { next ->
                repository.getPlaybackUri(next.id)?.let { mediaItem(next, it) }
            }
            if (queued.isNotEmpty()) player.addMediaItems(queued)
        }
    }

    fun togglePlay() { player.value?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { player.value?.seekToNext() }
    fun previous() { player.value?.seekToPrevious() }
    fun seek(position: Long) { playerConnection.seekTo(position) }
    fun toggleFavorite(song: Song) { viewModelScope.launch { repository.toggleFavorite(song) } }
    fun playNext(song: Song) {
        viewModelScope.launch {
            val url = repository.getPlaybackUri(song.id) ?: return@launch
            val item = mediaItem(song, url)
            playerConnection.player.value?.let { player ->
                val index = (player.currentMediaItemIndex + 1).coerceAtLeast(0)
                player.addMediaItem(index, item)
            }
        }
    }
    fun enqueue(song: Song) {
        viewModelScope.launch {
            val url = repository.getPlaybackUri(song.id) ?: return@launch
            playerConnection.player.value?.addMediaItem(mediaItem(song, url))
        }
    }
    fun startRadio(song: Song) {
        viewModelScope.launch {
            val radio = repository.searchSongs("${song.artist} radio")
            if (radio.isNotEmpty()) play(song, radio)
        }
    }
    fun saveToPlaylist(playlist: Playlist, song: Song) {
        viewModelScope.launch {
            val position = repository.getPlaylistSongs(playlist.id).size
            repository.addSongToPlaylist(playlist.id, song, position)
        }
    }
    fun download(song: Song) { viewModelScope.launch { repository.downloadSong(song) } }
    fun saveRemotePlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }
    fun saveCollection(kind: String, name: String, song: Song) {
        viewModelScope.launch {
            repository.createPlaylist("$kind: $name")
            repository.toggleFavorite(song)
        }
    }
    fun toggleShuffle() { playerConnection.toggleShuffle() }
    fun cycleRepeat() { playerConnection.cycleRepeatMode() }

    private fun mediaItem(song: Song, url: String) = MediaItem.Builder()
        .setMediaId(song.id)
        .setUri(url)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setArtworkUri(song.thumbnailUrl?.let(android.net.Uri::parse)).build())
        .build()
}

@Composable
fun MusicApp(viewModel: MusicAppViewModel = hiltViewModel()) {
    var section by remember { mutableStateOf(AppSection.DISCOVER) }
    var showPlayer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val currentPlayer by viewModel.player.collectAsState()
    val currentItem = currentPlayer?.currentMediaItem

    Surface(color = AppBackground) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxWidth < 600.dp
            val content: @Composable () -> Unit = {
                Box(Modifier.fillMaxSize()) {
                    when (section) {
                        AppSection.DISCOVER -> DiscoverScreen(viewModel, onSettingsClick = { showSettings = true })
                        AppSection.SONGS -> SongsScreen(viewModel)
                        AppSection.SEARCH -> SearchScreen(viewModel)
                        AppSection.LIBRARY -> LibraryScreen(viewModel)
                    }
                    currentItem?.let {
                        MiniPlayer(
                            player = currentPlayer,
                            onOpen = { showPlayer = true },
                            onPlayPause = viewModel::togglePlay,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            if (compact) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                    NavigationBar(containerColor = AppBackground) {
                        for (item in AppSection.values()) {
                            NavigationBarItem(
                                selected = section == item,
                                onClick = { section = item },
                                icon = { SectionIcon(item) },
                                label = { Text(item.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = AppBackground,
                        header = {
                            IconButton(onClick = { section = AppSection.DISCOVER }, modifier = Modifier.padding(top = 12.dp)) {
                                Icon(Icons.Default.Explore, contentDescription = "Discover", tint = AppAccent)
                            }
                        }
                    ) {
                        for (item in AppSection.values()) {
                            NavigationRailItem(
                                selected = section == item,
                                onClick = { section = item },
                                icon = { SectionIcon(item) },
                                label = { Text(item.label, fontSize = 11.sp) }
                            )
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) { content() }
                }
            }
        }
    }
    if (showPlayer && currentItem != null) {
        FullPlayer(viewModel = viewModel, onClose = { showPlayer = false })
    }
    if (showSettings) {
        SettingsOverlay(onClose = { showSettings = false })
    }
}

@Composable
private fun SectionIcon(item: AppSection) {
    Icon(
        when (item) {
            AppSection.DISCOVER -> Icons.Default.Explore
            AppSection.SONGS -> Icons.Default.QueueMusic
            AppSection.SEARCH -> Icons.Default.Search
            AppSection.LIBRARY -> Icons.Default.LibraryMusic
        },
        contentDescription = item.label
    )
}

@Composable
private fun DiscoverScreen(viewModel: MusicAppViewModel, onSettingsClick: () -> Unit) {
    val songs by viewModel.songs.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val source = (recent + songs).distinctBy(Song::id)
    LazyColumn(contentPadding = PaddingValues(28.dp, 28.dp, 28.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Good music,\nno noise.", fontSize = 42.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, color = AppInk)
                    Text("A quiet place for your next favorite song.", color = AppMuted, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppMuted)
                }
            }
        }
        item { CategoryStrip() }
        item { SectionTitle("Quick picks", "Fresh from your library") }
        item {
            if (source.isEmpty()) EmptyState("Your listening story starts here", "Search for a song to fill this space.")
            else LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(source.take(8)) { song -> ArtworkCard(song, { viewModel.play(song, source) }) }
            }
        }
        item { SectionTitle("Recently played", "Pick up where you left off") }
        items(recent.take(8)) { song -> SongRow(song, { viewModel.play(song, recent) }, { viewModel.toggleFavorite(song) }, viewModel) }
        if (playlists.isNotEmpty()) {
            item { SectionTitle("Your playlists", "Made by you") }
            item { PlaylistStrip(playlists) }
        }
    }
}

@Composable
private fun SongsScreen(viewModel: MusicAppViewModel) {
    val songs by viewModel.songs.collectAsState()
    var query by remember { mutableStateOf("") }
    val visible = songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Songs", "${visible.size} tracks in your library")
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 24.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Filter songs") })
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 110.dp)) {
            if (visible.isEmpty()) item { EmptyState("No songs yet", "Search and play music to build your library.") }
            items(visible, key = Song::id) { song -> SongRow(song, { viewModel.play(song, visible) }, { viewModel.toggleFavorite(song) }, viewModel) }
        }
    }
}

@Composable
private fun SearchScreen(viewModel: MusicAppViewModel) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val results by viewModel.searchResults.collectAsState()
    val artistResults by viewModel.artistResults.collectAsState()
    val albumResults by viewModel.albumResults.collectAsState()
    val playlistResults by viewModel.playlistResults.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val matchingPlaylists = playlists.filter { it.name.contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Search", "Find a song, artist, or album")
        OutlinedTextField(query, { query = it; viewModel.search(it) }, Modifier.fillMaxWidth().padding(horizontal = 24.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("What do you want to hear?") })
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("All", "Songs", "Artists", "Albums", "Playlists")) { option ->
                FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
            }
        }
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp), color = AppAccent)
        LazyColumn(contentPadding = PaddingValues(16.dp, 20.dp, 16.dp, 110.dp)) {
            if (query.isBlank()) item { EmptyState("Search the whole world of music", "Try an artist, mood, language, or song title.") }
            if (!searching && query.isNotBlank() && results.isEmpty() && artistResults.isEmpty() && albumResults.isEmpty() && matchingPlaylists.isEmpty() && playlistResults.isEmpty()) item { EmptyState("No matches", "Try a different search.") }
            if (filter == "All" || filter == "Artists") {
                if (artistResults.isNotEmpty()) {
                item { SectionTitle("Artists", "Matching artists") }
                item {
                    CollectionStrip(artistResults.distinctBy(Song::artist).mapNotNull { song -> song.artist.takeIf(String::isNotBlank)?.let { it to song } }, onSave = { name, song -> viewModel.saveCollection("Artist", name, song) })
                }
                }
            }
            if (filter == "All" || filter == "Albums") {
                if (albumResults.isNotEmpty()) {
                item { SectionTitle("Albums", "Matching albums") }
                item {
                    CollectionStrip(albumResults.distinctBy { it.album }.mapNotNull { song -> song.album?.takeIf(String::isNotBlank)?.let { it to song } }, onSave = { name, song -> viewModel.saveCollection("Album", name, song) })
                }
                }
            }
            if (filter == "All" || filter == "Playlists") {
                item { SectionTitle("Playlists", "Matching playlists") }
                if (matchingPlaylists.isEmpty() && playlistResults.isEmpty()) item { EmptyState("No matching playlists", "Try another search.") }
                items(matchingPlaylists) { playlist -> PlaylistTile(playlist) }
                items(playlistResults) { playlist -> RemotePlaylistTile(playlist, onSave = { viewModel.saveRemotePlaylist(playlist.title) }) }
            }
            if (filter == "All" || filter == "Songs") {
                if (results.isNotEmpty()) item { SectionTitle("Songs", "Matching songs") }
                items(results, key = Song::id) { song -> SongRow(song, { viewModel.play(song, results) }, { viewModel.toggleFavorite(song) }, viewModel) }
            }
        }
    }
}

@Composable
private fun LibraryScreen(viewModel: MusicAppViewModel) {
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val artists = favorites.groupBy { it.artist }.mapNotNull { (name, entries) -> entries.firstOrNull()?.let { name to it } }
    val albums = favorites.filter { !it.album.isNullOrBlank() }.groupBy { it.album.orEmpty() }.mapNotNull { (name, entries) -> entries.firstOrNull()?.let { name to it } }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Library", "Everything you keep close")
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { LibraryTile("Liked songs", "${favorites.size} favorites", Icons.Default.Favorite, AppAccent) }
            item { LibraryTile("Recently played", "${recent.size} songs", Icons.Default.History, Color(0xFFB56B47)) }
            item { LibraryTile("Downloads", "${downloads.size} available offline", Icons.Default.Download, Color(0xFF6C72A8)) }
            item { SectionTitle("Offline songs", "Automatically saved when played") }
            if (downloads.isEmpty()) item { EmptyState("No offline songs yet", "Play a song and it will be downloaded in the background.") }
            items(downloads, key = Song::id) { song ->
                SongRow(song, { viewModel.play(song, downloads) }, { viewModel.toggleFavorite(song) }, viewModel)
            }
            item { SectionTitle("Saved artists", "Artists from your favorite songs") }
            item {
                if (artists.isEmpty()) EmptyState("No artists yet", "Artists appear as songs enter your library.")
                else CollectionStrip(artists)
            }
            item { SectionTitle("Saved albums", "Albums from your favorite songs") }
            item {
                if (albums.isEmpty()) EmptyState("No albums yet", "Albums appear when songs include album information.")
                else CollectionStrip(albums)
            }
            item { SectionTitle("Playlists", "Your collections") }
            if (playlists.isEmpty()) item { EmptyState("No playlists yet", "Create your first collection from a song.") }
            items(playlists) { playlist -> PlaylistTile(playlist) }
            item { SectionTitle("Favorites", "Songs you saved") }
            items(favorites, key = Song::id) { song -> SongRow(song, { viewModel.play(song, favorites) }, { viewModel.toggleFavorite(song) }, viewModel) }
        }
    }
}

@Composable
private fun FullPlayer(viewModel: MusicAppViewModel, onClose: () -> Unit) {
    val player by viewModel.player.collectAsState()
    val playing by viewModel.playing.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val item = player?.currentMediaItem
    Surface(Modifier.fillMaxSize(), color = Color(0xFF17211E)) {
        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Close", tint = Color.White) }
                Text("NOW PLAYING", color = Color(0xFF9FCFBB), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
                IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "More", tint = Color.White) }
            }
            Spacer(Modifier.height(42.dp))
            AsyncImage(model = item?.mediaMetadata?.artworkUri, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxWidth().padding(top = 28.dp)) {
                Text(item?.mediaMetadata?.title?.toString() ?: "Nothing playing", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item?.mediaMetadata?.artist?.toString() ?: "", color = Color(0xFFA9B8B2), fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(22.dp))
            LinearProgressIndicator(progress = { if (duration > 0) position.toFloat() / duration else 0f }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF9FCFBB), trackColor = Color(0xFF40534B))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatTime(position), color = Color(0xFFA9B8B2)); Text(formatTime(duration), color = Color(0xFFA9B8B2)) }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::toggleShuffle) { Icon(Icons.Default.Shuffle, "Shuffle", tint = Color(0xFFA9B8B2)) }
                IconButton(onClick = viewModel::previous) { Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(38.dp)) }
                FilledIconButton(onClick = viewModel::togglePlay, modifier = Modifier.size(76.dp), shape = CircleShape) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", modifier = Modifier.size(42.dp)) }
                IconButton(onClick = viewModel::next) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(38.dp)) }
                IconButton(onClick = viewModel::cycleRepeat) { Icon(Icons.Default.History, "Repeat", tint = Color(0xFFA9B8B2)) }
            }
        }
    }
}

@Composable
private fun MiniPlayer(player: Player?, onOpen: () -> Unit, onPlayPause: () -> Unit, modifier: Modifier = Modifier) {
    val item = player?.currentMediaItem ?: return
    Card(modifier.fillMaxWidth().padding(12.dp).clickable(onClick = onOpen), colors = CardDefaults.cardColors(containerColor = Color(0xFF20332D)), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = item.mediaMetadata.artworkUri, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(item.mediaMetadata.title?.toString().orEmpty(), color = Color.White, maxLines = 1); Text(item.mediaMetadata.artist?.toString().orEmpty(), color = Color(0xFFA9B8B2), maxLines = 1, fontSize = 12.sp) }
            IconButton(onClick = onPlayPause) { Icon(if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White) }
        }
    }
}

@Composable
@androidx.compose.foundation.ExperimentalFoundationApi
private fun SongRow(song: Song, onClick: () -> Unit, onFavorite: () -> Unit, viewModel: MusicAppViewModel? = null) {
    var showActions by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { showActions = viewModel != null }).padding(vertical = 7.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = song.thumbnailUrl, contentDescription = null, modifier = Modifier.size(66.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = AppInk, fontSize = 16.sp); Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AppMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp)) }
        Text(song.durationText.orEmpty(), color = AppMuted, fontSize = 13.sp)
        IconButton(onClick = onFavorite) { Icon(if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = if (song.isFavorite) Color(0xFFC35D67) else AppMuted) }
    }
    if (showActions && viewModel != null) {
        SongActionsDialog(song, viewModel, onDismiss = { showActions = false })
    }
}

@Composable
private fun SongActionsDialog(song: Song, viewModel: MusicAppViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFFF3E5E4), modifier = Modifier.fillMaxWidth()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 14.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = song.thumbnailUrl, contentDescription = null, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(song.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = AppMuted, fontSize = 15.sp)
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(song) }) { Icon(if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = Color(0xFFB64B55)) }
                    }
                    Divider(color = Color(0xFFD9C4C3))
                }
                item { ActionRow(Icons.Default.SkipNext, "Play next") { viewModel.playNext(song); onDismiss() } }
                item { ActionRow(Icons.Default.QueueMusic, "Enqueue") { viewModel.enqueue(song); onDismiss() } }
                item { ActionRow(Icons.Default.Radio, "Start radio") { viewModel.startRadio(song); onDismiss() } }
                item { ActionRow(Icons.Default.PlaylistAdd, "Add to playlist") { } }
                if (playlists.isEmpty()) {
                    item { Text("No playlists yet. Create one from Library.", color = AppMuted, modifier = Modifier.padding(start = 70.dp, top = 2.dp, bottom = 8.dp)) }
                } else {
                    items(playlists) { playlist ->
                        TextButton(onClick = { viewModel.saveToPlaylist(playlist, song); onDismiss() }, modifier = Modifier.fillMaxWidth().padding(start = 56.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                            Text(playlist.name, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                        }
                    }
                }
                item { ActionRow(Icons.Default.Album, "Go to album") { viewModel.search(song.album ?: song.title); onDismiss() } }
                item { ActionRow(Icons.Default.Person, "More from ${song.artist}") { viewModel.search(song.artist); onDismiss() } }
                item { ActionRow(Icons.Default.OpenInNew, "Watch on YouTube") { openExternal(context, "https://www.youtube.com/watch?v=${song.id}"); onDismiss() } }
                item { ActionRow(Icons.Default.OpenInNew, "Open in YouTube Music") { openExternal(context, "https://music.youtube.com/watch?v=${song.id}"); onDismiss() } }
                item { ActionRow(Icons.Default.Download, "Pre-cache") { viewModel.download(song); onDismiss() } }
                item { ActionRow(Icons.Default.Block, "Add to blacklist") { onDismiss() } }
                item { ActionRow(Icons.Default.Share, "Share") { shareSong(context, song); onDismiss() } }
            }
        }
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(25.dp), tint = AppInk)
        Text(label, fontSize = 18.sp, color = AppInk, modifier = Modifier.padding(start = 28.dp))
    }
}

private fun openExternal(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun shareSong(context: android.content.Context, song: Song) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}\nhttps://music.youtube.com/watch?v=${song.id}")
    }, "Share song"))
}

@Composable
private fun ArtworkCard(song: Song, onClick: () -> Unit) {
    Column(Modifier.width(148.dp).clickable(onClick = onClick)) { AsyncImage(model = song.thumbnailUrl, contentDescription = null, modifier = Modifier.size(148.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop); Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)); Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AppMuted, fontSize = 13.sp) }
}

@Composable
private fun CollectionStrip(collections: List<Pair<String, Song>>, onSave: ((String, Song) -> Unit)? = null) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(collections) { (label, song) ->
            Column(Modifier.width(132.dp)) {
                Box {
                    AsyncImage(model = song.thumbnailUrl, contentDescription = null, modifier = Modifier.size(132.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                    onSave?.let { save ->
                        IconButton(onClick = { save(label, song) }, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Save $label", tint = Color.White)
                        }
                    }
                }
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AppMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun RemotePlaylistTile(
    playlist: com.talha.music.data.remote.InnerTubeService.CollectionResult,
    onSave: () -> Unit
) {
    Card(Modifier.width(210.dp), colors = CardDefaults.cardColors(containerColor = AppSoft), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = playlist.thumbnailUrl, contentDescription = null, modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(playlist.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AppMuted, fontSize = 12.sp)
            }
            IconButton(onClick = onSave) { Icon(Icons.Default.FavoriteBorder, contentDescription = "Save playlist", tint = AppAccent) }
        }
    }
}

@Composable
private fun PlaylistStrip(playlists: List<Playlist>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (playlist in playlists) {
            item { PlaylistTile(playlist, Modifier.width(180.dp)) }
        }
    }
}

@Composable
private fun PlaylistTile(playlist: Playlist, modifier: Modifier = Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = AppSoft), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.QueueMusic, null, tint = AppAccent, modifier = Modifier.size(32.dp)); Text(playlist.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) } } }

@Composable
private fun LibraryTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) { Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp)); Column(Modifier.padding(start = 14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = AppMuted, fontSize = 13.sp) } } } }

@Composable
private fun CategoryStrip() {
    val labels = listOf("Fresh", "Chill", "Focus", "Workout", "Urdu", "Hindi")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(labels) { label ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(50.dp)) {
                Text(label, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = AppAccent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) { Column { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppInk); Text(subtitle, color = AppMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp)) } }

@Composable
private fun ScreenHeader(title: String, subtitle: String) { Column(Modifier.padding(28.dp, 28.dp, 24.dp, 14.dp)) { Text(title, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = AppInk); Text(subtitle, color = AppMuted, modifier = Modifier.padding(top = 5.dp)) } }

@Composable
private fun EmptyState(title: String, message: String) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(22.dp)) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(message, color = AppMuted, modifier = Modifier.padding(top = 6.dp)) } } }

@Composable
private fun SettingsOverlay(onClose: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = AppBackground) {
        Column(Modifier.fillMaxSize().padding(28.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close settings") }
                Text("Settings", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AppInk, modifier = Modifier.padding(start = 8.dp))
            }
            Text("Make the listening space yours.", color = AppMuted, modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 24.dp))
            SettingRow("Playback", "Queue, speed, repeat, and shuffle")
            SettingRow("Appearance", "Use the system light or dark theme")
            SettingRow("Downloads", "Manage music saved for offline listening")
            SettingRow("Connection", "Local stream resolver status")
            Spacer(Modifier.height(18.dp))
            EmptyState("Settings are ready", "The playback controls are connected. More preferences can be added here without changing the player.")
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AppInk)
            Text(subtitle, fontSize = 13.sp, color = AppMuted, modifier = Modifier.padding(top = 3.dp))
        }
        Icon(Icons.Default.MoreVert, contentDescription = "Open $title", tint = AppMuted)
    }
}

private fun formatTime(milliseconds: Long): String { val total = milliseconds / 1000; return "%d:%02d".format(total / 60, total % 60) }
