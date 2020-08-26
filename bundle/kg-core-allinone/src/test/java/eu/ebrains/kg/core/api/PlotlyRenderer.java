/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.core.api;

import com.google.gson.Gson;

import java.util.*;

public class PlotlyRenderer {

    private static final String PLOTLY_HEADER = "<!DOCTYPE html>\n" +
            "<html lang=\"en\"><head>\n    <meta charset=\"UTF-8\">\n" +
            "               <script src=\"https://cdn.plot.ly/plotly-latest.min.js\"></script>\n" +
            "</head><body>";
    private static final String PLOTLY_FOOTER= "</body></html>";

    private StringBuilder source = new StringBuilder();
    private List<String> plots = new ArrayList<>();
    private Gson gson = new Gson();

    public static class BarTrace implements Trace{
        private List<Float> x = new ArrayList<>();
        private List<Float> y = new ArrayList<>();
        private String type = "bar";
        private String name;
        private String orientation = "h";
        private Map<String, Object> marker;

        public Map<String, Object> getMarker() {
            return marker;
        }

        public void setMarker(Map<String, Object> marker) {
            this.marker = marker;
        }

        public BarTrace(String name) {
            this(null, name);
        }

        public BarTrace(String type, String name) {
            if(type!=null){
                this.type = null;
            }
            this.name = name;
        }
        public BarTrace addValue(float x, float y){
            this.x.add(x);
            this.y.add(y);
            return this;
        }

        public void reverse(){
            Collections.reverse(this.x);
            Collections.reverse(this.y);
        }
    }

    public static class XYPlotTrace implements Trace{
        private List<Float> x = new ArrayList<>();
        private List<Float> y = new ArrayList<>();
        private String mode = "lines+markers";
        private String name;

        public XYPlotTrace(String name) {
            this(null, name);
        }

        public XYPlotTrace(String mode, String name) {
            if(mode!=null){
                this.mode = null;
            }
            this.name = name;
        }
        public XYPlotTrace addValue(float x, float y){
            this.x.add(x);
            this.y.add(y);
            return this;
        }

    }

    private interface Trace{}

    public static class Plot {
        private List<? extends Trace> traces;
        private PlotLayout plotLayout;

        public Plot(List<? extends Trace> traces) {
            this.traces = traces;
            this.plotLayout = new PlotLayout();
        }

        public PlotLayout getPlotLayout() {
            return plotLayout;
        }
    }

    public static class PlotLayout extends HashMap<String, String> {

        public void setTitle(String title){
            this.put("title", title);
        }
    }

    public synchronized void addSection(String title){
        this.plots.add(String.format("<h2>%s</h2>", title));
    }

    public synchronized void addPlot(Plot plot){
        StringBuilder sb = new StringBuilder();
        final int plotCounter = this.plots.size();
        sb.append("<div id=\"plot").append(plotCounter).append("\" style=\"width:100%;height:80vh;\"></div>");
        sb.append("<script>\n");
        sb.append("let plot").append(plotCounter).append(" = document.getElementById('plot").append(plotCounter).append("');\n");

        List<String> traceVars = new ArrayList<>();
        for (int i = 0; i < plot.traces.size(); i++) {
            String traceVar = "trace_"+plotCounter+"_"+i;
            traceVars.add(traceVar);
            sb.append(String.format("let %s = %s\n", traceVar, gson.toJson(plot.traces.get(i))));
        }

        sb.append(String.format("let data%d = [ %s ]\n", plotCounter, String.join(", ", traceVars)));
        sb.append(String.format("let layout%d = %s;\n", plotCounter, gson.toJson(plot.plotLayout)));
        sb.append(String.format("\tPlotly.newPlot( 'plot%d', data%d, layout%d);\n", plotCounter, plotCounter, plotCounter));
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
