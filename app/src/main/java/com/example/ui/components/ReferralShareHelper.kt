package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

object ReferralShareHelper {
    fun buildShareMessage(referralCode: String, referralLink: String): String {
        return """
            🏡 Discover luxury verified property reels and virtual walkthroughs on PropReel!

            Use my invite link: $referralLink
            Referral code: $referralCode

            Download PropReel today!
        """.trimIndent()
    }

    fun shareViaWhatsApp(context: Context, referralCode: String, referralLink: String) {
        val message = buildShareMessage(referralCode, referralLink)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Share PropReel Invite"))
        }
    }

    fun shareNative(context: Context, referralCode: String, referralLink: String) {
        val message = buildShareMessage(referralCode, referralLink)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, "Join me on PropReel")
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Share PropReel Invite via")
        context.startActivity(chooserIntent)
    }

    fun copyToClipboard(context: Context, text: String, label: String = "PropReel") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
