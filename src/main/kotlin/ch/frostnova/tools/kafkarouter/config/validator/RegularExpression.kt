package ch.frostnova.tools.kafkarouter.config.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.TYPE_PARAMETER
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.reflect.KClass


@Target(FIELD, VALUE_PARAMETER, TYPE_PARAMETER, TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = [RegularExpressionValidator::class])
@MustBeDocumented
annotation class RegularExpression(
    val message: String = "must be a valid regular expression",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<Any>> = []
)

class RegularExpressionValidator : ConstraintValidator<RegularExpression, String> {


    override fun isValid(value: String?, ctx: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrEmpty()) return true;
        try {
            Regex(value)
            return true
        } catch (ex: Exception) {
            return false
        }
    }
}