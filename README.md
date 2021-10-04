# Jvm self attach

Launch javaagent on self-hosted jvm.

## Supported Platforms

- JDK 1.8 (Tested with `Windows jdk1.8.0_181`)
- JDK 9+
- JRE 9+ (Maybe)

## Usage

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
