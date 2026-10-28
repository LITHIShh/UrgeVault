package com.betshield.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PasswordUnlockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REASON = "extra_reason"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = com.betshield.app.BetShieldApplication.instance
        val requireAlly = app.preferencesManager.isAllyPinEnabled && app.passwordManager.isAllyPinSet()

        val dialog = PasswordDialog(
            context = this,
            requireAllyPin = requireAlly,
            onSuccess = {
                finish()
            },
            onCancel = {
                finish()
            }
        )
        dialog.show()
    }
}
