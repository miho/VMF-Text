package eu.mihosoft.vmf.vmftext.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile


import org.gradle.api.Plugin;
import org.gradle.api.internal.*;
import org.gradle.api.internal.file.*;
import org.gradle.api.internal.file.collections.*;
import org.gradle.api.internal.tasks.*;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.*;
import org.codehaus.groovy.runtime.InvokerHelper;
//
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.SourceDirectorySetFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.util.ConfigureUtil;
import javax.inject.Inject
import java.util.stream.Collectors;
import groovy.grape.Grape;

import static org.gradle.api.reflect.TypeOf.typeOf;

/*
 * **NOTE:**
 *
 * This code is heavily influenced by the ANTLR plugin that ships with Gradle:
 *
 * -> see: https://github.com/gradle/gradle/tree/master/subprojects/antlr/src/main/java/org/gradle/api/plugins/antlr
 *
 * TODO 22.07.2018 find out whether adding a custom language folder, such as 'vmf-text' really needs that much code.
 */

/**
 * Contract for a Gradle "convention object" that acts as a handler for what the developer of the ANTLR gradle
 * plugin calls 'a virtual directory mapping',
 * injecting a virtual directory named 'vmf-text' into the project's various
 * {@link org.gradle.api.tasks.SourceSet source sets}.
 */
interface VMFTextSourceVirtualDirectory {
    String NAME = "vmftext";

    /**
     * All VMFText source for this source set.
     *
     * @return The VMFText source.  Never returns null.
     */
    SourceDirectorySet getVMFText();

    /**
     * Configures the VMFText source for this set. The given closure is used to configure the
     * {@link org.gradle.api.file.SourceDirectorySet} (see {@link #getVMFText}) which contains the VMFText source.
     *
     * @param configureClosure The closure to use to configure the VMFText source.
     * @return this
     */
    VMFTextSourceVirtualDirectory vmfText(Closure configureClosure);

    /**
     * Configures the VMFText source for this set. The given action is used to configure the
     * {@link org.gradle.api.file.SourceDirectorySet} (see
     * {@link #getVMFText}) which contains the VMFText source.
     *
     * @param configureAction The action to use to configure the VMFText source.
     * @return this
     */
    VMFTextSourceVirtualDirectory vmfText(Action<? super SourceDirectorySet> configureAction);
}

class VMFTextPluginExtension {
    // vmf-text version
    String vmfVersion     = "0.3-SNAPSHOT"
    String antlrVersion   = "4.7.2"
}


/**
 * The implementation of the {@link VMFTextSourceVirtualDirectory} contract.
 */
class VMFTextSourceVirtualDirectoryImpl implements VMFTextSourceVirtualDirectory, HasPublicType {
    private final SourceDirectorySet vmfText;

    public VMFTextSourceVirtualDirectoryImpl(String parentDisplayName, SourceDirectorySetFactory sourceDirectorySetFactory) {
        final String displayName = parentDisplayName + " VMF-Text source";
        vmfText = sourceDirectorySetFactory.create(displayName);
        vmfText.getFilter().include("**/*.g4");
    }

    @Override
    public SourceDirectorySet getVMFText() {
        return vmfText;
    }

    @Override
    public VMFTextSourceVirtualDirectory vmfText(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getVMFText());
        return this;
    }

    @Override
    public VMFTextSourceVirtualDirectory vmfText(Action<? super SourceDirectorySet> configureAction) {
        configureAction.execute(getVMFText());
        return this;
    }

    @Override
    public TypeOf<?> getPublicType() {
        return typeOf(VMFTextSourceVirtualDirectory.class);
    }
}

class VMFTextPlugin implements Plugin<Project> {

    private SourceDirectorySetFactory sourceDirectorySetFactory;

    @Inject
    public VMFTextPlugin(SourceDirectorySetFactory sourceDirectorySetFactory) {
        this.sourceDirectorySetFactory = sourceDirectorySetFactory;
    }

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        def extension = project.extensions.create('vmfText', VMFTextPluginExtension)

        project.repositories {
            mavenLocal()
            mavenCentral()
            jcenter()
        }

//        project.configurations(
//                {
//                    vmfText {
//                        description = "VMF-Text Dependencies"
//                        transitive = true
//                    }
//                }
//        )

