package com.byajbook.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// [FIX-DATEINPUT-1] & [FIX-DATEINPUT-SHORTFORMAT-1] Spec Requirement
@Composable
fun DateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var localError by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    
    val displayError = isError || localError
    val displayErrorMessage = if (localError) errorText else errorMessage

    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val clean = input.filter { it.isDigit() || it == '/' || it == '-' }
            
            val isRawDigits = clean.all { it.isDigit() }
            if (isRawDigits) {
                onValueChange(clean.take(8))
            } else {
                onValueChange(clean.take(10))
            }
            localError = false
        },
        modifier = modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused && value.isNotEmpty()) {
                val processed = processShortDateOrValidate(value)
                if (processed.isInvalid) {
                    localError = true
                    errorText = "Invalid date"
                } else {
                    localError = false
                    onValueChange(processed.digitsOnly)
                }
            }
        },
        label = { Text(label) },
        placeholder = { Text("DD/MM/YYYY") },
        enabled = enabled,
        isError = displayError,
        supportingText = {
            if (displayError && displayErrorMessage.isNotEmpty()) {
                Text(displayErrorMessage)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                val processed = processShortDateOrValidate(value)
                if (processed.isInvalid) {
                    localError = true
                    errorText = "Invalid date"
                } else {
                    localError = false
                    onValueChange(processed.digitsOnly)
                }
            }
        ),
        visualTransformation = DateVisualTransformation()
    )
}

class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val isRawDigits = text.text.all { it.isDigit() }
        if (!isRawDigits) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        return TransformedText(
            text = AnnotatedString(formatDateInput(text.text)),
            offsetMapping = DateOffsetMapping()
        )
    }
}

fun formatDateInput(input: String): String {
    val digits = input.take(8)
    var out = ""
    for (i in digits.indices) {
        out += digits[i]
        if (i == 1 || i == 3) out += "/"
    }
    return out
}

class DateOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        if (offset <= 1) return offset
        if (offset <= 3) return offset + 1
        return offset + 2
    }

    override fun transformedToOriginal(offset: Int): Int {
        if (offset <= 2) return offset
        if (offset <= 5) return offset - 1
        return offset - 2
    }
}

data class DateProcessResult(val digitsOnly: String, val isInvalid: Boolean)

fun processShortDateOrValidate(input: String): DateProcessResult {
    if (input.all { it.isDigit() } && input.length == 8) {
        return if (isValidDate(input)) DateProcessResult(input, false)
        else DateProcessResult(input, true)
    }
    if (input.all { it.isDigit() }) return DateProcessResult(input, true)
    if (input.contains("/") && input.contains("-")) return DateProcessResult(input, true)

    val separator = if (input.contains("/")) "/" else "-"
    val parts = input.split(separator)
    
    if (parts.size != 3) return DateProcessResult(input, true)
    
    val d = parts[0].padStart(2, '0')
    val m = parts[1].padStart(2, '0')
    var y = parts[2]
    
    if (y.length == 2) y = "20$y"
    if (y.length != 4) return DateProcessResult(input, true)
    
    val fullDigits = "$d$m$y"
    return if (isValidDate(fullDigits)) {
        DateProcessResult(fullDigits, false)
    } else {
        DateProcessResult(input, true)
    }
}

fun isValidDate(digits: String): Boolean {
    if (digits.length != 8) return false
    val formatted = formatDateInput(digits)
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val date = LocalDate.parse(formatted, formatter)
        date.year in 1900..2100
    } catch (_: DateTimeParseException) {
        false
    }
}