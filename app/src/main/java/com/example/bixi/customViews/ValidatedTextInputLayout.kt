package com.example.bixi.customViews

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout

class ValidatedTextInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {

    var validationRegex: Regex? = null
    var errorMessage: String = ""

    init {
        // Ascultă focus-ul după ce view-ul e complet construit
        post {
            editText?.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    validate()
                }
            }
        }
    }

    /**
     * Validează textul curent folosind regex-ul setat.
     * Returnează true dacă e valid, false altfel.
     */

    fun setRegex(validationRegex: Regex, errorMessage: String){
        this.validationRegex = validationRegex
        this.errorMessage = errorMessage
    }

    fun validate(): Boolean {
        val text = editText?.text?.toString() ?: return false
        val regex = validationRegex ?: return false

        return if (regex.matches(text)) {
            error = null
            true
        } else {
            error = errorMessage ?: "Câmp invalid"
            false
        }
    }
}
