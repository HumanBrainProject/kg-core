/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.testutils.TestDataFactory;
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
import java.util.function.Function;
import java.util.stream.Collectors;

public class PerformanceTestUtils {

    private final GoogleCharts plotter = new GoogleCharts();

    public enum Link {
        PREVIOUS, NEXT;
    }


    public void plotMetrics(Map<UUID, List<MethodExecution>> metrics) {
        plotter.addSection("Method metrics");
        Function<List<MethodExecution>, Long> durationFunction = c -> {
            MethodExecution firstExecution = c.stream().min(Comparator.comparing(MethodExecution::getStartTime)).get();
            MethodExecution lastExecution = c.stream().max(Comparator.comparing(MethodExecution::getEndTime)).get();
            return lastExecution.getEndTime() - firstExecution.getStartTime();
        };
        Comparator<List<MethodExecution>> durationComparator = Comparator.comparing(durationFunction);
        List<MethodExecution> fastestExecution = metrics.values().stream().min(durationComparator).get();
        List<MethodExecution> slowestExecution = metrics.values().stream().max(durationComparator).get();
        double[] durations = metrics.values().stream().mapToDouble(durationFunction::apply).toArray();
        double mean = new Mean().evaluate(durations);
        double median = new Median().evaluate(durations);
        List<MethodExecution> meanExecution = metrics.values().stream().min((a, b) -> {
            double diffToMeanA = Math.abs(durationFunction.apply(a) - mean);
            double diffToMeanB = Math.abs(durationFunction.apply(b) - mean);
            return Double.compare(diffToMeanA, diffToMeanB);
        }).get();
        List<MethodExecution> medianExecution = metrics.values().stream().min((a, b) -> {
            double diffToMeanA = Math.abs(durationFunction.apply(a) - median);
            double diffToMeanB = Math.abs(durationFunction.apply(b) - median);
            return Double.compare(diffToMeanA, diffToMeanB);
        }).get();

        List<List<MethodExecution>> relevantExecutions = Arrays.asList(fastestExecution,meanExecution, medianExecution, slowestExecution);
        relevantExecutions.forEach(v -> {
            List<GoogleCharts.Value> values = new ArrayList<>();
            MethodExecution firstExecution = v.stream().min(Comparator.comparing(MethodExecution::getStartTime)).get();
            MethodExecution lastExecution = v.stream().max(Comparator.comparing(MethodExecution::getEndTime)).get();

            long duration = lastExecution.getEndTime()-firstExecution.getStartTime();
            v.sort((a,b)->{
                int startTimeComp = a.getStartTime().compareTo(b.getStartTime());
                if(startTimeComp==0){
                    Long durationA = a.getEndTime()-a.getStartTime();
                    Long durationB = b.getEndTime()-b.getStartTime();
                    return durationB.compareTo(durationA);
                }
                return startTimeComp;
            });
            for (MethodExecution execution : v) {
                values.add(new GoogleCharts.Value(execution.getPackageName(), String.format("%s (%s)", execution.getMethodName(), execution.getPackageName()), execution.getStartTime()-firstExecution.getStartTime(), execution.getEndTime()-firstExecution.getStartTime()));
            }
            String label;
            if(v == fastestExecution){
                label = "Fastest";
            }
            else if (v==meanExecution){
                label = "Mean";
            }
            else if (v==medianExecution){
                label = "Median";
            }
            else {
                label = "Slowest";
            }

            plotter.addCategory(String.format("%s execution in %d ms", label, duration));
            plotter.addPlot("By class", values, false, true, duration);

            values = new ArrayList<>();
            for (MethodExecution execution : v) {
                values.add(new GoogleCharts.Value(String.format("%s (%s)", execution.getMethodName(), execution.getPackageName()), String.format("%s (%s)", execution.getMethodName(), execution.getPackageName()), execution.getStartTime()-firstExecution.getStartTime(), execution.getEndTime()-firstExecution.getStartTime()));
            }
            plotter.addPlot("By method", values, false, true, duration);

            values = new ArrayList<>();

            for (MethodExecution execution : v) {
                values.add(new GoogleCharts.Value(String.valueOf(values.size()), String.format("%s (%s)", execution.getMethodName(), execution.getPackageName()), execution.getStartTime()-firstExecution.getStartTime(), execution.getEndTime()-firstExecution.getStartTime()));
            }
            plotter.addPlot("Waterfall", values, true,false, duration);
        });
    }

    private static final int[] THREADS_ORDER = new int[]{1, 2, 8, 32};

    public interface CallableWithPayload<V> {
        V call(JsonLdDoc doc);
    }

    public void addSection(String title) {
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

    public <T> List<ResponseEntity<Result<T>>> executeMany(int numberOfFields, boolean normalize, int numberOfIteration, boolean parallelize, Link link, CallableWithPayload<ResponseEntity<Result<T>>> r) {
        List<ResponseEntity<Result<T>>> result = null;
        if (parallelize) {
            for (int i = 0; i < THREADS_ORDER.length; i++) {
                result = runWithThreads(numberOfFields, normalize, link, numberOfIteration, i * numberOfIteration, r, THREADS_ORDER[i]);
            }
            for (int i = THREADS_ORDER.length - 1; i >= 0; i--) {
                result = runWithThreads(numberOfFields, normalize, link, numberOfIteration, THREADS_ORDER.length * numberOfIteration + (THREADS_ORDER.length - i) * numberOfIteration, r, THREADS_ORDER[i]);
            }
        } else {
            result = runWithThreads(numberOfFields, normalize, link, numberOfIteration, 0, r, 1);
        }
        return result;
    }


    private <T> List<ResponseEntity<Result<T>>> runWithThreads(int numberOfFields, boolean normalize, Link link, int numberOfIteration, int idOffset, CallableWithPayload<ResponseEntity<Result<T>>> r, int threads) {
        List<ResponseEntity<Result<T>>> result;
        Instant start = Instant.now();
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        //When
        List<Future<ResponseEntity<Result<T>>>> futureResults = new ArrayList<>();
        for (int i = 0; i < numberOfIteration; i++) {
            final int iteration = i + idOffset;
            futureResults.add(executorService.submit(() -> {
                return r.call(TestDataFactory.createTestData(numberOfFields, !normalize, iteration, link == null ? null : link == Link.NEXT ? iteration + 1 : iteration - 1));
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

            List<GoogleCharts.Value> values = new ArrayList<>();
            Long startTime = result.stream().min(Comparator.comparing(res -> res.getBody().getStartTime())).get().getBody().getStartTime();
            for (ResponseEntity<Result<T>> res : result) {
                values.add(new GoogleCharts.Value(String.valueOf(values.size()), String.valueOf(res.getStatusCode()), res.getBody().getStartTime()-startTime, res.getBody().getStartTime()-startTime+res.getBody().getDurationInMs()));
            }
            double[] durations = result.stream().mapToDouble(res -> Objects.requireNonNull(res.getBody()).getDurationInMs().doubleValue()).toArray();
            plotter.addPlot(String.format("Run %d iterations with %d thread(s) in %dms (avg: %.2fms, median: %.2fms)", numberOfIteration, threads, Duration.between(start, end).toMillis(), new Mean().evaluate(durations), new Median().evaluate(durations)), values, false,false, Duration.between(start, end).toMillis());

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}
