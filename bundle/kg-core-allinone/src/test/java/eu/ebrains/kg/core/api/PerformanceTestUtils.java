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

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.Result;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.springframework.http.ResponseEntity;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class PerformanceTestUtils {

    private final PlotlyRenderer plotter = new PlotlyRenderer();

    public enum Link{
        PREVIOUS, NEXT;
    }


//    public void plotMetrics(Map<String, List<MethodExecution>> metrics){
//        plotter.addSection("Method metrics");
//        List<PlotlyRenderer.BarTrace> traces = new ArrayList<>();
//        PlotlyRenderer.Plot plot = new PlotlyRenderer.Plot(traces);
//        plot.getPlotLayout().put("barmode", "stack");
//        int row = 0;
//        for (List<MethodExecution> value : metrics.values()) {
//            for (int i = 0; i < value.size(); i++) {
//                if(i==traces.size()){
//                    traces.add(new PlotlyRenderer.BarTrace())
//                }
//            }
//        }
//
//       metrics.values().stream().min(Comparator.comparing(MethodExecution::getStartTime))
//
//
//        plot.getPlotLayout().setTitle(String.format("Run %d iterations with %d threads in %dms", numberOfIteration, threads, Duration.between(start, end).toMillis()));
//        plotter.addPlot(plot);
//
//    }

    private static final int[] THREADS_ORDER = new int[]{1, 2, 4, 8, 16, 32};

    public interface CallableWithPayload<V> {
        V call(JsonLdDoc doc);
    }

    public void addSection(String title){
        plotter.addSection(title);
    }

    public void commitReport() throws IOException {
        String nowInUTC = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        Path logDir = Paths.get(System.getProperty("user.home")).resolve("logs");
        Files.createDirectories(logDir);
        Path logFile = logDir.resolve(String.format("performance-%s.html", nowInUTC));
        try (FileWriter fw = new FileWriter(logFile.toFile())) {
            fw.write(plotter.build());
        }
    }

    public <T> List<ResponseEntity<Result<T>>> executeMany(int numberOfFields, boolean normalize, int numberOfIteration, boolean parallelize, Link link, CallableWithPayload<ResponseEntity<Result<T>>> r, ThreadLocal<AuthTokens> authTokens) {
        List<ResponseEntity<Result<T>>> result = null;
        if (parallelize) {
            for (int i = 0; i < THREADS_ORDER.length; i++) {
                result = runWithThreads(numberOfFields, normalize, link, numberOfIteration, i*numberOfIteration, r, THREADS_ORDER[i], authTokens);
            }
            for (int i = THREADS_ORDER.length - 1; i >= 0; i--) {
                result = runWithThreads(numberOfFields, normalize, link, numberOfIteration, THREADS_ORDER.length*numberOfIteration+(THREADS_ORDER.length-i)*numberOfIteration, r, THREADS_ORDER[i], authTokens);
            }
        } else {
            result = runWithThreads(numberOfFields, normalize, link, numberOfIteration, 0, r, 1, authTokens);
        }
        return result;
    }

    private <T> List<ResponseEntity<Result<T>>> runWithThreads(int numberOfFields, boolean normalize, Link link, int numberOfIteration, int idOffset, CallableWithPayload<ResponseEntity<Result<T>>> r, int threads, ThreadLocal<AuthTokens> authTokens) {
        List<ResponseEntity<Result<T>>> result;
        Instant start = Instant.now();
        List<PlotlyRenderer.BarTrace> traces = new ArrayList<>();
        PlotlyRenderer.Plot plot = new PlotlyRenderer.Plot(traces);
        plot.getPlotLayout().put("barmode", "stack");
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        //When
        List<Future<ResponseEntity<Result<T>>>> futureResults = new ArrayList<>();
        for (int i = 0; i < numberOfIteration; i++) {
            final int iteration = i+idOffset;
            futureResults.add(executorService.submit(() -> {
                AuthTokens authToken = new AuthTokens();
                authToken.setTransactionId(UUID.randomUUID());
                authTokens.set(authToken);
                return r.call(TestDataFactory.createTestData(numberOfFields, !normalize, iteration, link==null ? null : link == Link.NEXT ? iteration+1 : iteration-1));
            }));
        }
        try {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.HOURS);
            Instant end = Instant.now();
            result = futureResults.stream().map(tempres -> {
                try {
                    ResponseEntity<Result<T>> resultResponseEntity = tempres.get();
                    System.out.printf("Result: %d ms%n", Objects.requireNonNull(resultResponseEntity.getBody()).getDurationInMs());
                    return resultResponseEntity;
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    return null;
                }
            }).collect(Collectors.toList());
            PlotlyRenderer.BarTrace offsetTrace = new PlotlyRenderer.BarTrace(String.format("Wait", Duration.between(start, Instant.now()).toMillis()));
            Map<String, Object> marker = new HashMap<>();
            marker.put("color", "rgba(0,0,0,0.0)");
            Map<String, Object> line = new HashMap<>();
            marker.put("line", line);
            line.put("color", "rgba(0,0,0,0.0)");
            line.put("width", 0);
            offsetTrace.setMarker(marker);
            double[] durations = result.stream().mapToDouble(res -> Objects.requireNonNull(res.getBody()).getDurationInMs().doubleValue()).toArray();
            PlotlyRenderer.BarTrace durationTrace = new PlotlyRenderer.BarTrace(String.format("Execute (avg: %f, median: %f)", new Mean().evaluate(durations), new Median().evaluate(durations)));
            traces.add(offsetTrace);
            traces.add(durationTrace);
            for (int i = 0; i < result.size(); i++) {
                Result<T> body = result.get(i).getBody();
                offsetTrace.addValue(body.getStartTime() - start.toEpochMilli(), i);
                durationTrace.addValue(body.getDurationInMs(), i);
            }
            durationTrace.reverse();
            offsetTrace.reverse();
            plot.getPlotLayout().setTitle(String.format("Run %d iterations with %d threads in %dms", numberOfIteration, threads, Duration.between(start, end).toMillis()));
            plotter.addPlot(plot);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}
