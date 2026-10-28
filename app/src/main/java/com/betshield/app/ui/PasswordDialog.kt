package com.betshield.app.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.databinding.ActivityPasswordUnlockBinding

class PasswordDialog(
    private val context: Context,
    private val requireAllyPin: Boolean = false,
    private val onSuccess: () -> Unit,
    private val onCancel: (() -> Unit)? = null
) {

    private val dialog: Dialog = Dialog(context)
    private val binding: ActivityPasswordUnlockBinding =
        ActivityPasswordUnlockBinding.inflate(LayoutInflater.from(context))
    private val passwordManager = BetShieldApplication.instance.passwordManager

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val isFirstTime = !passwordManager.isPasswordSet() && !requireAllyPin

        if (requireAllyPin) {
            binding.tvPasswordDialogTitle.text = "Accountability Ally Lock"
            binding.tvPasswordDialogDesc.text = "Enter the secret PIN set by your Accountability Ally to proceed."
            binding.tilConfirmPassword.visibility = View.GONE
            binding.btnConfirmPassword.text = "Verify Ally PIN"
        } else if (isFirstTime) {
            binding.tvPasswordDialogTitle.setText(R.string.password_title_setup)
            binding.tvPasswordDialogDesc.setText(R.string.password_desc_setup)
            binding.tilConfirmPassword.visibility = View.VISIBLE
            binding.btnConfirmPassword.setText(R.string.btn_confirm)
        } else {
            binding.tvPasswordDialogTitle.setText(R.string.password_title_unlock)
            binding.tvPasswordDialogDesc.setText(R.string.password_desc_unlock)
            binding.tilConfirmPassword.visibility = View.GONE
            binding.btnConfirmPassword.setText(R.string.btn_unlock)
        }

        binding.btnConfirmPassword.setOnClickListener {
            val password = binding.etPassword.text?.toString().orEmpty()

            if (requireAllyPin) {
                if (passwordManager.verifyAllyPin(password)) {
                    dialog.dismiss()
                    onSuccess()
                } else {
                    showError("Incorrect Ally PIN. Only your ally can authorize this change.")
                }
            } else if (isFirstTime) {
                val confirm = binding.etConfirmPassword.text?.toString().orEmpty()
                if (password.length < 4) {
                    showError(context.getString(R.string.error_password_short))
                    return@setOnClickListener
                }
                if (password != confirm) {
                    showError(context.getString(R.string.error_password_mismatch))
                    return@setOnClickListener
                }
                passwordManager.setPassword(password)
                dialog.dismiss()
                onSuccess()
            } else {
                if (passwordManager.verifyPassword(password) || (passwordManager.isAllyPinSet() && passwordManager.verifyAllyPin(password))) {
                    dialog.dismiss()
                    onSuccess()
                } else {
                    showError(context.getString(R.string.error_password_incorrect))
                }
            }
        }

        binding.btnCancelPassword.setOnClickListener {
            dialog.dismiss()
            onCancel?.invoke()
        }
    }

    private fun showError(message: String) {
        binding.tvPasswordError.text = message
        binding.tvPasswordError.visibility = View.VISIBLE
    }

    fun show() {
        dialog.show()
    }
}
