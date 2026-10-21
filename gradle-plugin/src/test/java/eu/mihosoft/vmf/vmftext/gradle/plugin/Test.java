package eu.mihosoft.vmf.vmftext.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.testfixtures.ProjectBuilder;

import static junit.framework.TestCase.assertEquals;
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

    /** VMF snapshots come from the Central snapshot repository, which is declared regardless of the version. */
    @org.junit.Test
    public void centralSnapshotRepositoryIsDeclared() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("eu.mihosoft.vmftext");

        boolean found = false;
        for (ArtifactRepository r : project.getRepositories()) {
            if (r instanceof MavenArtifactRepository
                    && ((MavenArtifactRepository) r).getUrl().toString().contains("central.sonatype.com/repository/maven-snapshots")) {
                found = true;
            }
        }
        assertTrue(found);
    }

//    @org.junit.Test
//    public void vmfConfigurationPresentTest() {
//        Project project = ProjectBuilder.builder().build();
//        project.getPlugins().apply("eu.mihosoft.vmftext");
//
//        assertNotNull(project.getConfigurations().getByName("vmfText"));
//    }

}
