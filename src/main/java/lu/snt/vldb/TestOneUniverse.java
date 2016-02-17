package lu.snt.vldb;

import org.kevoree.modeling.KCallback;
import org.kevoree.modeling.KModel;
import org.kevoree.modeling.KObject;
import org.kevoree.modeling.defer.KDefer;
import org.kevoree.modeling.defer.impl.Defer;
import org.kevoree.modeling.memory.manager.DataManagerBuilder;
import org.kevoree.modeling.memory.strategy.impl.PressHeapMemoryStrategy;
import org.kevoree.modeling.meta.KMetaClass;
import org.kevoree.modeling.meta.KPrimitiveTypes;
import org.kevoree.modeling.meta.impl.MetaModel;
import org.kevoree.modeling.scheduler.impl.AsyncScheduler;
import org.kevoree.modeling.scheduler.impl.DirectScheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by assaad on 10/02/16.
 */
public class TestOneUniverse {
    final static long timeOrigin=1000;

    public static void main(String arg[]) {

       /* MetaModel dynamicMetaModel= new MetaModel("MyMetaModel");
        final KMetaClass sensorMetaClass= dynamicMetaModel.addMetaClass("Sensor");
        sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);
        int threads = Runtime.getRuntime().availableProcessors()-1;
        threads=3;
        //  System.out.println("Number of threads: "+threads);

        final KModel model= dynamicMetaModel.createModel(DataManagerBuilder.create().withMemoryStrategy(new PressHeapMemoryStrategy(20000)).withScheduler(new AsyncScheduler().workers(threads)).build());
        // final KModel model= dynamicMetaModel.createModel(DataManagerBuilder.create().withScheduler(new DirectScheduler()).build());

        final double[] res=new double[2];

        final AtomicLong counter = new AtomicLong(0);*/



       long val = Long.parseLong(arg[0]);
        int num = Integer.parseInt(arg[1])+1;

        Gaussian g = new Gaussian();

        for (int i = 0; i < num; i++) {
            double[] d = benchmark(val);
            System.gc();
            try {
                Thread.sleep(100);
            } catch (Exception ex) {

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
                System.out.print("Round " + i + ": ");
                g.print();
            }
        }

        System.out.println("final result "+val);
        System.out.print(val+" ");
        g.print();


    }


    public static double[] benchmark(final long valuesToInsert){
        MetaModel dynamicMetaModel= new MetaModel("MyMetaModel");
        final KMetaClass sensorMetaClass= dynamicMetaModel.addMetaClass("Sensor");
        sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);
        //int threads = Runtime.getRuntime().availableProcessors()-1;
        //System.out.println("Number of threads: "+threads);

       // final KModel model= dynamicMetaModel.createModel(DataManagerBuilder.create().withMemoryStrategy(new PressHeapMemoryStrategy(20000)).withScheduler(new AsyncScheduler().workers(threads)).build());
        final KModel model= dynamicMetaModel.createModel(DataManagerBuilder.create().withScheduler(new DirectScheduler()).build());

        final double[] res=new double[2];

       // final AtomicLong counter = new AtomicLong(0);
        final CountDownLatch cdt=new CountDownLatch((int) valuesToInsert);
        final CountDownLatch cdt2=new CountDownLatch((int) valuesToInsert);
        final CountDownLatch cdt3=new CountDownLatch(1);
        final long[] compare=new long[1];
        compare[0]=1000000;

        model.connect(new KCallback() {
            @Override
            public void on(Object o) {
                KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                long uuid=object.uuid();
                //Insert test
                long start,end;
                double speed;
                start=System.nanoTime();
                final long finalStart2 = start;
                for(long i=0;i<valuesToInsert;i++) {

                    final long ii= i;
                    final double value = ii * 0.3;
                    model.lookup(0, timeOrigin + ii, uuid, new KCallback<KObject>() {
                        @Override
                        public void on(KObject kObject) {
                          //  System.out.println("Inserting " + ii + " done");
                            kObject.set(kObject.metaClass().attribute("value"), value);
                            cdt.countDown();
                            long x=valuesToInsert-cdt.getCount();
                            if(x==compare[0]){
                                double end2=System.nanoTime();
                                double speed2=(end2- finalStart2);
                                double speed3=speed2/(x);
                                double perm=1000000.0/speed3;
                                compare[0]=compare[0]*2;
                                System.out.println("Count "+(x/1000000)+"M, time per value: "+speed3+", per sec:  "+perm+" k/s");
                            }
                        }
                    });
                }


                try {
                    cdt.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                    end=System.nanoTime();
                    speed=(end-start)/(valuesToInsert);
                    res[0]= speed;
                double perm=1000000.0/speed;
                System.out.println("Count "+(valuesToInsert/1000000)+"M, insert per value: "+speed+", per sec:  "+perm+" k/s");
                  //  System.out.println("Inserted "+valuesToInsert+" values in: "+speed+" ns/val");

                compare[0]=1000000;
                start=System.nanoTime();
                final long finalStart = start;
              //  counter.set(0);
                for(long i=0;i<valuesToInsert;i++) {
                    final long ii= i;
                    final double value = i * 0.3;
                    model.lookup(0, timeOrigin + ii, uuid, new KCallback<KObject>() {
                        @Override
                        public void on(KObject kObject) {
                            //   System.out.println("Inserting " + insertCounter[0] + " done");
                            double v = (Double) kObject.get(kObject.metaClass().attribute("value"));
                            if(v!=value){
                                System.out.println("Error in reading");
                            }
                            cdt2.countDown();
                            long x=valuesToInsert-cdt2.getCount();
                            if(x==compare[0]){
                                double end2=System.nanoTime();
                                double speed2=(end2- finalStart);
                                double speed3=speed2/(x);
                                double perm=1000000.0/speed3;
                                compare[0]=compare[0]*2;
                                System.out.println("Count "+(x/1000000)+"M, read per value: "+speed3+", per sec:  "+perm+" k/s");
                            }
                        }
                    });
                }

                try {
                    cdt2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                end=System.nanoTime();
                speed=(end-start)/(valuesToInsert);
                perm=1000000.0/speed;
                System.out.println("Count "+(valuesToInsert/1000000)+"M, time per value: "+speed+", per sec:  "+perm+" k/s");
                res[1]= speed;
               // System.out.println("Read "+valuesToInsert+" values in: "+speed+" ns/val");
                cdt3.countDown();


            }
        });


        try{
            cdt3.await();
            final CountDownLatch cdt4 = new CountDownLatch(1);
            model.disconnect(new KCallback() {
                @Override
                public void on(Object o) {
                //    System.out.println("Test over");
                    cdt4.countDown();
                }
            });
            cdt4.await();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return res;

    }
}
