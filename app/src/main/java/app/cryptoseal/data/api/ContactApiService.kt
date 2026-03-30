package app.cryptoseal.data.api

import app.cryptoseal.data.model.Contact
import app.cryptoseal.data.model.ContactIdRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import javax.net.ssl.HttpsURLConnection

/**
 * Handles contact-related API calls.
 */
class ContactApiService : BaseApiService() {

    suspend fun getContacts(): Result<List<Contact>> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/contacts", "GET")
            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                Result.success(gson.fromJson(response, Array<Contact>::class.java).toList())
            } else {
                Result.failure(Exception(parseError(response, "Failed to fetch contacts")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    suspend fun addContact(contactId: Int): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/contacts", "POST")
            conn.doOutput = true

            val request = ContactIdRequest(contactId)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode in 200..299) {
                Result.success(gson.fromJson(response, Contact::class.java))
            } else {
                Result.failure(Exception(parseError(response, "Failed to add contact")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }

    suspend fun removeContact(contactId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = authenticatedConnection("/auth/contacts", "DELETE")
            conn.doOutput = true

            val request = ContactIdRequest(contactId)
            OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(request)) }

            val responseCode = conn.responseCode
            val response = readResponse(conn)

            if (responseCode in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response, "Failed to remove contact")))
            }.also { conn.disconnect() }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }
}
