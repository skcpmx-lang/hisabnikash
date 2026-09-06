package com.hisabnikash.app.ui.vm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisabnikash.app.data.container.AppContainer

/**
 * Creates a ViewModel bound to the app container. The creator is only invoked
 * once per ViewModelStore, so each screen constructs one VM.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    container: AppContainer,
    key: String? = null,
    noinline create: (AppContainer) -> VM
): VM {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create(container) as T
        }
    }
    return viewModel(key = key, factory = factory)
}
