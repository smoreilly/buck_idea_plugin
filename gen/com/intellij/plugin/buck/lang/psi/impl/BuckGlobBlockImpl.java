// This is a generated file. Not intended for manual editing.
package com.intellij.plugin.buck.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugin.buck.lang.psi.BuckTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.plugin.buck.lang.psi.*;

public class BuckGlobBlockImpl extends ASTWrapperPsiElement implements BuckGlobBlock {

  public BuckGlobBlockImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BuckVisitor) ((BuckVisitor)visitor).visitGlobBlock(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public BuckGlobElements getGlobElements() {
    return findNotNullChildByClass(BuckGlobElements.class);
  }

  @Override
  @NotNull
  public PsiElement getGlobKeyword() {
    return findNotNullChildByType(GLOB_KEYWORD);
  }

}
