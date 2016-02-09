package lu.snt.vldb;

        import org.kevoree.modeling.KCallback;
        import org.kevoree.modeling.KModel;
        import org.kevoree.modeling.KObject;
        import org.kevoree.modeling.KUniverse;
        import org.kevoree.modeling.cdn.KContentDeliveryDriver;
        import org.kevoree.modeling.memory.manager.DataManagerBuilder;
        import org.kevoree.modeling.memory.manager.internal.KInternalDataManager;
        import org.kevoree.modeling.memory.strategy.impl.OffHeapMemoryStrategy;
        import org.kevoree.modeling.meta.KMetaClass;
        import org.kevoree.modeling.meta.KPrimitiveTypes;
        import org.kevoree.modeling.meta.impl.MetaModel;
        import org.kevoree.modeling.plugin.ChroniclePlugin;
        import org.kevoree.modeling.scheduler.impl.DirectScheduler;

        import java.io.File;
        import java.util.concurrent.CountDownLatch;

/**
 * Created by assaad on 22/09/15.
 */
public class KMFTest {


    private static MetaModel dynamicMetaModel;
    private static KMetaClass sensorMetaClass;
    private static KModel model;
    private static long objId;
    private static int saveStep=10000000; //save every 100 000 values

    public static void createMetaModel(){

        dynamicMetaModel = new MetaModel("MyMetaModel");

        sensorMetaClass = dynamicMetaModel.addMetaClass("Sensor");
        sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);
        sensorMetaClass.addRelation("siblings", sensorMetaClass, null);
       // model = dynamicMetaModel.createModel(DataManagerBuilder.create().withMemoryStrategy(new OffHeapMemoryStrategy()).withScheduler(new DirectScheduler()).build());

        model = dynamicMetaModel.createModel(DataManagerBuilder.create().withScheduler(new DirectScheduler()).build());


        // KContentDeliveryDriver levelDBDriver = new ChroniclePlugin(10000000,null);
       // model = dynamicMetaModel.createModel(DataManagerBuilder.create().withContentDeliveryDriver(levelDBDriver).withScheduler(new DirectScheduler()).build());

    }


    public static void main(String[] arg) {
        //Create initial metamodel and a model instance from the metamodel.

        createMetaModel();
        testStairs(10000);
    }

    public static double[] testStairs(int ts) {
        final long timeOrigin = 1000l;

        final int timestamped=ts;
        final double[] ress=new double[2];
        final int exp=2;
        model.connect(new KCallback() {
            @Override
            public void on(Object o) {
                KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                objId = object.uuid();
                object=null;

                long start,end;
                double res;


                start=System.nanoTime();
                for(int i=0;i<timestamped;i++){
                    insert(i,timeOrigin+i,i*0.3);
                    split(i);
                }

                end=System.nanoTime();
                res=(end-start)/(1000000);
                System.out.println("Inserting "+timestamped+" values in original stairs ins "+res+" ms");

                ress[0]=res;

                start=System.nanoTime();

                for(int j=0;j<exp;j++) {
                    for (int i = 0; i < timestamped; i++) {
                        if (get(timestamped-1, timeOrigin + i) != i * 0.3) {
                            System.out.println("error");
                        }
                    }
                }

                end=System.nanoTime();
                res=(end-start)/(1000000*exp);
                System.out.println("Reading "+timestamped+" values in 2nd universe in "+res+" ms");

            }
        });
        return ress;
    }


    public static double[] testMultiverse(int ts, int un, int td) {
        final long timeOrigin = 1000l;

        final int timestamped=ts;
        final int universes =un;
        final int tdinit=td;
        final double[] ress=new double[2];
        final int exp=20;
        model.connect(new KCallback() {
            @Override
            public void on(Object o) {
                KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                objId = object.uuid();
                object=null;

                long unit = 1000;
                long start,end;
                double res;


                start=System.nanoTime();
                for(int i=0;i<timestamped;i++){
                    insert(0,timeOrigin+i,i*0.3);
                    //ress[1]+=1;
                }

                end=System.nanoTime();
                res=(end-start)/(1000000);
                System.out.println("Inserting "+timestamped+" values in original universe ins "+res+" ms");

                start=System.nanoTime();
                for(int j=1;j<universes+1;j++){
                    split(0);
                    for(int i=tdinit;i<timestamped;i++){
                        insert(j,timeOrigin+i,i*0.3+j*7);
                        ress[1]+=1;
                    }

                }
                end=System.nanoTime();
                res=(end-start)/(1000000);
                System.out.println("Inserting "+ress[1]+" values in "+ universes+" universe ins "+res+" ms");
                ress[0]=res;

                start=System.nanoTime();

                for(int j=0;j<exp;j++) {
                    for (int i = 0; i < timestamped; i++) {
                        get(1, timeOrigin + i) ;
                    }
                }

                end=System.nanoTime();
                res=(end-start)/(1000000*exp);
                System.out.println("Reading "+timestamped+" values in 2nd universe in "+res+" ms");

            }
        });
        return ress;
    }


    public static void testOneUniverse(final long timestamped) {
        final long timeOrigin = 1000l;

        final int exp=5;
        model.connect(new KCallback() {
            @Override
            public void on(Object o) {
                KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                objId = object.uuid();
                //object=null;

                long unit = 1000;
                long start,end;
                double res;

                start=System.nanoTime();


                for(long i=0;i<timestamped;i++){
                    insert(0,timeOrigin+i,i*0.3);
                    if(i%saveStep==0){
                       save();
                    }
                }

                end=System.nanoTime();
                res=(end-start)/1000000;
                System.out.println("Inserting "+timestamped+" values in main universe in "+res+" ms");

                start=System.nanoTime();

                for(int j=0;j<exp;j++) {
                    for (long i = 0; i < timestamped; i++) {
                        if (get(0, timeOrigin + i) != i * 0.3) {
                            System.out.println("error");
                        }
                    }
                }

                end=System.nanoTime();
                res=(end-start)/(1000000*exp);
                System.out.println("Reading "+timestamped+" values in main universe in "+res+" ms");

            }
        });
    }

    private static void split(int parent) {
        KUniverse uni = model.universe(parent).diverge();
    }

    private static void insert(long uId, long time, final double value) {

        model.lookup(uId,time,objId, new KCallback<KObject>() {
            public void on(KObject kObject) {
                kObject.set(kObject.metaClass().attribute("value"), value);
            }
        });
    }


    public static void save(){
        model.save(new KCallback() {
            public void on(Object o) {
                KInternalDataManager ff= (KInternalDataManager) model.manager();
                System.out.println(ff.spaceSize());
            }
        });
    }


    public static double get(long uId, long time) {
        final Object[] myvalue = {null};
        final CountDownLatch latch = new CountDownLatch(1);

        model.lookup(uId,time,objId, new KCallback<KObject>() {
            public void on(KObject kObject) {
                myvalue[0] = kObject.get(kObject.metaClass().attribute("value"));
                latch.countDown();
            }
        });
        try {
                       latch.await();
            } catch (InterruptedException e) {
                        e.printStackTrace();
            }
        return (Double) myvalue[0];
    }

}



