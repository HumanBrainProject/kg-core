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

package eu.ebrains.kg.metrics;

import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class PlotlyRendererTest {


    @Test
    public void testPlotting() throws IOException {
        PlotlyRenderer plotlyRenderer = new PlotlyRenderer();
        try(FileWriter fw = new FileWriter("/tmp/plot_test.html")){
            plotlyRenderer.addPlot(new PlotlyRenderer.Plot(Arrays.asList(new PlotlyRenderer.XYPlotTrace("Hello world").addValue(1f, 1f).addValue(2f, 3f).addValue(3f, 4f), new PlotlyRenderer.XYPlotTrace("Blabla").addValue(1f, 2f).addValue(2f, 5f).addValue(3f, 6f))));
            plotlyRenderer.addPlot(new PlotlyRenderer.Plot(Arrays.asList(new PlotlyRenderer.XYPlotTrace("Hello world 2").addValue(1f, 1f).addValue(2f, 3f).addValue(3f, 4f), new PlotlyRenderer.XYPlotTrace("Blabla").addValue(1f, 2f).addValue(2f, 5f).addValue(3f, 6f))));
            fw.write(plotlyRenderer.build());
        }
    }

}