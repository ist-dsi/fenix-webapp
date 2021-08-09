package pt.ist.fenix.webapp.task.institutional.queueing;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CheckQueueingLogs extends ReadCustomTask {

    private static final Comparator<Action> COMPARATOR = (a1, a2) -> Collator.getInstance().compare(a1.instant, a2.instant);

    private static class Action implements Comparable<Action> {

        private String action;
        private String instant;

        private Action(final String action, final String instant) {
            this.action = action;
            this.instant = instant;
        }

        @Override
        public int compareTo(final Action o) {
            return COMPARATOR.compare(this, o);
        }

        private LocalDateTime dateTime() {
            return LocalDateTime.parse(instant, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    private int attendCount = 0;
    private int notAttendedCount = 0;
    private List<Long> waitTimes = new ArrayList<>();
    private List<Long> attendaceTimes = new ArrayList<>();

    @Override
    public void runTask() throws Exception {
        attendCount = 0;
        notAttendedCount = 0;
        waitTimes.clear();
        attendaceTimes.clear();

        final SortedMap<String, SortedSet<Action>> map = new TreeMap<>();

        final Spreadsheet stats = new Spreadsheet("Stats");
        final Spreadsheet drillDown = stats.addSpreadsheet("DrillDown");
        final Spreadsheet logSheet = drillDown.addSpreadsheet("Queue Attendance Logs");

        for (final String line : Files.readAllLines(new File("/tmp/logs.txt").toPath())) {
            final int i1 = line.indexOf(".gz:");
            if (i1 > 0) {
                final int i2 = line.indexOf(" ", i1 + 4);
                if (i2 > 0) {
                    final int i3 = line.indexOf(" ", i2 + 2);
                    if (i3 > 0) {
                        final int i4 = line.indexOf(" ", i3 + 1);
                        if (i4 > 0) {
                            final int i5 = line.indexOf("AttendanceSlot - ", i4 + 1);
                            if (i5 > 0) {
                                final int i6 = line.indexOf(" ", i5 + 17);
                                if (i6 > 0) {
                                    final int i7 = line.indexOf(" for ", i6 + 1);
                                    if (i7 > 0) {
                                        final int i8 = line.indexOf(" for queue ", i7 + 5);
                                        if (i8 > 0) {
                                            final int i9 = i8 + 11;
                                            if (i9 > 0) {
                                                final int i10 = line.indexOf(" by ", i9);

                                                final int day = Integer.parseInt(line.substring(i2 + 1, i3).trim());
                                                final String hms = line.substring(i3 + 1, i4);
                                                final String action = line.substring(i5 + 17, i6);
                                                final String account = line.substring(i7 + 5, i8);
                                                final String queue = i10 > 0 ? line.substring(i9, i10) : line.substring(i9);
                                                final String by = i10 > 0 ? line.substring(i10 + 4) : "";

                                                final String date = "2021-04-" + ((day < 10) ? ("0" + day) : day);

                                                taskLog("%s %s - %s : %s : %s%n",
                                                        day, hms, action, queue, account);

                                                final Spreadsheet.Row row = logSheet.addRow();
                                                row.setCell("queue", queue);
                                                row.setCell("day", date);
                                                row.setCell("hms", hms);
                                                row.setCell("action", action);
                                                row.setCell("account", account);
                                                row.setCell("operator", by);

                                                map.putIfAbsent(account, new TreeSet<>());
                                                map.get(account).add(new Action(action, date + " " + hms));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        map.forEach((account, set) -> {
            final Spreadsheet.Row row = drillDown.addRow();
            row.setCell("account", account);
            report("Finish", row, set, true);
            final String[] times = timeFor(set);
            row.setCell("Wait", times[0]);
            row.setCell("Attendance Time", times[1]);
            report("Scheduled", row, set, false);
            report("Checkin", row, set, false);
            report("Take", row, set, false);
        });

        report(stats, "Attendend", Integer.toString(attendCount));
        report(stats, "Not Attendend", Integer.toString(notAttendedCount));
        report(stats, "Wait", waitTimes);
        report(stats, "Attendace", attendaceTimes);

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stats.exportToXLSSheet(stream);
        output("logs.xls", stream.toByteArray());
    }

    private void report(final Spreadsheet spreadsheet, final String label, final String value) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Lavel", label);
        row.setCell("Value", value);
    }

    private void report(final Spreadsheet spreadsheet, final String description, final List<Long> times) {
        times.sort(Long::compareTo);
        final double average = average(times);
        final List<Long> outliers = reportLimits(spreadsheet, description, times);
        final List<Long> correctedTimes = times.stream().filter(l -> !outliers.contains(l)).collect(Collectors.toList());
        final double correctedAverage = average(correctedTimes);
        final double standardDeviation = standardDeviation(correctedTimes, correctedAverage);
        report(spreadsheet, "Corrected Average " + description, Double.toString(correctedAverage));
        report(spreadsheet, "Standard Deviation " + description, Double.toString(standardDeviation));
        report(spreadsheet, "Coefficient of Variation" + description, Double.toString(standardDeviation / correctedAverage));
        //report(spreadsheet, "Raw Average " + description, Double.toString(average));
    }

    private double standardDeviation(final List<Long> times, double average) {
        return Math.sqrt(times.stream().mapToDouble(t -> Math.pow(t.longValue() - average, 2)).sum() / (times.size() - 1));
    }

    private List<Long> reportLimits(final Spreadsheet spreadsheet, final String description, final List<Long> times) {
        times.sort(Long::compareTo);

        final double q1 = q1(times);
        final double mean = mean(times);
        final double q3 = q3(times);
        final double aiq = q3 - q1;
        final double[] limits = new double[] { q1 - (1.5 * aiq), q3 + (1.5 * aiq) };
        final List<Long> outliers = times.stream()
                .filter(t -> t.longValue() < limits[0] || t.longValue() > limits[1])
                .collect(Collectors.toList());

        report(spreadsheet, "Q1 " + description, Double.toString(q1));
        report(spreadsheet, "Mean " + description, Double.toString(mean));
        report(spreadsheet, "Q3 " + description, Double.toString(q3));
        report(spreadsheet, "Outliers " + description, Integer.toString(outliers.size()));

        return outliers;
    }

    private void report(final String action, final Spreadsheet.Row row, final SortedSet<Action> set, final boolean simple) {
        final String first = set.stream()
                .filter(a -> action.equals(a.action))
                .min(COMPARATOR)
                .map(a -> a.instant)
                .orElse(null);
        final String last = set.stream()
                .filter(a -> action.equals(a.action))
                .max(COMPARATOR)
                .map(a -> a.instant)
                .orElse(null);
        if (simple) {
            row.setCell(action, last);
        } else {
            row.setCell("First " + action, first);
            row.setCell("Last " + action, last);
        }
    }

    private String[] timeFor(final SortedSet<Action> set) {
        boolean attended = false;

        final long[] result = new long[] { 0l, 0l };
        Action chechin = null;
        Action take = null;
        Action conclude = null;
        for (final Action action : set) {
            if ("Checkin".equals(action.action)) {
                chechin = action;
            } else if ("Take".equals(action.action)) {
                take = action;
                final Long duration = calculate(chechin, take);
                if (duration != null) {
                    result[0] += duration.longValue();
                }
                chechin = null;
                attended = true;
            } else if ("Untake".equals(action.action) || "Finish".equals(action.action)) {
                conclude = action;
                final Long duration = calculate(take, conclude);
                if (duration != null) {
                    result[1] += duration.longValue();
                }
                take = null;
            }
        }
        if (result[0] > 0l) {
            waitTimes.add(result[0]);
        }
        if (result[1] > 0l) {
            attendaceTimes.add(result[1]);
        }
        if (attended) {
            attendCount++;
        } else {
            notAttendedCount++;
        }
        return new String[] { result[0] == 0l ? "" : Long.toString(result[0]),  result[1] == 0l ? "" : Long.toString(result[1])};
    }

    private Long calculate(final Action a1, final Action a2) {
        return a1 == null || a2 == null ? null : ChronoUnit.MINUTES.between(a1.dateTime(), a2.dateTime());
    }

    private double average(final List<Long> times) {
        return times.stream().mapToLong(l -> l.longValue()).average().orElse(0d);
    }

    private double mean(final List<Long> times) {
        final int n = times.size();
        if (n == 0) {
            return 0d;
        }
        return n % 2 == 0 ? times.get(n / 2) : ((times.get(n / 2) + times.get((n / 2) + 1)) / 2);
    }

    private double q1(final List<Long> times) {
        final int n = times.size();
        if (n == 0) {
            return 0d;
        }
        return n % 4 == 0 ? times.get(n / 4) : ((times.get(n / 4) + times.get((n / 4) + 1)) / 2);
    }

    private double q3(final List<Long> times) {
        final int n = times.size();
        if (n == 0) {
            return 0d;
        }
        return n % 4 == 0 ? times.get(3 * n / 4) : ((times.get(3 * n / 4) + times.get((3 * n / 4) + 1)) / 2);
    }

}

