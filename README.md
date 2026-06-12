# CybLight-Android

Мобильное приложение CybLight для устройств Android — вход в аккаунт, список друзей и сообщения.

## Возможности

- Вход через `api.cyblight.org` (логин, пароль, Cloudflare Turnstile)
- Поддержка 2FA
- Список друзей с онлайн-статусом
- Диалоги и отправка сообщений
- 3 языка: русский, украинский, английский
- Брендинг CybLight (логотип и цвета сайта)

## Стек

- Kotlin + Jetpack Compose + Material 3
- Retrofit / OkHttp
- DataStore (сессия и язык)
- minSdk 26, targetSdk 34

## Структура

```text
app/src/main/java/org/cyblight/android/
  data/           — API, репозитории, сессия
  ui/login/       — экран входа и 2FA
  ui/friends/     — список друзей
  ui/messages/    — диалоги и чат
  i18n/           — переключение языка
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

| Эндпоинт                       | Назначение         |
| ------------------------------ | ------------------ |
| `POST /auth/login`             | Вход               |
| `POST /auth/2fa/verify`        | 2FA                |
| `GET /auth/me`                 | Проверка сессии    |
| `GET /friends/list`            | Друзья             |
| `GET /messages/unread-summary` | Непрочитанные      |
| `GET /messages/{friendId}`     | История чата       |
| `POST /messages/send`          | Отправка сообщения |

Базовый URL: `https://api.cyblight.org`

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
3. Создайте GitHub Release с тегом `v0.3.0` (версия в теге должна быть выше текущей)
4. Прикрепите APK к релизу (файл с расширением `.apk`, например `cyblight-android-v0.3.0.apk`)
5. В описании релиза используйте шаблон из `release-notes-v0.4.9.md`:
   - блок **Что нового** — только для пользователей, без технических инструкций
   - блок **Установка** со ссылкой: `https://cyblight.org/ru/downloads/`
   - обновление на GitHub: `gh release edit vX.Y.Z --notes-file release-notes-vX.Y.Z.md`

Пользователь, нажавший «Позже», не увидит то же уведомление снова, пока не выйдет более новая версия.

## Лицензия

MIT — см. [LICENSE](LICENSE).
