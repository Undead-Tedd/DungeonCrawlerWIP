plugins {
    id("java")
    id("com.runemate") version "1.5.0"
}

runemate {
    devMode = true
    autoLogin = true
    debug = true

    manifests {
        create("The Dungeon Crawler") {
            mainClass = "fighter.DungeonCrawler"
            tagline = "Our first bot, of course it was this difficult"
            description = "Runs the Gauntlet, I hope"
            internalId = "DungeonCrawler"
            version = "1.0"
        }

        group = "com.runemate.undead_tedd"
        version = "1.0-SNAPSHOT"

        repositories {
            mavenCentral()
            maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        }

        dependencies {
            implementation(files("libs/runemate-game-api.jar")) // RuneMate API
            testImplementation(platform("org.junit:junit-bom:5.10.0"))
            testImplementation("org.junit.jupiter:junit-jupiter")
            implementation("org.openjfx:javafx-controls:17.0.1")
            implementation("org.openjfx:javafx-fxml:17.0.1")
        }

        tasks.test {
            useJUnitPlatform()
        }

        tasks.jar {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            manifest {
                attributes("Main-Class" to "fighter.DungeonCrawler")
            }

            from("src/main/resources") {
                include("**/*.fxml")
                include("manifest.xml")
            }
        }

        tasks.runClient {
            dependsOn(tasks.assemble)
        }
    }
}
