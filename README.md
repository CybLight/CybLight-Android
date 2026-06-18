# CybLight-Android

Мобильное приложение CybLight для Android: аккаунт, друзья, зашифрованные сообщения, безопасность и резервное копирование.

## Возможности

- Вход через `api.cyblight.org` (логин, пароль, passkey, Cloudflare Turnstile, 2FA)
- Главная: новости с сайта, проекты, баннер «Что нового», история версий
- Друзья, поиск, заявки, онлайн-статус
- Чаты: Signal-шифрование, ответы, реакции, закрепление, черновики, экспорт/импорт
- Безопасность: сессии, passkey, доверенные устройства, история входов
- Блокировка приложения (PIN / биометрия)
- Резервные копии ключей и чатов (файл + Google Drive)
- Push-уведомления о сообщениях и входах с другого устройства
- Пасхалки, автообновление с GitHub Releases
- 3 языка: русский, украинский, английский

## Стек

- Kotlin + Jetpack Compose + Material 3
- Retrofit / OkHttp, libsignal (Signal Protocol)
- DataStore (настройки, сессия, блокировка)
- WorkManager (фоновые опросы и автобэкап)
- Firebase Cloud Messaging (push)
- minSdk 26, targetSdk 34

## Структура проекта (для разработки)

Корень репозитория:

```text
CybLight-Android/
  app/build.gradle.kts     — версия, BuildConfig, зависимости, подпись release
  app/src/main/
    AndroidManifest.xml    — разрешения, FCM-сервис, FileProvider
    java/org/cyblight/android/
    res/                   — строки, иконки, темы
  keystore/                — release keystore (не в git)
  keystore.properties      — пароли keystore (не в git)
  release-notes-vX.Y.Z.md  — текст для GitHub Release
```

### Точки входа

| Файл | Назначение |
| ---- | ---------- |
| `MainActivity.kt` | Compose-дерево, навигация, диалоги, launchers (файлы, Google Sign-In, уведомления) |
| `CybLightApplication.kt` | Инициализация каналов уведомлений |
| `ui/AppViewModel.kt` | Состояние приложения, бизнес-логика, навигация между экранами |
| `data/ApiClient.kt` | Retrofit-клиент с токеном сессии |
| `data/api/CybLightApi.kt` | Все HTTP-эндпоинты |
| `data/api/ApiModels.kt` | DTO запросов/ответов |

### Пакеты `app/src/main/java/org/cyblight/android/`

```text
auth/                         — WebAuthn / passkey (Credential Manager)
crypto/                       — SignalCryptoManager, кэш расшифровки
crypto/backup/                — формат .cyblight-backup, шифрование ключей и чатов

data/
  api/                        — CybLightApi, ApiModels
  home/                       — контент главной, changelog, ChangelogLocalizedNotes
  preferences/                — AppPreferences (DataStore), ChatBackupFrequency, тема, жесты
  repository/                 — Auth, Friends, Messages, Sessions, Security, Easter
  session/                    — SessionManager (токен, userId, locale)

integrations/google_drive/    — OAuth, Drive API, GoogleDriveBackupService

notifications/
  CybLightMessagingService    — FCM: входящие push чатов
  LoginNotificationMonitor    — опрос истории входов
  MessageNotificationMonitor  — опрос непрочитанных (фон)
  NotificationHelper          — каналы и показ уведомлений
  PushMessagePreviewResolver  — расшифровка текста в push
  PushTokenRegistrar          — регистрация FCM-токена на API

security/                     — PIN-блокировка, биометрия, BackupPasswordStore

ui/
  AppViewModel.kt             — единый ViewModel
  login/                      — LoginScreen, TwoFactorScreen
  main/                       — MainScreen, нижние вкладки (MainTab)
  home/                       — HomeScreen, ChangelogScreen
  friends/                    — FriendsScreen
  messages/                   — MessagesScreen, ChatScreen, превью, форматирование
  profile/                    — ProfileScreen (свой и чужой)
  security/                   — SecurityScreen, Sessions, Passkeys, TrustedDevices, LoginHistory
  settings/                   — SettingsScreen, hub и подразделы (см. ниже)
  applock/                    — AppLockScreen
  easter/                     — EasterEggsScreen, LightCatcherGameDialog
  help/                       — HelpScreen
  components/                 — AppMenu, TurnstileWebView, CybTextField, AboutDialog
  navigation/                 — SwipeBackContainer
  theme/                      — CybLightTheme, цвета
  update/                     — диалоги проверки и установки обновления

update/                       — загрузка APK с GitHub, UpdateRepository, ApkInstaller
workers/                      — LoginNotificationWorker, MessageNotificationWorker, ChatBackupWorker
i18n/                         — LocaleManager
util/                         — PresenceFormatter, BugReport, SystemSettings, EasterLogger
```

