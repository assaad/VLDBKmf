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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.CountDownLatch;

/**
 * Created by assaad on 25/02/16.
 */
public class PercentageTest {
    final static NumberFormat formatter = new DecimalFormat("#0.00");
    final static MetaModel dynamicMetaModel = new MetaModel("MyMetaModel");
    final static KMetaClass sensorMetaClass = dynamicMetaModel.addMetaClass("Sensor");
    final static KMetaAttribute attribute = sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);
    final static long timeOrigin=1000;
    final static long divergentTime=2000;

    public static void main(String[] arg) {

        int numOfObj=10000;

        final int num = 10;
        final int step=100;

        for(int percent=0;percent<=100;percent+=10) {
            Gaussian g = new Gaussian();
            for (int i = 0; i < num; i++) {

                double[] d = bench(numOfObj, percent,step);
                System.gc();
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (i >0){
                    g.feed(d);
                }
               // System.out.println("done "+i);
            }
            System.out.print(percent+" ");
            for (int i = 0; i < 5; i++) {
                System.out.print(formatter.format(g.getAvg(i)) + " ");
            }
            System.out.println();
        }

    }


    public static double[] bench(final int numOfObj, final int percent, final int step){

        final PressHeapChunkSpace phc = new PressHeapChunkSpace(numOfObj * step*10, 100);

        final double[] res=new double[5];

        final KModel model = dynamicMetaModel.createModel(DataManagerBuilder.create()
                .withSpace(phc)
                .withScheduler(new DirectScheduler())
                .withSpaceManager(new ManualChunkSpaceManager())
                .build());



        model.connect(new KCallback() {
            @Override
            public void on(Object o) {
                long[] uids=new long[numOfObj];

                //create the objects
                for(int i=0;i<numOfObj;i++){
                    KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                    uids[i]=object.uuid();
                }

                long start=System.nanoTime();
                for(int i=0;i<numOfObj;i++){
                    for(int t=0;t<step;t++) {
                        final double vv = i+t;
                        model.lookup(0, timeOrigin+t, uids[i], new KCallback<KObject>() {
                            @Override
                            public void on(KObject kObject) {
                                kObject.set(attribute, vv);
                                kObject.destroy();
                            }
                        });
                    }
                }
                long end = System.nanoTime();
                double speed = (end - start) / (numOfObj*step);
                double perm = 1000000.0 / speed;
                res[0] = perm;
              //  System.out.println("Speed to write in main universe: "+formatter.format(res[0]));


                final int numToChange=numOfObj*percent/100;

                KUniverse uni = model.universe(0).diverge();
                final long newUniverse=uni.key();

                KUniverse uni2 = model.universe(newUniverse).diverge();
                final long newUniverse2=uni2.key();


                start=System.nanoTime();
                for(int i=0;i<numToChange;i++){
                    for(int t=0;t<step;t++) {
                        final double vv = numOfObj * 2 + i+t;
                        model.lookup(newUniverse, divergentTime+t, uids[i], new KCallback<KObject>() {
                            @Override
                            public void on(KObject kObject) {
                                kObject.set(attribute, vv);
                                kObject.destroy();
                            }
                        });
                    }
                }
                end = System.nanoTime();
                if(numToChange!=0) {
                    speed = (end - start) / (numToChange*step);
                    perm = 1000000.0 / speed;
                    res[1] = perm;
                }
                // System.out.println("Speed to write in second universe: "+formatter.format(res[1]));


                start=System.nanoTime();
                for(int i=0;i<numOfObj;i++) {
                    for (int t = 0; t < step; t++) {
                        final double vv = i + t;
                        model.lookup(0, timeOrigin+t, uids[i], new KCallback<KObject>() {
                            @Override
                            public void on(KObject kObject) {
                                double v = (Double) kObject.get(attribute);
                                if (v != vv) {
                                    System.out.println("Error in reading 1- "+vv+" "+v);
                                }
                                kObject.destroy();
                            }
                        });
                    }
                }
                end = System.nanoTime();
                speed = (end - start) / (numOfObj*step);
                perm = 1000000.0 / speed;
                res[2] = perm;
               // System.out.println("Speed to read in first universe: "+formatter.format(res[2]));


                start=System.nanoTime();
                for(int i=0;i<numOfObj;i++) {
                    for (int t = 0; t < step; t++) {
                        final double vv = i + t;
                        model.lookup(newUniverse2, timeOrigin+t, uids[i], new KCallback<KObject>() {
                            @Override
                            public void on(KObject kObject) {
                                double v = (Double) kObject.get(attribute);
                                if (v != vv) {
                                    System.out.println("Error in reading 2- "+vv+" "+v);
                                }
                                kObject.destroy();
                            }
                        });
                    }
                }
                end = System.nanoTime();
                speed = (end - start) / (numOfObj*step);
                perm = 1000000.0 / speed;
                res[3] = perm;
               // System.out.println("Speed to read in second universe before divergence: "+formatter.format(res[3]));



                start=System.nanoTime();
                for(int i=0;i<numOfObj;i++) {
                    for (int t = 0; t < step; t++) {
                        final double vv;
                        if (i < numToChange) {
                            vv = numOfObj * 2 + i+t;
                        } else {
                            vv = i+step-1;
                        }
                        model.lookup(newUniverse2, divergentTime+t, uids[i], new KCallback<KObject>() {
                            @Override
                            public void on(KObject kObject) {
                                double v = (Double) kObject.get(attribute);
                                if (v != vv) {
                                    System.out.println("Error in reading 3- "+vv+" "+v);
                                }
                                kObject.destroy();
                            }
                        });
                    }
                }
                end = System.nanoTime();
                speed = (end - start) / (numOfObj*step);
                perm = 1000000.0 / speed;
                res[4] = perm;
                //System.out.println("Speed to read in second universe after divergence: "+formatter.format(res[4]));

            }
        });

        return res;
    }

}
