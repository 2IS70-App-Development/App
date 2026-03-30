package app.cryptoseal.data.api

import android.content.Context
import app.cryptoseal.data.model.User

/**
 * Singleton facade for all API operations.
 * Delegated to specialized services to maintain a clean architecture and small file size.
 */
object ApiService {
    private var sessionManager: SessionManager? = null

    // Delegated Services
    private val authService = AuthApiService()
    private val userService = UserApiService()
    private val orderService = OrderApiService()
    private val scanService = ScanApiService()
    private val contactService = ContactApiService()
    private val activityService = ActivityApiService()

    var authToken: String? = null
        internal set

    var currentUser: User? = null
        internal set

    fun initialize(context: Context) {
        sessionManager = SessionManager(context)
        authToken = sessionManager?.authToken
        currentUser = sessionManager?.currentUser
    }

    suspend fun signup(email: String, password: String) = authService.signup(email, password)

    suspend fun login(email: String, password: String): Result<String> {
        val result = authService.login(email, password)
        result.onSuccess { token ->
            authToken = token
            sessionManager?.authToken = token
            fetchCurrentUser(email)
        }
        return result
    }

    private suspend fun fetchCurrentUser(email: String) {
        getUsers().onSuccess { users ->
            currentUser = users.find { it.email == email }
            sessionManager?.currentUser = currentUser
        }
    }

    fun logout() {
        authToken = null
        currentUser = null
        sessionManager?.clear()
    }

    fun isLoggedIn(): Boolean = (authToken ?: sessionManager?.authToken) != null

    // Delegated methods for Users
    suspend fun getUsers() = userService.getUsers()
    suspend fun getUserDetails(id: Int) = userService.getUserDetails(id)

    // Delegated methods for Orders
    suspend fun getOrders() = orderService.getOrders()
    suspend fun getOrderDetails(id: Int) = orderService.getOrderDetails(id)
    suspend fun createOrder(
        receiverId: Int,
        name: String,
        meta: String = "",
        comment: String = "",
        photo: String? = null
    ) =
        orderService.createOrder(receiverId, name, meta, comment, photo)

    suspend fun updateOrderStatus(orderId: Int, status: String) =
        orderService.updateOrderStatus(orderId, status)

    // Delegated methods for Scans
    suspend fun getOrderScans(orderId: Int) = scanService.getOrderScans(orderId)
    suspend fun createOrderScan(
        orderId: Int,
        photoBase64: String,
        condition: String,
        longitude: Float,
        latitude: Float,
        comment: String = ""
    ) =
        scanService.createOrderScan(orderId, photoBase64, condition, longitude, latitude, comment)

    suspend fun scanPackage(
        orderId: Int,
        photoBytes: ByteArray,
        condition: String,
        longitude: Float,
        latitude: Float,
        comment: String = ""
    ) =
        scanService.scanPackage(orderId, photoBytes, condition, longitude, latitude, comment)

    // Delegated methods for Contacts
    suspend fun getContacts() = contactService.getContacts()
    suspend fun addContact(contactId: Int) = contactService.addContact(contactId)
    suspend fun removeContact(contactId: Int) = contactService.removeContact(contactId)

    // Delegated methods for Activities
    suspend fun getActivities() = activityService.getActivities()
}
