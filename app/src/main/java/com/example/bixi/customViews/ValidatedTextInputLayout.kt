package com.example.bixi.customViews

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import android.text.TextWatcher

class ValidatedTextInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {

    data class Validator(val regex: Regex, val errorMessage: String)

    var onFocusLostListener: ((Boolean) -> Unit)? = null

    private val validators = mutableListOf<Validator>()

    init {
        post {
            editText?.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val isValid = validate()
                    onFocusLostListener?.invoke(isValid)
                }
            }
        }
    }

    fun bindTo(setter: (String) -> Unit) {
        post {
            editText?.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    setter(s.toString())
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    /**
     * Adaugă un validator la listă.
     */
    fun addValidator(regex: Regex, errorMessage: String) {
        validators.add(Validator(regex, errorMessage))
    }

    /**
     * Înlocuiește toți validatorii existenți.
     */
    fun setValidators(newValidators: List<Validator>) {
        validators.clear()
        validators.addAll(newValidators)
    }

    /**
     * Validează textul curent folosind toți validatorii.
     * Returnează true dacă toți sunt validați, false altfel.
     */
    fun validate(): Boolean {
        val text = editText?.text?.toString() ?: return false

        for (validator in validators) {
            if (!validator.regex.matches(text)) {
                error = validator.errorMessage
                return false
            }
        }

        error = null
        return true
    }
}

