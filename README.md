# Jvm self attach

Launch javaagent on self-hosted jvm.

## Supported Platforms

- JDK 1.8 (Tested with `Windows jdk1.8.0_181`)
- JDK 9+
- JRE 9+ (Maybe)

## Usage

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kasukusakura/jvm-self-attach.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.kasukusakura%22%20AND%20a:%22jvm-self-attach%22)

Jar was published as `io.github.kasukusakura:jvm-self-attach`


```java
import io.github.kasukusakura.jsa.JvmSelfAttach;
import java.io.File;

public class JSAStaticTest {
    public static void main(String[] args) throws Throwable {
        JvmSelfAttach.init(new File("tmp"));
        System.out.println(JvmSelfAttach.getInstrumentation());
    }
}
```
