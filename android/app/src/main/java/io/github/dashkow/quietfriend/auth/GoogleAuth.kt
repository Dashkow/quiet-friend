package io.github.dashkow.quietfriend.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import io.github.dashkow.quietfriend.R
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

sealed interface GoogleSignInResult {
    data object Success : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NoNetwork : GoogleSignInResult
    data object Failed : GoogleSignInResult
}

/**
 * Шаги 1–7 сценария UC-01: Credential Manager -> Google ID-токен -> вход в Firebase.
 * Токены в лог не пишутся.
 */
class GoogleAuth(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val isSignedIn: Boolean get() = auth.currentUser != null

    /** [activityContext] — именно Activity: Credential Manager показывает поверх неё окно выбора аккаунта. */
    suspend fun signIn(activityContext: Context): GoogleSignInResult {
        // Web client ID плагин google-services берёт из google-services.json
        val webClientId = activityContext.getString(R.string.default_web_client_id)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            return GoogleSignInResult.Cancelled  // 4а: пользователь закрыл окно
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager error: ${e.type}")
            return GoogleSignInResult.Failed
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Log.w(TAG, "Unexpected credential type")
            return GoogleSignInResult.Failed
        }

        val googleIdToken = try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Google ID token parsing failed")
            return GoogleSignInResult.Failed
        }

        return try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
            GoogleSignInResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            GoogleSignInResult.NoNetwork
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign-in failed: ${e.javaClass.simpleName}")
            GoogleSignInResult.Failed
        }
    }

    /** Firebase ID-токен (JWT, живёт 1 час). [forceRefresh] — получить новый, даже если старый не истёк. */
    suspend fun idToken(forceRefresh: Boolean): String? =
        auth.currentUser?.getIdToken(forceRefresh)?.await()?.token

    /** Выход из Firebase и сброс выбранного аккаунта в Credential Manager. */
    suspend fun signOut(context: Context) {
        auth.signOut()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "clearCredentialState failed: ${e.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "GoogleAuth"
    }
}
