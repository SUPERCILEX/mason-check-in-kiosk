package com.bymason.kiosk.checkin.feature.nda

import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import androidx.webkit.WebViewClientCompat
import com.bymason.kiosk.checkin.core.logBreadcrumb
import com.bymason.kiosk.checkin.core.model.Employee
import com.bymason.kiosk.checkin.core.model.Guest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class NdaViewModel(
        private val repository: NdaRepository
) : ViewModel() {
    private val _state = MutableLiveData<State>(State())
    val state: LiveData<State> get() = _state
    private val _actions = Channel<Action>(Channel.CONFLATED)
    val actions: Flow<Action> = flow { for (e in _actions) emit(e) }
    private val _viewActions = Channel<ViewAction>(Channel.CONFLATED)
    val viewActions: Flow<ViewAction> = flow { for (e in _viewActions) emit(e) }

    init {
        _actions.offer(Action.SignNda)
    }

    fun createWebViewClient() = object : WebViewClientCompat() {
        override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
        ): Boolean = if (request.url.toString().contains("redirect/docusign/app")) {
            onNdaSigned(request.url.getQueryParameter("event") ?: "error")
            true
        } else {
            false
        }
    }

    fun signNda(guest: Guest) {
        viewModelScope.launch {
            try {
                _viewActions.offer(ViewAction.VisitPage(repository.sign(guest.name, guest.email)))
            } catch (t: Throwable) {
                logBreadcrumb("Failed to get link to sign NDA", t)
                return@launch
            }
        }
    }

    fun finish(employee: Employee, guest: Guest) {
        viewModelScope.launch {
            try {
                repository.finish(employee.id, guest.name, guest.email)
                _actions.offer(Action.Navigate(NdaFragmentDirections.reset()))
            } catch (t: Throwable) {
                logBreadcrumb("Failed to fetch list of employees", t)
                return@launch
            }
        }
    }

    private fun onNdaSigned(result: String) {
        if (result == "signing_complete") {
            _state.postValue(_state.value!!.copy(isFinishButtonEnabled = true))
        } else {
            _actions.offer(Action.SignNda)
        }
    }

    sealed class Action {
        data class Navigate(val directions: NavDirections) : Action()

        object SignNda : Action()
    }

    sealed class ViewAction {
        data class VisitPage(val url: String) : ViewAction()
    }

    data class State(
            val isFinishButtonEnabled: Boolean = false
    )

    class Factory(
            private val repository: NdaRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            check(modelClass === NdaViewModel::class.java)

            @Suppress("UNCHECKED_CAST")
            return NdaViewModel(repository) as T
        }
    }
}
