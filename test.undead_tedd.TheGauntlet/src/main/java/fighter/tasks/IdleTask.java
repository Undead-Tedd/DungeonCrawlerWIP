package fighter.tasks;

import com.runemate.game.api.script.framework.tree.LeafTask;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class IdleTask extends LeafTask {

    @Override
    public void execute() {
        log.info("IdleTask: No actions to perform.");
        // This task does nothing, keeping the bot idle
    }
}
