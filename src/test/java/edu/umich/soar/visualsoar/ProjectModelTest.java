package edu.umich.soar.visualsoar;

import edu.umich.soar.visualsoar.operatorwindow.OperatorRootNode;
import edu.umich.soar.visualsoar.util.IdGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProjectModelTest {

  @Test
  void newProject() throws IOException, URISyntaxException {
    Path tempDir = Files.createTempDirectory("roundTripOperatorWindow");
    ProjectModel pm;
    // Force use of sequential IDs to make the project output deterministic.
    // TODO: would be nicer if we could inject an ID provider instead
    try (MockedStatic<IdGenerator> mockedStatic = Mockito.mockStatic(IdGenerator.class)) {
      AtomicInteger counter = new AtomicInteger();
      mockedStatic
          .when(IdGenerator::getId)
          .thenAnswer(
              (invocation -> {
                counter.getAndIncrement();
                return counter.toString();
              }));
      pm = ProjectModel.newProject("foo", tempDir);
    }

    OperatorRootNode root = (OperatorRootNode) pm.operatorHierarchy.getRoot();

    assertTrue(root.isJson(), "New projects should always be JSON");

    assertEquals(tempDir, pm.swmm.getDmPath());
    // needing getParent() here was totally unexpected! I think the API should be changed to not use
    // the parent like this
    assertEquals(tempDir.getParent(), Paths.get(root.getFullPathStart()));
    assertEquals(tempDir.getParent().resolve("foo"), Paths.get(root.getFolderName()));
    assertEquals(tempDir.getParent().resolve("foo.vsa.json"), Paths.get(root.getProjectFile()));

    Path expectedJsonPath =
        Paths.get(ProjectModelTest.class.getResource("expected_new_project.vsa.json").toURI());

    pm.writeProject(new File(root.getProjectFile()));

    Path actualJsonPath = Paths.get(root.getProjectFile());
    // line-ending fix required in Windows for some reason
    String actualJsonRaw = Files.readString(actualJsonPath).replaceAll("\r\n", "\n");
    String expectedJsonRaw = Files.readString(expectedJsonPath).replaceAll("\r\n", "\n");
    assertEquals(expectedJsonRaw, actualJsonRaw);
  }
}
