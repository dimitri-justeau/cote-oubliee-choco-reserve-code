package restopt;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        Data data = new Data(
                "/home/djusteau/testRestopt/habitat.tif",
                "/home/djusteau/testRestopt/accessible.tif",
                "/home/djusteau/testRestopt/restorable.tif"
        );

        BaseProblem baseProblem = new BaseProblem(data);
        baseProblem.postNbComponentsConstraint(1, 1);
        baseProblem.postCompactnessConstraint(6);
        baseProblem.postRestorableConstraint(90, 90, 23, 0.7);
//        baseProblem.maximizeMESH(3);
        baseProblem.maximizeIIC(6);
    }
}
