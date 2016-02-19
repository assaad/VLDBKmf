package lu.snt.vldb;

import org.kevoree.modeling.util.PrimitiveHelper;

import java.util.HashSet;
import java.util.Random;

/**
 * Created by assaad on 19/02/16.
 */
public class TestColl {
    public static void main(String[] arg){

        Random random=new Random();
        HashSet<Integer> sets=new HashSet<Integer>();

       System.out.println( (-100& 0x7FFFFFFF)%56);
        if(arg[0].equals("r")) {
            System.out.println("Random");
            long start = System.nanoTime();
            int x = 1000000000;
            for (int i = 0; i < x; i++) {
                int y = random.nextInt();
                //int y= PrimitiveHelper.tripleHash(0,1000+i/1000,i%250000);
                y = (y & 0x7FFFFFFF)%x;

                if (!sets.contains(y)) {
                    sets.add(y);
                }
                if (i % 1000000 == 0 && i != 0) {
                    double d = sets.size();
                    d = (i - d) * 100.0 / i;
                    System.out.println("i: " + i/1000000  + " " + sets.size() + " collisions: " + (i - sets.size()) + " percent: " + d + " %");
                }
            }
            long end = System.nanoTime();
            double dd = (end - start) / 1000000000;
            System.out.println(dd + " s");
            double d = sets.size();
            d = (x - d) * 100.0 / x;
            System.out.println(sets.size() + " / " + x + " collisions: " + (x - sets.size()) + " percent: " + d + " %");
        }
        else{
            long start = System.nanoTime();
            int x = 1000000000;
            for (int i = 0; i < x; i++) {

                int y= PrimitiveHelper.tripleHash(0,1000+i/1000,i%250000);
                y = (y & 0x7FFFFFFF)%x;

                if (!sets.contains(y)) {
                    sets.add(y);
                }
                if (i % 1000000 == 0 && i != 0) {
                    double d = sets.size();
                    d = (i - d) * 100.0 / i;
                    System.out.println("i: " + i/1000000 + " " + sets.size() + " collisions: " + (i - sets.size()) + " percent: " + d + " %");
                }
            }
            long end = System.nanoTime();
            double dd = (end - start) / 1000000000;
            System.out.println(dd + " s");
            double d = sets.size();
            d = (x - d) * 100.0 / x;
            System.out.println(sets.size() + " / " + x + " collisions: " + (x - sets.size()) + " percent: " + d + " %");

        }

    }
}
