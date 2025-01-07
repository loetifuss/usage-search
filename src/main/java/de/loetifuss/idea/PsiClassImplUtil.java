package de.loetifuss.idea;

import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiImplicitClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.util.TypeConversionUtil;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class PsiClassImplUtil {

	public static boolean isFieldEquivalentTo(@NotNull PsiField field, PsiElement another) {

		if (!(another instanceof PsiField)) {
			return false;
		} else {
			String name1 = field.getName();
			if (!another.isValid()) {
				return false;
			} else {
				String name2 = ((PsiField) another).getName();
				if (!name1.equals(name2)) {
					return false;
				} else {
					PsiClass aClass1 = field.getContainingClass();
					PsiClass aClass2 = ((PsiField) another).getContainingClass();
					// this is the only change to existing IntelliJ Code
					return aClass1 != null && aClass2 != null && isClassEquivalentTo(aClass1, aClass2);
				}
			}
		}
	}

	public static boolean isMethodEquivalentTo(@NotNull PsiMethod method1, PsiElement another) {

		if (method1 == another) {
			return true;
		} else if (!(another instanceof PsiMethod)) {
			return false;
		} else {
			PsiMethod method2 = (PsiMethod) another;
			if (!another.isValid()) {
				return false;
			} else if (!method1.getName().equals(method2.getName())) {
				return false;
			} else {
				PsiClass aClass1 = method1.getContainingClass();
				PsiClass aClass2 = method2.getContainingClass();
				PsiManager manager = method1.getManager();
				// this is the only change to existing IntelliJ Code
				if (aClass1 != null && aClass2 != null && isClassEquivalentTo(aClass1, aClass2)) {
					PsiParameter[] parameters1 = method1.getParameterList().getParameters();
					PsiParameter[] parameters2 = method2.getParameterList().getParameters();
					if (parameters1.length != parameters2.length) {
						return false;
					} else {
						for (int i = 0; i < parameters1.length; ++i) {
							PsiParameter parameter1 = parameters1[i];
							PsiParameter parameter2 = parameters2[i];
							PsiType type1 = parameter1.getType();
							PsiType type2 = parameter2.getType();
							if (!compareParamTypes(manager, type1, type2, new HashSet())) {
								return false;
							}
						}

						return true;
					}
				} else {
					return false;
				}
			}
		}
	}

	public static boolean isClassEquivalentTo(@NotNull PsiClass aClass, PsiElement another) {
		if (aClass == another) {
			return true;
		} else if (!(another instanceof PsiClass)) {
			return false;
		} else if (!another.isValid()) {
			return false;
		} else {
			boolean isImplicitClass = aClass instanceof PsiImplicitClass;
			boolean anotherImplicitClass = another instanceof PsiImplicitClass;
			if (isImplicitClass != anotherImplicitClass) {
				return false;
			} else {
				String name1;
				String name2;
				if (!isImplicitClass) {
					name1 = aClass.getName();
					if (name1 == null) {
						return false;
					}

					name2 = ((PsiClass) another).getName();
					if (name2 == null) {
						return false;
					}

					if (name1.hashCode() != name2.hashCode()) {
						return false;
					}

					if (!name1.equals(name2)) {
						return false;
					}
				}

				name1 = aClass.getQualifiedName();
				name2 = ((PsiClass) another).getQualifiedName();
				if (name1 != null && name2 != null) {
					if (!name1.equals(name2)) {
						return false;
					} else if (aClass.getOriginalElement().equals(another.getOriginalElement())) {
						return true;
					} else {
						PsiFile file1 = getOriginalFile(aClass);
						PsiFile file2 = getOriginalFile((PsiClass) another);
						PsiFile original1 = (PsiFile) file1.getUserData(PsiFileFactory.ORIGINAL_FILE);
						PsiFile original2 = (PsiFile) file2.getUserData(PsiFileFactory.ORIGINAL_FILE);
						if ((original1 != original2 || original1 == null) && original1 != file2 && original2 != file1
								&& file1 != file2) {
							FileIndexFacade fileIndex = FileIndexFacade.getInstance(file1.getProject());
							FileIndexFacade fileIndex2 = FileIndexFacade.getInstance(file2.getProject());
							VirtualFile vfile1 = file1.getViewProvider().getVirtualFile();
							VirtualFile vfile2 = file2.getViewProvider().getVirtualFile();
							boolean lib1 = fileIndex.isInLibraryClasses(vfile1);
							boolean lib2 = fileIndex2.isInLibraryClasses(vfile2);
							boolean inSource1 = fileIndex.isInSource(vfile1);
							boolean inSource2 = fileIndex2.isInSource(vfile2);
							// changed to also check for excluded files
							boolean inExcluded1 = fileIndex.isExcludedFile(vfile1);
							boolean inExcluded2 = fileIndex.isExcludedFile(vfile2);
							return ((inSource1 || lib1) && (inSource2 || lib2) || (inExcluded1 && inSource2)
									|| (inExcluded2 && inSource1));
						} else {
							return true;
						}
					}
				} else if (!Strings.areSameInstance(name1, name2)) {
					return false;
				} else if (aClass instanceof PsiTypeParameter && another instanceof PsiTypeParameter) {
					PsiTypeParameter p1 = (PsiTypeParameter) aClass;
					PsiTypeParameter p2 = (PsiTypeParameter) another;
					if (p1.getIndex() != p2.getIndex()) {
						return false;
					} else if (TypeConversionUtil.areSameFreshVariables(p1, p2)) {
						return true;
					} else {
						return !Boolean.FALSE
								.equals(RecursionManager.doPreventingRecursion(Pair.create(p1, p2), true, () -> {
									return aClass.getManager().areElementsEquivalent(p1.getOwner(), p2.getOwner());
								}));
					}
				} else {
					return false;
				}
			}
		}
	}

	private static @NotNull PsiFile getOriginalFile(@NotNull PsiClass aClass) {
		PsiFile file = aClass.getContainingFile();
		if (file == null) {
			PsiUtilCore.ensureValid(aClass);
			throw new IllegalStateException("No containing file for " + aClass.getLanguage() + " " + aClass.getClass());
		} else {
			return file.getOriginalFile();
		}
	}

	private static boolean compareParamTypes(@NotNull PsiManager manager, @NotNull PsiType type1,
			@NotNull PsiType type2, Set<? super String> visited) {

		if (type1 instanceof PsiArrayType) {
			if (type2 instanceof PsiArrayType) {
				PsiType componentType1 = ((PsiArrayType) type1).getComponentType();
				PsiType componentType2 = ((PsiArrayType) type2).getComponentType();
				if (compareParamTypes(manager, componentType1, componentType2, visited)) {
					return true;
				}
			}

			return false;
		} else if (type1 instanceof PsiClassType && type2 instanceof PsiClassType) {
			PsiClass class1 = ((PsiClassType) type1).resolve();
			PsiClass class2 = ((PsiClassType) type2).resolve();
			visited.add(type1.getCanonicalText());
			visited.add(type2.getCanonicalText());
			if (class1 instanceof PsiTypeParameter && class2 instanceof PsiTypeParameter) {
				if (Objects.equals(class1.getName(), class2.getName())
						&& ((PsiTypeParameter) class1).getIndex() == ((PsiTypeParameter) class2).getIndex()) {
					PsiClassType[] eTypes1 = class1.getExtendsListTypes();
					PsiClassType[] eTypes2 = class2.getExtendsListTypes();
					if (eTypes1.length != eTypes2.length) {
						return false;
					} else {
						for (int i = 0; i < eTypes1.length; ++i) {
							PsiClassType eType1 = eTypes1[i];
							PsiClassType eType2 = eTypes2[i];
							if (visited.contains(eType1.getCanonicalText())
									|| visited.contains(eType2.getCanonicalText())) {
								return false;
							}

							if (!compareParamTypes(manager, eType1, eType2, visited)) {
								return false;
							}
						}

						return true;
					}
				} else {
					return false;
				}
			} else {
				return isClassEquivalentTo(class1, class2);
			}
		} else {
			return type1.equals(type2);
		}
	}

}
