package support.bymason.kiosk.checkin.feature.nda

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import support.bymason.kiosk.checkin.helpers.TestCoroutineDispatcherRule

class NdaViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val dispatcherRule = TestCoroutineDispatcherRule()

    private val mockRepository = mock(NdaRepository::class.java)
    private val vm = NdaViewModel(mockRepository, "mySession")

    @Test
    fun `NDA signing action is immediately kicked off on create`() = dispatcherRule.runBlocking {
        launch {
            assertThat(vm.actions.take(1).single()).isEqualTo(NdaViewModel.Action.SignNda)
        }
    }

    @Test
    fun `View NDA action is sent after signing request`() = dispatcherRule.runBlocking {
        `when`(mockRepository.sign(any())).thenReturn("my_url")

        vm.onNdaSigningRequested()

        launch {
            assertThat(vm.viewActions.take(1).single())
                    .isEqualTo(NdaViewModel.ViewAction.VisitPage("my_url"))
        }
    }

    @Test
    fun `WebView client loads random web pages normally`() {
        val result = vm.createWebViewClient().shouldOverrideUrlLoading(
                WebView(null), createFakeRequest("https://google.com"))

        assertThat(result).isFalse()
    }

    @Test
    fun `WebView client sends NDA signed event on successful docusign response`() = dispatcherRule.runBlocking {
        val result = vm.createWebViewClient().shouldOverrideUrlLoading(
                WebView(null),
                createFakeRequest("https://mason-check-in-kiosk.firebaseapp.com" +
                                          "/redirect/docusign/app?event=signing_complete")
        )

        assertThat(result).isTrue()
        launch {
            assertThat(vm.actions.take(1).single())
                    .isEqualTo(NdaViewModel.Action.Navigate(NdaFragmentDirections.next()))
        }
        verify(mockRepository).finish(any())
    }

    @Test
    fun `WebView client restarts NDA signing on failed docusign response`() = dispatcherRule.runBlocking {
        val result = vm.createWebViewClient().shouldOverrideUrlLoading(
                WebView(null),
                createFakeRequest("https://mason-check-in-kiosk.firebaseapp.com" +
                                          "/redirect/docusign/app?event=failed")
        )

        assertThat(result).isTrue()
        launch {
            assertThat(vm.actions.take(1).single())
                    .isEqualTo(NdaViewModel.Action.SignNda)
        }
    }

    @Test
    fun `WebView chrome clears loading bar when page finishes loading`() {
        vm.createWebChromeClient().onProgressChanged(WebView(null), 100)

        assertThat(vm.state.value?.isLoading).isFalse()
    }

    @Test
    fun `WebView chrome shows loading bar when page is loading`() {
        vm.createWebChromeClient().onProgressChanged(WebView(null), 42)

        assertThat(vm.state.value?.isLoading).isTrue()
    }

    @Test
    fun `WebView is hidden while finalizing check in`() {
        dispatcherRule.pauseDispatcher()
        vm.createWebViewClient().shouldOverrideUrlLoading(
                WebView(null),
                createFakeRequest("https://mason-check-in-kiosk.firebaseapp.com" +
                                          "/redirect/docusign/app?event=signing_complete")
        )

        assertThat(vm.state.value?.isLoading).isTrue()
        assertThat(vm.state.value?.isWebViewVisible).isFalse()
    }

    private fun createFakeRequest(url: String) = object : WebResourceRequest {
        override fun getUrl() = mock(Uri::class.java).apply {
            `when`(toString()).thenReturn(url)
            `when`(getQueryParameter("event")).thenReturn(url.split("event=").last())
        }

        override fun isRedirect() = error("Not implemented")
        override fun getMethod() = error("Not implemented")
        override fun getRequestHeaders() = error("Not implemented")
        override fun hasGesture() = error("Not implemented")
        override fun isForMainFrame() = error("Not implemented")
    }
}
