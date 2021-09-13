package pt.ist.fenix.webapp.task.academic.enrolment;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.RootCurriculumGroup;
import org.fenixedu.academic.transitions.domain.DegreeCurricularTransitionPlan;
import org.fenixedu.academic.transitions.domain.StudentDegreeCurricularTransitionPlan;
import org.fenixedu.academic.transitions.service.TransitionService;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransitionSCPs extends CustomTask {

    private Map<Registration, Set<StudentDegreeCurricularTransitionPlan>> studentMap = null;

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        studentMap = new HashMap<>();
        DegreeCurricularPlan.readBolonhaDegreeCurricularPlans().stream()
                .flatMap(dcp -> dcp.getDestinationTransitionPlanSet().stream())
                .flatMap(transitionPlan -> transitionPlan.getStudentDegreeCurricularTransitionPlanSet().stream())
                //.filter(studentPlan -> studentPlan.getConfirmTransitionInstant() != null)
                .filter(studentPlan -> studentPlan.getFreezeInstant() != null)
                .forEach(studentPlan -> {
                    final Student student = studentPlan.getStudent();
//                    if (student.getNumber() == 69749) {
                    final DegreeCurricularTransitionPlan degreeCurricularTransitionPlan = studentPlan.getDegreeCurricularTransitionPlan();
                    final StudentCurricularPlan scp = TransitionService.getStudentCurricularPlanToTransition(student.getPerson().getUser(), degreeCurricularTransitionPlan);
                    if (scp != null) { //if the registration is not active (concluded or other)
                        final Set<CycleType> cycleTypesToTransition = TransitionService.getCyclesToTransition(degreeCurricularTransitionPlan.getCycleTypesToTransition(), scp);
                        if (!cycleTypesToTransition.isEmpty()) {
                            studentMap.computeIfAbsent(scp.getRegistration(), v -> new HashSet<>()).add(studentPlan);
                        }
//                        }
                    }
                });

        for (Registration registration : studentMap.keySet()) {
            try {
                transition(registration);
            } catch (Exception e) {
                taskLog("Problem transitioning student\t%s\t%s%n", registration.getPerson().getUsername(), e.getMessage());
                e.printStackTrace();
            }
        }
        ;
    }

    private void transition(Registration registration) {
        FenixFramework.atomic(() -> {
            final Person person = registration.getPerson();
            final Set<StudentDegreeCurricularTransitionPlan> studentTransitionPlans = studentMap.get(registration);
            if (studentTransitionPlans.size() == 1) {
                final StudentDegreeCurricularTransitionPlan studentPlan = studentTransitionPlans.iterator().next();
                final StudentCurricularPlan scp = TransitionService.getStudentCurricularPlanToTransition(registration.getPerson().getUser(),
                        studentPlan.getDegreeCurricularTransitionPlan());
                if (!scp.getDegree().isSecondCycle() && scp.getSecondCycle() != null) { //tem 2º ciclo em avanço
                    if (scp.getFirstCycle() != null && scp.getFirstCycle().isConcluded()) {
                        //tudo ok - é o plano de transição para o 2º ciclo
                    } else {
                        return; //não tem plano especializado para o 2º ciclo, só tem para o 1º
                    }
                } else if (scp.getDegree().isSecondCycle() && scp.getDegree().isFirstCycle()) { //mestrado integrado
                    if (scp.hasConcludedCycle(CycleType.FIRST_CYCLE)) {
                        //tudo ok - o 1º ciclo já está concluido, é só para migrar o 2º
                    } else if (studentPlan.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan().getDegree().getCycleTypes().size() == 2) {
                        //tudo ok - o plano de destino contém os 2 ciclos portanto o aluno só tem um plano de transição
                    } else if (scp.getCycleCurriculumGroups().size() == 1) {
                        //tudo ok - é um mestrado integrado mas o aluno só tem um ciclo aberto no scp
                    } else {
                        return;
                    }
                }
                TransitionService.run(studentPlan.getDegreeCurricularTransitionPlan(), person.getUser(), false, true, true, true);
            } else {
                for (StudentDegreeCurricularTransitionPlan studentPlan : studentTransitionPlans) {
                    TransitionService.run(studentPlan.getDegreeCurricularTransitionPlan(), person.getUser(), false, true, true, true);
                }
                final StudentDegreeCurricularTransitionPlan studentPlan = studentTransitionPlans.iterator().next();
                final StudentCurricularPlan scp = TransitionService.getStudentCurricularPlanToTransition(person.getUser(),
                        studentPlan.getDegreeCurricularTransitionPlan());
                if (!scp.getDegree().isSecondCycle() && scp.getSecondCycle() != null) {
                    //licenciatura e tem 2º ciclo em avanço
                    Registration firstCycleRegistration = findCycleRegistration(scp.getRegistration().getStudent(), studentTransitionPlans, CycleType.FIRST_CYCLE);
                    Registration secondCycleRegistration = findCycleRegistration(scp.getRegistration().getStudent(), studentTransitionPlans, CycleType.SECOND_CYCLE);
                    final RootCurriculumGroup firstCycleRoot = firstCycleRegistration.getLastStudentCurricularPlan().getRoot();
                    final CycleCurriculumGroup secondCycle = secondCycleRegistration.getLastStudentCurricularPlan().getSecondCycle();
                    secondCycle.setCurriculumGroup(firstCycleRoot);
                    secondCycleRegistration.delete();
                }
            }

            RegistrationState.createRegistrationState(registration, person, new DateTime(), RegistrationStateType.TRANSITED);
        });
    }

    private Registration findCycleRegistration(final Student student, final Set<StudentDegreeCurricularTransitionPlan> studentTransitionPlans, final CycleType cycleType) {
        return student.getRegistrationsSet().stream()
                .filter(r -> r.getDegree().getCycleTypes().contains(cycleType))
                .filter(r -> isSameDCP(r, studentTransitionPlans))
                .findAny().get();
    }

    private boolean isSameDCP(Registration registration, Set<StudentDegreeCurricularTransitionPlan> studentTransitionPlans) {
        return studentTransitionPlans.stream()
                .map(stp -> stp.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan())
                .anyMatch(dcp -> registration.getDegreeCurricularPlans().contains(dcp));
    }

}
