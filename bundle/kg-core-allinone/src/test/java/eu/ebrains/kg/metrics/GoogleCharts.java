/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.metrics;


import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.test.JsonAdapter4Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GoogleCharts {
    private static final String PLOTLY_HEADER = "<!DOCTYPE html>\n" +
            "<html lang=\"en\"><head>\n    <meta charset=\"UTF-8\">\n" +
            "               <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>" +
            " <style>body {\n" +
            "  font-family: Arial, Helvetica, sans-serif;\n" +
            "}</style><script type=\"text/javascript\">\n" +
            "      google.charts.load('current', {'packages':['timeline']});\n" +
            "      google.charts.setOnLoadCallback(drawCharts);\n" +
            "      let drawChartFunctions = [];\n" +
            "      function toggle(plot){\n" +
            "           let p = document.getElementById(plot);\n" +
            "           if (p.style.display === \"none\"){p.style.display = \"block\";} else{p.style.display = \"none\"}}\n" +
            "      function drawCharts() {\n" +
            "       for(drawChartFunction of drawChartFunctions){\n" +
            "drawChartFunction();" +
            "}\n" +
            "      }\n" +
            "    </script>\n";
    private static final String PLOTLY_FOOTER= "</body></html>";

    private StringBuilder source = new StringBuilder();
    private List<String> plots = new ArrayList<>();
    private JsonAdapter json = new JsonAdapter4Test();

    public synchronized void addSection(String title){
        this.plots.add(String.format("<h2>%s</h2>", title));
    }


    public synchronized void addCategory(String title){
        this.plots.add(String.format("<h3>%s</h3>", title));
    }

    public static class Column{
        private String type;
        private String id;

        public Column(String type, String id) {
            this.type = type;
            this.id = id;
        }
    }

    public static class Value{
        private String row;
        private String name;
        private Long start;
        private Long end;

        public List<Object> asSimpleList(){
            return Arrays.asList(row, name, start, end);
        }

        public Value(String row, String name, Long start, Long end) {
            this.row = row;
            this.name = name;
            this.start = start;
            this.end = end;
        }

        public void setRow(String row) {
            this.row = row;
        }

        public String getRow() {
            return row;
        }

        public String getName() {
            return name;
        }

        public Long getStart() {
            return start;
        }

        public Long getEnd() {
            return end;
        }
    }


    public synchronized void addPlot(String title, List<Value> values, boolean showBarLabels, boolean showRowLabels, long totalDuration){
        if(showRowLabels) {
            Map<String, List<Value>> byRow = values.stream().collect(Collectors.groupingBy(Value::getRow));
            values.forEach(v -> {
                List<Value> allValues = byRow.get(v.getRow());
                long duration = allValues.stream().mapToLong(value -> value.getEnd() - value.getStart()).sum();
                v.setRow(String.format("%s %dx, %dms/%.0f%%", v.getRow(), allValues.size(), duration, 100.0/(float)totalDuration*(float)duration));
            });
        }

        StringBuilder sb = new StringBuilder();
        final int plotCounter = this.plots.size();
        List<Column> columns = new ArrayList<>();
        columns.add(new Column("string", "Row"));
        columns.add(new Column("string", "Name"));
        columns.add(new Column("number", "Start"));
        columns.add(new Column("number", "End"));
        sb.append("<div><a onclick=\"toggle('plot").append(plotCounter).append("')\">").append(title).append("</a><div id=\"plot").append(plotCounter).append("\" style=\"width:100%;height:50vh;\"></div></div>");
        sb.append("<script>\n");
        sb.append("drawChartFunctions.push(function() {");
        sb.append("let plot").append(plotCounter).append(" = document.getElementById('plot").append(plotCounter).append("');\n");
        sb.append("let chart").append(plotCounter).append(" = new google.visualization.Timeline(plot").append(plotCounter).append(");\n");
        sb.append("let dataTable").append(plotCounter).append(" = new google.visualization.DataTable();\n");
        for (Column column : columns) {
            sb.append("dataTable").append(plotCounter).append(".addColumn(").append(json.toJson(column)).append(");\n");
        }
        sb.append("dataTable").append(plotCounter).append(".addRows(").append(json.toJson(values.stream().map(Value::asSimpleList).collect(Collectors.toList()))).append(");\n");
        sb.append("chart").append(plotCounter).append(".draw(dataTable").append(plotCounter).append(", {\n" +
                "      timeline: { showBarLabels: ").append(showBarLabels).append(", showRowLabels:").append(showRowLabels).append("}\n" +
                "    });\n" +
                "plot").append(plotCounter).append(".style.display='none';\n" );
        sb.append("});");
        sb.append("</script>");
        this.plots.add(sb.toString());
    }


    public String build(){
        StringBuilder sb = new StringBuilder();
        sb.append(PLOTLY_HEADER);
        plots.forEach(sb::append);
        sb.append(PLOTLY_FOOTER);
        return sb.toString();
    }

}
