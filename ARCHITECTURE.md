# Архитектура проекта

## Обзор

Проект построен на принципах **Clean Architecture** с разделением на модули по слоям ответственности.

## Диаграмма зависимостей

```
┌─────────────────────────────────────────────┐
│              Presentation Layer              │
│                                              │
│  ┌──────────────┐         ┌──────────────┐  │
│  │   :mobile    │         │    :wear     │  │
│  │  (Android)   │         │  (Wear OS)   │  │
│  └──────┬───────┘         └──────┬───────┘  │
│         │                        │           │
└─────────┼────────────────────────┼───────────┘
          │                        │
          ▼                        ▼
┌─────────────────────────────────────────────┐
│               Domain Layer                   │
│                 :domain                      │
│  ┌─────────────────────────────────────┐   │
│  │  • Entities (Message, ChatRequest)   │   │
│  │  • Use Cases (SendMessageUseCase)    │   │
│  │  • Repository Interfaces             │   │
│  └─────────────────────────────────────┘   │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│                Data Layer                    │
│                  :data                       │
│  ┌─────────────────────────────────────┐   │
│  │  • Repository Implementations        │   │
│  │  • API Services (YandexGptApi)       │   │
│  │  • DTOs & Mappers                    │   │
│  │  • Network Module (Ktor)             │   │
│  └─────────────────────────────────────┘   │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│               Core Layer                     │
│                 :core                        │
│  ┌─────────────────────────────────────┐   │
│  │  • DI Scopes & Annotations           │   │
│  │  • DispatchersProvider               │   │
│  │  • Result Wrapper                    │   │
│  │  • Common Utilities                  │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

## Модули

### :core

**Тип:** Android Library  
**Назначение:** Общие утилиты и базовые компоненты

**Содержимое:**

- `di/` - DI аннотации (`@AppScope`)
- `util/` - Утилиты (`Result<T>`)
- `DispatchersProvider` - абстракция для Coroutine Dispatchers

**Зависимости:** Нет

### :domain

**Тип:** Kotlin JVM Library  
**Назначение:** Бизнес-логика приложения

**Содержимое:**

- `model/` - Domain entities
  - `Message` - модель сообщения
  - `ChatRequest` - запрос к чату
- `repository/` - интерфейсы репозиториев
  - `ChatRepository`
- `usecase/` - use cases
  - `SendMessageUseCase`

**Зависимости:** Нет (чистый Kotlin)

### :data

**Тип:** Android Library  
**Назначение:** Работа с данными и внешними API

**Содержимое:**

- `remote/api/` - API клиенты
  - `YandexGptApi` - интеграция с Yandex GPT
- `remote/dto/` - Data Transfer Objects
  - `YandexGptRequest`, `YandexGptResponse`
- `repository/` - реализации репозиториев
  - `ChatRepositoryImpl`
- `di/` - DI модули
  - `NetworkModule` - настройка Ktor
  - `DataModule` - предоставление репозиториев

**Зависимости:** `:domain`, `:core`

### :mobile

**Тип:** Android Application  
**Назначение:** Android приложение с Material Design 3

**Содержимое:**

- `presentation/chat/` - UI чата
  - `ChatViewModel` - ViewModel
  - `ChatScreen` - Composable UI
- `di/` - DI компоненты
  - `AppComponent` - Dagger компонент
  - `ViewModelModule` - ViewModel factory

**Зависимости:** `:domain`, `:data`, `:core`

### :wear

**Тип:** Android Application  
**Назначение:** Wear OS приложение

**Содержимое:**

- `presentation/chat/` - UI чата для Wear
  - `WearChatViewModel` - ViewModel
  - `WearChatScreen` - Wear Compose UI
- `presentation/di/` - DI компоненты
  - `WearAppComponent` - Dagger компонент
  - `WearViewModelModule` - ViewModel factory

**Зависимости:** `:domain`, `:data`, `:core`

## Принципы архитектуры

### 1. Dependency Rule

Зависимости направлены от внешних слоёв к внутренним:

- `mobile/wear → data → domain → core`
- Внутренние слои ничего не знают о внешних

### 2. Single Responsibility

Каждый модуль имеет одну чёткую ответственность:

- **domain**: бизнес-логика
- **data**: получение данных
- **presentation**: UI и взаимодействие с пользователем

### 3. Dependency Inversion

Используются интерфейсы (абстракции) вместо конкретных реализаций:

- `ChatRepository` - интерфейс в domain
- `ChatRepositoryImpl` - реализация в data

## Data Flow

```
User Action (UI)
    ↓
