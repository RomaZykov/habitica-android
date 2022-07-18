package com.habitrpg.wearos.habitica.util

import android.util.Log
import com.habitrpg.android.habitica.R
import com.habitrpg.wearos.habitica.managers.AppStateManager
import com.habitrpg.wearos.habitica.models.DisplayedError
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.IOException
import javax.inject.Inject

@ViewModelScoped
class ExceptionHandlerBuilder @Inject constructor(val appStateManager: AppStateManager) {
    fun silent(handler: ((Throwable) -> Unit)? = null): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { context, throwable ->
            Log.e(context.toString(), "Error: ${throwable.cause}", throwable)
            handler?.invoke(throwable)
        }
    }

    fun userFacing(errorPresenter: ErrorPresenter): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            Log.e("Coroutine Error", "Error: ${throwable.cause}", throwable)
            if (throwable is IOException) {
                errorPresenter.errorValues.value = throwable.message?.let {
                    DisplayedError(R.drawable.disconnected, "Disconnected")
                }
            } else {
                errorPresenter.errorValues.value = throwable.message?.let {
                    DisplayedError(R.drawable.error, it)
                }
            }

            appStateManager.endLoading()
        }
    }
}