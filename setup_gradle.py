import os
import json
import shutil

root_dir = r"d:\Minecraft\projects\MyuLib-mc"
core_dir = os.path.join(root_dir, "myulib-core")
framework_dir = os.path.join(root_dir, "myulib-framework")

def setup_gradle():
    # 1. Update settings.gradle
    settings_file = os.path.join(root_dir, "settings.gradle")
    with open(settings_file, "r", encoding="utf-8") as f:
        settings_content = f.read()
    
    if "include 'myulib-core'" not in settings_content:
        settings_content += "\ninclude 'myulib-core'\ninclude 'myulib-framework'\n"
        with open(settings_file, "w", encoding="utf-8") as f:
            f.write(settings_content)

    # 2. Update root build.gradle
    root_build_file = os.path.join(root_dir, "build.gradle")
    with open(root_build_file, "r", encoding="utf-8") as f:
        root_build_content = f.read()

    # We will replace the entire root build.gradle to support subprojects
    new_root_build = """plugins {
    id 'fabric-loom' version "${loom_version}" apply false
    id 'maven-publish'
}

allprojects {
    version = project.mod_version
    group = project.maven_group

    repositories {
        gradlePluginPortal()
        mavenCentral()
            maven {
            name = "Modrinth"
            url = "https://api.modrinth.com/maven"
            content { includeGroup "maven.modrinth" }
        }
    }
}

subprojects {
    apply plugin: 'fabric-loom'
    apply plugin: 'maven-publish'

    base {
        archivesName = project.name
    }

    dependencies {
        minecraft "com.mojang:minecraft:${project.minecraft_version}"
        mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
        modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
        modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    
        testImplementation "org.junit.jupiter:junit-jupiter:5.10.0"
        testImplementation "org.junit.platform:junit-platform-suite-engine:1.10.0"
        testImplementation "org.mockito:mockito-core:5.12.0"
        testRuntimeOnly "org.junit.platform:junit-platform-launcher"
    }

    loom {
        splitEnvironmentSourceSets()
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
}
"""
    with open(root_build_file, "w", encoding="utf-8") as f:
        f.write(new_root_build)

    # 3. Create core build.gradle
    core_build = """dependencies {
}
"""
    with open(os.path.join(core_dir, "build.gradle"), "w", encoding="utf-8") as f:
        f.write(core_build)

    # 4. Create framework build.gradle
    framework_build = """dependencies {
    implementation project(path: ':myulib-core', configuration: 'namedElements')
}
"""
    with open(os.path.join(framework_dir, "build.gradle"), "w", encoding="utf-8") as f:
        f.write(framework_build)

def setup_mod_jsons():
    core_json_path = os.path.join(core_dir, "src", "main", "resources", "fabric.mod.json")
    framework_json_path = os.path.join(framework_dir, "src", "main", "resources", "fabric.mod.json")

    with open(core_json_path, "r", encoding="utf-8-sig") as f:
        core_data = json.load(f)

    # Split for Core
    core_data["id"] = "myulib-core"
    core_data["name"] = "MyuLib Core"
    core_data["description"] = "Core utilities for MyuLib."
    core_data["entrypoints"]["main"] = ["com.myudog.myulib.MyulibCore"]
    core_data["entrypoints"]["client"] = ["com.myudog.myulib.client.MyulibCoreClient"]
    
    with open(core_json_path, "w", encoding="utf-8") as f:
        json.dump(core_data, f, indent=2)

    if not os.path.exists(os.path.dirname(framework_json_path)):
        os.makedirs(os.path.dirname(framework_json_path), exist_ok=True)
    if not os.path.exists(framework_json_path):
        shutil.copy2(core_json_path, framework_json_path)
        
    with open(framework_json_path, "r", encoding="utf-8-sig") as f:
        framework_data = json.load(f)

    # Split for Framework
    framework_data["id"] = "myulib-framework"
    framework_data["name"] = "MyuLib Framework"
    framework_data["description"] = "High-level game framework for MyuLib."
    framework_data["entrypoints"]["main"] = ["com.myudog.myulib.MyulibFramework"]
    framework_data["entrypoints"]["client"] = ["com.myudog.myulib.client.MyulibFrameworkClient"]
    framework_data["depends"]["myulib-core"] = "*"
    
    with open(framework_json_path, "w", encoding="utf-8") as f:
        json.dump(framework_data, f, indent=2)

def main():
    print("Setting up Gradle...")
    setup_gradle()
    print("Setting up fabric.mod.json...")
    setup_mod_jsons()
    print("Setup complete. Please remember to rename Myulib.java to MyulibCore.java and MyulibFramework.java")

if __name__ == '__main__':
    main()
