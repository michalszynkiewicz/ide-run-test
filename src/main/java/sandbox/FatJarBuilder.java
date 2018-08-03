package sandbox;

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
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.toList;

/**
 * mstodo: Header
 * mstodo: try on license dictionary
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 7/30/18
 */
public class FatJarBuilder {

    private final List<URL> classPathUrls;
    private final File target;

    public FatJarBuilder(List<URL> classPathUrls, File target) {
        this.classPathUrls = classPathUrls;
        this.target = target;
    }

    public static void main(String[] args) throws Exception {
        File fatJar = new File(args[0]);

        URLClassLoader urlLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

        List<String> urlList =
                Stream.of(urlLoader.getURLs())
                        .map(URL::toString)
                        .collect(Collectors.toList());

        fatJar = FatJarBuilder.build(urlList, fatJar);

    }

    private static File build(List<String> classPathUrls, File target) throws Exception {
        List<URL> urls = new ArrayList<>();

        for (String urlString : classPathUrls) {
            urls.add(new URL(urlString));
        }

        FatJarBuilder b = new FatJarBuilder(urls, target);
        return b.doBuild();
    }

    public File doBuild() throws Exception {
        long start = System.currentTimeMillis();
        File result = _doBuild();
        System.out.printf("fat jar built in %d ms\n", System.currentTimeMillis() - start);
        return result;
    }

    private File _doBuild() throws Exception {
        final String type = "war";

        // NOTE: we don't know which props are transitive!!!
        List<ArtifactSpecOrUrl> classPathEntries = analyzeClasspath();

        // non-maven jars are not taken into account above
        // we need to make sure that none of them are skipped and add to war the ones that were
        // mstodo
        List<URL> nonMavenJars = classPathEntries.stream()
                .map(e -> e.url)
                .filter(Objects::nonNull)
                .collect(toList());

        LogManager.getLogManager();

        final BuildTool tool = new BuildTool(mavenArtifactResolvingHelper())
                .projectArtifact("tt",
                        "tt-user-app",
                        "0.1-SNAPSHOT",
                        type,
                        buildWar(classPathEntries),
                        "whatever.war") // mstodo
//                .properties(this.properties)
//                .mainClass(this.mainClass)
//                .bundleDependencies(this.bundleDependencies)
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


        // mstodo:
        // - go through the classpath jars
        //      -> for the ones built with maven, we have maven coordinates in the META-INF/maven/../pom.xml
        // - run the tool on it
        // - check which jars are missing from the fat jar, add them to war
        // - add classes to war
        tool.declaredDependencies(declaredDependencies(classPathEntries));


        // mstodo can any non-file url get here?
        this.classPathUrls.parallelStream()
                .filter(url -> !url.toString().matches(".*\\.[^/]*"))
                .forEach(r -> tool.resourceDirectory(r.getFile()));

        // mstodo replace/remove
        Path uberjarResourcesDir = File.createTempFile("uberjar-resources-placehodler", "bs").toPath();
        tool.uberjarResourcesDirectory(uberjarResourcesDir);

//        File jarFile = File.createTempFile("tt-fatjar", ".jar");
        System.out.println("triggering build");
        File jar = tool.build(target.getName(), target.getParentFile().toPath());
        System.out.println("done build " + jar);
        return jar;

//            if (this.project.getPackaging().equals(WAR)) {
//            mstodo: create a war with everything that didn't make into the jars above
//        tool.repackageWar(primaryArtifactFile);
//            }
    }

    /*
    mstodo: it is to early to add jars here!
     */
    /**
     * builds war with classes inside
     *
     * @param classPathEntries class path entries as ArtifactSpec or URLs
     * @return the war file
     */
    private File buildWar(List<ArtifactSpecOrUrl> classPathEntries) {
        try {
            List<URL> classesUrls = classPathEntries.stream()
                    .map(ArtifactSpecOrUrl::url)
                    .filter(Objects::nonNull)
                    .filter(url -> url.toString().endsWith("/classes/"))
                    .collect(Collectors.toList());

            return WarBuilder.build(classesUrls);
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

    private List<ArtifactSpecOrUrl> analyzeClasspath() {
        return classPathUrls.parallelStream()
                .map(this::urlToSpec)
                .collect(toList());
    }

    private DeclaredDependencies declaredDependencies(List<ArtifactSpecOrUrl> specsOrUrls) {
        List<ArtifactSpec> specs =
                specsOrUrls.stream()
                .map(specOrUrl -> specOrUrl.spec)
                .filter(Objects::nonNull)
                .collect(toList());
        return new DeclaredDependencies() {
            @Override
            public Collection<ArtifactSpec> getDirectDeps() {
                return specs;
            }

            @Override
            public Collection<ArtifactSpec> getTransientDeps(ArtifactSpec parent) {
                return Collections.emptyList();
            }
        };
    }

    private ArtifactSpecOrUrl urlToSpec(URL url) {
        if (!url.toString().endsWith(".jar")) {
            return ArtifactSpecOrUrl.url(url);
        }
        String zipFile = resolveUrlToFile(url);
        try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zipFile), getClass().getClassLoader())) {
            Path path = fs.getPath("/META-INF/maven");
            if (path == null || !Files.exists(path)) {
                return ArtifactSpecOrUrl.url(url);
            }                                                      /*mstodo: switch this to getPathMatcher?*/
            Optional<Path> maybePomXml = Files.walk(path)
                    .filter(p -> p.endsWith("pom.xml"))
                    .findAny();
            return maybePomXml
                    .map(pom -> toArtifactSpec(pom, zipFile))
                    .map(ArtifactSpecOrUrl::spec)
                    .orElse(ArtifactSpecOrUrl.url(url));
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


    public static class ArtifactSpecOrUrl {
        private final URL url;
        private final ArtifactSpec spec;

        private ArtifactSpecOrUrl(URL url, ArtifactSpec spec) {
            this.url = url;
            this.spec = spec;
        }

        public URL url() {
            return url;
        }

        public ArtifactSpec spec() {
            return spec;
        }

        private static ArtifactSpecOrUrl url(URL url) {
            return new ArtifactSpecOrUrl(url, null);
        }
        private static ArtifactSpecOrUrl spec(ArtifactSpec spec) {
            return new ArtifactSpecOrUrl(null, spec);
        }
    }
}
