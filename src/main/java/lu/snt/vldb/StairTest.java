package lu.snt.vldb;

import org.kevoree.modeling.KCallback;
import org.kevoree.modeling.KModel;
import org.kevoree.modeling.KObject;
import org.kevoree.modeling.KUniverse;
import org.kevoree.modeling.memory.manager.DataManagerBuilder;
import org.kevoree.modeling.memory.space.impl.ManualChunkSpaceManager;
import org.kevoree.modeling.memory.space.impl.press.PressHeapChunkSpace;
import org.kevoree.modeling.meta.KMetaAttribute;
import org.kevoree.modeling.meta.KMetaClass;
import org.kevoree.modeling.meta.KPrimitiveTypes;
import org.kevoree.modeling.meta.impl.MetaModel;
import org.kevoree.modeling.scheduler.impl.DirectScheduler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.CountDownLatch;

/**
 * Created by assaad on 10/02/16.
 */
public class StairTest {
    final static NumberFormat formatter = new DecimalFormat("#0.00");
    final static long timeOrigin = 1000;
    final static MetaModel dynamicMetaModel = new MetaModel("MyMetaModel");
    final static KMetaClass sensorMetaClass = dynamicMetaModel.addMetaClass("Sensor");
    final static KMetaAttribute attribute = sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);


    public static void main(String[] arg) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Enter the number of stairs to create?: ");
            String input = null;
            input = br.readLine();
            final int stairs = Integer.parseInt(input);

            System.out.println("Enter the number of point in each step: ");
           /* input = br.readLine();
            final int steps = Integer.parseInt(input);*/
            final int steps = 1000;
            final int valuesToInsert = stairs*steps;
            System.out.println("Total to insert is: " + valuesToInsert);

            //  System.out.println("Number of threads available: " + threads + " insert number of threads: ");


            System.out.println("Direct scheduler created");


            System.out.println("Number of time to repeat the experiment?: ");
            input = br.readLine();
            final int num = Integer.parseInt(input);

            Gaussian g = new Gaussian();

            for (int i = 0; i < num; i++) {
                double[] d = bench(valuesToInsert,steps,stairs);
                System.gc();
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if(i==0){
                    System.out.println("Round 0: " +formatter.format(d[0])+" "+formatter.format(d[1])+" "+formatter.format(d[2]));
                }
                if(i==1){
                    g.feed(d);
                    System.out.println("Round 1: " +formatter.format(d[0])+" "+formatter.format(d[1])+" "+formatter.format(d[2]));
                }

                //g.feed(d);
                if (i > 1) {
                    g.feed(d);
                    System.out.println("Round " + i + ": "+formatter.format(d[0])+" "+formatter.format(d[1])+" "+formatter.format(d[2]));
                  //  g.print();
                }
            }
            g.print();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static double[] bench(final int valuesToInsert, final int steps, final int stairs) throws InterruptedException {

        //  input = br.readLine();
        final PressHeapChunkSpace phc = new PressHeapChunkSpace(valuesToInsert*4,100);

        final KModel model = dynamicMetaModel.createModel(DataManagerBuilder.create()
                .withSpace(phc)
                .withScheduler(new DirectScheduler())
                .withSpaceManager(new ManualChunkSpaceManager())
                .build());


        final double[] res = new double[3];


        final CountDownLatch cdt = new CountDownLatch(valuesToInsert);
        final CountDownLatch cdt2 = new CountDownLatch(valuesToInsert);
        final CountDownLatch cdt3 = new CountDownLatch(valuesToInsert);
        final CountDownLatch cdt4 = new CountDownLatch(1);

        final long[] universekeys=new long[stairs];


        final long[][] times = new long[stairs][steps];
        int count = 0;
        for (int j = 0; j < stairs; j++) {
            for (int i = 0; i < steps; i++) {
                times[j][i] = timeOrigin + count*2;
                count++;
            }
        }

        model.connect(new KCallback() {
            @Override
            public void on(Object o) {
                final KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                final long uid = object.uuid();
                //     System.out.println("Lookup all is now: " + split + " , model connected, objects created, start inserting...");


                //Insert test
                long start, end;
                double speed;

                start = System.nanoTime();
                long universe=object.universe();


                for (int i = 0; i < stairs; i++) {
                    final int ii=i;
                    final long uu=universe;

                    double value = steps * ii*2;
                    for(int k=0;k<steps;k++){
                        final double vv=value;
                        model.lookup(uu, times[ii][k], uid, new KCallback<KObject>() {
                            @Override
                            public void on(KObject kObject) {
                                kObject.set(attribute, vv);
                                kObject.destroy();
                            }
                        });
                        value += 0.5;
                        cdt.countDown();
                    }
                    universekeys[ii]=universe;
                    KUniverse uni = model.universe(universe).diverge();
                    universe=uni.key();
                }

                try {
                    cdt.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                end = System.nanoTime();
                speed = (end - start) / (valuesToInsert);
                double perm = 1000000.0 / speed;
                res[0] = perm;
              //  System.out.println("Count " + (valuesToInsert / 1000000) + "M, insert pace: " + formatter.format(speed) + " ns/value, avg speed:  " + formatter.format(perm) + " kv/s");

                for (int i = 1; i < stairs; i++) {
                    final int ii=i;

                    double value = steps * ii*3;
                    for(int k=0;k<steps;k++){
                        final double vv=value;
                        model.lookup(universekeys[0], times[ii][k], uid, new KCallback<KObject>() {
                            @Override
                            public void on(KObject kObject) {
                                kObject.set(attribute, vv);
                                kObject.destroy();
                            }
                        });
                        value += 0.5;
                    }
                }

                start = System.nanoTime();
                for (int i = 0; i < stairs; i++) {
                    final int ii=i;
                    model.lookupAllTimes( universekeys[0], times[ii], uid, new KCallback<KObject[]>() {
                        @Override
                        public void on(KObject[] kObjects) {
                            double value;
                            if(ii==0) {
                                value = steps * ii * 2;
                            }
                            else{
                                value= steps * ii*3;
                            }
                            for (int k = 0; k < kObjects.length; k++) {
                                double v = (Double) kObjects[k].get(attribute);
                                kObjects[k].destroy();
                                if (value != v) {
                                    System.out.println("1- Error in reading " + kObjects[k].now() + " universe id: " + universekeys[0] + " expected: " + value + " got: " + v);
                                }
                                value += 0.5;
                                cdt2.countDown();
                            }
                        }
                    });
                }
                try {
                    cdt2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                end = System.nanoTime();
                speed = (end - start) / (valuesToInsert);
                perm = 1000000.0 / speed;
                res[1] = perm;




                start = System.nanoTime();
                //  counter.set(0);

                for (int i = 0; i < stairs; i++) {
                    final int ii=i;
                    model.lookupAllTimes( universekeys[stairs-1], times[ii], uid, new KCallback<KObject[]>() {
                        @Override
                        public void on(KObject[] kObjects) {
                            double value;
                            value = steps * ii*2;
                            for (int k = 0; k < kObjects.length; k++) {
                                double v = (Double) kObjects[k].get(attribute);
                                kObjects[k].destroy();
                                if (value != v) {
                                    System.out.println("5- Error in reading " + kObjects[k].now() + " universe id: " + universekeys[stairs-1] + " expected: " + value + " got: " + v);
                                }
                                value += 0.5;
                                cdt3.countDown();
                            }
                        }
                    });
                }


                try {
                    cdt3.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                end = System.nanoTime();
                speed = (end - start) / (valuesToInsert);
                perm = 1000000.0 / speed;
                res[2] = perm;



          //      System.out.println("Count " + (valuesToInsert / 1000000) + "M, read pace: " + formatter.format(speed) + " ns/value, avg speed:  " + formatter.format(perm) + " kv/s");
                cdt4.countDown();


            }
        });


        cdt4.await();
        final CountDownLatch cdt5 = new CountDownLatch(1);
        model.disconnect(new KCallback() {
            @Override
            public void on(Object o) {
           //     System.out.println("Test over");
                cdt5.countDown();
            }
        });
        cdt5.await();

        return res;


    }


}
