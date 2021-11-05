package pt.ist.fenix.webapp.task;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

@Task(englishTitle = "Create events from templates", readOnly = true)
public class CreateEventsFromTemplate extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getRegistrationDataByExecutionYearSet().stream()
                .parallel()
                .forEach(this::process);
    }

    private void process(final RegistrationDataByExecutionYear dataByExecutionYear) {
        FenixFramework.atomic(() -> {
            final EventTemplate eventTemplate = EventTemplate.templateFor(dataByExecutionYear);
            if (eventTemplate != null) {
                eventTemplate.createEventsFor(dataByExecutionYear);
            }
        });
    }

}
