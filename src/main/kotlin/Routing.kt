package com.example

import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.application.Application
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.receive
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.application.install
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import com.example.CustomerOuterClass.Customer
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import kotlinx.serialization.ExperimentalSerializationApi
import io.ktor.serialization.ContentConverter
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import com.google.protobuf.util.JsonFormat
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readText
import kotlinx.serialization.Serializable

class ProtobufCustomerConverter : ContentConverter {
    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): OutgoingContent? {
        TODO("Not yet implemented")
    }

    override suspend fun deserialize(
        charset: Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel
    ): Any? {
        val jsonString = content.readRemaining().readText() // Correctly use readText from ByteReadPacket
        val builder = Customer.newBuilder()
        JsonFormat.parser().merge(jsonString, builder)
        return builder.build()
    }
}

@Serializable
data class CustomerJson(val id: Int, val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureRouting() {
    install(RequestValidation) {
        validate<Customer> { customer ->
            if (customer.id <= 0)
                ValidationResult.Invalid("Customer ID must be greater than 0")
            else ValidationResult.Valid
        }
        validate<CustomerJson> { customer ->
            if (customer.id <= 0)
                ValidationResult.Invalid("Customer ID must be greater than 0")
            else ValidationResult.Valid
        }
    }
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, "Validation error: ${cause.reasons.joinToString(", ")}")
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, "Invalid request: ${cause.message}")
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        route("/customer") {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, ProtobufCustomerConverter())
            }
            post {
                val customer = call.receive<Customer>()
                call.respondText("[json-protobuf] Customer with ID ${customer.id} and name ${customer.name} created successfully!")
            }
        }
        route("/customer/json") {
            install(ContentNegotiation) {
                json()
            }
            post {
                val customer = call.receive<CustomerJson>()
                call.respondText("[json] Customer with ID ${customer.id} and name ${customer.name} created successfully!")
            }
        }
    }
}
