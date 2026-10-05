package io.github.quasideus.kekkai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.quasideus.kekkai.repository.Repository

/** Shared search/list screen for the launcher and Autofill picker; filters by name only. */
@Composable
fun EntryPickerScreen(
    repository: Repository,
    onEntrySelected: (String) -> Unit,
) {
    var allEntries by remember { mutableStateOf<List<String>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(repository) {
        allEntries = repository.listEntries()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val filtered = remember(allEntries, query) {
        if (query.isBlank()) {
            allEntries
        } else {
            allEntries.filter { it.contains(query, ignoreCase = true) }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search kekkai…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(focusRequester),
            )

            LazyColumn {
                items(filtered) { entryName ->
                    ListItem(
                        headlineContent = { Text(entryName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEntrySelected(entryName) },
                    )
                }
            }
        }
    }
}
