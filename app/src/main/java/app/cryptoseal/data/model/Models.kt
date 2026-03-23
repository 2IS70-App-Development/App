package app.cryptoseal.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val email: String,
    @SerializedName("created_at")
    val createdAt: String
)

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

data class CreateOrderRequest(
    @SerializedName("receiver_id")
    val receiverId: Int,
    val name: String,
    val meta: String,
    val comment: String,
    val photo: String? = null
)

data class UpdateOrderStatusRequest(
    @SerializedName("order_id")
    val orderId: Int,
    val status: String
)

data class CreateScanRequest(
    @SerializedName("order_id")
    val orderId: Int,
    @SerializedName("photo_base64")
    val photoBase64: String,
    val condition: String,
    val longitude: Float,
    val latitude: Float,
    val comment: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class SignupRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String
)

data class ErrorResponse(
    val error: String
)

data class Contact(
    @SerializedName("owner_id")
    val ownerId: Int,
    @SerializedName("contact_id")
    val contactId: Int
)

data class ContactIdRequest(
    @SerializedName("contact_id")
    val contactId: Int
)
