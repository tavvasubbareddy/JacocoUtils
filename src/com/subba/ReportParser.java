package com.subba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses a jacoco report (index.html) and prints out code coverage information.
 * It is expecting the following element structure in the report file (<thead> here for reference):
 *      <thead>
            <tr>
                <td class="sortable" id="a" onclick="toggleSort(this)">Element</td>
                <td class="down sortable bar" id="b" onclick="toggleSort(this)">Missed Instructions</td>
                <td class="sortable ctr2" id="c" onclick="toggleSort(this)">Cov.</td>
                <td class="sortable bar" id="d" onclick="toggleSort(this)">Missed Branches</td>
                <td class="sortable ctr2" id="e" onclick="toggleSort(this)">Cov.</td>
                <td class="sortable ctr1" id="f" onclick="toggleSort(this)">Missed</td>
                <td class="sortable ctr2" id="g" onclick="toggleSort(this)">Cxty</td>
                <td class="sortable ctr1" id="h" onclick="toggleSort(this)">Missed</td>
                <td class="sortable ctr2" id="i" onclick="toggleSort(this)">Lines</td>
                <td class="sortable ctr1" id="j" onclick="toggleSort(this)">Missed</td>
                <td class="sortable ctr2" id="k" onclick="toggleSort(this)">Methods</td>
                <td class="sortable ctr1" id="l" onclick="toggleSort(this)">Missed</td>
                <td class="sortable ctr2" id="m" onclick="toggleSort(this)">Classes</td>
            </tr>
        </thead>
        <tfoot>
            <tr>
                <td>Total</td>
                <td class="bar">126,863 of 183,008</td>
                <td class="ctr2">30%</td>
                <td class="bar">16,304 of 20,309</td>
                <td class="ctr2">19%</td>
                <td class="ctr1">13,424</td>
                <td class="ctr2">16,694</td>
                <td class="ctr1">29,567</td>
                <td class="ctr2">42,831</td>
                <td class="ctr1">4,103</td>
                <td class="ctr2">6,471</td>
                <td class="ctr1">103</td>
                <td class="ctr2">547</td>
            </tr>
        </tfoot>
 */
public class ReportParser
{

    private final Pattern REGEX = Pattern.compile("<td class=\"[^\"]+\">([0-9of,% ]+)</td>");
    private final int NUM_MATCHES = 12;
    private final double PERCENT = 100.0;

    // index of elements in the foot
    private final int INDEX_INSTRUCTIONS = 0;
    private final int INDEX_BRANCHES = 2;
    private final int INDEX_CXTY_MISS = 4;
    private final int INDEX_CXTY_TOTAL = 5;
    private final int INDEX_LINES_MISS = 6;
    private final int INDEX_LINES_TOTAL = 7;
    private final int INDEX_METHODS_MISS = 8;
    private final int INDEX_METHODS_TOTAL = 9;
    private final int INDEX_CLASSES_MISS = 10;
    private final int INDEX_CLASSES_TOTAL = 11;

    // path to result index.html (relative to basedir)
    private String report;

    public void setReport(String report)
    {
        this.report = report;
    }

    public void execute() throws Exception
    {

        File file = new File(report);

        if (!file.exists())
        {
            throw new Exception("Report file doesn't exist at " + file.getPath());
        }

        String content = readFileToString(file);

        // cut down to only the <tfoot> element
        content = content.substring(content.indexOf("<tfoot>"), content.indexOf("</tfoot>"));

        Matcher matcher = REGEX.matcher(content);

        // should be exactly 13 matches. Put them in an array for use later
        List<String> matches = new ArrayList<>();
        for (int i = 0; i < NUM_MATCHES; i++)
        {
            if (!matcher.find())
            {
                throw new Exception("Match #" + i + " not found containing coverage information");
            }
            matches.add(matcher.group(1));
        }
        calculateCoverage(matches);

    }

    // Calculate coverage percentages and print them out.
    private void calculateCoverage(List<String> matches)
    {

        // Instructions - formatted "X of Y"
        String[] instructions = matches.get(INDEX_INSTRUCTIONS).split(" of ");
        double instructionsCov = calculatePercentage(instructions[0], instructions[1]);

        // branches - formatted "X of Y"
        String[] branches = matches.get(INDEX_BRANCHES).split(" of ");
        double branchesCov = calculatePercentage(branches[0], branches[1]);

        // cyclomatic complexity
        String cxtyMiss = matches.get(INDEX_CXTY_MISS);
        String cxtyTotal = matches.get(INDEX_CXTY_TOTAL);
        double cxtyCov = calculatePercentage(cxtyMiss, cxtyTotal);

        // lines
        String linesMiss = matches.get(INDEX_LINES_MISS);
        String linesTotal = matches.get(INDEX_LINES_TOTAL);
        double linesCov = calculatePercentage(linesMiss, linesTotal);

        // methods
        String methodsMiss = matches.get(INDEX_METHODS_MISS);
        String methodsTotal = matches.get(INDEX_METHODS_TOTAL);
        double methodsCov = calculatePercentage(methodsMiss, methodsTotal);

        // classes
        String classesMiss = matches.get(INDEX_CLASSES_MISS);
        String classesTotal = matches.get(INDEX_CLASSES_TOTAL);
        double classesCov = calculatePercentage(classesMiss, classesTotal);

        // print out results
        System.out.println("===========================");
        System.out.println("Coverage results");
        System.out.println("===========================");
        System.out.println(String.format("Instructions: %.2f%% (%s of %s missed)", instructionsCov, instructions[0], instructions[1]));
        System.out.println(String.format("Branches    : %.2f%% (%s of %s missed)", branchesCov, branches[0], branches[1]));
        System.out.println(String.format("Complexity  : %.2f%% (%s of %s missed)", cxtyCov, cxtyMiss, cxtyTotal));
        System.out.println(String.format("Lines       : %.2f%% (%s of %s missed)", linesCov, linesMiss, linesTotal));
        System.out.println(String.format("Methods     : %.2f%% (%s of %s missed)", methodsCov, methodsMiss, methodsTotal));
        System.out.println(String.format("Classes     : %.2f%% (%s of %s missed)", classesCov, classesMiss, classesTotal));
    }

    /**
     * Calculate coverage percentage
     * 
     * @param string
     * @param string2
     * @return
     */
    private double calculatePercentage(String missedStr, String totalStr)
    {
        Double missed = Double.valueOf(missedStr.replaceAll(",", ""));
        Double total = Double.valueOf(totalStr.replaceAll(",", ""));

        return ((total - missed) / total) * PERCENT;
    }

    /**
     * Reads file content to string.
     * 
     * @param file
     * @return
     * @throws Exception
     */
    private String readFileToString(File file) throws Exception
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            throw new Exception("Error reading from report file " + file.getPath());
        }
    }

    public static void main(String[] args) throws Exception
    {
        ReportParser parser = new ReportParser();
        parser.setReport("xyz\\report\\index.html");
        parser.execute();
    }
}