ViewModel
    ↓
Use Case (Domain)
    ↓
Repository Interface (Domain)
    ↓
Repository Implementation (Data)
    ↓
API Service (Data)
    ↓
Network (Ktor)
    ↓
Yandex GPT API
```

## Dependency Injection

### Dagger 2 Setup

**Компоненты:**

- `AppComponent` (mobile) - корневой компонент для Android
- `WearAppComponent` (wear) - корневой компонент для Wear OS

**Модули:**

- `CoreModule` - предоставляет `DispatchersProvider`
- `DataModule` - предоставляет `Repository`, `API`
- `NetworkModule` - настраивает `HttpClient`
- `ViewModelModule` - предоставляет `ViewModelFactory`

**Scope:**

- `@AppScope` - Singleton scope для всего приложения

### Граф зависимостей

```
AppComponent
    │
    ├─ CoreModule
    │    └─ DispatchersProvider
    │
    ├─ DataModule
    │    ├─ ChatRepository
    │    ├─ YandexGptApi
    │    └─ API credentials
    │
    ├─ NetworkModule
    │    ├─ HttpClient
    │    └─ Json
    │
    └─ ViewModelModule
         └─ ViewModelFactory
              └─ ChatViewModel
```

## Асинхронность

### Coroutines + Flow

**ViewModel:**

```kotlin
private val _uiState = MutableStateFlow(ChatUiState())
val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
```

**Use Case:**

```kotlin
suspend operator fun invoke(request: ChatRequest): Result<String>
```

**Repository:**

```kotlin
suspend fun sendMessage(request: ChatRequest): Result<String>
```

### Dispatchers

Используется `DispatchersProvider` для тестируемости:

- `Main` - UI операции
- `IO` - сетевые запросы, БД
- `Default` - CPU-intensive операции

## UI Layer

### Mobile (Material Design 3)

**Compose компоненты:**

- `ChatScreen` - основной экран чата
- `MessageItem` - элемент сообщения
- `MessageInput` - поле ввода

**Features:**

- Автопрокрутка к новым сообщениям
- Loading индикатор
- Error handling через Snackbar
- Поддержка тёмной темы

### Wear OS (Wear Compose)

**Compose компоненты:**

- `WearChatScreen` - чат для Wear
- `WearMessageItem` - сообщение для Wear
- Quick replies - быстрые ответы

**Features:**

- Круглый экран
- `ScalingLazyColumn` для прокрутки
- `TimeText` и `Vignette`
- Оптимизированный UI для маленьких экранов

## Тестирование

### Слои для тестирования

1. **Domain Layer** - unit тесты use cases
2. **Data Layer** - unit тесты repositories + mock API
3. **Presentation** - UI тесты Compose

### Преимущества архитектуры для тестирования

- Domain layer - чистый Kotlin, легко тестировать
- Интерфейсы - легко создать mock/fake
- DI - легко заменить зависимости
- ViewModel - тестируется отдельно от UI

## Build Configuration

### Gradle Version Catalog

Все зависимости определены в `gradle/libs.versions.toml`:

- Централизованное управление версиями
- Типобезопасность
- Легко обновлять

### Build Config

API ключи загружаются из `local.properties`:

```kotlin
buildConfigField("String", "YANDEX_API_KEY", "...")
```

## Лучшие практики

1. ✅ Separation of Concerns
2. ✅ SOLID принципы
3. ✅ Dependency Inversion
4. ✅ Single Source of Truth (StateFlow)
5. ✅ Unidirectional Data Flow
6. ✅ Error Handling
7. ✅ Resource Management
8. ✅ Type Safety

## Возможные улучшения

1. **Кэширование** - добавить Room для offline режима
2. **Пагинация** - для истории сообщений
3. **Unit тесты** - покрытие тестами
4. **CI/CD** - автоматизация сборки
5. **Feature флаги** - управление фичами
6. **Analytics** - отслеживание событий
7. **Crash reporting** - Firebase Crashlytics

## Заключение

Данная архитектура обеспечивает:

- ✅ Масштабируемость
- ✅ Тестируемость
- ✅ Поддерживаемость
- ✅ Переиспользование кода
- ✅ Независимость от фреймворков
