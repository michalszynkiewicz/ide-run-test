package sandbox;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class Runner {

    private Runner() {
    }

    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        // classpath has much less artifacts than fat jar
        // TODO: maybe scanning the classpath for  
        URLClassLoader loader = createClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> swarmClass = loader.loadClass("org.wildfly.swarm.bootstrap.Main");
        Method main = swarmClass.getMethod("main", String[].class);
        System.out.println("starting main wth custom loader");        // mstodo remove
        main.invoke(null, (Object) args);
    }

    private static URLClassLoader createClassLoader() throws IOException, ReflectiveOperationException {
        System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
        URLClassLoader urlLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

        buildFatJar(urlLoader.getURLs());


        URL[] urls = Stream.of(urlLoader.getURLs())
                .peek(System.out::println)
                .filter(Runner::shouldInclude)
                .peek(url -> System.out.println("adding: " + url))
                .toArray(URL[]::new);
        return new URLClassLoader(urls, null);
//        File fatJar = buildFatJar();
//
//        URL jarUrl = fatJar.toURI().toURL();
//        return new URLClassLoader(new URL[]{jarUrl}, (ClassLoader) null);
    }

    private static boolean shouldInclude(URL aUrl) {
        final Set<String> ttWhitelist = new HashSet<>(asList("container", "bootstrap"));
        final Set<String> ttBlackList = new HashSet<>(asList("thorntail", "swarm"));
        String url = aUrl.toString();
        return ttBlackList.stream().noneMatch(url::contains) ||
                ttWhitelist.stream().anyMatch(url::contains)
//                || url.contains("swarm")
                || url.endsWith("/classes/");
    }

    private static File buildFatJar(URL[] urls) throws IOException, ReflectiveOperationException {
        // mstodo it's dirty:
        Collection<String> tt = asList("thorntail", "swarm");
        Set<ArtifactInfo> ttUrls = Stream.of(urls)
                .filter(u -> tt.stream().anyMatch(u.toString()::contains))
                .flatMap(Runner::resolveDependencies)
                .collect(Collectors.toSet());



        System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
        URLClassLoader urlLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();


        Stream.of(urlLoader.getURLs()).forEach(System.out::println);

        return new File("/home/michal/job/tmp/thorn-1980/target/sandbox-0.0.1-SNAPSHOT-thorntail.jar");
    }

    private static Stream<ArtifactInfo> resolveDependencies(URL url) {
        
    }

    private static class ArtifactInfo {
        private final String gav;

        private ArtifactInfo(String gav) {
            this.gav = gav;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArtifactInfo that = (ArtifactInfo) o;
            return Objects.equals(gav, that.gav);
        }

        @Override
        public int hashCode() {
            return Objects.hash(gav);
        }
    }
}