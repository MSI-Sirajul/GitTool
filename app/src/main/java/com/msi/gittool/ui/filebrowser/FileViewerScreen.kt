package com.msi.gittool.ui.filebrowser

import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    viewModel: FileViewerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var viewRawMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadFile(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.path.substringAfterLast("/"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.shareFileContent(context) },
                        modifier = Modifier.testTag("share_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Link")
                    }
                    IconButton(
                        onClick = {
                            val clipManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipData = android.content.ClipData.newPlainText("Raw Code", uiState.decodedContent)
                            clipManager.setPrimaryClip(clipData)
                            Toast.makeText(context, "Copied code to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_code_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Raw Code")
                    }
                    IconButton(
                        onClick = { viewRawMode = !viewRawMode },
                        modifier = Modifier.testTag("toggle_raw_button")
                    ) {
                        Icon(
                            imageVector = if (viewRawMode) Icons.Default.Code else Icons.Default.Description,
                            contentDescription = if (viewRawMode) "Syntax Highlighted View" else "Raw Selection View"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.downloadFile(context) },
                        modifier = Modifier.testTag("download_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Download File")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.errorMessage.isNullOrEmpty()) {
                if (uiState.isImage) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.downloadImageUsingDownloadManager(context) },
                        icon = { Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Download Image") },
                        text = { Text("Download Image") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("download_image_fab")
                    )
                } else if (!uiState.isVideo) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val clipManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipData = android.content.ClipData.newPlainText("Raw Code", uiState.decodedContent)
                            clipManager.setPrimaryClip(clipData)
                            Toast.makeText(context, "Copied entire raw code content to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        icon = { Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Raw Code") },
                        text = { Text("Copy Code") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("copy_code_fab")
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (!uiState.errorMessage.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                if (uiState.isImage) {
                    // Premium interactive pinch-to-zoom image viewer
                    var scale by remember { mutableStateOf(1f) }
                    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                        offset += offsetChange
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = if (scale > 1f) 1f else 2.5f
                                        offset = androidx.compose.ui.geometry.Offset.Zero
                                    }
                                )
                            }
                            .clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uiState.contentItem?.download_url,
                            contentDescription = "Preview of ${uiState.path}",
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .transformable(state = state)
                                .fillMaxSize()
                        )
                    }
                } else if (uiState.isVideo) {
                    // Premium native Android video rendering with seek controller
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        val videoUrl = uiState.contentItem?.download_url
                        if (!videoUrl.isNullOrEmpty()) {
                            AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        setVideoURI(android.net.Uri.parse(videoUrl))
                                        val mediaController = android.widget.MediaController(ctx)
                                        mediaController.setAnchorView(this)
                                        setMediaController(mediaController)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            )
                        } else {
                            Text("No video link is accessible.", color = Color.White)
                        }
                    }
                } else {
                    if (viewRawMode) {
                        SelectionContainer {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0D1117))
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = uiState.decodedContent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFC9D1D9)
                                )
                            }
                        }
                    } else {
                        // Premium WebView Syntax highlighting mode (using Prism.js with CDNs, Autoloader & Line Numbers)
                        val rawCode = uiState.decodedContent
                        val escaped = rawCode.escapeHtml()
                        
                        val extension = uiState.path.substringAfterLast(".", "").lowercase()
                        val prismLang = when (extension) {
                            "kt", "kts" -> "kotlin"
                            "java" -> "java"
                            "rs" -> "rust"
                            "py" -> "python"
                            "js" -> "javascript"
                            "ts" -> "typescript"
                            "swift" -> "swift"
                            "cpp", "hpp", "cc", "cxx" -> "cpp"
                            "c", "h" -> "clike"
                            "cs" -> "csharp"
                            "go" -> "go"
                            "rb" -> "ruby"
                            "sh", "bash" -> "bash"
                            "json" -> "json"
                            "xml", "html", "xhtml" -> "markup"
                            "css" -> "css"
                            "md" -> "markdown"
                            "yaml", "yml" -> "yaml"
                            "sql" -> "sql"
                            else -> "none"
                        }

                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                              <meta name="viewport" content="width=device-width, initial-scale=1.0">
                              <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css">
                              <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.css">
                              <style>
                                html, body {
                                  margin: 0;
                                  padding: 0;
                                  background-color: #0d1117 !important;
                                  color: #c9d1d9;
                                }
                                pre[class*="language-"] {
                                  margin: 0 !important;
                                  padding: 16px 16px 16px 52px !important;
                                  background: #0d1117 !important;
                                  border: none !important;
                                  box-sizing: border-box !important;
                                  overflow-x: auto !important;
                                }
                                code[class*="language-"] {
                                  font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas, Liberation Mono, monospace !important;
                                  font-size: 13px !important;
                                  line-height: 1.5 !important;
                                }
                                .line-numbers .line-numbers-rows {
                                  border-right: 1px solid #21262d !important;
                                  left: 0 !important;
                                  padding: 16px 0 !important;
                                  background-color: #0d1117 !important;
                                }
                                .line-numbers-rows > span:before {
                                  color: #484f58 !important;
                                }
                              </style>
                              <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
                              <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.js"></script>
                              <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/autoloader/prism-autoloader.min.js"></script>
                              <script>
                                Prism.plugins.autoloader.languages_path = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/';
                              </script>
                            </head>
                            <body class="line-numbers">
                              <pre class="line-numbers language-$prismLang"><code class="language-$prismLang">$escaped</code></pre>
                            </body>
                            </html>
                        """.trimIndent()

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

private fun String.escapeHtml(): String {
    return this.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#039;")
}
