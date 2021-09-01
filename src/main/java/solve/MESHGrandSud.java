package solve;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import restopt.BaseProblem;
import restopt.BaseProblemTwoRegions;
import restopt.BaseProblemTwoRegionsVars;
import restopt.Data;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.BitSet;

public class MESHGrandSud {

    public static final String BASEPATH = baseproblem.Data.class.getResource("/data").getPath();

    public static void main(String[] args) throws IOException, ContradictionException {
        int precision = 3;
        String forest = Paths.get(BASEPATH, "forest_binary_480x480_0.7_wgs84.tif").toString();
        String accessible = Paths.get(BASEPATH, "buffer_1000m_wgs84.tif").toString();
        String restorable = Paths.get(BASEPATH, "restorable_area_480x480_wgs84_ha.tif").toString();
//        String forest = Paths.get(BASEPATH, "forest_binary_0.7.tif").toString();
//        String accessible = Paths.get(BASEPATH, "accessible_and_forest_presence_.tif").toString();
//        String accessible = Paths.get(BASEPATH, "accessible_300m_buffer_roads.tif").toString();
//        String restorable = Paths.get(BASEPATH, "restorable.tif").toString();
//        BaseProblem baseProblem = new BaseProblem(new Data(forest, accessible, restorable), 2);
//        BaseProblemTwoRegionsVars baseProblem = new BaseProblemTwoRegionsVars(new Data(forest, accessible, restorable), 1, 2);
        BaseProblemTwoRegions baseProblem = new BaseProblemTwoRegions(new Data(forest, accessible, restorable), 1, 2);
        baseProblem.postNbComponentsConstraint(1, 1);
        baseProblem.postCompactnessConstraint(6);
//        baseProblem.postRestorableConstraint(0, 20000, 817, 0.7);
        baseProblem.postRestorableConstraint(90, 110, 200, 23, 0.7);
//        baseProblem.postRestorableConstraint(90, 110, 23, 0.7);
        baseProblem.maximizeMESH(precision, "/home/djusteau/Documents/Rapport RELIQUES 2021/result", 60, false);
    }
}
