package sandbox;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;

// mstodo test with webapp dir

public class Runner {

    private Runner() {
    }

    public static void main(String[] args) throws Exception {
        URLClassLoader loader = createClassLoader();
        callWithClassloader(loader,
                "org.wildfly.swarm.bootstrap.Main",
                "main",
                new Class<?>[]{String[].class},
                (Object) args);
    }

    private static <T> T callWithClassloader(ClassLoader loader,
                                             String className,
                                             String methodName,
                                             Class<?>[] argumentTypes,
                                             Object... arguments) throws Exception {
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> aClass = loader.loadClass(className);
        Method method = aClass.getMethod(methodName, argumentTypes);
        return (T) method.invoke(null, arguments);
    }

    private static URLClassLoader createClassLoader() throws Exception {
        File fatJar = File.createTempFile("t-t", ".jar"); // mstodo better name?
        buildJar(fatJar);
        System.out.println("Built " + fatJar.getAbsolutePath());

        URL jarUrl = fatJar.toURI().toURL();
        return new URLClassLoader(new URL[]{jarUrl}, null); //(ClassLoader) ClassLoader.getSystemClassLoader().getParent());
    }

    private static void buildJar(File fatJar) throws IOException, InterruptedException {
        String classpath = Arrays.stream(((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs())
                .map(URL::getFile)
                .collect(Collectors.joining(File.pathSeparator));

        List<String> command = buildCommand(fatJar, classpath);

        Process fatJarBuilder = new ProcessBuilder(command)
                .inheritIO()
                .start();


        int exitCode = fatJarBuilder.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to generate the uber jar.");
        }
    }

    /*
    builds a command like:
    /my/path/to/java -cp all:elements:of:classpath -Dall -Dsystem=properties JarBuilderClassName pathToTargetJar
     */
    private static List<String> buildCommand(File fatJar, String classpath) {
        List<String> command = new ArrayList<>(
                asList(
                        javaCommand(),
                        "-cp",
                        classpath)
        );

        command.addAll(properties());
        command.addAll(asList(
                FatJarBuilder.class.getCanonicalName(),
                fatJar.getAbsolutePath()
        ));
        return command;
    }

    private static Collection<String> properties() {
        return System.getProperties()
                .entrySet()
                .stream()
                .map(Runner::propertyToString)
                .collect(Collectors.toList());
    }

    private static String propertyToString(Map.Entry<Object, Object> property) {
        return property.getValue() == null
                ? format("-D%s", property.getKey())
                : format("-D%s=%s", property.getKey(), property.getValue());
    }

    private static String javaCommand() {
        Path javaBinPath = Paths.get(System.getProperty("java.home"), "bin");
        File javaExecutable = javaBinPath.resolve("java").toFile();
        if (!javaExecutable.exists()) {
            javaExecutable = javaBinPath.resolve("java.exe").toFile();
        }
        return javaExecutable.getAbsolutePath();
    }
}