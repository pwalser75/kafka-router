package ch.frostnova.tools.kafkarouter.util

import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import java.util.Locale

val validator: Validator by lazy {
    Locale.setDefault(Locale.ENGLISH)
    Validation.byDefaultProvider().configure()
        //    .messageInterpolator(ParameterMessageInterpolator())
        .buildValidatorFactory()
        .validator
}

fun validate(any: Any) {
    var constraintViolations = (if (any is Collection<*>) any.flatMap { validator.validate(it) }.toList()
    else validator.validate(any))
        // the transferableType is set on serialization - if we set it before, it will appear twice in the JSON
        .filter { it.propertyPath.toString() != "transferableType" || it.invalidValue != null }

    if (constraintViolations.isNotEmpty()) {
        throw ValidationException(constraintViolations.joinToString("\n") { "${it.propertyPath}: (${it.invalidValue}) ${it.message}" })
    }
}