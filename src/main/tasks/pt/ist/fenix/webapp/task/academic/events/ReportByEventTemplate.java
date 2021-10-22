package pt.ist.fenix.webapp.task.academic.events;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class ReportByEventTemplate extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Report");

        Bennu.getInstance().getEventTemplateSet().stream()
                .filter(eventTemplate -> eventTemplate.getEventTemplateFromAlternativeSet().isEmpty())
                .forEach(eventTemplate -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Code", eventTemplate.getCode());
                    row.setCell("Plan", eventTemplate.getTitle().getContent());
                    row.setCell("Registrations", eventTemplate.getRegistrationSet().size());
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("report.xlsx", stream.toByteArray());
    }

}
