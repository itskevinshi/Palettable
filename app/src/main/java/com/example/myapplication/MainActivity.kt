package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlin.random.Random


class MainActivity : ComponentActivity() {
    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PhotoPickerScreen()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun PhotoPickerScreen() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri
        }
    )

    val context = LocalContext.current // Needed for takePersistableUriPermission() later

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()), // Add this line
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            selectedImageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(model = uri),
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .padding(16.dp)
                )

                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

            }
            GetRandomColors(imageBitmap = selectedImageUri?.let { uriToImageBitmap(context, it) })
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        singlePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).padding(end = 16.dp) // Note the padding
                ) {
                    Text("Select From Gallery")
                }

                Button(
                    onClick = {
                        singlePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open Camera")
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

@Composable
fun GetRandomColors(imageBitmap: ImageBitmap?) {
    if (imageBitmap == null) return

    val androidBitmap = imageBitmap.asAndroidBitmap()
    val width = androidBitmap.width
    val height = androidBitmap.height

    // Key the remember on the imageBitmap
    var colors by remember(imageBitmap) {
        mutableStateOf(generateRandomColors(androidBitmap, width, height))
    }


    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        colors.forEach { color ->
            ColoredText(
                hexColor = color,
                text = color,
                boxWidth = 100.dp
            )
        }
    }
    Button(onClick = {
        colors = generateRandomColors(androidBitmap, width, height) // Update the state
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
    val color = Color(hexColor.removePrefix("#").toLong(16) or 0xFF000000L)
    Box(
        modifier = Modifier
            .width(boxWidth) // Set a fixed width for each box
            .padding(4.dp) // Reduce padding between boxes
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(8.dp), // Reduce padding within the box
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(color = Color.White, fontSize = 12.sp), // Smaller font size
            textAlign = TextAlign.Center, // Center the text
            modifier = Modifier.fillMaxWidth() // Make text fill the box width
        )
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