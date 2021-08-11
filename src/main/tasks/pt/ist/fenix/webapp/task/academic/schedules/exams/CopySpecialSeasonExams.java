package pt.ist.fenix.webapp.task.academic.schedules.exams;

import org.fenixedu.academic.domain.Exam;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.util.Season;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

public class CopySpecialSeasonExams extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(executionSemester -> executionSemester.getAssociatedExecutionCoursesSet().stream())
                .flatMap(executionCourse -> executionCourse.getAssociatedEvaluationsSet().stream())
                .filter(Exam.class::isInstance)
                .map(Exam.class::cast)
                .filter(exam -> exam.getSeason().equals(Season.SPECIAL_SEASON_OBJ))
                .distinct()
                .forEach(this::copy);

        for (int i = 0; i < init.length; i++) {
            if (init[i] > 0) {
                taskLog("Day %s eval count = %s%n", i, init[i]);
            }
        }

        for (int i = 0; i < fin.length; i++) {
            if (fin[i] > 0) {
                taskLog("Day %s eval count = %s%n", i, fin[i]);
            }
        }

        throw new Error("Abort TX");
    }

    private final int[] init = new int[31+1];
    private final int[] fin = new int[31+1];

    private void copy(final Exam exam) {
        final YearMonthDay yearMonthDay = exam.getDayDateYearMonthDay();
        if (yearMonthDay.getYear() == 2021 && yearMonthDay.getMonthOfYear() == 7) {
            final int day = yearMonthDay.getDayOfMonth();
            if (day >= 21 && day <= 30) {
                final YearMonthDay newYearMonthDay = map(day);
                init[day]++;
                fin[newYearMonthDay.getDayOfMonth()]++;
                taskLog("Mapping %s to %s : %s %s %s%n",
                        yearMonthDay.toString("yyyy-MM-dd"),
                        newYearMonthDay.toString("yyyy-MM-dd"),
                        exam.getPresentationName(),
                        exam.getAssociatedExecutionCoursesSet().stream()
                                .map(ec -> ec.getName())
                                .collect(Collectors.joining(", ")),
                        exam.getAssociatedExecutionCoursesSet().stream()
                                .map(ec -> ec.getDegreePresentationString())
                                .collect(Collectors.joining(", ")));
                final DateTime begin = toDateTime(exam.getBeginningDateTime(), newYearMonthDay);
                final DateTime end = toDateTime(exam.getEndDateTime(), newYearMonthDay);
                new Exam(begin.toDate(), begin.toDate(), end.toDate(),
                        new ArrayList<>(exam.getAssociatedExecutionCoursesSet()),
                        exam.getDegreeModuleScopes(),
                        Collections.EMPTY_LIST, //exam.getAssociatedRooms(),
                        exam.getGradeScale(),
                        Season.EXTRAORDINARY_SEASON_OBJ);
            }
        }
    }

    private YearMonthDay map(final int day) {
/*
        int newDay = day - 20;
        if (newDay < 4) {
            newDay += 7;
        }
*/
        int newDay = day == 21 ? 6
                   : day == 22 ? 7
                   : day == 23 ? 8
                   : day == 24 ? 4
                   : day == 26 ? 9
                   : day == 27 ? 10
                   : day == 28 ? 8
                   : day == 29 ? 9
                   : day == 30 ? 10
                   : -1;
        return new YearMonthDay(2021, 9, newDay);
    }

    private DateTime toDateTime(final DateTime dt, final YearMonthDay yearMonthDay) {
        return new DateTime(yearMonthDay.getYear(), yearMonthDay.getMonthOfYear(), yearMonthDay.getDayOfMonth(),
                dt.getHourOfDay(), dt.getMinuteOfHour(), 0, 0);
    }

}