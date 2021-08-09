package pt.ist.fenix.webapp.task.academic.report;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.PersonalInformation;

import java.io.ByteArrayOutputStream;

public class ExportActiveStudents extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("ActiveStudents");
        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(executionSemester -> executionSemester.getEnrolmentsSet().stream())
                .map(enrolment -> enrolment.getRegistration())
                .distinct()
                .filter(registration -> registration.isActive())
                .forEach(registration -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("User", registration.getPerson().getUsername());
                    row.setCell("Nome", registration.getPerson().getName());
                    row.setCell("Documento", documentNumber(registration.getPerson().getUser()));
                    row.setCell("Curso", registration.getDegree().getPresentationName());
                    row.setCell("Cargo", "N/A");
                    row.setCell("Tempo Integral", isFullTime(registration));
                });
        Bennu.getInstance().getPhdProgramsSet().stream()
                .flatMap(phdProgram -> phdProgram.getIndividualProgramProcessesSet().stream())
                .filter(programProcess -> programProcess.isProcessActive())
                .forEach(programProcess -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("User", programProcess.getPerson().getUsername());
                    row.setCell("Nome", programProcess.getPerson().getName());
                    row.setCell("Documento", documentNumber(programProcess.getPerson().getUser()));
                    row.setCell("Curso", programProcess.getPhdProgram().getPresentationName(ExecutionYear.readCurrentExecutionYear()));
                    row.setCell("Cargo", "N/A");
                    row.setCell("Tempo Integral", "N/A");
                });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("activeStudents.xls", stream.toByteArray());
    }

    private String isFullTime(final Registration registration) {
        final RegistrationRegimeType regimeType = registration.getRegimeType(ExecutionYear.readCurrentExecutionYear());
        return regimeType == RegistrationRegimeType.PARTIAL_TIME ? "Não" : "Sim";
    }

    private String documentNumber(final User user) {
        final Identity identity = user.getIdentity();
        if (identity != null) {
            final PersonalInformation personalInformation = identity.getPersonalInformation();
            if (personalInformation != null) {
                final IdentificationDocument identificationDocument = personalInformation.getIdentificationDocument();
                if (identificationDocument != null) {
                    return identificationDocument.getDocumentNumber();
                }
            }
        }
        return user.getPerson().getDocumentIdNumber();
    }


}
