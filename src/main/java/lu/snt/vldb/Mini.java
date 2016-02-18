package lu.snt.vldb;

import org.kevoree.modeling.KCallback;
import org.kevoree.modeling.KModel;
import org.kevoree.modeling.KObject;
import org.kevoree.modeling.memory.manager.DataManagerBuilder;
import org.kevoree.modeling.memory.manager.internal.KInternalDataManager;
import org.kevoree.modeling.memory.space.impl.press.PressHeapChunkSpace;
import org.kevoree.modeling.meta.KMetaClass;
import org.kevoree.modeling.meta.KPrimitiveTypes;
import org.kevoree.modeling.meta.impl.MetaModel;
import org.kevoree.modeling.scheduler.impl.AsyncScheduler;
import org.kevoree.modeling.scheduler.impl.DirectScheduler;

import java.util.concurrent.atomic.AtomicInteger;

public class Mini {


    public static void main(String[] arg) {
        final int valuesToInsert = 100;
        final long timeOrigin = 1000;
        MetaModel dynamicMetaModel = new MetaModel("MyMetaModel");
        final KMetaClass sensorMetaClass = dynamicMetaModel.addMetaClass("Sensor");
        sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of threads: " + threads);
        final KModel model = dynamicMetaModel.createModel(
                DataManagerBuilder.create()
                        .withSpace(new PressHeapChunkSpace(1000))
                        .withScheduler(new AsyncScheduler().workers(threads))
                        //.withScheduler(new DirectScheduler())
                        .build());
        //  final KModel model= dynamicMetaModel.createModel(DataManagerBuilder.create().withScheduler(new DirectScheduler()).build());

        model.connect(new KCallback() {
            @Override
            public void on(Object o) {
                KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                long uuid = object.uuid();
                final AtomicInteger at = new AtomicInteger(0);
                for (long i = 0; i < valuesToInsert; i++) {
                    final double value = i * 0.3;
                    model.lookup(0, timeOrigin + i, uuid, new KCallback<KObject>() {
                        @Override
                        public void on(KObject kObject) {
                            kObject.set(kObject.metaClass().attribute("value"), value);
                            at.incrementAndGet();
                            //kill the object
                            kObject.destroy();
                        }
                    });
                }
                try {
                    while (at.get() != valuesToInsert) {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                object.destroy();

                KInternalDataManager manager = (KInternalDataManager) model.manager();
                manager.space().printDebug(model.metaModel());
            }
        });
    }


}
