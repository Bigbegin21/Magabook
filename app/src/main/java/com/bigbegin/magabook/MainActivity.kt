package com.bigbegin.magabook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MagabookContextHolder.context = applicationContext

        setContent {
            MagabookApp()
        }
    }
}

object MagabookContextHolder {
    lateinit var context: Context

    fun open(uri: Uri): ParcelFileDescriptor {
        return context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open PDF")
    }
}

@Composable
fun MagabookApp() {

    val background = Color(0xFF10131C)
    val surface = Color(0xFF1D2330)
    val accent = Color(0xFF8B7CFF)
    val white = Color(0xFFF5F5F5)

    var renderer by remember {
        mutableStateOf<PdfRenderer?>(null)
    }

    var descriptor by remember {
        mutableStateOf<ParcelFileDescriptor?>(null)
    }

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var pageNumber by remember {
        mutableIntStateOf(0)
    }

    var pageCount by remember {
        mutableIntStateOf(0)
    }

    var fileName by remember {
        mutableStateOf("No magazine selected")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    fun renderPage(index: Int) {

        val pdf = renderer ?: return

        if (index !in 0 until pdf.pageCount) return

        try {

            val page = pdf.openPage(index)

            val newBitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )

            try {
                page.render(
                    newBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
            } finally {
                page.close()
            }

            bitmap?.recycle()
            bitmap = newBitmap

            pageNumber = index
            errorMessage = ""

        } catch (e: Exception) {
            errorMessage = "Unable to display this page."
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri != null) {

            try {

                renderer?.close()
                renderer = null

                descriptor?.close()
                descriptor = null

                bitmap?.recycle()
                bitmap = null

                val newDescriptor =
                    MagabookContextHolder.open(uri)

                val newRenderer =
                    PdfRenderer(newDescriptor)

                descriptor = newDescriptor
                renderer = newRenderer

                pageCount = newRenderer.pageCount
                pageNumber = 0

                fileName =
                    uri.lastPathSegment ?: "Selected magazine"

                errorMessage = ""

                if (pageCount > 0) {
                    renderPage(0)
                }

            } catch (e: Exception) {

                errorMessage = "Could not open this PDF."

            }
        }
    }

    DisposableEffect(Unit) {

        onDispose {

            renderer?.close()
            descriptor?.close()
            bitmap?.recycle()

        }
    }

    MaterialTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            Text(
                text = "MAGABOOK",
                color = accent,
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Your digital reading space",
                color = Color.LightGray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "My Library",
                color = white,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = {
                    pdfPicker.launch(
                        arrayOf("application/pdf")
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("＋ Import PDF")
            }

            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = fileName,
                color = white,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(15.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 350.dp)
                    .background(
                        surface,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {

                val currentBitmap = bitmap

                if (currentBitmap != null) {

                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "PDF page",
                        modifier = Modifier.fillMaxWidth()
                    )

                } else {

                    Text(
                        text = "Your magazine pages will appear here.",
                        color = Color.LightGray
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (pageCount > 0) {

                Text(
                    text = "Page ${pageNumber + 1} of $pageCount",
                    color = white,
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Button(
                        onClick = {
                            renderPage(pageNumber - 1)
                        },
                        enabled = pageNumber > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = surface
                        )
                    ) {
                        Text("Previous")
                    }

                    Button(
                        onClick = {
                            renderPage(pageNumber + 1)
                        },
                        enabled = pageNumber < pageCount - 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent
                        )
                    ) {
                        Text("Next")
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Read. Explore. Discover.",
                color = accent,
                fontSize = 14.sp,
                modifier = Modifier.align(
                    Alignment.CenterHorizontally
                )
            )
        }
    }
}