### Навигация и экраны

Состояние навигации — в `AppViewModel` (`AppUiState`):

- `screen` — `Loading` | `Login` | `TwoFactor` | `Main`
- `detailScreen` — поверх основного UI: настройки, чат, профиль, безопасность и т.д.
- `selectedMainTab` — вкладки нижней панели: Home, Friends, Messages, Easter

`MainActivity` рендерит `detailScreen` поверх `MainScreen` (вкладки) или отдельные full-screen экраны.

**Нижние вкладки** (`ui/main/MainTab.kt`):

| Вкладка | Экран |
| ------- | ----- |
| Home | `ui/home/HomeScreen.kt` |
| Friends | `ui/friends/FriendsScreen.kt` |
| Messages | `ui/messages/MessagesScreen.kt` |
| Easter | `ui/easter/EasterEggsScreen.kt` |

**Детальные экраны** (`DetailScreen` в `AppViewModel.kt`):

| Экран | Файл |
| ----- | ---- |
| Настройки | `ui/settings/SettingsScreen.kt` |
| Чат | `ui/messages/ChatScreen.kt` (через `chatFriendId` в state) |
| Профиль | `ui/profile/ProfileScreen.kt` |
| Справка | `ui/help/HelpScreen.kt` |
| История версий | `ui/home/ChangelogScreen.kt` |
| Сессии, passkey, устройства, история входов | `ui/security/*.kt` |

**Разделы настроек** (`SettingsSection.kt` → `SettingsScreen.kt`):

| Раздел | Где реализован |
| ------ | -------------- |
| Hub (список) | `SettingsHubScreen.kt` |
| Безопасность | `SecurityScreen.kt` (вложен в настройки) |
| Блокировка и ключи | `SettingsSections.kt` + `SignalBackupSection.kt` |
| Сообщения | `SettingsChatsSection.kt` |
| Резервная копия чатов | `ChatBackupSettingsScreen.kt`, `GoogleAccountPickerDialog.kt` |
| Уведомления, внешний вид, жесты, фон | `SettingsSections.kt` |
| О приложении | `SettingsAboutSection` в `SettingsSections.kt` |

Меню приложения (⋮): `ui/components/AppMenu.kt` — настройки, обновления, баг-репорт, донат, выход. Справка и «О приложении» только через настройки.

### Данные и API

| Слой | Путь |
| ---- | ---- |
| Интерфейс API | `data/api/CybLightApi.kt` |
| Модели | `data/api/ApiModels.kt` |
| Авторизация | `data/repository/AuthRepository.kt` |
| Друзья | `data/repository/FriendsRepository.kt` |
| Сообщения + Signal | `data/repository/MessagesRepository.kt`, `crypto/SignalCryptoManager.kt` |
| Превью чатов (расшифровка) | `data/repository/ConversationPreviewEnricher.kt` |
| Безопасность (обзор) | `data/repository/SecurityRepository.kt` |
| Сессии | `data/repository/SessionsRepository.kt` |
| Сессия (токен) | `data/session/SessionManager.kt` |
| Локальные настройки | `data/preferences/AppPreferences.kt` |

Базовый URL и ключи задаются в `app/build.gradle.kts` → `BuildConfig` (`API_BASE_URL`, `LOGIN_URL`, `TURNSTILE_SITEKEY`, `GOOGLE_DRIVE_*_CLIENT_ID`).

### Ресурсы и локализация

```text
res/values/strings.xml       — русский (по умолчанию)
res/values-en/strings.xml    — английский
res/values-uk/strings.xml    — украинский
res/xml/locales_config.xml   — список локалей для Android 13+
res/values/themes.xml        — тема Material
```

Переключение языка: `i18n/LocaleManager.kt` + `AppPreferences` + `android:localeConfig`.

### Push и фоновые задачи

| Компонент | Файл |
| --------- | ---- |
| FCM-сервис | `notifications/CybLightMessagingService.kt` |
| Регистрация токена | `notifications/PushTokenRegistrar.kt` |
| Опрос входов (30 мин) | `workers/LoginNotificationWorker.kt` |
| Опрос сообщений | `workers/MessageNotificationWorker.kt` |
| Автобэкап в Drive | `workers/ChatBackupWorker.kt` |

Для расшифрованных push чатов backend (`login_BackEnd`) должен слать data-only payload с ciphertext — см. `src/push/fcm.ts`.

### Сборка и конфигурация

