package solve;

import baseproblem.BaseProblem;
import baseproblem.Data;
import chocoreserve.grid.regular.square.PartialRegularSquareGrid;
import chocoreserve.solver.constraints.choco.graph.PropPerimeterSquareGridFourConnected;
import chocoreserve.solver.constraints.choco.graph.spatial.PropIICSpatialGraph;
import chocoreserve.solver.constraints.spatial.NbEdges;
import chocoreserve.solver.constraints.spatial.PerimeterSquareGridFourConnected;
import chocoreserve.solver.feature.raster.ProbabilisticRasterFeature;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

public class BestTradeoffs {

    public static String B1_PATH = Paths.get(Data.BASEPATH, "optimal_areas_freq/B1.tif").toString();
    public static String B2_PATH = Paths.get(Data.BASEPATH, "optimal_areas_freq/B2.tif").toString();
    public static String U1_PATH = Paths.get(Data.BASEPATH, "optimal_areas_freq/U1.tif").toString();
    public static String U2_PATH = Paths.get(Data.BASEPATH, "optimal_areas_freq/U2.tif").toString();

    public static void main(String[] args) throws IOException {

        Data data = new Data();

        int[] outPixels = IntStream.range(0, data.forest_binary_data.length)
                .filter(i -> data.forest_binary_data[i] <= -1)
                .toArray();

        PartialRegularSquareGrid grid = new PartialRegularSquareGrid(data.height, data.width, outPixels);

        double[] b1_data = new ProbabilisticRasterFeature(B1_PATH).getProbabilisticData();
        double[] b2_data = new ProbabilisticRasterFeature(B2_PATH).getProbabilisticData();
        double[] u1_data = new ProbabilisticRasterFeature(U1_PATH).getProbabilisticData();
        double[] u2_data = new ProbabilisticRasterFeature(U2_PATH).getProbabilisticData();

        int[] b1_sites = IntStream.range(0, b1_data.length)
                .filter(i -> b1_data[i] > 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] b2_sites = IntStream.range(0, b2_data.length)
                .filter(i -> b2_data[i] > 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] u1_sites = IntStream.range(0, u1_data.length)
                .filter(i -> u1_data[i] > 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] u2_sites = IntStream.range(0, u2_data.length)
                .filter(i -> u2_data[i] > 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        BaseProblem bp = new BaseProblem("B2+U1");
        Model model = bp.reserveModel.getChocoModel();

        int nbSites = bp.reserveModel.getSites().length;
        int[] minArea = new int[nbSites];
        int[] maxRestorableArea = new int[nbSites];
        for (int i = 0; i < nbSites; i++) {
            int restorable = data.restorable_area_data[grid.getCompleteIndex(i)];
            minArea[i] = restorable <= 7 ? 0 : restorable - 7;
            maxRestorableArea[i] = restorable;
        }

        model.subsetEq(bp.reforestBorendy.getSetVar(), model.setVar(b2_sites)).post();
        model.subsetEq(bp.reforestUnia.getSetVar(), model.setVar(u1_sites)).post();

        // NC
        IntVar nbCC = bp.potentialForest.getNbCC();

        // L
        int nbEdgesForest = 0;
        ISet env = bp.forest.getSetVar().getPotentialNodes();
        for (int i : env) {
            nbEdgesForest += bp.forest.getSetVar().getMandNeighOf(i).size();
        }
        nbEdgesForest /= 2;

        NbEdges cNbEdge = new NbEdges(bp.reserveModel, bp.potentialForest);
        IntVar nbEdges = cNbEdge.nbEdges;
        cNbEdge.post();

        // PA
        int forestArea = bp.forest.getSetVar().getUB().size();
        PropPerimeterSquareGridFourConnected per = new PropPerimeterSquareGridFourConnected(
                bp.forest.getSetVar(),
                model.intVar(0)
        );
        int forestPerimeter = per.getPerimeter(bp.forest.getSetVar().getLB());
        IntVar area = bp.potentialForest.getNbSites();

        PerimeterSquareGridFourConnected cPerB = new PerimeterSquareGridFourConnected(
                bp.reserveModel, bp.potentialForest
        );
        cPerB.post();

        IntVar perimeterBorendy = cPerB.perimeter;

        IntVar PA = model.intVar("ratioPerimeterArea", 0, (int) (1000 * forestPerimeter / forestArea));
        model.div(model.intScaleView(perimeterBorendy, 1000), area, PA).post();

        // IIC
        IntVar iic = model.intVar(0, 10000);
        PropIICSpatialGraph propIIC = new PropIICSpatialGraph(bp.potentialForestGraphVar, iic, 4);
        model.post(new Constraint("IIC", propIIC));

        Solver solver = model.getSolver();
        solver.showStatistics();

        solver.setSearch(Search.minDomUBSearch(bp.reserveModel.getSites()));

        solver.plugMonitor((IMonitorSolution) () -> {
            System.out.println("L = " + nbEdges.getValue());
            System.out.println("NC = " + nbCC.getValue());
            System.out.println("PA = " + PA.getValue());
            System.out.println("IIC = " + iic.getValue());
        });

        List<Solution> pareto = solver.findParetoFront(new IntVar[] {nbEdges, model.intMinusView(nbCC), model.intMinusView(PA), iic}, true);

        for (Solution sol : pareto) {
            System.out.println("L,NC,PA,IIC");
            System.out.println(sol.getIntVal(nbEdges) + "," + sol.getIntVal(nbCC) + "," + sol.getIntVal(PA) + "," + sol.getIntVal(iic));
        }
    }
}
