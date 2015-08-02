package com.intellij.plugin.buck.build;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.plugin.buck.config.BuckSettingsProvider;
import com.intellij.plugin.buck.ui.BuckToolWindowFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Run buck build commands in background thread, then parse the output from stderr and print to
 * IntelliJ's console window.
 * <p/>
 * We can also create a websocket client and collect information from buck's local http server.
 * However, http server of buck is still immaturity, lots of useful information is not available.
 * For example, we can't get compiler error outputs. Therefore the only way for us is parsing the
 * stderr output of the buck process.
 */
public class BuckBuildManager {

  private static BuckBuildManager sInstance;

  private ProgressIndicator mProgressIndicator;
  private boolean mIsBuilding = false;
  private boolean mIsKilling = false;
  private Boolean mIsBuckProject;

  private BuckBuildManager() {
    new BuckProjectListener();
  }

  public static synchronized BuckBuildManager getInstance() {
    if (sInstance == null) {
      sInstance = new BuckBuildManager();
    }
    return sInstance;
  }

  public synchronized void setProgress(double fraction) {
    if (mProgressIndicator == null) {
      return;
    }
    mProgressIndicator.setFraction(fraction);
    mProgressIndicator.checkCanceled();
  }

  /**
   * Get saved target for this project from settings
   */
  public String getCurrentSavedTarget(Project project) {
    if (BuckSettingsProvider.getInstance().getState().lastAlias == null) {
      return null;
    }
    return BuckSettingsProvider.getInstance().getState().lastAlias.get(project.getBasePath());
  }

  public boolean isBuilding() {
    return mIsBuilding;
  }

  public synchronized void setBuilding(boolean value) {
    mIsBuilding = value;
    BuckToolWindowFactory.updateActionsNow();
  }

  public boolean isKilling() {
    return mIsKilling;
  }

  public synchronized void setKilling(boolean value) {
    mIsKilling = value;
    BuckToolWindowFactory.updateActionsNow();
  }

  public boolean isBuckProject(Project project) {
    if (project == null) {
      return false;
    }
    if (mIsBuckProject == null) {
      mIsBuckProject = project.getBaseDir().findChild(BuckBuildUtil.BUCK_CONFIG_FILE) != null;
    }
    return mIsBuckProject;
  }

  public void setBuckProject(boolean isBuckProject) {
    mIsBuckProject = isBuckProject;
  }

  /**
   * Print "no selected target" error message to console window
   * Also provide a hyperlink which can directly jump to "Choose Target" GUI window
   */
  public void showNoTargetMessage() {
    BuckToolWindowFactory.outputConsoleMessage("Please ", ConsoleViewContentType.ERROR_OUTPUT);
    BuckToolWindowFactory.outputConsoleHyperlink(
        "choose a build target!\n",
        new HyperlinkInfo() {
          @Override
          public void navigate(Project project) {
            JComponent frame = WindowManager.getInstance().getIdeFrame(project).getComponent();
            AnAction action = ActionManager.getInstance().getAction("buck.ChooseTarget");
            action.actionPerformed(
                new AnActionEvent(null, DataManager.getInstance().getDataContext(frame),
                    ActionPlaces.UNKNOWN, action.getTemplatePresentation(),
                    ActionManager.getInstance(), 0)
            );
          }
        });
  }

  /**
   * Execute simple process asynchronously with progress
   *
   * @param handler        a handler
   * @param operationTitle an operation title shown in progress dialog
   */
  public void runBuckCommand(final BuckCommandHandler handler,
                                    final String operationTitle) {
    // Always save files
    FileDocumentManager.getInstance().saveAllDocuments();

    String exec = BuckSettingsProvider.getInstance().getState().buckExecutable;
    if (exec == null) {
      BuckToolWindowFactory.outputConsoleMessage("Please specify the buck executable path!\n",
          ConsoleViewContentType.ERROR_OUTPUT);
      BuckToolWindowFactory.outputConsoleMessage(
          "Preference -> Tools -> Buck -> Path to Buck executable\n",
          ConsoleViewContentType.NORMAL_OUTPUT);
      return;
    }

    final ProgressManager manager = ProgressManager.getInstance();
    manager.run(new Task.Backgroundable(handler.project(), operationTitle, true) {
      public void run(@NotNull final ProgressIndicator indicator) {
        mProgressIndicator = indicator;
        runInCurrentThread(handler, indicator, true, operationTitle);
      }
    });
  }

  /**
   * Run handler in the current thread
   *
   * @param handler              a handler to run
   * @param indicator            a progress manager
   * @param setIndeterminateFlag if true handler is configured as indeterminate
   * @param operationName
   */
  public void runInCurrentThread(final BuckCommandHandler handler,
                                        final ProgressIndicator indicator,
                                        final boolean setIndeterminateFlag,
                                        @Nullable final String operationName) {
    runInCurrentThread(handler, new Runnable() {
      public void run() {
        if (indicator != null) {
          indicator.setText(operationName);
          indicator.setText2("");
          if (setIndeterminateFlag) {
            indicator.setIndeterminate(true);
          }
        }
      }
    });
  }

  /**
   * Run handler in the current thread
   *
   * @param handler         a handler to run
   * @param postStartAction an action that is executed
   */
  public void runInCurrentThread(
      final BuckCommandHandler handler, @Nullable final Runnable postStartAction) {
    handler.runInCurrentThread(postStartAction);
  }
}