| Что | Где |
| --- | --- |
| Версия приложения | `app/build.gradle.kts` → `versionCode`, `versionName` |
| Подпись release | `keystore.properties` + `keystore/cyblight-release.jks` |
| Firebase / FCM | `app/google-services.json` |
| ProGuard | `app/proguard-rules.pro` (minify выключен) |

**OneDrive:** папка `app/build/` иногда блокируется синхронизацией. Перед сборкой:

```bat
gradlew.bat --stop
rmdir /s /q app\build
gradlew.bat assembleRelease
```

## Требования

- Android Studio Ladybug (2024.2+) или новее
- JDK 17+ (встроенный JBR из Android Studio подходит)
- Android SDK Platform 34

## Запуск

1. Откройте папку `CybLight-Android` в Android Studio.
2. Дождитесь синхронизации Gradle.
3. Запустите конфигурацию `app` на эмуляторе или устройстве.

Из командной строки (Windows), если `java` в PATH указывает на Java 8:

```bat
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew.bat assembleDebug
```

APK: `app\build\outputs\apk\debug\app-debug.apk`

## Установка APK

При установке с GitHub или скачанного файла Android может показать предупреждение о неизвестном источнике. Нажмите **«Всё равно установить»**.

Если обновление не ставится поверх старой версии, удалите предыдущую сборку и установите APK заново (подпись release отличается от ранних debug-сборок).

## Release-сборка (подписанный APK)

Release подписывается фиксированным keystore в `keystore/cyblight-release.jks`.

1. Скопируйте `keystore.properties.example` в `keystore.properties` и укажите пароли.
2. Если keystore ещё нет, создайте его (пароли — как в `keystore.properties`):

```bat
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
mkdir keystore
"%JAVA_HOME%\bin\keytool.exe" -genkeypair -v -keystore keystore\cyblight-release.jks -alias cyblight -keyalg RSA -keysize 2048 -validity 10000 -storepass YOUR_STORE_PASSWORD -keypass YOUR_STORE_PASSWORD -dname "CN=CybLight, OU=Mobile, O=CybLight, C=UA"
```

3. Соберите release:

```bat
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew.bat assembleRelease
```

APK: `app\build\outputs\apk\release\app-release.apk`

Файлы `keystore/` и `keystore.properties` не коммитятся — храните их в безопасном месте.

## API

Полный список эндпоинтов — в `data/api/CybLightApi.kt`. Основные группы:

| Группа | Примеры |
| ------ | ------- |
| Авторизация | `POST /auth/login`, `POST /auth/2fa/verify`, `GET /auth/me`, passkey |
| Друзья | `GET /friends/list`, `POST /friends/add` |
| Сообщения | `GET /messages/unread-summary`, `GET /messages/{friendId}`, `POST /messages/send` |
| Signal | `GET /crypto/keys/status`, `POST /crypto/keys/register` |
| Безопасность | `GET /auth/sessions`, `GET /auth/login-history`, passkey, trusted-devices |
| Push | `POST /push/register` |
| Пасхалки | `POST /auth/easter/*` |

Базовый URL: `https://api.cyblight.org` (см. `BuildConfig.API_BASE_URL`).

## Связанные репозитории

- [CybLight](https://github.com/CybLight/CybLight) — сайт
- [login](https://github.com/CybLight/login) — веб-аккаунты
- [login_BackEnd](https://github.com/CybLight/login_BackEnd) — API
- [Cyblight-Admin](https://github.com/CybLight/Cyblight-Admin) — админ-панель

## Обновление приложения

При запуске приложение проверяет последний релиз на GitHub:
`https://api.github.com/repos/CybLight/CybLight-Android/releases/latest`

Если версия новее установленной:

1. Показывается диалог «Доступно обновление»
2. Кнопка **Скачать** — загрузка APK с прогрессом
3. После загрузки кнопка **Обновить** — установка через системный установщик

### Публикация нового релиза

1. Увеличьте `versionName` и `versionCode` в `app/build.gradle.kts`
2. Соберите подписанный release APK: `gradlew.bat assembleRelease` (см. раздел «Release-сборка»)
3. Создайте GitHub Release с тегом `v0.8.0` (версия в теге должна быть выше текущей)
4. Прикрепите APK к релизу (например `cyblight-android-v0.8.0.apk`)
5. В описании релиза используйте шаблон из `release-notes-v0.8.0.md`:
   - блок **Что нового** — только для пользователей, без технических инструкций
   - блок **Установка** со ссылкой: `https://cyblight.org/ru/downloads/`
   - обновление на GitHub: `gh release edit vX.Y.Z --notes-file release-notes-vX.Y.Z.md`

Пользователь, нажавший «Позже», не увидит то же уведомление снова, пока не выйдет более новая версия.

## Лицензия

MIT — см. [LICENSE](LICENSE).
