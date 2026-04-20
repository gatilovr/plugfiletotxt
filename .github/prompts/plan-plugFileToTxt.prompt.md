**Plan: Аудит проекта IntelliJ-плагина**

TL;DR: Провести безопасный, поэтапный аудит IntelliJ-плагина: убрать артефакты Android, добавить тесты и статический анализ, глубже ревью кода экспорта/работы с FS и вызовов внешних команд. Подход: Quick wins → Medium → Deep.

**Steps**
1. Quick wins
- Добавить минимальные unit-тесты: создать `app/src/test/java` и покрыть критичные методы `ProjectExportService` и `ExportAction` (smoke tests).
- Убрать/прояснить Android-артефакты: удалить `app/src/main/AndroidManifest.xml` и убрать Android-зависимости в [gradle/libs.versions.toml](gradle/libs.versions.toml) если они не нужны.
- Интегрировать форматтер/линтер: добавить `spotless`/`checkstyle`/`ktlint` и запуск в CI.
- Зафиксировать JDK для сборки/рантайма (уточнить необходимость Java 21) и документировать в `README.md`.

2. Medium tasks
- Добавить CI-шаги для запуска юнит-тестов и статического анализа (обновить [/.github/workflows/ci.yml](.github/workflows/ci.yml)).
- Ревью безопасности работы с путями: проверить `exportFile`, `matchesGitignorePattern`, `openFolder` на path traversal, экранирование, права записи.
- Покрыть сценарии экспорта тестами: большие файлы, прерывание задачи, параллельный экспорт.
- Добавить dependency vulnerability scan (например `owasp-dependency-check`) в CI.

3. Deep tasks
- Полный security/concurrency audit `ProjectExportService`: review executor lifecycle, обработка InterruptedException, утечки потоков, atomic writes.
- Сравнение `matchesGitignorePattern` с поведением gitignore; при несоответствии — заменить на библиотеку.
- Matrix тестирования на нескольких версиях IntelliJ IDEA и JVM (включая поддерживаемые `sinceBuild`/`untilBuild`).
- Подготовка подписывания и публикации (если планируете выпуск в Marketplace): безопасное хранение ключей.

**Relevant files**
- [app/src/main/java/com/example/plugfiletotxt/service/ProjectExportService.java](app/src/main/java/com/example/plugfiletotxt/service/ProjectExportService.java)
- [app/src/main/java/com/example/plugfiletotxt/window/MyToolWindowFactory.java](app/src/main/java/com/example/plugfiletotxt/window/MyToolWindowFactory.java)
- [app/src/main/java/com/example/plugfiletotxt/actions/ExportAction.java](app/src/main/java/com/example/plugfiletotxt/actions/ExportAction.java)
- [app/src/main/resources/META-INF/plugin.xml](app/src/main/resources/META-INF/plugin.xml)
- [app/build.gradle.kts](app/build.gradle.kts)
- [gradle/libs.versions.toml](gradle/libs.versions.toml)
- [gradle.properties](gradle.properties)
- [/.github/workflows/ci.yml](.github/workflows/ci.yml)
- [app/proguard-rules.pro](app/proguard-rules.pro)
- [README.md](README.md)

**Verification**
1. Запустить CI: `verifyPluginConfiguration` + `buildPlugin` + unit tests + static analysis — все проходят.
2. Юнит-тесты для `ProjectExportService`: имитация больших файлов, проверка атомарности записи, проверка игнор-паттернов.
3. Security checks: автоматический scan зависимостей + ручной ревью `openFolder`/`Runtime.exec` и проверка на path traversal.
4. Compatibility: собрать и запустить плагин на IDEA 2024.3 и на ещё одной старшей/младшей поддерживаемой версии.

**Decisions / Assumptions**
- Предполагаю, что Android-артефакты не нужны (скорее шаблонные остатки). Если они нужны — откатить удаление и документировать причину.
- Java 21 заявлена в сборке; подтвердить, что целевые среды поддерживают Java 21.

**Further Considerations**
1. Хотите, чтобы я подготовил PR с quick wins (удаление Android-остатков + skeleton тестов + CI линтер)? Рекомендую именно этот порядок.
2. Нужен ли регресс-тест matrix для нескольких версий IDEA сейчас или отложить после quick wins?

---
План сохранён в `/memories/session/plan.md`. Скажите, с какого шага начать: quick wins PR, тесты или глубокий ревью `ProjectExportService`?