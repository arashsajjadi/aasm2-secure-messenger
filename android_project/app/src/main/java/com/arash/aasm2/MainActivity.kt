package com.arash.aasm2

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.arash.aasm2.crypto.Aasm2Crypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Aasm2Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Aasm2App()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Aasm2App() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var inputText by rememberSaveable { mutableStateOf("") }
    var outputText by rememberSaveable { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by rememberSaveable { mutableStateOf("No file selected") }
    var pendingSaveBytes by remember { mutableStateOf<ByteArray?>(null) }
    var statusText by rememberSaveable { mutableStateOf("Ready") }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = queryDisplayName(context, uri) ?: "selected_file"
            statusText = "File selected."
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = pendingSaveBytes
        if (uri != null && bytes != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error("Could not open output destination.")
                    }
                }.onSuccess {
                    statusText = "Output saved."
                    snackbarHostState.showSnackbar("File saved successfully")
                }.onFailure {
                    statusText = it.message ?: "Save failed."
                    snackbarHostState.showSnackbar(statusText)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AASM2 Secure Messenger") },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Compatible AASM2 encryption for Android, Windows, and Linux",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Text") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Files") })
            }

            if (selectedTab == 0) {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            label = { Text("Input") }
                        )
                        OutlinedTextField(
                            value = outputText,
                            onValueChange = { outputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            label = { Text("Output") }
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(onClick = {
                                scope.launch {
                                    runCatching {
                                        require(password.isNotBlank()) { "Please enter a password." }
                                        withContext(Dispatchers.Default) {
                                            Aasm2Crypto.encryptTextToBase64(inputText, password)
                                        }
                                    }.onSuccess {
                                        outputText = it
                                        statusText = "Text encrypted."
                                    }.onFailure {
                                        statusText = it.message ?: "Encryption failed."
                                        snackbarHostState.showSnackbar(statusText)
                                    }
                                }
                            }) {
                                Text("Encrypt Text")
                            }
                            Button(onClick = {
                                scope.launch {
                                    runCatching {
                                        require(password.isNotBlank()) { "Please enter a password." }
                                        withContext(Dispatchers.Default) {
                                            Aasm2Crypto.decryptTextFromBase64(inputText.trim(), password)
                                        }
                                    }.onSuccess {
                                        outputText = it
                                        statusText = "Text decrypted."
                                    }.onFailure {
                                        statusText = it.message ?: "Decryption failed."
                                        snackbarHostState.showSnackbar(statusText)
                                    }
                                }
                            }) {
                                Text("Decrypt Text")
                            }
                            Button(onClick = {
                                clipboard.setText(AnnotatedString(outputText))
                                statusText = "Output copied."
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Copy Output")
                            }
                            TextButton(onClick = {
                                inputText = outputText
                                statusText = "Output moved to input."
                            }) {
                                Text("Load Output To Input")
                            }
                            TextButton(onClick = {
                                inputText = ""
                                outputText = ""
                                statusText = "Text fields cleared."
                            }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            } else {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = selectedFileName, style = MaterialTheme.typography.bodyLarge)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(onClick = {
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            }) {
                                Text("Choose File")
                            }
                            Button(onClick = {
                                scope.launch {
                                    runCatching {
                                        require(password.isNotBlank()) { "Please enter a password." }
                                        val uri = selectedFileUri ?: error("Please choose a file first.")
                                        val sourceBytes = readUriBytes(context, uri)
                                        val outputBytes = withContext(Dispatchers.Default) {
                                            Aasm2Crypto.encryptBytes(
                                                sourceBytes,
                                                password,
                                                mapOf("type" to "file", "name" to selectedFileName)
                                            )
                                        }
                                        pendingSaveBytes = outputBytes
                                        "${selectedFileName}.aasm2"
                                    }.onSuccess {
                                        statusText = "Choose where to save the encrypted file."
                                        createDocumentLauncher.launch(it)
                                    }.onFailure {
                                        statusText = it.message ?: "Encryption failed."
                                        snackbarHostState.showSnackbar(statusText)
                                    }
                                }
                            }) {
                                Text("Encrypt Selected File")
                            }
                            Button(onClick = {
                                scope.launch {
                                    runCatching {
                                        require(password.isNotBlank()) { "Please enter a password." }
                                        val uri = selectedFileUri ?: error("Please choose an encrypted file first.")
                                        val sourceBytes = readUriBytes(context, uri)
                                        val result = withContext(Dispatchers.Default) {
                                            Aasm2Crypto.decryptBytes(sourceBytes, password)
                                        }
                                        pendingSaveBytes = result.plaintext
                                        result.metadata["name"] ?: "decrypted_output.bin"
                                    }.onSuccess {
                                        statusText = "Choose where to save the decrypted file."
                                        createDocumentLauncher.launch(it)
                                    }.onFailure {
                                        statusText = it.message ?: "Decryption failed."
                                        snackbarHostState.showSnackbar(statusText)
                                    }
                                }
                            }) {
                                Text("Decrypt Selected File")
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun readUriBytes(context: Context, uri: Uri): ByteArray {
    return withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open the selected file.")
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex)
        }
    }
    return DocumentFile.fromSingleUri(context, uri)?.name
}

@Composable
fun Aasm2Theme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