        project.dependencies {
            //vmfText    group: 'eu.mihosoft.vmf', name: 'vmf-text',          version: extension.version
            compile    group: 'eu.mihosoft.vmf', name: 'vmf-runtime',       version: extension.vmfVersion
            compile    group: 'org.antlr',       name: 'antlr4-runtime',    version: extension.antlrVersion
        }

        // load VMF class (depending on version)
//        def vmfTextClassPath = []
//        project.configurations.vmfText.each { entry ->
//            vmfTextClassPath.add(entry.toURI().toURL())
//        }
//
//        def classLoader = new URLClassLoader(vmfTextClassPath as URL[])
        def vmfTextClass = eu.mihosoft.vmf.vmftext.VMFText.class;//.loadClass("eu.mihosoft.vmf.vmftext.VMFText")
        // we need to set this classloader because otherwise vmf-text won't find the correct
        // vmf core classes
        //vmfTextClass.setCompileClassLoader(classLoader);

        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(
                new Action<SourceSet>() {
                    public void execute(final SourceSet sourceSet) {
                        // For each source set we will:
                        //
                        // 1) Add a new 'vmfText' virtual directory mapping
                        final VMFTextSourceVirtualDirectoryImpl vmfTextDirectoryDelegate =
                                new VMFTextSourceVirtualDirectoryImpl(
                                        ((DefaultSourceSet) sourceSet).getDisplayName(),
                                        sourceDirectorySetFactory
                                );
                        new DslObject(sourceSet).getConvention().getPlugins().put(
                                VMFTextSourceVirtualDirectory.NAME, vmfTextDirectoryDelegate
                        );
                        final String srcDir = "src/" + sourceSet.getName() + "/vmf-text";
                        SourceDirectorySet sourceDirectorySet = vmfTextDirectoryDelegate.getVMFText();
                        sourceDirectorySet.srcDir(srcDir);
                        sourceSet.getAllSource().source(vmfTextDirectoryDelegate.getVMFText());

                        // 2) Create an VMFTextTask for this sourceSet following the gradle
                        //    naming conventions via call to sourceSet.getTaskName()
                        final String taskName = sourceSet.getTaskName("vmfTextGen", "Code");

                        // 3) Set up the VMFText output directory (adding to javac inputs!)
                        final String outputDirectoryName =
                                project.getBuildDir().absolutePath+"/generated-src/vmf-text/" + sourceSet.getName();
                        final File outputDirectory = new File(outputDirectoryName);
                        sourceSet.getJava().srcDir(outputDirectory);
                        final String modelOutputDirectoryName =
                                project.getBuildDir().absolutePath+"/generated-src/vmf-text-modelgen/" + sourceSet.getName();
                        final File modelOutputDirectory = new File(modelOutputDirectoryName);

                        project.getTasks().create(taskName, CompileVMFTextTask.class, new Action<CompileVMFTextTask>() {
                            @Override
                            void execute(CompileVMFTextTask vmfTextTask) {
                                // 4) Set up convention mapping for default sources (allows user to not have to specify)
                                //    and set up a task description
                                vmfTextTask.setDescription(
                                        "Processes the " + sourceSet.getName() + " VMFText grammars."
                                );
                                vmfTextTask.group = "vmf-text"
                                vmfTextTask.inputFiles = vmfTextDirectoryDelegate.getVMFText() as FileCollection;
                                vmfTextTask.outputFolder = outputDirectory;
                                vmfTextTask.modelOutputDirectory = modelOutputDirectory;
                                vmfTextTask.sourceSet = sourceSet;
                                vmfTextTask.sourceDirectorySet = sourceDirectorySet;
                                vmfTextTask.vmfTextClass = vmfTextClass;
                            }
                        });

                        // 5) register fact that vmf-text should be run before compiling
                        project.tasks.getByName(sourceSet.getCompileJavaTaskName()).dependsOn(taskName)

                        final String cleanTaskName = sourceSet.getTaskName("vmfTextClean", "");

                        // 6) clean the generated code and vmf model
                        project.task(cleanTaskName, group: 'vmf-text',
                                description: 'Cleans generated VMF & Java code.') {
                            doLast {
                                outputDirectory.listFiles().each {
                                    f -> f.deleteDir()
                                }
                                modelOutputDirectory.listFiles().each {
                                    f -> f.deleteDir()
                                }
                            }
                        }
                    }
                });
    }
}

