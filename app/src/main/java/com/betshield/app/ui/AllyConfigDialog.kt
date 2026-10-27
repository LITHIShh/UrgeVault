package com.betshield.app.ui

import android.app.Activity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.databinding.DialogConfigureAllyBinding

object AllyConfigDialog {

    fun show(
        activity: Activity,
        shouldDialAfterSave: Boolean = false,
        onSaved: (name: String, phone: String) -> Unit = { _, _ -> }
    ) {
        val prefs = BetShieldApplication.instance.preferencesManager
        val binding = DialogConfigureAllyBinding.inflate(activity.layoutInflater)

        binding.etAllyName.setText(prefs.allyName)
        binding.etAllyPhone.setText(prefs.allyPhone)

        val positiveBtnText = if (shouldDialAfterSave) "Save & Call" else "Save"

        AlertDialog.Builder(activity, R.style.Theme_BetShield_Dialog)
            .setView(binding.root)
            .setPositiveButton(positiveBtnText) { _, _ ->
                val name = binding.etAllyName.text.toString().trim()
                val phone = binding.etAllyPhone.text.toString().trim()

                prefs.allyName = name
                prefs.allyPhone = phone

                Toast.makeText(
                    activity,
                    if (name.isNotEmpty()) "Emergency Ally '$name' saved." else "Emergency Ally contact saved.",
                    Toast.LENGTH_SHORT
                ).show()

                onSaved(name, phone)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
