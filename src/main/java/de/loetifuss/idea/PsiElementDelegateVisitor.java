package de.loetifuss.idea;

import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.light.LightClass;
import com.intellij.psi.impl.light.LightField;
import com.intellij.psi.impl.light.LightMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A visitor that creates a given PsiElement which delegates the call to isEquivalentTo() to the element to be searched
 * in the source file. All other methods call the implementation of the .class file.
 */
public class PsiElementDelegateVisitor extends JavaElementVisitor {

	private final PsiElement searchElement;

	private PsiElement delegate;

	public PsiElementDelegateVisitor(PsiElement searchElement) {
		this.searchElement = searchElement;
	}

	// default
	@Override
	public void visitElement(@NotNull PsiElement element) {
		this.delegate = element;
	}

	@Override
	public void visitField(@NotNull PsiField field) {
		this.delegate = new DelegatingPsiField(field, (PsiField) searchElement);
	}

	@Override
	public void visitMethod(@NotNull PsiMethod method) {
		this.delegate = new DelegatingPsiMethod(method, (PsiMethod) searchElement);
	}

	@Override
	public void visitClass(@NotNull PsiClass aClass) {
		this.delegate = new DelegatingPsiClass(aClass, (PsiClass) searchElement);
	}

	public PsiElement getDelegate() {
		return delegate;
	}

}

class DelegatingPsiClass extends LightClass {
	private final PsiClass searchedClass;

	public DelegatingPsiClass(@NotNull PsiClass delegate, @NotNull PsiClass searchedClass) {
		super(delegate);
		this.searchedClass = searchedClass;
	}

	@Override
	public boolean isEquivalentTo(PsiElement another) {
		return searchedClass.isEquivalentTo(another);
	}
}

class DelegatingPsiField extends LightField {
	private final PsiField searchedField;

	public DelegatingPsiField(@NotNull PsiField delegate, @NotNull PsiField searchedField) {
		super(searchedField.getManager(), delegate, Objects.requireNonNull(delegate.getContainingClass()));
		this.searchedField = searchedField;
	}

	@Override
	public boolean isEquivalentTo(PsiElement another) {
		return searchedField.isEquivalentTo(another);
	}
}

class DelegatingPsiMethod extends LightMethod {
	private final PsiMethod searchedMethod;

	public DelegatingPsiMethod(@NotNull PsiMethod delegate, @NotNull PsiMethod searchedMethod) {
		super(searchedMethod.getManager(), delegate, Objects.requireNonNull(delegate.getContainingClass()));
		this.searchedMethod = searchedMethod;
	}

	@Override
	public boolean isEquivalentTo(PsiElement another) {
		return searchedMethod.isEquivalentTo(another);
	}
}