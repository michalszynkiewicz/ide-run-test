package sandbox;

import org.wildfly.swarm.spi.meta.SimpleLogger;
import org.wildfly.swarm.tools.ArtifactResolvingHelper;
import org.wildfly.swarm.tools.ArtifactSpec;
import org.wildfly.swarm.tools.BuildTool;
import org.wildfly.swarm.tools.DeclaredDependencies;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 7/30/18
 */
public class FatJarBuilder {

    public static final Stream<ArtifactSpec> NO_MVN_ARTIFACTS = Stream.empty();
    private final List<URL> classPathUrls;

    public FatJarBuilder(List<URL> classPathUrls) {
        this.classPathUrls = classPathUrls;
    }

    public void doBuild() throws IOException {
//        final Artifact primaryArtifact = this.project.getArtifact();
//        final String finalName = this.project.getBuild().getFinalName();
        final String type = "war";

//        final DeclaredDependencies declaredDependencies = new DeclaredDependencies();

        final BuildTool tool = new BuildTool(mavenArtifactResolvingHelper())
                .projectArtifact("tt",
                        "tt-user-app",
                        "0.1-SNAPSHOT",
                        type,
                        File.createTempFile("primary artifact :O", ".war"),
                        "whatever.jar") // mstodo
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
           /*
        this.project.getResources()
                .forEach(r -> tool.resourceDirectory(r.getDirectory()));

        Path uberjarResourcesDir = null;
        if (this.uberjarResources == null) {
            uberjarResourcesDir = Paths.get(this.project.getBasedir().toString()).resolve("src").resolve("main").resolve("uberjar");
        } else {
            uberjarResourcesDir = Paths.get(this.uberjarResources);
        }
        tool.uberjarResourcesDirectory(uberjarResourcesDir);

        this.additionalModules.stream()
                .map(m -> new File(this.project.getBuild().getOutputDirectory(), m))
                .filter(File::exists)
                .map(File::getAbsolutePath)
                .forEach(tool::additionalModule);

        try {
            String jarFinalName;
            if (this.finalName != null) {
                jarFinalName = this.finalName;
            } else {
                jarFinalName = finalName + "-" + (this.hollow ? HOLLOWJAR_SUFFIX : UBERJAR_SUFFIX);
            }
            jarFinalName += JAR_FILE_EXTENSION;
            File jar = tool.build(jarFinalName, Paths.get(this.projectBuildDir));
            ArtifactHandler handler = new DefaultArtifactHandler("jar");
            Artifact swarmJarArtifact = new DefaultArtifact(
                    "tt",
                    "tt",
                    "0.1-SNAPSHOT",
                    "compile",
                    "jar",
                    "tt",
                    handler
            );

            swarmJarArtifact.setFile(jar);
            this.project.addAttachedArtifact(swarmJarArtifact);

            if (this.project.getPackaging().equals(WAR)) {
                tool.repackageWar(primaryArtifactFile);
            }
        } catch (Exception e) {
            throw new MojoFailureException("Unable to create " + UBERJAR_SUFFIX + JAR_FILE_EXTENSION, e);
        }     */
    }

    private ArtifactResolvingHelper mavenArtifactResolvingHelper() {
        return null;  // TODO: Customise this generated block
    }

    private DeclaredDependencies gatherMavenDependencies() {
        List<ArtifactSpec> specs = classPathUrls.parallelStream()
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
            return NO_MVN_ARTIFACTS;
        }
        String zipFile = resolveUrlToFile(url);
        try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zipFile), null)) {
            Path path = fs.getPath("/META-INF/maven");
            if (path == null) {
                return NO_MVN_ARTIFACTS;
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse jar: " + url);
        }

        return null;  // TODO: Customise this generated block
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
