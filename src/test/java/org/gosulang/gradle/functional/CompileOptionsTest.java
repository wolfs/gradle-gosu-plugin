package org.gosulang.gradle.functional;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CompileOptionsTest extends AbstractGradleTest {

  @Rule
  public final TemporaryFolder _testProjectDir = new TemporaryFolder();

  private File _javaSourceRoot;
  private File _gosuSourceRoot;
  private File _javaTestRoot;
  private File _gosuTestRoot;
  private File _buildFile;
  private File _pojo;
  private File _pogo;
  private File _enhancement;
  private File _javaTest;
  private File _gosuTest;

  @BeforeClass
  public static void beforeClass() {
    //noop
  }

  @Before
  public void beforeMethod() throws IOException {
    _buildFile = _testProjectDir.newFile("build.gradle");
    _javaSourceRoot = _testProjectDir.newFolder("src", "main", "java");
    _gosuSourceRoot = _testProjectDir.newFolder("src", "main", "gosu");
    _javaTestRoot = _testProjectDir.newFolder("src", "test", "java");
    _gosuTestRoot = _testProjectDir.newFolder("src", "test", "gosu");
  }

  @Test
  public void passBuildEvenWithFailingTestIfOptionIsSet() throws IOException {
    String buildFileContent = getBasicBuildScriptForTesting();
    buildFileContent += LF +
        "compileGosu {" + LF +
        "    options.failOnError = false" + LF +
//        "    options.fork = true" + LF + //TODO setting fork = true causes NPE.
        "}";
    writeFile(_buildFile, buildFileContent);

    String errantPogoContent =
        "package example.gradle" + LF +
            LF +
            "public class ErrantPogo {" + LF +
            "  function doIt() {" + LF +
            "    var x : int = \"Intentional error\"" + LF +
            "  }" + LF +
            "}" + LF;

    String simplePogoContent =
        "package example.gradle" + LF +
            LF +
            "public class SimplePogo {" + LF +
            "  function doIt() {" + LF +
            "    var x = 1" + LF +
            "  }" + LF +
            "}" + LF;

    _pogo = new File(_gosuSourceRoot, "example/gradle/ErrantPogo.gs");
    _pogo.getParentFile().mkdirs();
    writeFile(_pogo, errantPogoContent);

    _pogo = new File(_gosuSourceRoot, "example/gradle/SimplePogo.gs");
    _pogo.getParentFile().mkdirs();
    writeFile(_pogo, simplePogoContent);
    
    System.out.println("--- Dumping build.gradle ---");
    System.out.println(buildFileContent);
    System.out.println("--- Done dumping build.gradle ---");

    GradleRunner runner = GradleRunner.create();
    BuildResult result = null;
    try {
      runner.withProjectDir(_testProjectDir.getRoot())
          .withArguments("compileGosu", "-is");
      result = runner.build();
    } catch (UnexpectedBuildFailure e) {
      fail(e.getMessage());
    }

    System.out.println("--- Dumping stdout ---");
    System.out.println(result.getStandardOutput());
    System.out.println("--- Done dumping stdout ---");
    System.out.println();
    System.out.println("--- Dumping stderr ---");
    System.out.println(result.getStandardError());
    System.out.println("--- Done dumping stderr ---");

    assertTrue(result.getStandardOutput().contains("Initializing Gosu compiler..."));
    assertTrue(result.getStandardOutput().contains("Gosu compilation completed with 1 error:"));
    assertTrue(result.getStandardOutput().contains("Gosu Compiler: Ignoring compilation failure as 'failOnError' was set to false"));
    assertTrue(result.getStandardError().isEmpty());
    assertEquals(TaskOutcome.SUCCESS, result.task(":compileGosu").getOutcome());

    //did we actually compile anything?
    assertTrue(new File(_testProjectDir.getRoot(), "build/classes/main/example/gradle/SimplePogo.class").exists());
    assertFalse(new File(_testProjectDir.getRoot(), "build/classes/main/example/gradle/ErrantPogo.class").exists());
  }
}