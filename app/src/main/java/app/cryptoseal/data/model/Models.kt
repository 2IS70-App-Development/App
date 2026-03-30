package app.cryptoseal.data.model

import com.google.gson.annotations.SerializedName

/**
 * User Data Model
 * Represents a registered user in the CryptoSeal system.
 * 
 * @property id The unique numeric ID assigned by the database.
 * @property email The unique email address used for login and identification.
 * @property createdAt ISO 8601 timestamp of when the account was created.
 */
data class User(
    val id: Int,
    val email: String,
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Order Data Model
 * Represents a physical package or "shipment" being tracked.
 * 
 * An Order is the primary entity in the system, moving from a Sender to a Receiver.
 * 
 * @property id Unique identifier for the order.
 * @property senderId ID of the user who initiated the shipment.
 * @property receiverId ID of the user intended to receive the shipment.
 * @property name User-provided title for the package (e.g., "MacBook Pro").
 * @property status Current lifecycle state: "SENT", "DELIVERED", or "CANCELLED".
 * @property meta JSON or string-based metadata for extensible attributes.
 * @property comment A general description provided by the sender.
 * @property photo Optional Base64 encoded string of the package's initial state.
 * @property createdAt Timestamp of when the order was first registered.
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
 * Scan Data Model
 * Represents a tracking event or "checkpoint" in a package's journey.
 * 
 * A Scan is recorded every time a package is handed over or inspected.
 * It provides the "Chain of Custody" by linking an order to a courier at a specific location.
 * 
 * @property id Unique identifier for this scan record.
 * @property orderId The ID of the order being scanned.
 * @property courierId The ID of the user who performed the scan.
 * @property photo Base64 encoded image taken during the scan (handover proof).
 * @property condition Physical state of the package: "Good", "Missing", or "Damaged".
 * @property longitude GPS longitude coordinate of the scan location.
 * @property latitude GPS latitude coordinate of the scan location.
 * @property comment Additional notes about this specific scan event.
 * @property createdAt Timestamp of when the scan was performed.
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
 * Activity Data Model
 * Represents a notification or system event log for the user.
 * 
 * @property actorId The ID of the user who performed the action.
 * @property userId The ID of the user who is notified of this action.
 * @property type The category of event (e.g., "order_created", "status_changed").
 * @property summary A concise, human-readable description of what happened.
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

// --- Request Bodies for API Communication ---

/**
 * Data needed to create a new order via POST /auth/orders.
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
 * Data needed to update an order's status via PUT /auth/orders/status.
 */
data class UpdateOrderStatusRequest(
    @SerializedName("order_id")
    val orderId: Int,
    val status: String
)

/**
 * Data needed to register a new scan via POST /auth/orders/scan.
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
 * Data for user authentication via POST /jwt/create.
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Data for user registration via POST /signup.
 */
data class SignupRequest(
    val email: String,
    val password: String
)

/**
 * Response structure for successful authentication.
 */
data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String
)

/**
 * Standard error structure returned by the API on failure.
 */
data class ErrorResponse(
    val error: String
)

/**
 * Represents a contact link between two users in the database.
 */
data class Contact(
    @SerializedName("owner_id")
    val ownerId: Int,
    @SerializedName("contact_id")
    val contactId: Int
)

/**
 * Request body for adding or removing a contact by their user ID.
 */
data class ContactIdRequest(
    @SerializedName("contact_id")
    val contactId: Int
)
