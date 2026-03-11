package com.ixuea.course.weather.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ixuea.course.weather.data.model.AIAdviceEvent
import com.ixuea.course.weather.data.model.AIAdviceRequest
import com.ixuea.course.weather.data.model.AIAdviceState
import com.ixuea.course.weather.data.model.Forecast
import com.ixuea.course.weather.data.model.WeatherResponse
import com.ixuea.course.weather.data.repository.AIRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * AIViewModel - AI建议页面的ViewModel
 *
 * 【职责】
 * 1. 管理AI建议UI状态
 * 2. 调用Repository获取AI建议
 * 3. 处理流式响应更新UI
 * 4. 管理导航事件
 */
class AIViewModel(
    private val repository: AIRepository,
    private val currentWeather: WeatherResponse?,
    private val forecast: List<Forecast>
) : ViewModel() {

    companion object {
        const val TAG = "AIViewModel"
    }

    // UI状态
    private val _adviceState = MutableStateFlow(AIAdviceState())
    val adviceState: StateFlow<AIAdviceState> = _adviceState.asStateFlow()

    // 导航事件
    private val _navigateBack = MutableStateFlow(false)
    val navigateBack: StateFlow<Boolean> = _navigateBack.asStateFlow()

    init {
        // 初始化时自动请求AI建议
        if (currentWeather != null) {
            onEvent(AIAdviceEvent.GenerateAdvice)
        } else {
            _adviceState.update {
                it.copy(
                    isLoading = false,
                    error = "暂无天气数据，请先获取当前位置的天气"
                )
            }
        }
    }

    /**
     * 统一事件处理入口
     */
    fun onEvent(event: AIAdviceEvent) {
        when (event) {
            is AIAdviceEvent.GenerateAdvice -> generateAdvice()
            is AIAdviceEvent.AdviceUpdated -> updateAdvice(event.content)
            is AIAdviceEvent.AdviceCompleted -> completeAdvice()
            is AIAdviceEvent.Error -> handleError(event.message)
            is AIAdviceEvent.NavigateBack -> navigateBack()
        }
    }

    /**
     * 重置导航状态
     */
    fun onNavigatedBack() {
        _navigateBack.value = false
    }

    /**
     * 生成AI建议
     */
    private fun generateAdvice() {
        val weather = currentWeather ?: return

        viewModelScope.launch {
            val request = AIAdviceRequest(
                currentWeather = weather,
                forecast = forecast
            )

            repository.generateAdvice(request)
                .onStart {
                    _adviceState.update {
                        it.copy(isLoading = true, error = null, advice = "")
                    }
                }
                .catch { e ->
                    Log.e(TAG, "Error generating advice", e)
                    handleError("获取AI建议失败: ${e.localizedMessage}")
                }
                .onCompletion {
                    _adviceState.update { it.copy(isLoading = false) }
                }
                .collectLatest { content ->
                    Log.d(TAG, "Received content: ${content.length} chars")
                    _adviceState.update {
                        it.copy(
                            advice = content,
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * 更新AI建议内容
     */
    private fun updateAdvice(content: String) {
        _adviceState.update {
            it.copy(advice = it.advice + content)
        }
    }

    /**
     * 完成AI建议接收
     */
    private fun completeAdvice() {
        _adviceState.update { it.copy(isLoading = false) }
    }

    /**
     * 处理错误
     */
    private fun handleError(message: String) {
        _adviceState.update {
            it.copy(
                isLoading = false,
                error = message
            )
        }
    }

    /**
     * 返回上一页
     */
    private fun navigateBack() {
        _navigateBack.value = true
    }
}

/**
 * AIViewModel工厂
 */
class AIViewModelFactory(
    private val repository: AIRepository,
    private val currentWeather: WeatherResponse?,
    private val forecast: List<Forecast>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AIViewModel::class.java)) {
            return AIViewModel(repository, currentWeather, forecast) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}