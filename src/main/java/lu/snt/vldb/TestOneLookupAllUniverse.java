package lu.snt.vldb;

import org.kevoree.modeling.KCallback;
import org.kevoree.modeling.KModel;
import org.kevoree.modeling.KObject;
import org.kevoree.modeling.memory.manager.DataManagerBuilder;
import org.kevoree.modeling.memory.space.impl.ManualChunkSpaceManager;
import org.kevoree.modeling.memory.space.impl.press.PressHeapChunkSpace;
import org.kevoree.modeling.meta.KMetaAttribute;
import org.kevoree.modeling.meta.KMetaClass;
import org.kevoree.modeling.meta.KPrimitiveTypes;
import org.kevoree.modeling.meta.impl.MetaModel;
import org.kevoree.modeling.scheduler.impl.AsyncScheduler;
import org.kevoree.modeling.scheduler.impl.DirectScheduler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.CountDownLatch;

/**
 * Created by assaad on 16/02/16.
 */
public class TestOneLookupAllUniverse {
    final static NumberFormat formatter = new DecimalFormat("#0.00");
    final static long timeOrigin = 1000;
    final static MetaModel dynamicMetaModel = new MetaModel("MyMetaModel");
    final static KMetaClass sensorMetaClass = dynamicMetaModel.addMetaClass("Sensor");
    final static KMetaAttribute attribute = sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);
    static KModel model;

    public static void main(String[] arg) {
        try {
            int threads = Runtime.getRuntime().availableProcessors();
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Enter the number of time series to create?: ");
            String input = null;
            input = br.readLine();

            final int ts = Integer.parseInt(input);

            System.out.println("Enter the number of point in each Time serie?: ");
            input = br.readLine();
            final int tsp = Integer.parseInt(input);
            final int valuesToInsert = ts * tsp;

            System.out.println("Total to insert is: " + valuesToInsert);

          //  System.out.println("Number of threads available: " + threads + " insert number of threads: ");
          //  input = br.readLine();
            final PressHeapChunkSpace phc = new PressHeapChunkSpace(valuesToInsert,10);


            System.out.println("Direct scheduler created");
            model = dynamicMetaModel.createModel(DataManagerBuilder.create().withSpace(phc).withScheduler(new DirectScheduler()).withSpaceManager(new ManualChunkSpaceManager()).build());


            System.out.println("Number of time to repeat the experiment?: ");
            input = br.readLine();
            final int num = Integer.parseInt(input);




            Gaussian g = new Gaussian();

            for (int i = 0; i < num; i++) {
                double[] d = bench(valuesToInsert,tsp,ts);
                System.gc();
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if(i==0){
                    System.out.println("Round 0:" +d[0]+" "+d[1]);
                }
                if(i==1){
                    g.feed(d);
                    System.out.println("Round 1:" +d[0]+" "+d[1]);
                }

                //g.feed(d);
                if (i > 1) {
                    g.feed(d);
                    System.out.println("Round " + i + ": "+d[0]+" "+d[1]);
                    g.print();
                }
            }
            g.print();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double[] bench(final int valuesToInsert, final int tsp, final int ts) throws InterruptedException {

        final double[] res= new double[2];
        // final AtomicLong counter = new AtomicLong(0);
        final CountDownLatch cdt = new CountDownLatch( valuesToInsert);
        final CountDownLatch cdt2 = new CountDownLatch(valuesToInsert);
        final CountDownLatch cdt3 = new CountDownLatch(1);
        final long[] compare = new long[1];
        compare[0] = 1000000;

        final int split=1000;

        final int dim = (tsp - 1) / split + 1;


        final long[][] times = new long[dim][split];
        int count = 0;
        for (int j = 0; j < dim; j++) {
            for (int i = 0; i < split; i++) {
                times[j][i] = timeOrigin + count;
                count++;
            }
        }

        model.connect(new KCallback() {
            @Override
            public void on(Object o) {
                final long[] uids = new long[ts];
                for (int i = 0; i < ts; i++) {
                    KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                    uids[i] = object.uuid();
                }
           //     System.out.println("Lookup all is now: " + split + " , model connected, objects created, start inserting...");


                //Insert test
                long start, end;
                double speed;

                start = System.nanoTime();
                final long finalStart2 = start;


                for (int i = 0; i < ts; i++) {
                    final long uuid = uids[i];
                    for (int j = 0; j < dim; j++) {
                        final int jj = j;
                        model.lookupAllTimes(0, times[jj], uuid, new KCallback<KObject[]>() {
                            @Override
                            public void on(KObject[] kObjects) {
                                double value;
                                value = uuid * 7 + 0.7 * jj;
                                for (int k = 0; k < kObjects.length; k++) {
                                    kObjects[k].set(attribute, value);
                                    kObjects[k].destroy();
                                    value += 0.7;
                                    cdt.countDown();
                                }
                                long x = valuesToInsert - cdt.getCount();
                                if (x >= compare[0] || (x > 0 && x % 50000000 == 0)) {
                                    double end2 = System.nanoTime();
                                    double speed2 = (end2 - finalStart2);
                                    double speed3 = speed2 / (x);
                                    double perm = 1000000.0 / speed3;
                                    if (x >= compare[0]) {
                                        compare[0] = compare[0] * 2;
                                    }
                          //          System.out.println("Count " + (x / 1000000) + "M, insert pace: " + formatter.format(speed3) + " ns/value, avg speed:  " + formatter.format(perm) + " kv/s");
                                }
                            }
                        });
                    }
                }


                try {
                    cdt.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                end = System.nanoTime();
                speed = (end - start) / (valuesToInsert);
                double perm = 1000000.0 / speed;
                res[0]=perm;
              //  System.out.println("Count " + (valuesToInsert / 1000000) + "M, insert pace: " + formatter.format(speed) + " ns/value, avg speed:  " + formatter.format(perm) + " kv/s");

                compare[0] = 1000000;
                start = System.nanoTime();
                final long finalStart = start;
                //  counter.set(0);


                for (int i = 0; i < ts; i++) {
                    final long uuid = uids[i];
                    for (int j = 0; j < dim; j++) {
                        final int jj = j;
                        model.lookupAllTimes(0, times[jj], uuid, new KCallback<KObject[]>() {
                            @Override
                            public void on(KObject[] kObjects) {
                                double value;
                                value = uuid * 7 + 0.7 * jj;
                                for (int k = 0; k < kObjects.length; k++) {
                                    double v = (Double) kObjects[k].get(attribute);
                                    kObjects[k].destroy();
                                    if (v != value) {
                                        System.out.println("Error in reading " + kObjects[k].now() + " id: " + uuid + " expected: " + value + " got: " + v);
                                    }
                                    value += 0.7;
                                    cdt2.countDown();
                                }
                                long x = valuesToInsert - cdt2.getCount();
                                if (x >= compare[0] || (x > 0 && x % 50000000 == 0)) {
                                    double end2 = System.nanoTime();
                                    double speed2 = (end2 - finalStart);
                                    double speed3 = speed2 / (x);
                                    double perm = 1000000.0 / speed3;
                                    if (x >= compare[0]) {
                                        compare[0] = compare[0] * 2;
                                    }
                                 //   System.out.println("Count " + (x / 1000000) + "M, read pace: " + formatter.format(speed3) + " ns/value, avg speed:  " + formatter.format(perm) + " kv/s");
                                }
                            }
                        });
                    }
                }


                try {
                    cdt2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                end = System.nanoTime();
                speed = (end - start) / (valuesToInsert);
                perm = 1000000.0 / speed;
                res[1]=perm;

               // System.out.println("Count " + (valuesToInsert / 1000000) + "M, read pace: " + formatter.format(speed) + " ns/value, avg speed:  " + formatter.format(perm) + " kv/s");
                cdt3.countDown();


            }
        });


        cdt3.await();
        final CountDownLatch cdt4 = new CountDownLatch(1);
        model.disconnect(new KCallback() {
            @Override
            public void on(Object o) {
                System.out.println("Test over");
                cdt4.countDown();
            }
        });
        cdt4.await();

        return res;
    }
}

