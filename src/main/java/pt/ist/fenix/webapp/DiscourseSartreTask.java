package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accessControl.ActiveTeachersGroup;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.collaboration.domain.discourse.Client;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Task(readOnly = true, englishTitle = "Integration with Sartre Discourse Forum")
public class DiscourseSartreTask extends CronTask {

    @Override
    public void runTask() throws Exception {
        final Set<User> remoteUsers = new HashSet<>();
        final Set<User> usersToCreate = Stream
                .concat(new ActiveTeachersGroup().getMembers(), new ActiveResearchers().getMembers())
                .collect(Collectors.toSet());

        int[] count = new int[] { 0, 0 };
        Client.listUsers(u -> {
            count[1]++;
            final String username = u.get("username").getAsString();
            final User user = User.findByUsername(username);
            if (user != null) {
                remoteUsers.add(user);
                usersToCreate.remove(user);
                count[0]++;
            }
        });

        usersToCreate.stream()
            .filter(user -> user.getEmail() != null && !user.getEmail().isEmpty())
            .forEach(user -> {
                Client.createUser(user.getUsername(), user.getEmail(), user.getDisplayName());
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    throw new Error(e);
                }
            });

        final Set<JsonObject> groups = new HashSet<>();
        Client.listGroups(g -> {
            final JsonObject group = g.getAsJsonObject();
            groups.add(group);
        });

        final JsonObject teachers = groups.stream()
                .filter(g -> "teachers".equals(g.get("name").getAsString()))
                .findAny().orElseGet(() -> Client.createGroup("teachers", "Teachers"));
        Client.updateMembers(remoteUsers, teachers, new ActiveTeachersGroup());

        final JsonObject researcher = groups.stream()
                .filter(g -> "researchers".equals(g.get("name").getAsString()))
                .findAny().orElseGet(() -> Client.createGroup("researchers", "Researchers"));
        Client.updateMembers(remoteUsers, researcher, new ActiveResearchers());
    }

}
