# DATASIGHT: keystore и подписанный AAB через Codemagic

Подготовлено по пользовательской шпаргалке `кодмеджик.md` и прочитанным примерам BG_NEW LOOK/docs/codemagic и исправленному HUN_SAFORA/codemagic.yaml. Рабочий upload key на Mac не создавался. Пользователь подтвердил: приложение ещё не публиковалось, зарегистрированного upload key нет.

## Фактические параметры

- Название продукта: DATASIGHT Prompt Builder; launcher: DATASIGHT.
- applicationId: `com.datasight.prompts`; модуль `app`.
- Java 17, Kotlin 2.2.20, AGP 8.13.2, Gradle 8.14.3.
- compileSdk 36, targetSdk 36, minSdk 26; versionCode 1, versionName 1.0.
- ABI: armeabi-v7a, arm64-v8a, x86, x86_64.
- Release signingConfig читает `release/upload-key.properties`. `storeFile` разрешается относительно корня проекта через rootProject.file, не относительно app/.
- Минификация выключена; mapping.txt не создаётся.
- Собственный приватный debug keystore не требуется: чистый checkout использует стандартную debug-подпись Android.
- В исходной рабочей папке нет Git-репозитория, локального release keystore и сертификата upload key. Создание remote, публикация исходников и запуск облака не выполнялись.

## 1. Отправьте исходники в Git

Codemagic читает файлы из выбранной ветки Git, а не из папки Mac. Сейчас корневой `codemagic.yaml` содержит **только одноразовый генератор** `datasight-create-upload-key-once`.

Пример команд из корня проекта (подставьте URL своего репозитория):

```sh
git init
git branch -M main
git add .gitignore README.md build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat gradle app/build.gradle.kts app/src docs/codemagic codemagic.yaml
git diff --cached --name-only
git commit -m "Configure DATASIGHT Codemagic signing"
git remote add origin YOUR_REPOSITORY_URL
git push -u origin main
```

Не используйте URL примера буквально. Проверьте список staged files перед commit. JKS/keystore, приватные properties, local.properties, generated-signing, APK/AAB и ZIP исключены .gitignore. Если файл уже отслеживается, ignore не удаляет его из истории. В текущей папке отслеживаемых файлов нет, так как Git ещё не инициализирован.

## 2. Одноразовая генерация ключа на Codemagic

1. Подключите Git-репозиторий в Codemagic, выберите конфигурацию codemagic.yaml и ветку main.
2. В Environment variables создайте группу **datasight_key_generation**.
3. В группу добавьте два значения и включите Secret у обоих:
   - `NEW_KEYSTORE_PASSWORD` — пароль хранилища;
   - `NEW_KEY_PASSWORD` — пароль записи ключа.
4. Сохраните оба пароля в своём менеджере паролей. В YAML и чат их записывать не нужно. Используйте пароли длиной не менее 6 символов (требование keytool).
5. Запустите вручную workflow **DATASIGHT - ONE-TIME create upload key**.
6. Скачайте два артефакта:
   - `generated-signing/datasight-upload.jks` — приватный keystore;
   - `generated-signing/upload_certificate.pem` — публичный сертификат.
7. Сохраните резервную копию JKS и пароли отдельно. Приватный JKS не добавляется в Git и не передаётся публичной ссылкой.

Параметры генерации: Java 17, Linux X2, JKS, RSA 4096, SHA256withRSA, 10000 дней, alias **upload**, subject **CN=DATASIGHT**. Юридическая форма, страна и другие не предоставленные данные в сертификат не добавлены.

Одна генерация создаёт и keystore, и upload key. Повторный запуск на другой чистой build-машине создаст другой ключ даже при тех же имени файла и alias. Этот workflow не предназначен для каждой версии приложения. Проверки секретов, umask 077 и запрет перезаписи файлов сохранены.

## 3. Загрузите JKS в identities

Team settings → codemagic.yaml settings → Code signing identities → Android keystores.

