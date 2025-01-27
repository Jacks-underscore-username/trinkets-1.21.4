package jacksunderscoreusername.ancient_trinkets.quest;

import jacksunderscoreusername.ancient_trinkets.StateSaverAndLoader;
import jacksunderscoreusername.ancient_trinkets.dialog.DialogPage;

abstract public class Task {
    public StateSaverAndLoader.StoredData.currentPlayerQuestsEntry entry;

    abstract public String encode();

    abstract public DialogPage getPage();
}
