# AI Advent Challenge - AI Agent Chat

Приложение для Android и Wear OS с интеграцией Yandex GPT API.

## Архитектура

Проект использует **Clean Architecture** с многомодульной структурой:

### Модули

- **`:domain`** - Бизнес-логика (Use Cases, Entities, Repository интерфейсы)
- **`:data`** - Реализация репозиториев, API клиенты, Data Transfer Objects
- **`:core`** - Общие утилиты, DI scope annotations, DispatchersProvider
- **`:mobile`** - Android мобильное приложение (Compose UI)
- **`:wear`** - Wear OS приложение (Wear Compose UI)

### Технологический стек

- **Kotlin** - основной язык программирования
- **Jetpack Compose** - Modern UI для Android
- **Wear Compose** - UI для Wear OS
- **Coroutines** - асинхронность
- **Dagger 2** - Dependency Injection
- **Ktor Client** - HTTP клиент для работы с API
- **Kotlinx Serialization** - сериализация JSON

## Настройка проекта

### 1. Добавьте API ключи

Создайте файл `local.properties` в корне проекта (если его нет) и добавьте ваши credentials:

```properties
# Yandex Cloud API Key
YANDEX_API_KEY=ваш_api_ключ

# Yandex Cloud Folder ID
YANDEX_FOLDER_ID=ваш_folder_id
```

**Важно:** Файл `local.properties` уже добавлен в `.gitignore` и не будет закоммичен в репозиторий.

Пример файла можно найти в `local.properties.example`.

### 2. Получение API ключа Yandex Cloud

1. Зарегистрируйтесь в [Yandex Cloud](https://cloud.yandex.ru/)
2. Создайте сервисный аккаунт
3. Получите API ключ для YandexGPT
4. Запишите Folder ID вашего проекта

Документация: https://cloud.yandex.ru/docs/iam/concepts/authorization/api-key

### 3. Синхронизация Gradle

```bash
./gradlew sync
```

## Запуск приложения

### Mobile приложение

1. Откройте проект в Android Studio
2. Выберите конфигурацию `:mobile`
3. Запустите на эмуляторе или физическом устройстве (API 26+)

### Wear OS приложение

1. Создайте Wear OS эмулятор или подключите Wear OS устройство
2. Выберите конфигурацию `:wear`
3. Запустите приложение

## Особенности реализации

### Mobile приложение

- **Material Design 3** с поддержкой светлой/темной темы
- Адаптивный чат интерфейс с автопрокруткой
- Плавная анимация отправки сообщений
- Обработка ошибок через Snackbar

### Wear OS приложение

- **Wear Material Design** с поддержкой круглых экранов
- Quick replies для быстрых ответов
- Оптимизированный UI для маленьких экранов
- Time Text и Vignette для лучшего UX

### Clean Architecture

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│    (Mobile & Wear UI, ViewModels)       │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│           Domain Layer                   │
│    (Use Cases, Entities, Interfaces)    │
└─────────────┬───────────────────────────┘
              │
┌─────────────▼───────────────────────────┐
│           Data Layer                     │
│  (Repository Impl, API, DTOs)           │
└─────────────────────────────────────────┘
```

### Dependency Injection

Используется **Dagger 2** с компонентной архитектурой:

- `AppComponent` для mobile
- `WearAppComponent` для wear
- Модули: `CoreModule`, `DataModule`, `NetworkModule`, `ViewModelModule`

## API Integration

Приложение интегрировано с **Yandex GPT API**:

- Endpoint: `https://llm.api.cloud.yandex.net/foundationModels/v1/completion`
- Модель: `yandexgpt-lite`
- JSON Schema валидация ответов
- Поддержка conversation history

### Пример запроса

```kotlin
{
  "modelUri": "gpt://YOUR_FOLDER_ID/yandexgpt-lite",
  "completionOptions": {
    "stream": false,
    "temperature": 0.8,
    "maxTokens": 3000
  },
  "messages": [
    {
      "role": "system",
      "text": "System prompt..."
    },
    {
      "role": "user",
      "text": "User message"
    }
  ]
}
```

## Структура проекта

```
aiadvent1/
├── core/                    # Общие утилиты и DI
│   └── src/main/java/
│       └── com/arekalov/aiadvent1/core/
│           ├── di/          # DI annotations
│           └── util/        # Result wrapper
├── domain/                  # Бизнес логика
│   └── src/main/java/
│       └── com/arekalov/aiadvent1/domain/
│           ├── model/       # Domain entities
│           ├── repository/  # Repository interfaces
│           └── usecase/     # Use cases
├── data/                    # Данные и API
│   └── src/main/java/
│       └── com/arekalov/aiadvent1/data/
│           ├── di/          # Data DI modules
│           ├── remote/      # API & DTOs
│           └── repository/  # Repository implementations
├── mobile/                  # Android UI
│   └── src/main/java/
│       └── com/arekalov/aiadvent1/
│           ├── di/          # Mobile DI
│           └── presentation/# UI & ViewModels
└── wear/                    # Wear OS UI
    └── src/main/java/
        └── com/arekalov/aiadvent1/presentation/
            ├── di/          # Wear DI
            └── chat/        # Chat UI
```

## Требования

- **Android Studio**: Hedgehog (2023.1.1) или новее
- **Gradle**: 8.0+
- **JDK**: 11
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Kotlin**: 2.0.21

## Лицензия

Проект создан для AI Advent Challenge от Алексея Гладкова.

## Автор

Ваше имя

---

**Note:** Не забудьте добавить API ключи перед запуском!
