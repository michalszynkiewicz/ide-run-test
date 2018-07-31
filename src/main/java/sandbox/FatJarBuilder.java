package sandbox;

import com.sun.nio.zipfs.ZipFileSystem;
import org.w3c.dom.Document;
import org.wildfly.swarm.spi.meta.SimpleLogger;
import org.wildfly.swarm.tools.ArtifactResolvingHelper;
import org.wildfly.swarm.tools.ArtifactSpec;
import org.wildfly.swarm.tools.BuildTool;
import org.wildfly.swarm.tools.DeclaredDependencies;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Arrays.asList;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 7/30/18
 */
public class FatJarBuilder {

    private final List<URL> classPathUrls;

    public FatJarBuilder(List<URL> classPathUrls) {
        this.classPathUrls = classPathUrls;
    }

    public static File build(List<String> classPathUrls) throws Exception {
        System.out.println("da loader: " + FatJarBuilder.class.getClassLoader());
        System.out.println("da thread loader: " + Thread.currentThread().getContextClassLoader());
        List<URL> urls = new ArrayList<>();

        for (String urlString : classPathUrls) {
            urls.add(new URL(urlString));
        }

        FatJarBuilder b = new FatJarBuilder(urls);
        return b.doBuild();
    }

    public File doBuild() throws Exception {
        long start = System.currentTimeMillis();
        File result = _doBuild();
        System.out.printf("fat jar built in %d ms\n", System.currentTimeMillis() - start);
        return result;
    }

    private File _doBuild() throws Exception {
//        final Artifact primaryArtifact = this.project.getArtifact();
//        final String finalName = this.project.getBuild().getFinalName();
        final String type = "war";

//        final DeclaredDependencies declaredDependencies = new DeclaredDependencies();

        final BuildTool tool = new BuildTool(mavenArtifactResolvingHelper())
                .projectArtifact("tt",
                        "tt-user-app",
                        "0.1-SNAPSHOT",
                        type,
                        buildWar(),
                        "whatever.war") // mstodo
//                .properties(this.properties)
//                .mainClass(this.mainClass)
//                .bundleDependencies(this.bundleDependencies)
//                .executable(executable)
//                .executableScript(executableScript)
                .fractionDetectionMode(BuildTool.FractionDetectionMode.when_missing) // mstodo is this reasonable?
                .hollow(false)
                .logger(new SimpleLogger() {
                    // mstodo proper logging
                    @Override
                    public void debug(String msg) {
                        System.out.println(msg);
                    }

                    @Override
                    public void info(String msg) {
                        System.out.println(msg);
                    }

                    @Override
                    public void error(String msg) {
                        System.out.println(msg);
                    }

                    @Override
                    public void error(String msg, Throwable t) {
                        System.out.println(msg);
                        t.printStackTrace();
                    }
                });

//        mstodo replace with system or env property
//        this.fractions.forEach(f -> {
//            if (f.startsWith(EXCLUDE_PREFIX)) {
//                tool.excludeFraction(ArtifactSpec.fromFractionDescriptor(FractionDescriptor.fromGav(FractionList.get(), f.substring(1))));
//            } else {
//                tool.fraction(ArtifactSpec.fromFractionDescriptor(FractionDescriptor.fromGav(FractionList.get(), f)));
//            }
//        });

//        Map<ArtifactSpec, Set<ArtifactSpec>> buckets = createBuckets(this.project.getArtifacts(), this.project.getDependencies());

        // mstodo remove comment: gather direct and transient deps - weird :O
//        for (ArtifactSpec directDep : buckets.keySet()) {
//
//            if (!(directDep.scope.equals("compile") || directDep.scope.equals("runtime"))) {
//                continue; // ignore anything but compile and runtime
//            }
//
//            Set<ArtifactSpec> transientDeps = buckets.get(directDep);
//            if (transientDeps.isEmpty()) {
//                declaredDependencies.add(directDep);
//            } else {
//                for (ArtifactSpec transientDep : transientDeps) {
//                    declaredDependencies.add(directDep, transientDep);
//                }
//            }
//        }

        // mstodo:
        // - go through the classpath jars
        //      -> for the ones built with maven, we have maven coordinates in the META-INF/maven/../pom.xml
        // - run the tool on it
        // - check which jars are missing from the fat jar, add them to war
        // - add classes to war

        // NOTE: we don't know which props are transitive!!!

        tool.declaredDependencies(gatherMavenDependencies());
        // mstodo can any non-file url get here?
        this.classPathUrls.parallelStream()
                .filter(url -> !url.toString().matches(".*\\.[^/]*"))
                .forEach(r -> tool.resourceDirectory(r.getFile()));

        // mstodo replace/remove
        Path uberjarResourcesDir = File.createTempFile("uberjar-resources-placehodler", "bs").toPath();
        tool.uberjarResourcesDirectory(uberjarResourcesDir);

        String jarFinalName = File.createTempFile("tt-fatjar", ".jar").getAbsolutePath();
        System.out.println("triggering build");
        File jar = tool.build(jarFinalName, Paths.get("/tmp")); // mstodo betta dir, not portable!
        System.out.println("done build " + jar);
        return jar;

//            if (this.project.getPackaging().equals(WAR)) {
//            mstodo: create a war with everything that didn't make into the jars above
//        tool.repackageWar(primaryArtifactFile);
//            }
    }

