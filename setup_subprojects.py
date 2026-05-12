import os
import shutil

root_dir = r"d:\Minecraft\projects\MyuLib-mc"
core_dir = os.path.join(root_dir, "myulib-core")
framework_dir = os.path.join(root_dir, "myulib-framework")

def main():
    root_build_file = os.path.join(root_dir, "build.gradle")
    
    # 1. Restore root build.gradle to original but clear its loom mods and sourceSets
    # Actually, root project in a multi-project Fabric setup usually doesn't need Loom if it's just a container.
    # But let's just make it a container.
    root_build_content = """plugins {
    id 'maven-publish'
}

allprojects {
    version = project.mod_version
    group = project.maven_group
}
"""
    with open(root_build_file, "w", encoding="utf-8") as f:
        f.write(root_build_content)

    # 2. Subproject build.gradle template
    subproject_template = """plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
    id 'maven-publish'
}

base {
    archivesName = project.name
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven {
        name = "Modrinth"
        url = "https://api.modrinth.com/maven"
        content { includeGroup "maven.modrinth" }
    }
}

loom {
    splitEnvironmentSourceSets()
    mods {
        "${project.name}" {
            sourceSet sourceSets.main
            sourceSet sourceSets.client
        }
    }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"

    testImplementation "org.junit.jupiter:junit-jupiter:5.10.0"
    testImplementation "org.junit.platform:junit-platform-suite-engine:1.10.0"
    testImplementation "org.mockito:mockito-core:5.12.0"
    testRuntimeOnly "org.junit.platform:junit-platform-launcher"
    
    // REPLACE_DEPS
}

tasks.named('test') {
    useJUnitPlatform()
}

processResources {
    def expandProps = [
            "version": project.version
    ]
    inputs.properties expandProps
    filesMatching("fabric.mod.json") {
        expand expandProps
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 25
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

jar {
    inputs.property "archivesName", base.archivesName
    from("../../LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}
"""

    # Core build.gradle
    core_build = subproject_template.replace("// REPLACE_DEPS", "")
    with open(os.path.join(core_dir, "build.gradle"), "w", encoding="utf-8") as f:
        f.write(core_build)

    # Framework build.gradle
    framework_build = subproject_template.replace("// REPLACE_DEPS", "implementation project(':myulib-core')")
    with open(os.path.join(framework_dir, "build.gradle"), "w", encoding="utf-8") as f:
        f.write(framework_build)

if __name__ == '__main__':
    main()
