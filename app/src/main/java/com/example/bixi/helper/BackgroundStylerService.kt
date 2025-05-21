package com.example.bixi.helper

import android.content.res.ColorStateList
import android.graphics.drawable.*
import android.os.Build
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Px

object BackgroundStylerService {

    /**
     * Setează fundalul unui View cu colțuri rotunjite, ripple effect și opțional bordură (stroke).
     *
     * @param view View-ul căruia i se setează fundalul
     * @param backgroundColor Culoarea de fundal
     * @param cornerRadius Raza colțurilor (în px)
     * @param withRipple Dacă să includă ripple effect
     * @param rippleColor Culoarea efectului ripple (default: gri transparent)
     * @param strokeWidth Grosimea bordurii (în px); dacă este 0, nu se adaugă bordură
     * @param strokeColor Culoarea bordurii
     */
    fun setRoundedBackground(
        view: View,
        @ColorInt backgroundColor: Int,
        @Px cornerRadius: Float,
        withRipple: Boolean = false,
        @ColorInt rippleColor: Int = 0x22000000,
        @Px strokeWidth: Int = 0,
        @ColorInt strokeColor: Int = 0
    ) {
        val shapeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            this.cornerRadius = cornerRadius

            if (strokeWidth > 0) {
                setStroke(strokeWidth, strokeColor)
            }
        }

        val finalDrawable: Drawable = if (withRipple) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                RippleDrawable(
                    ColorStateList.valueOf(rippleColor),
                    shapeDrawable,
                    null
                )
            } else {
                shapeDrawable
            }
        } else {
            shapeDrawable
        }

        view.background = finalDrawable
    }
}
