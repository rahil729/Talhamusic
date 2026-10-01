@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.talha.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.C
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private val AppBackground = Color(0xFF000000)
private val AppInk = Color(0xFFFFFFFF)
private val AppMuted = Color(0xFF8E8E8E)
private val AppAccent = Color(0xFFFFFFFF)
private val AppSoft = Color(0xFF1C1C1C)

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
    val searchHistory = repository.getSearchHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val player = playerConnection.player
    val playing = playerConnection.isPlaying
    val buffering = playerConnection.isBuffering
    val canGoNext = playerConnection.canGoNext
    val queue = playerConnection.queue
    val position = playerConnection.currentPosition
    val duration = playerConnection.duration
    val shuffle = playerConnection.shuffleEnabled
    val repeatMode = playerConnection.repeatMode
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
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()
    private val _downloads = MutableStateFlow<List<Song>>(emptyList())
    val downloads = _downloads.asStateFlow()
    
    private val _trending = MutableStateFlow<List<Song>>(emptyList())
    val trending = _trending.asStateFlow()
    
    private val _newReleases = MutableStateFlow<List<Song>>(emptyList())
    val newReleases = _newReleases.asStateFlow()
    
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { playerConnection.pollPosition() }
        refreshDownloads()
        loadHomeContent()
    }

    private fun loadHomeContent() {
        viewModelScope.launch {
            try {
                _trending.value = repository.searchSongs("Trending Music")
                _newReleases.value = repository.searchSongs("New Releases")
            } catch (e: Exception) {
                Log.e("MusicAppViewModel", "Failed to load home content", e)
            }
        }
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
            searchJob?.cancel()
            _searching.value = false
            _searchResults.value = emptyList()
            _artistResults.value = emptyList()
            _albumResults.value = emptyList()
            _playlistResults.value = emptyList()
            _searchError.value = null
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            _searching.value = true
            _searchError.value = null
            _searchResults.value = emptyList()
            _artistResults.value = emptyList()
            _albumResults.value = emptyList()
            _playlistResults.value = emptyList()
            try {
                _searchResults.value = repository.searchSongs(query)
                _artistResults.value = repository.searchArtists(query)
                _albumResults.value = repository.searchAlbums(query)
                _playlistResults.value = repository.searchPlaylists(query)
            } catch (exception: kotlinx.coroutines.CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e("MusicAppViewModel", "Search failed for $query", exception)
                val backendMessage = exception.message?.trim()
                _searchError.value = when {
                    backendMessage.isNullOrBlank() -> "Search failed. Check your connection and try again."
                    backendMessage.contains("404", ignoreCase = true) -> "Search backend is unavailable right now. Please try again later."
                    backendMessage.contains("502", ignoreCase = true) || backendMessage.contains("503", ignoreCase = true) -> "Search service is unavailable. Please try again in a moment."
                    backendMessage.length > 160 -> "Search failed. Check your connection and try again."
                    else -> backendMessage
                }
            } finally {
                _searching.value = false
            }
        }
    }

    fun submitSearch(query: String) {
        viewModelScope.launch { repository.saveSearch(query) }
        search(query)
    }

    fun play(song: Song, queue: List<Song> = listOf(song)) {
        viewModelScope.launch {
            try {
                val tracks = queue.takeIf { items -> items.any { it.id == song.id } } ?: listOf(song)
                val mediaItems = tracks.mapNotNull { track ->
                    repository.getPlaybackUri(track.id)?.let { mediaItem(track, it) }
                }
                val startIndex = mediaItems.indexOfFirst { it.mediaId == song.id }
                if (startIndex < 0) {
                    Log.e("MusicAppViewModel", "No playable item found for ${song.id}")
                    return@launch
                }
                val player = withTimeoutOrNull(10_000L) { playerConnection.awaitPlayer() }
                if (player == null) {
                    Log.e("MusicAppViewModel", "Playback service did not connect")
                    return@launch
                }

                player.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                player.prepare()
                player.play()

                repository.recordPlay(song)
            } catch (error: Throwable) {
                Log.e("MusicAppViewModel", "Playback tap failed for ${song.id}", error)
            }
        }
    }

    fun togglePlay() {
        player.value?.let { currentPlayer ->
            if (currentPlayer.isPlaying) {
                currentPlayer.pause()
            } else {
                if (currentPlayer.playbackState == Player.STATE_IDLE || currentPlayer.playbackState == Player.STATE_ENDED) {
                    currentPlayer.seekToDefaultPosition()
                    currentPlayer.prepare()
                }
                currentPlayer.play()
            }
        }
    }

    fun stop() = playerConnection.stop()
    fun next() { player.value?.seekToNext() }
    fun previous() {
        player.value?.let { currentPlayer ->
            when {
                currentPlayer.currentPosition > 3_000L -> currentPlayer.seekTo(0L)
                currentPlayer.previousMediaItemIndex != C.INDEX_UNSET -> currentPlayer.seekToPreviousMediaItem()
                else -> currentPlayer.seekTo(0L)
            }
        }
    }
    fun playQueueItem(index: Int) = playerConnection.playQueueItem(index)
    fun seek(position: Long) { playerConnection.seekTo(position) }
    fun toggleFavorite(song: Song) { viewModelScope.launch { repository.toggleFavorite(song) } }
    fun toggleFavoriteById(songId: String?) {
        songId?.let { id ->
            (songs.value + favorites.value + searchResults.value + downloads.value)
                .firstOrNull { it.id == id }
                ?.let(::toggleFavorite)
        }
    }
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
    fun download(song: Song) {
        viewModelScope.launch {
            repository.downloadSong(song)
            refreshDownloads()
        }
    }

    fun download(song: Song, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.downloadSong(song)
            _downloads.value = repository.getDownloadedSongs()
            onComplete()
        }
    }
    fun saveRemotePlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }
    fun saveCollection(kind: String, name: String, song: Song) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist("$kind: $name")
            repository.addSongToPlaylist(playlistId, song, 0)
        }
    }
    fun toggleShuffle() { playerConnection.toggleShuffle() }
    fun cycleRepeat() { playerConnection.cycleRepeatMode() }

    private fun mediaItem(song: Song, url: String): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setArtworkUri(song.thumbnailUrl?.let(android.net.Uri::parse)).build())
        if (url.startsWith("file:")) builder.setMimeType(MimeTypes.AUDIO_UNKNOWN)
        return builder.build()
    }
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
                    NavigationBar(containerColor = AppBackground, contentColor = AppMuted) {
                        for (item in AppSection.values()) {
                            NavigationBarItem(
                                selected = section == item,
                                onClick = { section = item },
                                icon = { SectionIcon(item) },
                                label = { Text(item.label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppAccent,
                                    selectedTextColor = AppAccent,
                                    unselectedIconColor = AppMuted,
                                    unselectedTextColor = AppMuted,
                                    indicatorColor = Color.Transparent
                                )
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
    val trending by viewModel.trending.collectAsState()
    val newReleases by viewModel.newReleases.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    
    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Explore", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppInk)
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppInk)
                    }
                }
                Spacer(Modifier.height(16.dp))
                CategoryStrip()
            }
        }

        if (recent.isNotEmpty()) {
            item { SectionTitle("Quick picks", "Based on your recent listening") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recent.take(10)) { song ->
                        ArtworkCard(song, { viewModel.play(song, recent) })
                    }
                }
            }
        }

        item { SectionTitle("Trending Now", "Popular right now") }
        item {
            if (trending.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppAccent)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(trending) { song ->
                        ArtworkCard(song, { viewModel.play(song, trending) })
                    }
                }
            }
        }

        item { SectionTitle("New Releases", "Just landed") }
        item {
            if (newReleases.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppAccent)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newReleases) { song ->
                        ArtworkCard(song, { viewModel.play(song, newReleases) })
                    }
                }
            }
        }

        if (favorites.isNotEmpty()) {
            item { SectionTitle("Your Favorites", "Your liked songs") }
            items(favorites.take(5)) { song ->
                Box(Modifier.padding(horizontal = 20.dp)) {
                    SongRow(song, { viewModel.play(song, favorites) }, { viewModel.toggleFavorite(song) }, viewModel)
                }
            }
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
    var submittedQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val searchHistory by viewModel.searchHistory.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val artistResults by viewModel.artistResults.collectAsState()
    val albumResults by viewModel.albumResults.collectAsState()
    val playlistResults by viewModel.playlistResults.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val matchingPlaylists = playlists.filter { it.name.contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Search", "Find a song, artist, or album")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; submittedQuery = ""; viewModel.search("") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                Row {
                    IconButton(
                        enabled = query.isNotBlank() && !searching,
                        onClick = { submittedQuery = query; viewModel.submitSearch(query) }
                    ) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; submittedQuery = ""; viewModel.search("") }) {
                            Icon(Icons.Default.Close, "Clear search")
                        }
                    }
                }
            },
            placeholder = { Text("What do you want to hear?") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submittedQuery = query; viewModel.submitSearch(query) })
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("All", "Songs", "Artists", "Albums", "Playlists")) { option ->
                FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
            }
        }
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp), color = AppAccent)
        LazyColumn(contentPadding = PaddingValues(16.dp, 20.dp, 16.dp, 110.dp)) {
            if (query.isBlank()) {
                if (searchHistory.isNotEmpty()) {
                    item { SectionTitle("Recent searches", "Pick up where you left off") }
                    items(searchHistory, key = { it.query }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = entry.query
                                    submittedQuery = entry.query
                                    viewModel.submitSearch(entry.query)
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, null, tint = AppMuted)
                            Text(entry.query, Modifier.padding(start = 12.dp), color = AppInk)
                        }
                    }
                } else {
                    item { EmptyState("Search the whole world of music", "Try an artist, mood, language, or song title.") }
                }
            }
            if (submittedQuery == query && !searching && searchError != null) item { EmptyState("Search unavailable", searchError!!) }
            else if (submittedQuery == query && !searching && query.isNotBlank() && results.isEmpty() && artistResults.isEmpty() && albumResults.isEmpty() && matchingPlaylists.isEmpty() && playlistResults.isEmpty()) item { EmptyState("No matches", "Try a different search.") }
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
    val buffering by viewModel.buffering.collectAsState()
    val shuffle by viewModel.shuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val canGoNext by viewModel.canGoNext.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val currentSong = player?.currentMediaItem
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Cover", "Up Next", "Lyrics")

    Surface(Modifier.fillMaxSize(), color = AppBackground) {
        Column(Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, "Close", tint = AppInk, modifier = Modifier.size(32.dp)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING FROM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppMuted, letterSpacing = 1.sp)
                    Text("Your Mix", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppInk)
                }
                IconButton(onClick = viewModel::stop, enabled = currentSong != null) {
                    Icon(Icons.Default.Stop, "Stop", tint = AppInk)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Content Area (Art or Lyrics or Queue)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (selectedTab) {
                    0 -> { // Album Art
                        AsyncImage(
                            model = currentSong?.mediaMetadata?.artworkUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(AppSoft),
                            contentScale = ContentScale.Crop
                        )
                    }
                    1 -> {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
                            itemsIndexed(queue, key = { _, item -> item.mediaId }) { index, item ->
                                val active = index == player?.currentMediaItemIndex
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { viewModel.playQueueItem(index) }
                                        .background(if (active) AppSoft else Color.Transparent)
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (active && playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (active) AppAccent else AppMuted
                                    )
                                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                        Text(item.mediaMetadata.title?.toString().orEmpty(), color = AppInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.mediaMetadata.artist?.toString().orEmpty(), color = AppMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        Text("Lyrics coming soon...", color = AppMuted, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Info & Controls
            Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            currentSong?.mediaMetadata?.title?.toString() ?: "Nothing playing",
                            color = AppInk,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            currentSong?.mediaMetadata?.artist?.toString() ?: "",
                            color = AppMuted,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFavoriteById(currentSong?.mediaId) }, enabled = currentSong != null) {
                        val liked = favorites.any { it.id == currentSong?.mediaId }
                        Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = if (liked) Color(0xFFC35D67) else AppInk, modifier = Modifier.size(28.dp))
                    }
                }

                if (buffering) {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        color = AppAccent
                    )
                    Text("Loading audio...", color = AppMuted, fontSize = 12.sp)
                }

                Spacer(Modifier.height(24.dp))

                Slider(
                    value = position.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                    onValueChange = { viewModel.seek(it.toLong()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = AppInk,
                        activeTrackColor = AppInk,
                        inactiveTrackColor = AppSoft
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(position), color = AppMuted, fontSize = 12.sp)
                    Text(formatTime(duration), color = AppMuted, fontSize = 12.sp)
                }

                Spacer(Modifier.height(32.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::toggleShuffle, enabled = currentSong != null) {
                        Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) AppAccent else AppMuted)
                    }
                    IconButton(onClick = viewModel::previous) { Icon(Icons.Default.SkipPrevious, "Previous", tint = AppInk, modifier = Modifier.size(42.dp)) }
                    
                    Surface(
                        onClick = viewModel::togglePlay,
                        shape = CircleShape,
                        color = AppInk,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Play/Pause",
                                tint = AppBackground,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(onClick = viewModel::next, enabled = canGoNext) {
                        Icon(Icons.Default.SkipNext, "Next", tint = if (canGoNext) AppInk else AppMuted, modifier = Modifier.size(42.dp))
                    }
                    IconButton(onClick = viewModel::cycleRepeat, enabled = currentSong != null) {
                        Icon(Icons.Default.Repeat, "Repeat", tint = if (repeatMode == Player.REPEAT_MODE_OFF) AppMuted else AppAccent)
                    }
                }

                Spacer(Modifier.height(40.dp))

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = AppInk,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AppInk,
                            height = 2.dp
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(player: Player?, onOpen: () -> Unit, onPlayPause: () -> Unit, modifier: Modifier = Modifier) {
    val item = player?.currentMediaItem ?: return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen),
        color = AppSoft,
        tonalElevation = 8.dp
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.mediaMetadata.artworkUri,
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    item.mediaMetadata.title?.toString().orEmpty(),
                    color = AppInk,
                    maxLines = 1,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.mediaMetadata.artist?.toString().orEmpty(),
                    color = AppMuted,
                    maxLines = 1,
                    fontSize = 13.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "Play",
                    tint = AppInk,
                    modifier = Modifier.size(28.dp)
                )
            }
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
                item { ActionRow(Icons.Default.Download, "Download for offline") { viewModel.download(song); onDismiss() } }
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
    Column(
        Modifier
            .width(160.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppSoft)
        )
        Text(
            song.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = AppInk,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            song.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = AppMuted,
            fontSize = 12.sp
        )
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryStrip() {
    val labels = listOf("All", "Songs", "Videos", "Artists", "Playlists")
    var selected by remember { mutableStateOf("All") }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(labels) { label ->
            FilterChip(
                selected = selected == label,
                onClick = { selected = label },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppAccent,
                    selectedLabelColor = AppBackground,
                    containerColor = AppSoft,
                    labelColor = AppInk
                ),
                border = null,
                shape = CircleShape
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AppInk)
        Text(subtitle, color = AppMuted, fontSize = 13.sp)
    }
}

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
    }
}

private fun formatTime(milliseconds: Long): String { val total = milliseconds / 1000; return "%d:%02d".format(total / 60, total % 60) }
