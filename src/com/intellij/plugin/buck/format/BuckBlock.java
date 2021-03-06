package com.intellij.plugin.buck.format;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.formatting.WrapType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import static com.intellij.plugin.buck.format.BuckFormatUtil.hasElementType;
import com.intellij.plugin.buck.lang.psi.BuckArrayElements;
import com.intellij.plugin.buck.lang.psi.BuckListElements;
import com.intellij.plugin.buck.lang.psi.BuckObjectElements;
import com.intellij.plugin.buck.lang.psi.BuckRuleBody;
import com.intellij.plugin.buck.lang.psi.BuckTypes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A BuckBlock is actually a abstract syntax tree block, which mainly used for formatting a buck
 * code block.
 */
public class BuckBlock implements ASTBlock {

  private static final TokenSet BUCK_CONTAINERS = TokenSet.create(
      BuckTypes.ARRAY_ELEMENTS,
      BuckTypes.RULE_BODY,
      BuckTypes.LIST_ELEMENTS,
      BuckTypes.OBJECT_ELEMENTS);

  private static final TokenSet BUCK_OPEN_BRACES =
      TokenSet.create(BuckTypes.L_BRACKET, BuckTypes.L_PARENTHESES, BuckTypes.L_CURLY);

  private static final TokenSet BUCK_CLOSE_BRACES =
      TokenSet.create(BuckTypes.R_BRACKET, BuckTypes.R_PARENTHESES, BuckTypes.R_CURLY);

  private static final TokenSet BUCK_ALL_BRACES =
      TokenSet.orSet(BUCK_OPEN_BRACES, BUCK_CLOSE_BRACES);

  private final BuckBlock myParent;
  private final Alignment myAlignment;
  private final Indent myIndent;
  private final PsiElement myPsiElement;
  private final ASTNode myNode;
  private final Wrap myWrap;
  private final Wrap myChildWrap;
  private final CodeStyleSettings mySettings;
  private final SpacingBuilder mySpacingBuilder;

  private List<BuckBlock> mySubBlocks = null;

  public BuckBlock(@Nullable final BuckBlock parent,
                   @NotNull final ASTNode node,
                   @NotNull CodeStyleSettings settings,
                   @Nullable final Alignment alignment,
                   @NotNull final Indent indent,
                   @Nullable final Wrap wrap) {
    myParent = parent;
    myAlignment = alignment;
    myIndent = indent;
    myNode = node;
    myPsiElement = node.getPsi();
    myWrap = wrap;
    mySettings = settings;

    mySpacingBuilder = BuckFormattingModelBuilder.createSpacingBuilder(settings);

    if (myPsiElement instanceof BuckArrayElements ||
        myPsiElement instanceof BuckRuleBody ||
        myPsiElement instanceof BuckListElements ||
        myPsiElement instanceof BuckObjectElements) {
      myChildWrap = Wrap.createWrap(CommonCodeStyleSettings.WRAP_ALWAYS, true);
    } else {
      myChildWrap = null;
    }
  }

  @Override
  public ASTNode getNode() {
    return myNode;
  }

  @NotNull
  @Override
  public TextRange getTextRange() {
    return myNode.getTextRange();
  }

  @NotNull
  @Override
  public List<Block> getSubBlocks() {
    if (mySubBlocks == null) {
      mySubBlocks = buildSubBlocks();
    }
    return new ArrayList<Block>(mySubBlocks);
  }

  /**
   * Recursively build sub blocks
   */
  private List<BuckBlock> buildSubBlocks() {
    final List<BuckBlock> blocks = new ArrayList<BuckBlock>();
    for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      final IElementType childType = child.getElementType();

      if (child.getTextRange().isEmpty()) {
        continue;
      }
      if (childType == TokenType.WHITE_SPACE) {
        continue;
      }
      // In most cases, glob block doesn't need reformatting. So just ignore it for now.
      if (childType == BuckTypes.GLOB_BLOCK) {
        continue;
      }
      blocks.add(buildSubBlock(child));
    }
    return Collections.unmodifiableList(blocks);
  }

  private BuckBlock buildSubBlock(ASTNode childNode) {
    Indent indent = Indent.getNoneIndent();
    Alignment alignment = null;
    Wrap wrap = null;

    if (hasElementType(myNode, BUCK_CONTAINERS)) {
      if (hasElementType(childNode, BuckTypes.COMMA)) {
        wrap = Wrap.createWrap(WrapType.NONE, true);
      } else if (!hasElementType(childNode, BUCK_ALL_BRACES)) {
        assert myChildWrap != null;
        wrap = myChildWrap;
        indent = Indent.getNormalIndent();
      }
    }
    return new BuckBlock(this, childNode, mySettings, alignment, indent, wrap);
  }

  @Nullable
  @Override
  public Wrap getWrap() {
    return myWrap;
  }

  @Nullable
  @Override
  public Indent getIndent() {
    assert myIndent != null;
    return myIndent;
  }

  @Nullable
  @Override
  public Alignment getAlignment() {
    return myAlignment;
  }

  @Nullable
  @Override
  public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
    return mySpacingBuilder.getSpacing(this, child1, child2);
  }

  @NotNull
  @Override
  public ChildAttributes getChildAttributes(int newChildIndex) {
    if (hasElementType(myNode, BUCK_CONTAINERS)) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    } else if (myNode.getPsi() instanceof PsiFile) {
      return new ChildAttributes(Indent.getNoneIndent(), null);
    } else {
      return new ChildAttributes(null, null);
    }
  }

  @Override
  public boolean isIncomplete() {
    final ASTNode lastChildNode = myNode.getLastChildNode();

    boolean ret = false;
    if (hasElementType(myNode, TokenSet.create(BuckTypes.ARRAY_ELEMENTS))) {
      ret = lastChildNode != null && lastChildNode.getElementType() != BuckTypes.R_BRACKET;
    } else if (hasElementType(myNode, TokenSet.create(BuckTypes.RULE_BODY))) {
      ret = lastChildNode != null && lastChildNode.getElementType() != BuckTypes.R_PARENTHESES;
    } else if (hasElementType(myNode, TokenSet.create(BuckTypes.LIST_ELEMENTS))) {
      ret = lastChildNode != null && lastChildNode.getElementType() != BuckTypes.R_PARENTHESES;
    } else if (hasElementType(myNode, TokenSet.create(BuckTypes.OBJECT_ELEMENTS))) {
      ret = lastChildNode != null && lastChildNode.getElementType() != BuckTypes.R_CURLY;
    }
    return ret;
  }

  @Override
  public boolean isLeaf() {
    return myNode.getFirstChildNode() == null;
  }
}
