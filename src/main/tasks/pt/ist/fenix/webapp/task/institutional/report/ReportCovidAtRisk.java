package pt.ist.fenix.webapp.task.institutional.report;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class ReportCovidAtRisk extends ReadCustomTask {

    private int[] numbers = new int[] {
    };

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Students");
        for (int i = 0; i < numbers.length; i++) {
            final Student student = Student.readStudentByNumber(numbers[i]);
            final Person person = student.getPerson();
            final Spreadsheet.Row row = spreadsheet.addRow();
            row.setCell("TecnicoID", person.getUsername());
            row.setCell("Name", person.getName());
            row.setCell("DateOfBirth", person.getDateOfBirthYearMonthDay().toString("yyyy-MM-dd"));
            row.setCell("Phone", phone(person));
            row.setCell("Email", email(person));
        }
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("students.xls", stream.toByteArray());
    }

    private String email(final Person person) {
        return person.getUser().getEmail();
    }

    private String phone(final Person person) {
        final String mobile = person.getMobile();
        return mobile == null || mobile.isEmpty() ? person.getPartyContactsSet().stream()
                .filter(MobilePhone.class::isInstance)
                .map(MobilePhone.class::cast)
                .map(m -> m.getNumber())
                .findAny().orElse(null) : mobile;
    }

}
