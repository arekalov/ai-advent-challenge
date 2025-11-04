# Быстрый старт

## Шаги для запуска проекта

### 1. Клонируйте репозиторий

```bash
git clone <repository-url>
cd aiadvent1
```

### 2. Настройте API ключи

Откройте файл `local.properties` (создайте, если нет) и добавьте:

```properties
YANDEX_API_KEY=ваш_api_ключ_yandex_cloud
YANDEX_FOLDER_ID=ваш_folder_id
```

### 3. Синхронизируйте Gradle

```bash
./gradlew sync
```

или через Android Studio: **File → Sync Project with Gradle Files**

### 4. Запустите приложение

#### Mobile (Android):

1. Выберите конфигурацию `:mobile`
2. Запустите на эмуляторе или устройстве

#### Wear OS:

1. Создайте Wear OS эмулятор
2. Выберите конфигурацию `:wear`
3. Запустите приложение

## Как получить API ключи Yandex Cloud

1. Перейдите на [Yandex Cloud Console](https://console.cloud.yandex.ru/)
2. Создайте или выберите существующий каталог (folder)
3. Скопируйте **Folder ID** из URL или настроек каталога
4. Создайте сервисный аккаунт:
   - Перейдите в **IAM → Сервисные аккаунты**
   - Нажмите **Создать сервисный аккаунт**
   - Назначьте роль `ai.languageModels.user`
5. Создайте API-ключ:
   - Откройте сервисный аккаунт
   - Нажмите **Создать новый ключ → API-ключ**
   - Скопируйте ключ (он показывается только один раз!)

## Проверка работы

После запуска вы увидите:

- Приветственное сообщение от AI-анекдотчика
- Поле для ввода текста
- После отправки сообщения - ответ от AI

## Решение проблем

### Ошибка "API Key not found"

Проверьте, что `local.properties` содержит корректные значения и файл находится в корне проекта.

### Ошибка сети

Убедитесь, что:

- У вас есть интернет соединение
- API ключ активен
- Folder ID корректный

### Gradle sync failed

Попробуйте:

```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

## Структура модулей

```
:core → :domain → :data → :mobile
                      ↘ :wear
```

- **core**: DI аннотации, утилиты
- **domain**: Бизнес-логика (use cases)
- **data**: API, репозитории
- **mobile**: Android UI
- **wear**: Wear OS UI

## Дополнительно

- Полная документация: [README.md](README.md)
- Технологии: Kotlin, Compose, Dagger 2, Ktor, Coroutines
- Архитектура: Clean Architecture + Multi-module
