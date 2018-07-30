package eu.mihosoft.vmf.vmftext.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

import static junit.framework.TestCase.assertNotNull;

public class Test {

    @org.junit.Test
    public void pluginTasksPresentTest() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("eu.mihosoft.vmftext");

        assertNotNull(project.getTasks().getByName("vmfTextClean"));
        assertNotNull(project.getTasks().getByName("vmfTextGenCode"));
    }

//    @org.junit.Test
//    public void vmfConfigurationPresentTest() {
//        Project project = ProjectBuilder.builder().build();
//        project.getPlugins().apply("eu.mihosoft.vmftext");
//
//        assertNotNull(project.getConfigurations().getByName("vmfText"));
//    }

}
