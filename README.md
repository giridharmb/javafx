# javafx

#### Final

```
mvn clean compile package javafx:jlink jpackage:jpackage
```

> Will Generate >>

```
target/dist/MyApp-1.0.0.pkg
```

> Log >>

```
[INFO] Using: /Users/giri/Library/Java/JavaVirtualMachines/openjdk-23.0.1/Contents/Home/bin/jpackage, major version: 23
[INFO] jpackage options:
[INFO]   --name MyApp
[INFO]   --dest /Users/giri/git/java/javafx/target/dist
[INFO]   --type pkg
[INFO]   --app-version 1.0.0
[INFO]   --runtime-image /Users/giri/git/java/javafx/target/gbhujanfx1
[INFO]   --input /Users/giri/git/java/javafx/target
[INFO]   --main-class org.example.gbhujanfx1.Application
[INFO]   --main-jar gbhujanfx1-1.0-SNAPSHOT.jar
```

---

```
mvn clean install

mvn clean package
```

---

#### Run > Edit Configurations > Template = "Application"

#### Run > Edit Configurations > VM Options >>

```
--module-path /Users/giri/javafx/javafx-sdk-23.0.1/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar target/gbhujanFX1-1.0-SNAPSHOT.jar --add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED --add-exports javafx.base/com.sun.javafx.reflect=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED --add-exports javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
```

#### Main Class

```
org.example.gbhujanfx1.Application
```

#### Working Directory >

```
$PROJECT_DIR$
```

---

### GIT Structure

```
.gitignore  # Include IDE files, build artifacts
```

```
pom.xml
src/
└── main/
    ├── java/
    │   └── org/example/gbhujanfx1/
    │       ├── HelloApplication.java
    │       └── module-info.java
    └── resources/  # If you add resources later
README.md  # Add installation/running instructions
```

#### Query JavaFX Packages >>

```
jar tf /Users/giri/.m2/repository/org/example/gbhujanfx1/1.0-SNAPSHOT/gbhujanfx1-1.0-SNAPSHOT.jar|grep javafx
```