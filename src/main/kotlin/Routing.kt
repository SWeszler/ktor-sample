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
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readText

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

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureRouting() {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, ProtobufCustomerConverter())
    }
    install(RequestValidation) {
        validate<Customer> { customer ->
            if (customer.id.toInt() <= 0)
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
        post("/customer") {
            val customer = call.receive<Customer>()
            call.respondText("Customer with ID ${customer.id} and name ${customer.name} created successfully!")
        }
    }
}
