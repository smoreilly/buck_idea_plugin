// This is a generated file. Not intended for manual editing.
package com.intellij.plugin.buck.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface BuckPair extends PsiElement {

  @NotNull
  BuckValue getValue();

  @Nullable
  PsiElement getDoubleQuotedString();

  @Nullable
  PsiElement getSingleQuotedString();

}
