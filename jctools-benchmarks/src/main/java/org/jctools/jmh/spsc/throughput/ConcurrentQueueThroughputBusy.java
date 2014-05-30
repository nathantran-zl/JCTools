/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jctools.jmh.spsc.throughput;

import java.util.concurrent.TimeUnit;

import org.jctools.ConcurrentQueue;
import org.jctools.ConcurrentQueueConsumer;
import org.jctools.ConcurrentQueueFactory;
import org.jctools.ConcurrentQueueProducer;
import org.jctools.spsc.FFBufferWithOfferBatch;
import org.jctools.spsc.SPSCConcurrentQueueFactory;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.logic.Control;

@State(Scope.Group)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class ConcurrentQueueThroughputBusy {
    private final ConcurrentQueue<Integer> q = SPSCConcurrentQueueFactory.createQueue();
    private final ConcurrentQueueProducer<Integer> producer = q.producer();
    private final ConcurrentQueueConsumer<Integer> consumer = q.consumer();
    private final static Integer ONE = 777;

    @GenerateMicroBenchmark
    @Group("tpt")
    @GroupThreads(1)
    public void offer(Control cnt) {
        while (!producer.offer(ONE) && !cnt.stopMeasurement) {
        }
    }

    @GenerateMicroBenchmark
    @Group("tpt")
    @GroupThreads(1)
    public void poll(Control cnt, ConsumerMarker cm) {
        while (consumer.poll() == null && !cnt.stopMeasurement) {
        }
    }
    private static ThreadLocal<Object> marker = new ThreadLocal<>();

    @State(Scope.Thread)
    public static class ConsumerMarker {
        public ConsumerMarker() {
            marker.set(this);
        }
    }
    @TearDown(Level.Iteration)
    public void emptyQ() {
        if(marker.get() == null)
            return;
        // sadly the iteration tear down is performed from each participating thread, so we need to guess
        // which is which (can't have concurrent access to poll).
        while (consumer.poll() != null)
            ;
    }
}