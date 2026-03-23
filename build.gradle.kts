// Пустой или с общими настройками
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}