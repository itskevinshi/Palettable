package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.FAVORITE_PALETTES_KEY
import com.example.myapplication.utils.THEME_KEY
import com.example.myapplication.utils.dataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random


class MainActivity : androidx.activity.ComponentActivity() {
    @SuppressLint("NewApi")
    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            setContent {
                var isDarkTheme by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    dataStore.data.map { preferences ->
                        preferences[THEME_KEY] ?: false
                    }.collect { theme ->
                        isDarkTheme = theme
                    }
                }

                MyApplicationTheme(darkTheme = isDarkTheme) {
                    PhotoPickerScreen()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        cameraPermission.launch(android.Manifest.permission.CAMERA)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun PhotoPickerScreen() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var expandedFavorites by remember { mutableStateOf(false) }
    var expandedHistory by remember { mutableStateOf(false) }
    var paletteHistory by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var currentPalette by remember { mutableStateOf<List<String>>(emptyList()) }
    var favoritePalettes by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var isDarkTheme by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        loadFavoritePalettes(context) { loadedPalettes ->
            favoritePalettes = loadedPalettes
        }
    }

    LaunchedEffect(Unit) {
        context.dataStore.data.map { preferences ->
            preferences[THEME_KEY] ?: false
        }.collect { isDarkTheme = it }
    }

    // Function to handle new image selection and generate palette
    val handleNewImage: (Uri) -> Unit = { uri ->
        selectedImageUri = uri
        // Clear history when new image is loaded
        paletteHistory = emptyList()
        currentPalette = emptyList()
        // Generate new palette immediately for the new image
        val newImageBitmap = uriToImageBitmap(context, uri)
        newImageBitmap?.let { bitmap ->
            val newPalette = generateRandomColors(
                bitmap.asAndroidBitmap(),
                bitmap.width,
                bitmap.height
            )
            currentPalette = newPalette
            paletteHistory = listOf(newPalette)
        }
    }

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                handleNewImage(it)
            }
        }
    )

    val toggleFavorite: (List<String>) -> Unit = { palette: List<String> ->
        favoritePalettes = if (favoritePalettes.contains(palette)) {
            favoritePalettes - listOf(palette)
        } else {
            favoritePalettes + listOf(palette)
        }

        scope.launch {
            saveFavoritePalettes(context, favoritePalettes)
        }
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))

                    // Favorites Section
                    NavigationDrawerItem(
                        label = { Text("Favorites") },
                        selected = expandedFavorites,
                        onClick = { expandedFavorites = !expandedFavorites }
                    )
                    if (expandedFavorites) {
                        Column(Modifier.padding(start = 16.dp)) {
                            favoritePalettes.forEach { palette ->
                                NavigationDrawerItem(
                                    label = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            palette.forEach { colorHex ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.White)
                                                        .padding(1.dp)
                                                        .clip(RoundedCornerShape(11.dp))
                                                        .background(Color(colorHex.removePrefix("#").toLong(16) or 0xFF000000L))
                                                )
                                            }
                                        }
                                    },
                                    selected = false,
                                    onClick = { currentPalette = palette }
                                )
                            }
                        }
                    }

                    // History Section
                    NavigationDrawerItem(
                        label = { Text("History") },
                        selected = expandedHistory,
                        onClick = { expandedHistory = !expandedHistory }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (expandedHistory) {
                        Column(
                            Modifier.padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            paletteHistory.asReversed().forEachIndexed { index, palette ->
                                NavigationDrawerItem(
                                    label = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            palette.forEach { colorHex ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.White)
                                                        .padding(1.dp)
                                                        .clip(RoundedCornerShape(11.dp))
                                                        .background(Color(colorHex.removePrefix("#").toLong(16) or 0xFF000000L))
                                                )
                                            }
                                        }
                                    },
                                    selected = palette == currentPalette,
                                    onClick = { currentPalette = palette }
                                )
                            }
                        }
                    }

                    NavigationDrawerItem(
                        label = { Text(if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme") },
                        selected = isDarkTheme,
                        onClick = {
                            isDarkTheme = !isDarkTheme
                            scope.launch {
                                context.dataStore.edit { preferences ->
                                    preferences[THEME_KEY] = isDarkTheme
                                }
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Palettable",
                            style = TextStyle(fontSize = 20.sp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            if (showCamera) {
                if (hasPermission) {
                    CameraPreview(
                        onDismiss = { showCamera = false },
                        onImageCaptured = { uri ->
                            showCamera = false
                            handleNewImage(uri)
                        }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    selectedImageUri?.let { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(model = uri),
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 9999.dp)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    GetRandomColors(
                        imageBitmap = selectedImageUri?.let {
                            uriToImageBitmap(context, it)
                        },
                        onNewPalette = { newPalette ->
                            paletteHistory = (paletteHistory + listOf(newPalette)).takeLast(5)
                            currentPalette = newPalette
                        },
                        currentPalette = currentPalette,
                        favoritePalettes = favoritePalettes,
                        onToggleFavorite = toggleFavorite
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                singlePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                        ) {
                            Text("Upload Image")
                        }
                        Button(
                            onClick = { showCamera = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Camera")
                        }
                    }
                }
            }
        }
    }
}

fun uriToImageBitmap(context: Context, imageUri: Uri): ImageBitmap? {
    val inputStream = context.contentResolver.openInputStream(imageUri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    return bitmap?.asImageBitmap()
}

suspend fun saveFavoritePalettes(context: Context, palettes: List<List<String>>) {
    val gson = Gson()
    val jsonString = gson.toJson(palettes)

    context.dataStore.edit { preferences ->
        preferences[FAVORITE_PALETTES_KEY] = jsonString
    }
}

suspend fun loadFavoritePalettes(context: Context, onLoaded: (List<List<String>>) -> Unit) {
    val gson = Gson()
    context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[FAVORITE_PALETTES_KEY] ?: "[]"
            gson.fromJson(jsonString, Array<Array<String>>::class.java).map { it.toList() }
        }
        .collect { palettes ->
            onLoaded(palettes)
        }
}

@Composable
fun PaletteRow(palette: List<String>, isFavorited: Boolean, onToggleFavorite: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Favorite",
            modifier = Modifier
                .padding(start = 12.dp)
                .size(24.dp)
                .clickable { onToggleFavorite() },
            tint = if (isFavorited) Color.Red else Color.Gray
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            palette.forEach { colorHex ->
                ColoredText(hexColor = colorHex, text = colorHex, boxWidth = 100.dp)
            }
        }
    }
}

@Composable
fun GetRandomColors(
    imageBitmap: ImageBitmap?,
    onNewPalette: (List<String>) -> Unit,
    currentPalette: List<String>,
    favoritePalettes: List<List<String>>,
    onToggleFavorite: (List<String>) -> Unit
) {
    if (imageBitmap == null) return

    val androidBitmap = imageBitmap.asAndroidBitmap()
    val width = androidBitmap.width
    val height = androidBitmap.height

    var colors by remember(imageBitmap) {
        mutableStateOf(currentPalette.ifEmpty { generateRandomColors(androidBitmap, width, height) })
    }

    PaletteRow(
        palette = colors,
        isFavorited = favoritePalettes.contains(colors),
        onToggleFavorite = { onToggleFavorite(colors) }
    )

    Button(onClick = {
        colors = generateRandomColors(androidBitmap, width, height)
        onNewPalette(colors)
    }) {
        Text("Generate New Palette")
    }
}



private fun generateRandomColors(bitmap: android.graphics.Bitmap, width: Int, height: Int): List<String> {
    val randomPixels = List(5) {
        Pair(Random.nextInt(width), Random.nextInt(height))
    }
    return randomPixels.map { (x, y) ->
        val pixel = bitmap.getPixel(x, y)
        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        String.format("#%02X%02X%02X", red, green, blue)
    }
}


@Composable
fun ColoredText(hexColor: String, text: String, boxWidth: Dp) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    val color = Color(hexColor.removePrefix("#").toLong(16) or 0xFF000000L)
    // Calculate luminance
    val luminance = (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue)
    // Choose white or black text based on background brightness
    val textColor = if (luminance > 0.5f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .width(boxWidth)
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(8.dp)
            .clickable {
                val clip = android.content.ClipData.newPlainText("Color Code", hexColor)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(context, "Color code copied!", Toast.LENGTH_SHORT).show()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(color = textColor, fontSize = 12.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
fun CameraPreview(onDismiss: () -> Unit, onImageCaptured: (Uri) -> Unit) {
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            bindToLifecycle(lifecycleOwner)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (capturedImageUri == null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_START
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        controller = cameraController
                    }
                },
                onRelease = { cameraController.unbind() }
            )
        } else {
            AsyncImage(
                model = capturedImageUri,
                contentDescription = "Captured photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (capturedImageUri == null) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text("Close Camera")
                }
                Button(
                    onClick = {
                        val photoFile = File(
                            context.cacheDir,
                            "photo_${System.currentTimeMillis()}.jpg"
                        )
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        cameraController.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val savedUri = Uri.fromFile(photoFile)
                                    capturedImageUri = savedUri
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    // error handling whatever
                                }
                            }
                        )
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text("Take Photo")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        capturedImageUri?.let { onImageCaptured(it) }
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text("Accept")
                }
                Button(
                    onClick = {
                        capturedImageUri = null
                        cameraController.unbind()
                        cameraController.bindToLifecycle(lifecycleOwner)
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text("Retake")
                }
            }
        }
    }
}



@SuppressLint("NewApi")
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        PhotoPickerScreen()
    }
}