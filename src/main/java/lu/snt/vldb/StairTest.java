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
    static KModel model;

    public static void main(String[] arg) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Enter the number of stairs to create?: ");
            String input = null;
            input = br.readLine();
            final int stairs = Integer.parseInt(input);

            System.out.println("Enter the number of point in each step: ");
            input = br.readLine();
            final int step = Integer.parseInt(input);
            final int valuesToInsert = stairs * step;

            System.out.println("Total to insert is: " + valuesToInsert);

            //  System.out.println("Number of threads available: " + threads + " insert number of threads: ");
            //  input = br.readLine();
            final PressHeapChunkSpace phc = new PressHeapChunkSpace(valuesToInsert*4,100);


            System.out.println("Direct scheduler created");
            model = dynamicMetaModel.createModel(DataManagerBuilder.create().withSpace(phc).withScheduler(new DirectScheduler()).withSpaceManager(new ManualChunkSpaceManager()).build());


            System.out.println("Number of time to repeat the experiment?: ");
            input = br.readLine();
            final int num = Integer.parseInt(input);

            Gaussian g = new Gaussian();

            for (int i = 0; i < num; i++) {
                double[] d = bench(valuesToInsert,step,stairs);
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






        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static double[] bench(int valuesToInsert, int step, int stairs) {
        double[] res=new double[2];



        return res;
    }


}
