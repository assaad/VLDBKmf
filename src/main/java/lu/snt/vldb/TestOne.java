package lu.snt.vldb;

import org.kevoree.modeling.KCallback;
import org.kevoree.modeling.KModel;
import org.kevoree.modeling.KObject;
import org.kevoree.modeling.memory.manager.DataManagerBuilder;
import org.kevoree.modeling.memory.strategy.impl.PressHeapMemoryStrategy;
import org.kevoree.modeling.meta.KMetaClass;
import org.kevoree.modeling.meta.KPrimitiveTypes;
import org.kevoree.modeling.meta.impl.MetaModel;
import org.kevoree.modeling.scheduler.impl.AsyncScheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by assaad on 16/02/16.
 */
public class TestOne {
    public static void main(String[] arg){

        final int valuesToInsert = 10000;

        final long timeOrigin=1000;
        MetaModel dynamicMetaModel= new MetaModel("MyMetaModel");
        final KMetaClass sensorMetaClass= dynamicMetaModel.addMetaClass("Sensor");
        sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);
        int threads = Runtime.getRuntime().availableProcessors()-3;
        System.out.println("Number of threads: "+threads);

        final KModel model= dynamicMetaModel.createModel(DataManagerBuilder.create().withMemoryStrategy(new PressHeapMemoryStrategy(20000)).withScheduler(new AsyncScheduler().workers(threads)).build());
        //  final KModel model= dynamicMetaModel.createModel(DataManagerBuilder.create().withScheduler(new DirectScheduler()).build());

        final double[] res=new double[2];

     //   final CountDownLatch cdt=new CountDownLatch(valuesToInsert);
        final CountDownLatch cdt2=new CountDownLatch(valuesToInsert);
        final CountDownLatch cdt3=new CountDownLatch(1);

        model.connect(new KCallback() {
            @Override
            public void on(Object o) {
                KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                long uuid=object.uuid();
                long start;
                long end;
                double speed;
                start=System.nanoTime();

                final AtomicInteger at = new AtomicInteger(0);

                for(long i=0;i<valuesToInsert;i++) {

                    final long ii= i;
                    final double value = ii * 0.3;
                    model.lookup(0, timeOrigin + ii, uuid, new KCallback<KObject>() {
                        @Override
                        public void on(KObject kObject) {
                            System.out.println(" Inserting " + ii + " done");
                            kObject.set(kObject.metaClass().attribute("value"), value);
                           // cdt.countDown();
                            at.incrementAndGet();
                        }
                    });
                }


                try {
                    while (at.get()!=valuesToInsert){
                        Thread.sleep(100);
                    }
                  //  cdt.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                end=System.nanoTime();
                speed=(end-start)/(valuesToInsert);
                res[0]= speed;
                System.out.println("Inserted "+valuesToInsert+" values in: "+speed+" ns/val");

                start=System.nanoTime();
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
                                System.out.println(" Error in reading");
                            }
                            cdt2.countDown();
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
                res[0]= speed;
                System.out.println("Read "+valuesToInsert+" values in: "+speed+" ns/val");
                cdt3.countDown();


            }
        });



    }
}
