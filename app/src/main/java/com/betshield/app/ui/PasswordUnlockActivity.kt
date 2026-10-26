package com.betshield.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PasswordUnlockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REASON = "extra_reason"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dialog = PasswordDialog(
            context = this,
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
