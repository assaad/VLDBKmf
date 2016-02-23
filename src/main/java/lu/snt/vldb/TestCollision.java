package lu.snt.vldb;

import org.kevoree.modeling.util.PrimitiveHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Random;

/**
 * Created by assaad on 19/02/16.
 */
public class TestCollision {
    public static void main(String[] arg){

        try {
            Random random = new Random();
            HashSet<Integer> sets = new HashSet<Integer>();

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Enter the number of max hash: ");
            String input = null;
            input = br.readLine();
            int zz= Integer.parseInt(input);

            System.out.println("Enter the number of hash: ");
            input = br.readLine();
            int x = Integer.parseInt(input);

            System.out.println("r for random or anything else for kevoree hash: ");
            input = br.readLine();

            if (input.equals("r")) {
                System.out.println("Random");
                long start = System.nanoTime();
                for (int i = 0; i < x; i++) {
                    int y = random.nextInt() % zz;
                    if (!sets.contains(y)) {
                        sets.add(y);
                    }
                    if (i % 10000000 == 0 && i != 0) {
                        System.out.println("i: " + i / 10000000);
                    }
                }
                long end = System.nanoTime();
                double dd = (end - start) / 1000000000;
                System.out.println(dd + " s");
                double d = sets.size();
                d = (x - d) * 100.0 / x;
                System.out.println(sets.size() + " / " + x + " collisions: " + (x - sets.size()) + " percent: " + d + " %");
            } else {
                long start = System.nanoTime();
                for (int i = 0; i < x; i++) {

                    int y = PrimitiveHelper.tripleHash(0, 1000 + i / 1000, i % 250000) % zz;
                    if (!sets.contains(y)) {
                        sets.add(y);
                    }

                    if (i % 10000000 == 0 && i != 0) {
                        System.out.println("i: " + i / 10000000);
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
        catch (Exception ex){
            ex.printStackTrace();
        }

    }
}
