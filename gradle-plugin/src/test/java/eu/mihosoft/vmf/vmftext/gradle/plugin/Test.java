package eu.mihosoft.vmf.vmftext.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class Test {

    @org.junit.Test
    public void pluginTasksPresentTest() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("eu.mihosoft.vmftext");

        assertNotNull(project.getTasks().getByName("vmfTextClean"));
        assertNotNull(project.getTasks().getByName("vmfTextGenCode"));
    }

    private static String versionOf(Project project, String group, String name) {
        for (Dependency d : project.getConfigurations().getByName("implementation").getDependencies()) {
            if (group.equals(d.getGroup()) && name.equals(d.getName())) {
                return d.getVersion();
            }
        }
        return null;
    }

    /** #74: vmfText { vmfVersion; antlrVersion } is set after the plugin is applied and must still count. */
    @org.junit.Test
    public void runtimeDependenciesFollowTheExtension() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("eu.mihosoft.vmftext");

        VMFTextPluginExtension ext = project.getExtensions().getByType(VMFTextPluginExtension.class);
        ext.setVmfVersion("0.2.11");
        ext.setAntlrVersion("4.13.1");

        assertEquals("0.2.11", versionOf(project, "eu.mihosoft.vmf", "vmf-runtime"));
        assertEquals("4.13.1", versionOf(project, "org.antlr", "antlr4-runtime"));
    }

    /** Without a setting the runtime is the VMF version the generator produces code for. */
    @org.junit.Test
    public void defaultRuntimeIsTheGeneratorVmfVersion() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("eu.mihosoft.vmftext");

        assertEquals(new VMFTextPluginExtension().getVmfVersion(), versionOf(project, "eu.mihosoft.vmf", "vmf-runtime"));
        assertEquals(new VMFTextPluginExtension().getAntlrVersion(), versionOf(project, "org.antlr", "antlr4-runtime"));
    }

    /** The stable default resolves without adding a snapshot repository to consumer builds. */
    @org.junit.Test
    public void stableRuntimeDoesNotAddSnapshotRepository() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("eu.mihosoft.vmftext");
        ((ProjectInternal) project).evaluate();

        assertFalse(hasCentralSnapshotRepository(project));
    }

    @org.junit.Test
    public void snapshotOverrideAddsSnapshotRepository() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("eu.mihosoft.vmftext");
        project.getExtensions().getByType(VMFTextPluginExtension.class).setVmfVersion("0.4.0-SNAPSHOT");
        ((ProjectInternal) project).evaluate();

        assertEquals("0.4.0-SNAPSHOT", versionOf(project, "eu.mihosoft.vmf", "vmf-runtime"));
        assertTrue(hasCentralSnapshotRepository(project));
    }

    private static boolean hasCentralSnapshotRepository(Project project) {
        for (ArtifactRepository r : project.getRepositories()) {
            if (r instanceof MavenArtifactRepository
                    && ((MavenArtifactRepository) r).getUrl().toString().contains("central.sonatype.com/repository/maven-snapshots")) {
                return true;
            }
        }
        return false;
    }

//    @org.junit.Test
//    public void vmfConfigurationPresentTest() {
//        Project project = ProjectBuilder.builder().build();
//        project.getPlugins().apply("eu.mihosoft.vmftext");
//
//        assertNotNull(project.getConfigurations().getByName("vmfText"));
//    }

}
