package com.intellij.plugin.buck.settings;

import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Queue;

@State(
    name = "BuckSettings",
    storages = {
        @Storage(file = StoragePathMacros.PROJECT_FILE),
        @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/buck_settings.xml",
            scheme = StorageScheme.DIRECTORY_BASED)
    }
)
public class BuckSettings implements PersistentStateComponent<BuckSettings.MyState> {

  public static final int MAX_HISTORIES = 10;

  private GlobalState myGlobalState = new GlobalState();

  public static BuckSettings getInstance() {
    return ServiceManager.getService(BuckSettings.class);
  }

  @NotNull
  public GlobalState getGlobalState() {
    return myGlobalState;
  }

  @Override
  public MyState getState() {
    final MyState state = new MyState();
    state.setState(myGlobalState);
    return state;
  }

  @Override
  public void loadState(MyState state) {
    myGlobalState = state.getState();
  }

  public static class MyState {
    private GlobalState myGlobalState = new GlobalState();

    public GlobalState getState() {
      return myGlobalState;
    }

    public void setState(GlobalState state) {
      myGlobalState = state;
    }
  }

  public static class GlobalState {
    public static Queue<String> HISTORICAL_PROJECT_NAMES = new LinkedList<String>();

    public static void addHistory(String project) {
      if (HISTORICAL_PROJECT_NAMES.contains(project)) {
        return;
      }

      HISTORICAL_PROJECT_NAMES.add(project);
      while (HISTORICAL_PROJECT_NAMES.size() > MAX_HISTORIES) {
        HISTORICAL_PROJECT_NAMES.remove();
      }
    }

    @Nullable
    public static String peekHistory() {
      return HISTORICAL_PROJECT_NAMES.peek();
    }
  }
}
