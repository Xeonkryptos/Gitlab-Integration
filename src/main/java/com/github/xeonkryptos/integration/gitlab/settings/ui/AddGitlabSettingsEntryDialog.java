package com.github.xeonkryptos.integration.gitlab.settings.ui;

import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier;
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount;
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.GitlabLoginData;
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.LoginTask;
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.TokenLoginUI;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBusConnection;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AddGitlabSettingsEntryDialog extends DialogWrapper {

    private final Project project;
    private final TokenLoginUI tokenLoginUI;

    public AddGitlabSettingsEntryDialog(Project project) {
        super(project, true, IdeModalityType.IDE);

        this.project = project;

        setTitle("Add New Entry");
        setHorizontalStretch(1.3f);
        centerRelativeToParent();

        tokenLoginUI = new TokenLoginUI(project);
        Disposer.register(getDisposable(), tokenLoginUI);
        init();

        final Application application = ApplicationManager.getApplication();
        MessageBusConnection connection = application.getMessageBus().connect(getDisposable());
        connection.subscribe(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC, new GitlabLoginChangeNotifier() {
            @Override
            public void onSignIn(@NotNull GitlabAccount gitlabAccount) {
                Runnable action = () -> setOKActionEnabled(true);
                if (SwingUtilities.isEventDispatchThread()) {
                    action.run();
                } else {
                    SwingUtilities.invokeLater(action);
                }
            }

            @Override
            public void onSignOut(@NotNull GitlabAccount gitlabAccount) {
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return tokenLoginUI.getTokenLoginPanel();
    }

    @Override
    protected void doOKAction() {
        GitlabLoginData gitlabLoginData = tokenLoginUI.getGitlabLoginData();
        new LoginTask(project, gitlabLoginData, result -> {
            setErrorText(result);
            if (result == null) {
                super.doOKAction();
            }
        }).doLogin();
    }
}
