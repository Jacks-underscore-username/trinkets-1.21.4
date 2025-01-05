package jacksunderscoreusername.trinkets.quest;

import jacksunderscoreusername.trinkets.StateSaverAndLoader;
import jacksunderscoreusername.trinkets.dialog.DialogPage;

abstract public class Task {
    public StateSaverAndLoader.StoredData.currentPlayerQuestsEntry entry;

    abstract public String encode();

    abstract public DialogPage getPage();
}
