package com.reprush.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reprush.app.R
import com.reprush.app.ui.auth.SignInActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RepRushMessagingService : FirebaseMessagingService() {

    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var auth: FirebaseAuth

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "New Notification"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("fcmToken", token)
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, SignInActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "reprush_notifications"
    }
}
