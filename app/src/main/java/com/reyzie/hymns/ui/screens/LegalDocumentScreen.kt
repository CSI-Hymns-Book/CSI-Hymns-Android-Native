package com.reyzie.hymns.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.data.ConsentManager
import com.reyzie.hymns.data.LegalDocumentKind
import com.reyzie.hymns.data.LegalDocuments
import com.reyzie.hymns.ui.motion.PredictiveExpressiveBackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    kind: LegalDocumentKind,
    onBackClick: () -> Unit
) {
    PredictiveExpressiveBackHandler(enabled = true, onBack = onBackClick)
    val language by ConsentManager.language.collectAsState()
    val context = LocalContext.current
    val options = ConsentManager.LegalLanguage.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kind.title(language), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, lang ->
                    SegmentedButton(
                        selected = language == lang,
                        onClick = { ConsentManager.setLanguage(lang) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) {
                        Text(lang.label)
                    }
                }
            }

            Text(
                "Version ${ConsentManager.CURRENT_POLICY_VERSION}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LegalDocuments.sections(kind, language).forEach { section ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            section.heading,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            section.body,
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (kind == LegalDocumentKind.PRIVACY) {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(LegalDocuments.LEGACY_PRIVACY_URL))
                        )
                    }
                ) {
                    Text("Open previous web privacy page")
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit) {
    LegalDocumentScreen(kind = LegalDocumentKind.PRIVACY, onBackClick = onBackClick)
}

@Composable
fun TermsOfUseScreen(onBackClick: () -> Unit) {
    LegalDocumentScreen(kind = LegalDocumentKind.TERMS, onBackClick = onBackClick)
}
