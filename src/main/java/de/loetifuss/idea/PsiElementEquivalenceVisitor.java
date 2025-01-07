package de.loetifuss.idea;

import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

/**
 * A visitor that checks if a given PsiElement is equivalent to another PsiElement.
 * Elements from .class files and .java files are considered equivalent if they are based on the same class.
 * IntelliJ usually ignores files in the "build" directory, so the check must be overridden here.
 */
public class PsiElementEquivalenceVisitor extends JavaElementVisitor {

	private final PsiElement other;

	private boolean equivalent = false;

	public PsiElementEquivalenceVisitor(PsiElement other) {
		this.other = other;
	}

	// default
	@Override
	public void visitElement(@NotNull PsiElement element) {
		equivalent = other.isEquivalentTo(element);
	}

	@Override
	public void visitField(@NotNull PsiField field) {
		equivalent = PsiClassImplUtil.isFieldEquivalentTo(field, other);
	}

	@Override
	public void visitMethod(@NotNull PsiMethod method) {
		equivalent = PsiClassImplUtil.isMethodEquivalentTo(method, other);
	}

	@Override
	public void visitClass(@NotNull PsiClass aClass) {
		equivalent = PsiClassImplUtil.isClassEquivalentTo(aClass, other);
	}

	public boolean isEquivalent() {
		return equivalent;
	}

}