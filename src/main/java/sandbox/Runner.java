package sandbox;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// mstodo test with webapp dir
// mstodo move the fat jar to memory

public class Runner {

    private Runner() {
    }

    public static void main(String[] args) throws Exception {
        // classpath has much less artifacts than fat jar
        // TODO: maybe scanning the classpath for
        URLClassLoader loader = createClassLoader();
//        Thread.currentThread().setContextClassLoader(loader);
//        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
//        Class<?> swarmClass = loader.loadClass("org.wildfly.swarm.bootstrap.Main");
//        Method main = swarmClass.getMethod("main", String[].class);
//        System.out.println("starting main wth custom loader");        // mstodo remove
//        main.invoke(null, (Object) args);
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
//        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        Class<?> aClass = loader.loadClass(className);
        Method method = aClass.getMethod(methodName, argumentTypes);
        System.out.printf("running %s.%s with custom loader", className, methodName);        // mstodo remove
        return (T) method.invoke(null, arguments);
    }

    private static URLClassLoader createClassLoader() throws Exception {
        System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
        File fatJar;

        URLClassLoader urlLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        URLClassLoader isolatedLoader = new URLClassLoader(urlLoader.getURLs(), (ClassLoader) null);

        List<String> urlList =
                Stream.of(urlLoader.getURLs())
                        .map(URL::toString)
                        .collect(Collectors.toList());

        fatJar = callWithClassloader(isolatedLoader,
                FatJarBuilder.class.getCanonicalName(),
                "build",
                new Class<?>[]{List.class},
                urlList);

//        Class<?> builderClass = isolatedLoader.loadClass(FatJarBuilder.class.getCanonicalName());
//        Method buildMethod = builderClass.getMethod("build", URL[].class);
//        fatJar = (File) buildMethod.invoke(null, (Object)urlLoader.getURLs());
//        FatJarBuilder builder = new FatJarBuilder(asList(urlLoader.getURLs()));
//        File fatJar = builder.doBuild();

        fatJar = new File("/home/michal/job/tmp/thorn-1980/target/sandbox-0.0.1-SNAPSHOT-thorntail.jar");
        URL jarUrl = fatJar.toURI().toURL();
        return new URLClassLoader(new URL[]{jarUrl}, (ClassLoader) null);
    }
}