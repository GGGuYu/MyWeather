# 🌤️ MyWeather - 智能天气助手

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/API-26%2B-brightgreen?style=for-the-badge" alt="API 26+">
</p>

<p align="center">
  <b>一款现代化的 Android 天气应用，结合 AI 智能提供个性化穿衣建议</b>
</p>

<p align="center">
  <a href="#-功能特性">功能特性</a> •
  <a href="#-技术栈">技术栈</a> •
  <a href="#-项目结构">项目结构</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-截图预览">截图预览</a>
</p>

---

## ✨ 功能特性

### 🌡️ 实时天气
- **精准定位** - 自动获取当前位置的天气信息
- **详细数据** - 温度、体感温度、湿度、风速、气压
- **空气质量** - 实时 AQI 指数及健康建议

### 📅 天气预报
- **24小时预报** - 逐小时天气变化趋势
- **5日预报** - 未来5天的天气预测
- **智能分组** - 自动分组显示每日最高/最低温度

### 🤖 AI 穿衣建议
- **智能分析** - 基于当前天气 + 未来12小时趋势
- **个性化建议** - 上衣、裤子、鞋子搭配推荐
- **贴心提醒** - 是否需要带伞、外套、防晒等
- **活动建议** - 适合室内/户外活动推荐

### 🎨 精美界面
- **Material Design 3** - 遵循最新设计规范
- **动态主题** - 自适应系统主题
- **流畅动画** - 优雅的过渡和加载效果
- **响应式布局** - 完美适配各种屏幕尺寸

---

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| **编程语言** | Kotlin |
| **UI 框架** | Jetpack Compose |
| **架构模式** | MVVM + Repository |
| **状态管理** | StateFlow / Flow |
| **网络请求** | Retrofit + OkHttp |
| **JSON 解析** | Gson |
| **图片加载** | Coil |
| **导航** | Navigation Compose |
| **定位服务** | Google Play Services Location |
| **AI 服务** | Moonshot AI API |
| **天气数据** | OpenWeatherMap API |

---

## 📁 项目结构

```
app/src/main/java/com/ixuea/course/weather/
├── MainActivity.kt                 # 应用入口
├── config/
│   └── Config.kt                   # 配置常量
├── data/
│   ├── api/
│   │   └── WeatherApiService.kt    # 天气 API 接口
│   ├── location/
│   │   └── LocationClient.kt       # 定位服务
│   ├── model/
│   │   ├── WeatherResponse.kt      # 天气数据模型
│   │   ├── AIAdviceModels.kt       # AI 建议模型
│   │   └── ...                     # 其他模型
│   └── repository/
│       ├── WeatherRepository.kt    # 天气数据仓库
│       └── AIRepository.kt         # AI 建议仓库
├── ui/
│   ├── WeatherScreen.kt            # 天气主界面
│   ├── AIScreen.kt                 # AI 建议界面
│   ├── WeatherViewModel.kt         # 天气 ViewModel
│   ├── AIViewModel.kt              # AI ViewModel
│   ├── theme/                      # 主题配置
│   └── navigation/                 # 导航配置
└── utils/
    ├── AQICalculator.kt            # AQI 计算工具
    └── DateFormatter.kt            # 日期格式化
```

---

## 🚀 快速开始

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11 或更高版本
- Android SDK API 26+
- OpenWeatherMap API Key
- Moonshot AI API Key

### 配置步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/yourusername/MyWeather.git
   cd MyWeather
   ```

2. **创建 local.properties**
   
   在项目根目录创建 `local.properties` 文件：
   ```properties
   # OpenWeatherMap API Key
   # 获取地址：https://openweathermap.org/api
   WEATHER_API_KEY=your_openweather_api_key
   
   # Moonshot AI API Key
   # 获取地址：https://platform.moonshot.cn/
   MOONSHOT_API_KEY=your_moonshot_api_key
   ```

3. **同步并构建**
   
   打开 Android Studio，点击 **Sync Project with Gradle Files**，然后运行项目。

---

## 📱 截图预览

<p align="center">
  <i>（截图待添加）</i>
</p>

---

## 🔧 核心代码亮点

### 1️⃣ 状态驱动的 UI 设计
```kotlin
// 根据状态自动切换界面
when {
    !permissionState.hasAnyLocationPermission() -> PermissionRationaleView(...)
    locationState.isLoading || weatherState.isLoading -> LoadingScreen()
    locationState.error != null -> ErrorScreen(...)
    weatherState.currentWeather != null -> WeatherContent(...)
}
```

### 2️⃣ 响应式数据流
```kotlin
// 使用 Flow 处理异步数据
repository.generateAdvice(request)
    .onStart { /* 开始加载 */ }
    .catch { e -> /* 错误处理 */ }
    .collectLatest { content -> /* 更新 UI */ }
```

### 3️⃣ AI Prompt 工程
```kotlin
// 智能构建 AI 提示词
val prompt = """
    你是一位专业的穿衣顾问，请根据以下天气信息给出穿衣和出门建议：
    
    当前天气：
    - 地点：${weather.name}
    - 温度：${weather.main.temp.toInt()}°C
    - 体感温度：${weather.main.feelsLike.toInt()}°C
    
    未来12小时预报：...
    
    请提供穿衣建议、出门准备、活动建议和特别提醒。
""".trimIndent()
```

---

## 📚 相关文档

- [Jetpack Compose 官方文档](https://developer.android.com/jetpack/compose)
- [OpenWeatherMap API 文档](https://openweathermap.org/api)
- [Moonshot AI 文档](https://platform.moonshot.cn/docs)
- [Kotlin Flow 指南](https://kotlinlang.org/docs/flow.html)
- [Material Design 3](https://m3.material.io/)

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

---

## 📄 开源许可

```
Copyright (c) 2025 MyWeather Contributors

Licensed under the MIT License (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://opensource.org/licenses/MIT

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

<p align="center">
  Made with ❤️ and ☕ by <a href="https://github.com/yourusername">Your Name</a>
</p>

<p align="center">
  ⭐ Star this repo if you find it helpful!
</p>
