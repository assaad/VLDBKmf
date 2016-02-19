package lu.snt.vldb;

import org.kevoree.modeling.util.PrimitiveHelper;

/**
 * Created by assaad on 19/02/16.
 */
public class TestHash {
    public static void main(String arg[]){
        System.out.println(PrimitiveHelper.tripleHash(0,10,20));
        System.out.println(PrimitiveHelper.tripleHash(0,10,21));
        System.out.println(PrimitiveHelper.tripleHash(0,11,20));
        System.out.println(PrimitiveHelper.tripleHash(0,11,21));

        System.out.println(PrimitiveHelper.tripleHash(10,0,20));
        System.out.println(PrimitiveHelper.tripleHash(10,20,0));
        System.out.println(PrimitiveHelper.tripleHash(20,0,10));
        System.out.println(PrimitiveHelper.tripleHash(20,10,0));
    }
}

