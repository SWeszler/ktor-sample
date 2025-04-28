package com.example

import io.ktor.serialization.kotlinx.json.json
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
import kotlinx.serialization.Serializable

@Serializable
data class Customer(val id: String, val name: String)

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
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