    private File buildWar() {
        try {
            File war = File.createTempFile("tt-war", ".war");
            try (FileOutputStream fos = new FileOutputStream(war);
                 ZipOutputStream out = new ZipOutputStream(fos)) {
                ZipEntry test = new ZipEntry("test");
                out.putNextEntry(test);
                out.write("test".getBytes("UTF-8"));
                out.closeEntry();

                return war;
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to build war", e);
        }
    }

    private ArtifactResolvingHelper mavenArtifactResolvingHelper() {
        // all artifacts should have files defined, no need to resolve anything
        return new ArtifactResolvingHelper() {
            @Override
            public ArtifactSpec resolve(ArtifactSpec spec) throws Exception {
                return spec;
            }

            @Override
            public Set<ArtifactSpec> resolveAll(Collection<ArtifactSpec> specs, boolean transitive, boolean defaultExcludes) throws Exception {
                return new HashSet<>(specs);
            }
        };
    }

    private DeclaredDependencies gatherMavenDependencies() {
        System.out.printf("me has loaders: %s and TCCL: %s\n", getClass().getClassLoader(), Thread.currentThread().getContextClassLoader());
        List<ArtifactSpec> specs = classPathUrls.stream() // mstodo parallelize
                .flatMap(this::urlToSpec)
                .collect(Collectors.toList());

        return new DeclaredDependencies() {
            @Override
            public Collection<ArtifactSpec> getDirectDeps() {
                return specs;
            }

            @Override
            public Collection<ArtifactSpec> getTransientDeps(ArtifactSpec parent) {
                // mstodo do sth about transitives - we probably don't want to do anything with them
                return Collections.emptyList();
            }
        };
    }

    private Stream<ArtifactSpec> urlToSpec(URL url) {
        if (!url.toString().endsWith(".jar")) {
            return Stream.empty();
        }
        String zipFile = resolveUrlToFile(url);
        try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zipFile), getClass().getClassLoader())) {
            Path path = fs.getPath("/META-INF/maven");
            if (path == null || !Files.exists(path)) {
                return Stream.empty();
            }                                                      /*mstodo: switch this to getPathMatcher?*/
            Optional<Path> maybePomXml = Files.walk(path)
                    .filter(p -> p.endsWith("pom.xml"))
                    .findAny();
            return maybePomXml
                    .map(pom -> toArtifactSpec(pom, zipFile))
                    .map(Stream::of)
                    .orElse(Stream.empty());

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse jar: " + url, e);
        }
    }

    private ArtifactSpec toArtifactSpec(Path pom, String zipFilePath) {
        System.out.println("found pom: " + pom + " should return spec from it");
        try {
            // mstodo watch out for properties?
            String groupId = extract(pom, "/project/groupId",
                    () -> extract(pom, "/project/parent/groupId"));
            String artifactId = extract(pom, "/project/artifactId");
            String version = extract(pom, "/project/version",
                    () -> extract(pom, "/project/parent/version"));
            String packaging = extract(pom, "/project/packaging", "jar");
            String classifier = extract(pom, "/project/classifier", (String) null);

            return new ArtifactSpec("compile",
                    groupId, artifactId, version, packaging, classifier,
                    new File(zipFilePath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read artifact spec from pom " + pom, e);
        }
    }

    private String extract(Path source, String expression, Supplier<String> defaultValueProvider) {
        String extracted = extract(source, expression);

        return extracted == null || "".equals(extracted)
                ? defaultValueProvider.get()
                : extracted;
    }

    private String extract(Path source, String expression, String defaultValue) {
        String extracted = extract(source, expression);

        return extracted == null || "".equals(extracted)
                ? defaultValue
                : extracted;
    }

    private String extract(Path sourcePath, String expression) {   // mstodo simplify, make the extract great again
        try (InputStream source = Files.newInputStream(sourcePath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(source);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile(expression);
            return (String) expr.evaluate(doc, XPathConstants.STRING);
        } catch (Exception any) {
            throw new RuntimeException("Failure when trying to find a match for " + expression, any);
        }
    }

    private String resolveUrlToFile(URL url) {
        String zipFile = url.getFile();
        if (zipFile == null) {
            try (InputStream stream = url.openStream()) {
                zipFile = File.createTempFile("tt-dependency", ".jar").getAbsolutePath();
                Files.copy(stream, Paths.get(zipFile));
            } catch (IOException e) {
                throw new RuntimeException("Unable to resolve: " + url);
            }
        }
        return zipFile;
    }


}
