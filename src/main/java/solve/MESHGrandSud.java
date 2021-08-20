package solve;

import restopt.BaseProblem;
import restopt.Data;

import java.io.IOException;
import java.nio.file.Paths;

public class MESHGrandSud {

    public static final String BASEPATH = baseproblem.Data.class.getResource("/data").getPath();

    public static void main(String[] args) throws IOException {

        int precision = 3;
//        String forest = Paths.get(BASEPATH, "forest_binary_480x480_0.7_wgs84.tif").toString();
//        String accessible = Paths.get(BASEPATH, "buffer_1000m_wgs84.tif").toString();
//        String restorable = Paths.get(BASEPATH, "restorable_area_480x480_wgs84_ha.tif").toString();
        String forest = Paths.get(BASEPATH, "forest_binary_0.7.tif").toString();
        String accessible = Paths.get(BASEPATH, "accessible_and_forest_presence.tif").toString();
        String restorable = Paths.get(BASEPATH, "restorable.tif").toString();
        BaseProblem baseProblem = new BaseProblem(new Data(forest, accessible, restorable), 1);
        baseProblem.postNbComponentsConstraint(1, 1);
        baseProblem.postCompactnessConstraint(6);
        baseProblem.postRestorableConstraint(0, 20000, 817, 0.7);
//        baseProblem.postRestorableConstraint(100, 200, 23, 0.7);
        baseProblem.maximizeMESH(precision, Paths.get(BASEPATH, "result").toString(), 0, false);
    }
}
