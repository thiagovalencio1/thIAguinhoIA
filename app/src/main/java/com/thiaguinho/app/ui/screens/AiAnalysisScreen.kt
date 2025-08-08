package com.thiaguinho.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thiaguinho.app.ui.viewmodels.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAnalysisScreen(
    dtcCode: String,
    state: MainUiState,
    onGetAnalysis: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(key1 = dtcCode) {
        onGetAnalysis(dtcCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análise do thIAguinho") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dtcCode,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Análise por Inteligência Artificial",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (state.isAiLoading) {
                CircularProgressIndicator()
                Text(
                    "Consultando o especialista... aguarde.",
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                state.aiAnalysisResult?.let { result ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            result.fold(
                                onSuccess = { analysisText ->
                                    Text(
                                        text = analysisText, // Idealmente, usar um componente que renderize Markdown
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                onFailure = { error ->
                                    Text(
                                        text = "Erro ao obter análise: ${error.message}",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
