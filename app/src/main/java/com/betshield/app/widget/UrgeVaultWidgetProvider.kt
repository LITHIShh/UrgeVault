package com.betshield.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.data.VaultCalculator
import com.betshield.app.ui.MainActivity
import com.betshield.app.ui.UrgeSosActivity
import com.betshield.app.util.CurrencyFormatter

class UrgeVaultWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val app = BetShieldApplication.instance
            val repo = app.urgeVaultRepository
            val prefs = app.preferencesManager

            val profiles = repo.getHabitProfiles().filter { it.isActive }
            val checkIns = repo.getDailyCheckIns()
            val urgeSessions = repo.getUrgeSessions()
            val summary = VaultCalculator.calculateVaultSummary(profiles, checkIns, urgeSessions)

            val views = RemoteViews(context.packageName, R.layout.widget_urge_vault)

            val streakText = if (summary.longestActiveDays == 1L) "🔥 1 Day" else "🔥 ${summary.longestActiveDays} Days"
            views.setTextViewText(R.id.tvWidgetStreakDays, streakText)
            views.setTextViewText(R.id.tvWidgetMoneySaved, CurrencyFormatter.format(summary.totalMoneyProtected))

            if (prefs.isShieldEnabled) {
                views.setTextViewText(R.id.tvWidgetStatus, "● Protected")
                views.setTextColor(R.id.tvWidgetStatus, 0xFF34D399.toInt())
            } else {
                views.setTextViewText(R.id.tvWidgetStatus, "○ Paused")
                views.setTextColor(R.id.tvWidgetStatus, 0xFF9CA3AF.toInt())
            }

            // PendingIntent for Main App Launch
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, mainPendingIntent)

            // PendingIntent for Quick SOS Emergency Launch
            val sosIntent = Intent(context, UrgeSosActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val sosPendingIntent = PendingIntent.getActivity(
                context,
                1,
                sosIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetSos, sosPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, UrgeVaultWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UrgeVaultWidget", "Failed to update widgets", e)
            }
        }
    }
}
