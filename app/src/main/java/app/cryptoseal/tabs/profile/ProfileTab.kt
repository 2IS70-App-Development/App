package app.cryptoseal.tabs.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cryptoseal.data.api.ApiService
import app.cryptoseal.data.model.User
import kotlinx.coroutines.launch

/**
 * UI representation of a contact for display purposes.
 */
data class Contact(val contactId: Int, val email: String)

/**
 * The "Profile" tab UI.
 * Displays the current user's information, provides a logout option,
 * and manages a list of saved contacts.
 *
 * @param onLogout Callback triggered when the user successfully logs out.
 */
@Composable
fun ProfileTab(onLogout: () -> Unit) {
    val currentUser = ApiService.currentUser
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    /**
     * Fetches the user's contact list and resolves their emails from the server.
     */
    fun loadContacts() {
        scope.launch {
            isLoading = true
            ApiService.getContacts().fold(
                onSuccess = { contactList ->
                    // Resolve contact emails by fetching user details for each contact ID.
                    val displays = contactList.mapNotNull { contact ->
                        ApiService.getUserDetails(contact.contactId).getOrNull()?.let { user ->
                            Contact(contact.contactId, user.email)
                        }
                    }
                    contacts = displays
                },
                onFailure = { contacts = emptyList() }
            )
            isLoading = false
        }
    }

    // Refresh contacts when the screen is first displayed.
    LaunchedEffect(Unit) {
        loadContacts()
    }

    // Display the contact search/addition dialog.
    if (showAddContactDialog) {
        AddContactDialog(
            currentUserId = currentUser?.id,
            onDismiss = { showAddContactDialog = false },
            onContactAdded = {
                showAddContactDialog = false
                loadContacts()
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: User Profile Header
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Avatar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.email ?: "Loading...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        currentUser?.let {
                            Text(
                                text = "ID: ${it.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Logout Button
                    IconButton(onClick = {
                        ApiService.logout()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Section: Contact Details
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Contact Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User ID",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "User ID: ${currentUser?.id ?: "Loading..."}",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = currentUser?.email ?: "Loading...",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Section Header: Saved Contacts
        item {
            Column {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saved Contacts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(onClick = { showAddContactDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Contact")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
        }

        // List of Contacts or Loading Indicator
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(contacts) { contact ->
                ContactListItem(
                    contact = contact,
                    onRemove = { loadContacts() }
                )
            }
        }
    }
}

/**
 * A single item in the saved contacts list.
 * Displays the contact's email and a delete button.
 *
 * @param contact The contact data to display.
 * @param onRemove Callback triggered after the contact is successfully removed.
 */
@Composable
fun ContactListItem(contact: Contact, onRemove: () -> Unit) {
    val scope = rememberCoroutineScope()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.email,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = {
                scope.launch {
                    // Remove contact via API then trigger local UI refresh.
                    ApiService.removeContact(contact.contactId).onSuccess { onRemove() }
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Contact",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * A dialog that allows users to search for and add other users to their contacts.
 *
 * @param currentUserId The ID of the currently logged-in user, used to filter them out of search results.
 * @param onDismiss Callback to close the dialog.
 * @param onContactAdded Callback triggered when a contact is successfully added.
 */
@Composable
fun AddContactDialog(
    currentUserId: Int?,
    onDismiss: () -> Unit,
    onContactAdded: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    // Fetch all users on initialization to enable local searching.
    LaunchedEffect(Unit) {
        ApiService.getUsers().fold(
            onSuccess = { userList ->
                // Filter out the current user as they cannot add themselves as a contact.
                users = userList.filter { it.id != currentUserId }
            },
            onFailure = { }
        )
        isLoading = false
    }

    // Filter the user list based on the search query entered in the text field.
    val filteredUsers = remember(searchQuery, users) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            users.filter { it.email.contains(searchQuery, ignoreCase = true) }
                .take(5)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        selectedUser = null
                    },
                    label = { Text("Search by email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAdding
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (filteredUsers.isEmpty() && searchQuery.isNotBlank()) {
                    Text(
                        text = "No users found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    filteredUsers.forEach { user ->
                        Card(
                            onClick = { selectedUser = user },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (user == selectedUser)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = user.email,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                if (isAdding) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss, enabled = !isAdding) {
                Text("Cancel")
            }
        }
    )

    // Automatically trigger the API call to add a contact when a user is selected from the list.
    LaunchedEffect(selectedUser) {
        selectedUser?.let { user ->
            isAdding = true
            ApiService.addContact(user.id).fold(
                onSuccess = { onContactAdded() },
                onFailure = {
                    isAdding = false
                    selectedUser = null
                    searchQuery = ""
                }
            )
        }
    }
}
