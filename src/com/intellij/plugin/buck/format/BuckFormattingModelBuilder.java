package com.intellij.plugin.buck.format;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugin.buck.lang.BuckLanguage;
import com.intellij.plugin.buck.lang.psi.BuckTypes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuckFormattingModelBuilder implements FormattingModelBuilderEx, CustomFormattingModelBuilder {
  @NotNull
  @Override
  public FormattingModel createModel(@NotNull PsiElement element,
                                     @NotNull CodeStyleSettings settings,
                                     @NotNull FormattingMode mode) {
    //OrderOptimizer.optimzeDeps(element.getContainingFile());

    final BuckBlockContext context =
        new BuckBlockContext(settings, createSpacingBuilder(settings), mode);
    final BuckBlock block =
        new BuckBlock(null, element.getNode(), null, Indent.getNoneIndent(), null, context);

    return FormattingModelProvider.createFormattingModelForPsiFile(
        element.getContainingFile(),
        block,
        settings);
  }

  @Nullable
  @Override
  public CommonCodeStyleSettings.IndentOptions getIndentOptionsToUse(
      @NotNull PsiFile file,
      @NotNull FormatTextRanges ranges,
      @NotNull CodeStyleSettings settings) {
    return null;
  }

  @NotNull
  @Override
  public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
    return createModel(element, settings, FormattingMode.REFORMAT);
  }

  @Nullable
  @Override
  public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
    return null;
  }

  @Override
  public boolean isEngagedToFormat(PsiElement context) {
    PsiFile file = context.getContainingFile();
    return file != null && file.getLanguage() == BuckLanguage.INSTANCE;
  }

  protected SpacingBuilder createSpacingBuilder(CodeStyleSettings settings) {
    final IFileElementType file =
        LanguageParserDefinitions.INSTANCE.forLanguage(BuckLanguage.INSTANCE).getFileNodeType();
//    final PyCodeStyleSettings pySettings = settings.getCustomSettings(PyCodeStyleSettings.class);
//    final TokenSet STATEMENT_OR_DECLARATION =
//        TokenSet.orSet(PythonDialectsTokenSetProvider.INSTANCE.getStatementTokens(), CLASS_OR_FUNCTION);

    final CommonCodeStyleSettings commonSettings =
        settings.getCommonSettings(BuckLanguage.INSTANCE);
    return new SpacingBuilder(commonSettings);
  }
}