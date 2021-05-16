package com.github.xeonkryptos.integration.gitlab.settings.ui;

import com.github.xeonkryptos.integration.gitlab.api.GitlabUserApi;
import com.github.xeonkryptos.integration.gitlab.internal.messaging.GitlabLoginChangeNotifier;
import com.github.xeonkryptos.integration.gitlab.service.data.GitlabAccount;
import com.github.xeonkryptos.integration.gitlab.ui.cloneDialog.TokenLoginUI;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AddGitlabSettingsEntryDialog extends JDialog implements Disposable {

    private final TokenLoginUI tokenLoginUI;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel settingsEntryPane;

    public AddGitlabSettingsEntryDialog(Project project) {
        setTitle("Add New Entry");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setSize(400, 300);
        setResizable(true);
        setLocationRelativeTo(null);

        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // TODO: Using pack is far from perfect, but there isn't an easy way to update the size depending on the errorLabel text that I'm aware of. It's only a small detail. Maybe the error message will only be available
        //  in the logs or with a different UI design, like an error label independent from tokenLoginPanel.
        tokenLoginUI = new TokenLoginUI(project, new GitlabUserApi(project), this::pack);
        DialogPanel tokenLoginPanel = tokenLoginUI.getTokenLoginPanel();
        settingsEntryPane.add(tokenLoginPanel);

        final Application application = ApplicationManager.getApplication();
        MessageBusConnection connection = application.getMessageBus().connect(this);
        connection.subscribe(GitlabLoginChangeNotifier.LOGIN_STATE_CHANGED_TOPIC, new GitlabLoginChangeNotifier() {
            @Override
            public void onSignIn(@NotNull GitlabAccount gitlabAccount) {
                Runnable action = () -> buttonOK.setEnabled(true);
                if (SwingUtilities.isEventDispatchThread()) {
                    action.run();
                } else {
                    SwingUtilities.invokeLater(action);
                }
            }

            @Override
            public void onSignOut(@NotNull GitlabAccount gitlabAccount) {}
        });
    }

    private void onOK() {
        Disposer.dispose(this);
    }

    private void onCancel() {
        Disposer.dispose(this);
        tokenLoginUI.cancel();
    }
}
