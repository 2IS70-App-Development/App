package app.cryptoseal.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a user within the CryptoSeal system.
 * @property id Unique identifier for the user.
 * @property email The user's registered email address.
 * @property createdAt Timestamp indicating when the user account was created.
 */
data class User(
    val id: Int,
    val email: String,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Represents a physical package or "order" being tracked in the system.
 * @property id Unique identifier for the order.
 * @property senderId User ID of the person who sent the package.
 * @property receiverId User ID of the intended recipient.
 * @property name A descriptive name for the package.
 * @property status Current delivery status (e.g., "SENT", "DELIVERED").
 * @property meta Additional metadata associated with the package.
 * @property comment A general comment or description provided by the sender.
 * @property photo Optional Base64 encoded string of a photo of the package.
 * @property createdAt Timestamp when the order was first created.
 */
data class Order(
    val id: Int,
    @SerializedName("sender_id")
    val senderId: Int,
    @SerializedName("receiver_id")
    val receiverId: Int,
    val name: String,
    val status: String,
    val meta: String,
    val comment: String,
    val photo: String? = null,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Represents a tracking event (scan) for a specific order.
 * Recorded whenever a package is handed over or checked.
 * @property id Unique identifier for the scan record.
 * @property orderId ID of the order this scan belongs to.
 * @property courierId User ID of the person performing the scan.
 * @property photo Optional Base64 encoded string of a photo taken during the scan.
 * @property condition Description of the package's physical state (e.g., "MINT", "DAMAGED").
 * @property longitude GPS longitude where the scan occurred.
 * @property latitude GPS latitude where the scan occurred.
 * @property comment Additional notes about this specific scan.
 * @property createdAt Timestamp when the scan was recorded.
 */
data class Scan(
    val id: Int,
    @SerializedName("order_id")
    val orderId: Int,
    @SerializedName("courier_id")
    val courierId: Int,
    val photo: String? = null,
    val condition: String,
    val longitude: Float,
    val latitude: Float,
    val comment: String,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Represents a system activity or notification.
 * Used for displaying a feed of events to the user.
 * @property actorId The ID of the user who performed the action.
 * @property userId The ID of the user whom the action concerns (often the current user).
 * @property type The category of activity (e.g., "order_created", "status_updated").
 * @property summary A brief text description of what happened.
 */
data class Activity(
    val id: Int,
    @SerializedName("actor_id")
    val actorId: Int,
    @SerializedName("user_id")
    val userId: Int,
    val type: String,
    val summary: String,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Request body for creating a new order.
 */
data class CreateOrderRequest(
    @SerializedName("receiver_id")
    val receiverId: Int,
    val name: String,
    val meta: String,
    val comment: String,
    val photo: String? = null
)

/**
 * Request body for updating the status of an existing order.
 */
data class UpdateOrderStatusRequest(
    @SerializedName("order_id")
    val orderId: Int,
    val status: String
)

/**
 * Request body for creating a new scan record.
 */
data class CreateOrderScanRequest(
    @SerializedName("order_id")
    val orderId: Int,
    @SerializedName("photo_base64")
    val photoBase64: String,
    val condition: String,
    val longitude: Float,
    val latitude: Float,
    val comment: String
)

/**
 * Request body for user authentication (Login).
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Request body for user registration (Signup).
 */
data class SignupRequest(
    val email: String,
    val password: String
)

/**
 * Successful authentication response containing the JWT token.
 */
data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String
)

/**
 * Standard error response structure from the API.
 */
data class ErrorResponse(
    val error: String
)

/**
 * Represents a contact relationship between two users.
 */
data class Contact(
    @SerializedName("owner_id")
    val ownerId: Int,
    @SerializedName("contact_id")
    val contactId: Int
)

/**
 * Request body for operations involving a specific contact ID.
 */
data class ContactIdRequest(
    @SerializedName("contact_id")
    val contactId: Int
)
