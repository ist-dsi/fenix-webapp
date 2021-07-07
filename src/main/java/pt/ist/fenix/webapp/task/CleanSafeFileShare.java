package pt.ist.fenix.webapp.task;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.safeFileShare.domain.UserFileShare;
import pt.ist.fenixframework.FenixFramework;

@Task(englishTitle = "Clean user file shares and create any necessary new shares", readOnly = true)
public class CleanSafeFileShare extends CronTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getUserSet().stream()
                .parallel()
                .forEach(this::cleanup);

        final Group group = Group.parse("activeEmployees | activeTeachers");
        group.getMembers().distinct()
                .filter(user -> user.getUserFileShare() == null)
                .forEach(this::createUserFileShare);
    }

    private void cleanup(final User user) {
        FenixFramework.atomic(() -> {
            final UserFileShare userFileShare = user.getUserFileShare();
            if (userFileShare != null) {
                userFileShare.getSharedFileSet().stream()
                        .filter(sharedFile -> sharedFile.getShareValidUntilDateTime().plusWeeks(1).isBeforeNow())
                        .forEach(sharedFile -> sharedFile.delete());
            }
        });
    }

    private void createUserFileShare(final User user) {
        UserFileShare.userFileShareFor(user);
    }

}