/**
 * Generates language models from *.g4 grammars
 */
class CompileVMFTextTask extends DefaultTask {

    @InputFiles
    FileCollection inputFiles;

    @OutputDirectory
    File outputFolder;

    File modelOutputDirectory;

    SourceSet sourceSet;
    SourceDirectorySet sourceDirectorySet;

    Class<?> vmfTextClass;

    @TaskAction
    void vmfTextGenModelSources(IncrementalTaskInputs inputs) {

//        // directory set
//        println(" -> directories:")
//        for(File f : sourceDirectorySet.srcDirs) {
//            println("   --> dir:  " + f)
//        }
//
//        // all inputs
//        println(" -> all inputs:")
//        for (File f : inputFiles) {
//            println("   --> file: " + f)
//        }
//
//        // inputs that are out of date
//        println(" -> out-of-date inputs:")
//        inputs.outOfDate {
//            println("   --> file: " + it.file)
//        }
//
//        // output directory
//        println(" -> output directory:")
//        println("   --> folder: " + outputFolder)

        checkValidFileStructure();

        // call VMFText.generate(...)

        def grammarsOutOfDate = []

        inputs.outOfDate {
            if(it.file.isFile()) {
                grammarsOutOfDate.add(it.file)
            }
        }

        for(File gF : grammarsOutOfDate) {
            String packageName = this.getPackageNameFromFile(gF);
            println("  -> processing file: " + gF)

            this.vmfTextClass.generate(
                    // grammar file
                    gF,
                    // desired package name
                    packageName,
                    // desired output directory
                    outputFolder,
                    // model output dir for debugging
                    modelOutputDirectory
            )
        }
    }

    private void checkValidFileStructure() {
        Map<String,List<String>> numFilesPerPackageName = new HashMap<>();

        for(File f : inputFiles) {

            String filePath = f.absolutePath;
            String key = getPackageNameFromFile(f);

            List<String> filesPerPackage = numFilesPerPackageName.get(key);
            if(filesPerPackage==null) {
                filesPerPackage = [];
                numFilesPerPackageName.put(key,filesPerPackage);
            }

            filesPerPackage.add(filePath);

        }

        boolean hasEmptyKeys = numFilesPerPackageName.keySet().stream().
                filter({k->k.trim().isEmpty()}).count() > 0;

        String emptyKeyMsg = "Grammar files in default package are not supported:\n";

        for(Map.Entry<String,List<String>> entry : numFilesPerPackageName.entrySet()) {
            if(entry.getKey().trim().isEmpty()) {
                for(String entryPath : entry.value) {
                    emptyKeyMsg += " -> " + entryPath + "\n";
                }
            }
        }

        if(hasEmptyKeys) {
            throw new RuntimeException(emptyKeyMsg);
        }

        for(List<String> filesPerPackage : numFilesPerPackageName.values()) {
            if(filesPerPackage.size()>1) {

                String msg = "Error: multiple grammar files per packages are not supported!\n";

                for(String fName : filesPerPackage) {
                    msg += " -> " + fName + "\n";
                }

                throw new RuntimeException(msg);
            }
        }
    }

    private String getPackageNameFromFile(File file) {

        // We want to figure out the package name. There fore we
        // - remove the front part, e.g., '/Users/myname/path/to/project/src/main/vmf-text'
        // - remove the file name from end of the remaining string, e.g., 'MyGrammar.g4'
        // - the result is the package name
        for (File dir : sourceDirectorySet.srcDirs) {

            String absolutePath = dir.absolutePath;
            String packageName = file.absolutePath;

            if (packageName.startsWith(absolutePath)) {

                packageName = packageName.substring(absolutePath.length(), packageName.length());

                if(packageName.endsWith(file.getName())) {
                    packageName = packageName.substring(0,packageName.length()-file.getName().length());
                }

                if(packageName.startsWith(File.separator)) {
                    packageName = packageName.substring(1,packageName.length());
                }

                if(packageName.endsWith(File.separator)) {
                    packageName = packageName.substring(0,packageName.length()-1);
                }

                packageName = packageName.replace(File.separator,'.');

                return packageName;
            }
        }

        throw new RuntimeException("Cannot detect package name of " + file.absolutePath);
    }
}
