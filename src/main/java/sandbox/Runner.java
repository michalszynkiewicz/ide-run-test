package sandbox;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.wildfly.swarm.bootstrap.logging.BackingLoggerManager;
import org.wildfly.swarm.bootstrap.logging.BootstrapLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// mstodo test with webapp dir
// mstodo *move the fat jar to memory

// mstodo: failing with logger again, test with fat jar, last changes were probably on war

public class Runner {

    private static ModuleClassLoader loggingModuleClassLoader;

    private Runner() {
    }

    public static void main(String[] args) throws Exception {
        enableJBossLogging();
        URLClassLoader loader = createClassLoader();
        callWithClassloader(loader,
                "org.wildfly.swarm.bootstrap.Main",
                "main",
                new Class<?>[]{String[].class},
                (Object) args);
    }

    private static void enableJBossLogging() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        System.setProperty("boot.module.loader", "org.wildfly.swarm.bootstrap.modules.BootModuleLoader");
        try {
            Module loggingModule = Module.getBootModuleLoader().loadModule("org.wildfly.swarm.logging:runtime");

            ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
            try {
                loggingModuleClassLoader = loggingModule.getClassLoader();
                Thread.currentThread().setContextClassLoader(loggingModuleClassLoader);
                System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
                System.setProperty("org.jboss.logmanager.configurator", "org.wildfly.swarm.container.runtime.wildfly.LoggingConfigurator");
                //force logging init
                LogManager.getLogManager();
                Class<?> logManagerClass = loggingModuleClassLoader.loadClass("org.wildfly.swarm.container.runtime.logging.JBossLoggingManager");
                BootstrapLogger.setBackingLoggerManager((BackingLoggerManager) logManagerClass.newInstance());

//                org.apache.commons.logging.LogFactory.getLog(Runner.class).info("initializing apache commons logging");
//                org.apache.commons.logging.LogFactory.getFactory();
            } finally {
                Thread.currentThread().setContextClassLoader(originalCl);
            }
        } catch (ModuleLoadException e) {
            System.err.println("[WARN] logging not available, logging will not be configured");
            e.printStackTrace();
        }
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
        System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
//        File fatJar;
//
//        URLClassLoader urlLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
//
//        List<String> urlList =
//                Stream.of(urlLoader.getURLs())
//                        .map(URL::toString)
//                        .collect(Collectors.toList());
//
//        fatJar = FatJarBuilder.build(urlList);
//        fatJar = new File("/home/michal/job/tmp/thorn-1980/target/sandbox-0.0.1-SNAPSHOT-thorntail.jar");
        URL jarUrl = fatJar.toURI().toURL();
        return new URLClassLoader(new URL[]{jarUrl}, loggingModuleClassLoader); //(ClassLoader) ClassLoader.getSystemClassLoader().getParent());
    }
}