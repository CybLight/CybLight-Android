package org.cyblight.android.data.home

object ChangelogLocalizedNotes {
    private val notes: Map<String, Map<String, String>> = mapOf(
        "0.8.0" to mapOf(
            "ru" to """
                Что нового

                - Настройки в новом стиле: разделы «Безопасность», «Блокировка и ключи», «Уведомления», «Внешний вид» и др.
                - Расшифровка превью зашифрованных сообщений в списке чатов и push-уведомлениях
                - По нажатию на уведомление о входе — переход в раздел «Сессии»
                - Можно скрыть баннер «Что нового» на главной
                - Резервное копирование чатов и ключей шифрования в Google Drive
                - Экспорт и импорт чатов в настройках
                - Разделители дат в чате и переход к цитируемому сообщению
                - Кнопка «Открепить» для закреплённого сообщения
                - Скрытие панели форматирования в чате

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Settings in a new style: Security, Lock & keys, Notifications, Appearance, and more
                - Decrypted previews for encrypted messages in chat list and push notifications
                - Tap sign-in alert to open Sessions
                - Hide the "What's new" banner on Home
                - Google Drive backup for chats and encryption keys
                - Export and import chats in settings
                - Date separators in chat and jump to quoted message
                - Unpin button for pinned messages
                - Hide formatting toolbar in chat

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Налаштування в новому стилі: розділи «Безпека», «Блокування та ключі», «Сповіщення», «Зовнішній вигляд» тощо
                - Розшифрування превʼю зашифрованих повідомлень у списку чатів і push-сповіщеннях
                - Натискання на сповіщення про вхід відкриває розділ «Сесії»
                - Можна приховати банер «Що нового» на головній
                - Резервне копіювання чатів і ключів шифрування в Google Drive
                - Експорт і імпорт чатів у налаштуваннях
                - Роздільники дат у чаті та перехід до цитованого повідомлення
                - Кнопка «Відкріпити» для закріпленого повідомлення
                - Приховування панелі форматування в чаті

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.7.0" to mapOf(
            "ru" to """
                Что нового

                - Подсказки к паролю резервной копии шифрования (минимум 8 символов)
                - Исправлена блокировка приложения: сохранение после убийства процесса, таймаут, кнопка «Назад»
                - Блокировка сразу после настройки PIN
                - Переход с биометрии на ввод PIN

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Hints for encryption backup password (at least 8 characters)
                - Fixed app lock: survives process death, timeout, and Back button handling
                - Lock screen right after PIN setup
                - Switch from biometrics to PIN entry

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Підказки до пароля резервної копії шифрування (мінімум 8 символів)
                - Виправлено блокування застосунку: збереження після завершення процесу, таймаут, кнопка «Назад»
                - Блокування одразу після налаштування PIN
                - Перехід з біометрії на введення PIN

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.9" to mapOf(
            "ru" to """
                Что нового

                - Исправлен сбой при экспорте резервной копии ключей шифрования
                - При экспорте открывается системный диалог «Сохранить как…» — можно выбрать папку и имя файла

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Fixed crash when exporting encryption key backup
                - Export opens the system Save as dialog — you can choose folder and file name

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Виправлено збій під час експорту резервної копії ключів шифрування
                - Під час експорту відкривається системний діалог «Зберегти як…» — можна вибрати папку та імʼя файлу

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.8" to mapOf(
            "ru" to """
                Что нового

                - Напоминание о резервной копии ключей шифрования в разделе чатов и в переписке
                - Закрытие напоминания в чате с сохранением выбора
                - Переход к резервной копии в настройках по кнопке в баннере

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Encryption key backup reminder in chats and conversations
                - Dismiss reminder in chat with your choice saved
                - Open backup settings from the banner button

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Нагадування про резервну копію ключів шифрування в розділі чатів і в переписці
                - Закриття нагадування в чаті зі збереженням вибору
                - Перехід до резервної копії в налаштуваннях кнопкою в банері

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.7" to mapOf(
            "ru" to """
                Что нового

                - Исправлен сбой при открытии настроек в v0.6.6

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Fixed settings crash in v0.6.6

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Виправлено збій під час відкриття налаштувань у v0.6.6

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.6" to mapOf(
            "ru" to """
                Что нового

                - Резервная копия шифрования: экспорт и импорт ключей Signal в файл .cyblight-backup
                - Один и тот же файл можно восстановить в приложении или в браузере (login.cyblight.org)
                - Помогает читать сообщения после входа с нового устройства или браузера

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Encryption backup: export and import Signal keys to a .cyblight-backup file
                - The same file can be restored in the app or browser (login.cyblight.org)
                - Helps read messages after signing in on a new device or browser

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Резервна копія шифрування: експорт і імпорт ключів Signal у файл .cyblight-backup
                - Той самий файл можна відновити в застосунку або в браузері (login.cyblight.org)
                - Допомагає читати повідомлення після входу з нового пристрою або браузера

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.5" to mapOf(
            "ru" to """
                Что нового

                - Исправлено: свои сообщения больше не превращаются в «зашифрованные» после отправки
                - Входящие зашифрованные сообщения корректно расшифровываются в чате

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Fixed: your own messages no longer turn into "encrypted" after sending
                - Incoming encrypted messages decrypt correctly in chat

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Виправлено: власні повідомлення більше не стають «зашифрованими» після надсилання
                - Вхідні зашифровані повідомлення коректно розшифровуються в чаті

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.4" to mapOf(
            "ru" to """
                Что нового

                - Свои сообщения в чате отображаются сразу с текстом, а не как «зашифрованные»
                - Исправлена отправка и чтение сообщений после установки сессии шифрования

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Your messages in chat show text immediately instead of "encrypted"
                - Fixed sending and reading messages after encryption session setup

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Власні повідомлення в чаті відображаються одразу з текстом, а не як «зашифровані»
                - Виправлено надсилання та читання повідомлень після встановлення сесії шифрування

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.3" to mapOf(
            "ru" to """
                Что нового

                - Исправлено отображение зашифрованных сообщений в чате
                - Сообщения с веб-версии и с телефона теперь читаются корректно
                - Свои отправленные сообщения больше не показываются как «не удалось расшифровать»

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Fixed display of encrypted messages in chat
                - Messages from web and phone now read correctly
                - Your sent messages no longer show as "could not decrypt"

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Виправлено відображення зашифрованих повідомлень у чаті
                - Повідомлення з вебверсії та з телефону тепер читаються коректно
                - Власні надіслані повідомлення більше не показуються як «не вдалося розшифрувати»

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.2" to mapOf(
            "ru" to """
                Что нового

                - Сквозное шифрование сообщений (Signal Protocol)
                - Кнопка «Регистрируйся!» на экране входа — как на сайте
                - Вставка из буфера обмена в поля логина, пароля и 2FA

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - End-to-end message encryption (Signal Protocol)
                - "Sign up!" button on login screen — like on the website
                - Paste from clipboard into login, password, and 2FA fields

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Наскрізне шифрування повідомлень (Signal Protocol)
                - Кнопка «Реєструйся!» на екрані входу — як на сайті
                - Вставка з буфера обміну в поля логіна, пароля та 2FA

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.1" to mapOf(
            "ru" to """
                Что нового

                - Ответы на сообщения: цитата в пузырьке и при наборе, как на сайте
                - Исправлено кодирование ответов из приложения (пробелы вместо «+» на сайте)
                - Расширенный набор эмодзи в стиле Telegram: 10 категорий, поиск
                - Пасхалка Developer Mode в списке приложения
                - Главная: баннер «Что нового» с сайта, подписи вкладок «Чаты» / «Защита»
                - Локализация главной на английском и украинском

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Message replies: quote in bubble and while typing, like on the website
                - Fixed reply encoding from the app (spaces instead of "+" on the website)
                - Expanded Telegram-style emoji: 10 categories, search
                - Developer Mode easter egg in the app list
                - Home: "What's new" banner from the site, tab labels "Chats" / "Protection"
                - Home screen localized in English and Ukrainian

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Відповіді на повідомлення: цитата в бульбашці та під час набору, як на сайті
                - Виправлено кодування відповідей із застосунку (пробіли замість «+» на сайті)
                - Розширений набір емодзі в стилі Telegram: 10 категорій, пошук
                - Пасхалка Developer Mode у списку застосунків
                - Головна: банер «Що нового» з сайту, підписи вкладок «Чати» / «Захист»
                - Локалізація головної англійською та українською

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
        "0.6.0" to mapOf(
            "ru" to """
                Что нового

                - Вкладка «Главная»: что нового в приложении, новости с cyblight.org, карусель проектов
                - Ссылка «Что было в предыдущих версиях» — история релизов
                - Превью последних сообщений и реакций в списке чатов
                - Черновики сообщений при выходе из чата
                - Свайп слева направо и кнопка «Назад» листают экраны; выход только с «Главной»
                - Настройки жестов: чувствительность, зона свайпа, поведение на главной вкладке
                - Ссылки безопасности (почта, пароль, 2FA) открывают login.cyblight.org

                Установка

                Скачайте APK ниже или на сайте: https://cyblight.org/ru/downloads/
            """.trimIndent(),
            "en" to """
                What's new

                - Home tab: what's new, news from cyblight.org, project carousel
                - "See previous versions" link — release history
                - Previews of latest messages and reactions in chat list
                - Message drafts when leaving a chat
                - Swipe right and Back button navigate screens; exit only from Home
                - Gesture settings: sensitivity, swipe zone, behavior on home tab
                - Security links (email, password, 2FA) open login.cyblight.org

                Installation

                Download the APK below or at: https://cyblight.org/en/downloads/
            """.trimIndent(),
            "uk" to """
                Що нового

                - Вкладка «Головна»: що нового в застосунку, новини з cyblight.org, карусель проєктів
                - Посилання «Що було в попередніх версіях» — історія релізів
                - Превʼю останніх повідомлень і реакцій у списку чатів
                - Чернетки повідомлень при виході з чату
                - Свайп зліва направо і кнопка «Назад» гортують екрани; вихід лише з «Головної»
                - Налаштування жестів: чутливість, зона свайпу, поведінка на головній вкладці
                - Посилання безпеки (пошта, пароль, 2FA) відкривають login.cyblight.org

                Встановлення

                Завантажте APK нижче або на сайті: https://cyblight.org/uk/downloads/
            """.trimIndent(),
        ),
    )

    fun resolve(version: String, locale: String, githubFallback: String): String {
        val lang = normalizeLocale(locale)
        notes[version]?.get(lang)?.let { return it }
        return translateReleaseNoteHeaders(githubFallback, lang)
    }

    private fun normalizeLocale(locale: String): String = when (locale.lowercase().take(2)) {
        "uk" -> "uk"
        "en" -> "en"
        else -> "ru"
    }

    private fun translateReleaseNoteHeaders(text: String, lang: String): String {
        if (lang == "ru" || text.isBlank()) return text
        val whatsNew = if (lang == "uk") "Що нового" else "What's new"
        val install = if (lang == "uk") "Встановлення" else "Installation"
        val downloadLine = if (lang == "uk") {
            "Завантажте APK нижче або на сайті:"
        } else {
            "Download the APK below or at:"
        }
        return text
            .replace("Что нового", whatsNew)
            .replace("Установка", install)
            .replace("Скачайте APK ниже или на сайте:", downloadLine)
            .replace("https://cyblight.org/ru/downloads/", "https://cyblight.org/$lang/downloads/")
    }
}