| Поле | Значение |
|---|---|
| File | скачанный datasight-upload.jks |
| Keystore password | значение NEW_KEYSTORE_PASSWORD |
| Key alias | upload |
| Key password | значение NEW_KEY_PASSWORD |
| Reference name | datasight_upload |

**upload** — имя записи внутри JKS. **datasight_upload** — ссылка на identity в Codemagic. Эти названия не взаимозаменяемы.

## 4. Переключите корневой YAML на AAB-сборку

После успешной генерации, скачивания и загрузки identity:

```sh
cp docs/codemagic/codemagic.yaml codemagic.yaml
git add codemagic.yaml docs/codemagic app/build.gradle.kts .gitignore
git commit -m "Enable signed DATASIGHT AAB builds"
git push
```

Выберите в Codemagic новую ревизию ветки и запустите **DATASIGHT - Signed AAB**, workflow ID `datasight-aab`.

Обычный workflow:

1. Находит sdkmanager по полному пути, выбирая максимальный Pkg.Revision среди установленных command-line tools. Не полагается на PATH или название latest.
2. Устанавливает точные пакеты **platforms;android-36** и **build-tools;36.0.0**. Их наличие проверено в официальном repository2-3.xml Google; пакет android-37.0 из чужого проекта не скопирован.
3. Codemagic identity предоставляет `CM_KEYSTORE_PATH`, `CM_KEYSTORE_PASSWORD`, `CM_KEY_ALIAS`, `CM_KEY_PASSWORD`.
4. prepare_signing.py создаёт `release/signing/upload-keystore.jks` и `release/upload-key.properties` с правами 600. Отказывается перезаписывать существующие файлы; Java Properties корректно экранирует Unicode и спецсимволы. Значения секретов не печатаются.
5. Выполняет:

```sh
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:bundleRelease
python3 docs/codemagic/verify_aab.py app/build/outputs/bundle/release/app-release.aab
```

6. Verifier проверяет JAR-подпись, сертификат выбранной identity, все четыре ABI и отсутствие приватных signing-файлов внутри AAB.

Не включайте set -x / printenv и не выводите содержимое приватных properties. Release без signing properties не подменяется debug-подписью; задача checkUploadSigning перед упаковкой release отклонит отсутствующие signing properties или keystore.

## 5. Результат обычного билда

Скачайте `app-release.aab`, отчёт lint и отчёт unit tests из артефактов Codemagic. Обычный workflow не возвращает JKS и не публикует в Google Play. mapping.txt отсутствует при текущем isMinifyEnabled=false.

Для нового приложения App signing key можно оставить Google Play; ваш upload key используется для загрузки. Перед дальнейшими загрузками увеличивайте versionCode, если прежнее значение уже использовано. Сейчас versionCode 1 сохранён, поскольку пользователь подтвердил отсутствие публикации.

## Проверки и границы

`local-validation.json` содержит фактические результаты. `validate_local.py` проверяет YAML/bash, обязательные CM_*, права и пути, отказ перезаписи, Java Properties roundtrip, mock SDK и verifier на подписанных/неподписанных тестовых архивах. Флаг `--real-bundle` дополнительно собирает настоящий release AAB в временной чистой копии с одноразовым fixture-ключом; всё приватное и этот тестовый AAB удаляются. Это не рабочий ключ для Play и не артефакт для публикации.

Для повторения локальных helper checks нужен PyYAML в отдельном Python-окружении, JAVA_HOME с JDK 17; настоящая тестовая AAB-сборка также требует Android SDK и Gradle. Python helpers CI используют только стандартную библиотеку, PyYAML для облачной сборки не нужен.

Облачная генерация и облачная AAB-сборка пока **NOT RUN**. Успех локальной проверки не означает успех Codemagic; его подтверждают только статус конкретного build, commit и скачанные облачные артефакты.

Официальные источники:
- https://docs.codemagic.io/yaml-code-signing/signing-android/
- https://docs.codemagic.io/yaml-quick-start/building-a-native-android-app/
- https://developer.android.com/studio/publish/app-signing
- https://dl.google.com/android/repository/repository2-3.xml
