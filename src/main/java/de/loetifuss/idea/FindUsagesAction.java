package de.loetifuss.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FindUsagesAction extends com.intellij.find.actions.FindUsagesAction {

    @Override
    protected void startFindUsages(@NotNull PsiElement elementToSearch) {
        if (isInProjectContent(elementToSearch)) {
            // external references to Java source files of the project must be searched via the compiled
            // class, otherwise IntelliJ only finds source references in the local project
            List<PsiElement> searchElements = findElementsByCompiledClass(elementToSearch, elementToSearch.getProject());
            searchElements.forEach(searchElement -> {
                // create elements that delegate the call to isEquivalentTo() to the original element
                PsiElementDelegateVisitor visitor = new de.loetifuss.idea.PsiElementDelegateVisitor(elementToSearch);
                searchElement.accept(visitor);

                super.startFindUsages(visitor.getDelegate());
            });
        } else {
            super.startFindUsages(elementToSearch);
        }
    }

    private List<PsiElement> findElementsByCompiledClass(PsiElement searchElement, Project project) {
        Optional<ClsFileImpl> classFile = findClassFile(searchElement, project);
        if (classFile.isEmpty()) {
            Messages.showErrorDialog("No .class file found for: " + searchElement.toString(), "Error");
            return Collections.emptyList();
        }
        return findElementsInPsiClass(searchElement, classFile.get().getClasses());
    }

    private boolean isInProjectContent(PsiElement element) {
        return ProjectFileIndex.getInstance(element.getProject())
                .isInContent(element.getContainingFile().getVirtualFile());
    }

    /**
     * Finds the class file for the given search element.
     *
     * @param searchElement The PSI element to search for, e.g. a class, method, constant,
     *                      etc.
     * @param project       The current project.
     * @return An Optional containing the found class file, or an empty
     * Optional if no class file was found.
     */
    private static Optional<ClsFileImpl> findClassFile(PsiElement searchElement, Project project) {
        VirtualFile sourceRoot = ProjectFileIndex.getInstance(project)
                .getSourceRootForFile(searchElement.getContainingFile().getVirtualFile());
        if (sourceRoot == null || sourceRoot.getParent() == null || sourceRoot.getParent().getCanonicalPath() == null) {
            return Optional.empty();
        }
        String sourceSet = sourceRoot.getName().equals("src") ? "main" : sourceRoot.getName();
        String relativePath = VfsUtilCore.getRelativePath(searchElement.getContainingFile().getVirtualFile(),
                sourceRoot);
        if (relativePath == null) {
            return Optional.empty();
        }
        // Assumption: Classes are always located in the 'build/classes/java' directory
        Path classFilePath = Paths.get(sourceRoot.getParent().getCanonicalPath(), "build/classes/java", sourceSet,
                relativePath.replaceAll("\\.java", ".class"));
        VirtualFile virtualClassFile = LocalFileSystem.getInstance().findFileByPath(classFilePath.toString());

        if (virtualClassFile == null) {
            return Optional.empty();
        }

        ClsFileImpl classFile = (ClsFileImpl) PsiManager.getInstance(project).findFile(virtualClassFile);
        if (classFile == null) {
            return Optional.empty();

        }
        return Optional.of(classFile);
    }

    public List<PsiElement> findElementsInPsiClass(PsiElement targetElement, PsiClass... clsFile) {

        List<PsiElement> foundElements = new ArrayList<>();
        Arrays.stream(clsFile).forEach(c -> {
            searchRecursively(c, targetElement, foundElements);
        });

        if (foundElements.isEmpty()) {
            Messages.showErrorDialog(String.format("No matching element %s found in compiled class for %s", targetElement.getText(), targetElement.getContainingFile().getName()), "Error");
        }
        return foundElements;
    }

    private void searchRecursively(PsiElement classFileElement, PsiElement searchedElement,
                                   List<PsiElement> foundElements) {
        try {
            PsiElementEquivalenceVisitor equivalenceCheck = new PsiElementEquivalenceVisitor(searchedElement);
            classFileElement.accept(equivalenceCheck);
            if (equivalenceCheck.isEquivalent()) {
                foundElements.add(classFileElement);
                return;
            }
            for (PsiElement child : classFileElement.getChildren()) {
                searchRecursively(child, searchedElement, foundElements);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}