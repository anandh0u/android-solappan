package com.solappan.agent

/** Only known codes become UI copy; provider response text is never echoed. */
internal fun accountErrorMessage(status: Int, code: String): String = when {
    code == "email_not_confirmed" -> "Verify the email sent to your inbox before signing in. Check spam and confirm the address spelling."
    code == "invalid_credentials" -> "Email or password is incorrect. Use your SOL account, not your Supabase dashboard login."
    code == "email_address_invalid" -> "Enter a valid email address."
    code == "weak_password" -> "Choose a stronger password of at least 12 characters."
    status == 429 -> "Too many attempts. Wait before trying again."
    status in listOf(400, 401, 403) -> "Account access was rejected. Check your credentials and email verification."
    else -> "Account service unavailable. Try again later."
}
