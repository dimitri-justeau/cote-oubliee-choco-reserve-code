package restopt;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.PartialRegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.region.ComposedRegion;
import chocoreserve.solver.region.Region;
import chocoreserve.util.connectivity.ConnectivityIndices;
import chocoreserve.util.fragmentation.FragmentationIndices;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.loop.lns.INeighborFactory;
import org.chocosolver.solver.search.loop.lns.neighbors.INeighbor;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.stream.IntStream;

public class BaseProblem {

    public Data data;
    public PartialRegularSquareGrid grid;
    public Region habitat, nonHabitat, restore;
    public ComposedRegion potentialHabitat;
    public ReserveModel reserveModel;

    public IntVar minRestore;
    public IntVar MESH;
    public IntVar IIC;

    public BaseProblem(Data data) {

        this.data = data;

        // ------------------ //
        // PREPARE INPUT DATA //
        // ------------------ //

        System.out.println("Height = " + data.height);
        System.out.println("Width = " + data.width);

        int[] outPixels = IntStream.range(0, data.habitat_binary_data.length)
                .filter(i -> data.habitat_binary_data[i] <= -1)
                .toArray();

        this.grid = new PartialRegularSquareGrid(data.height, data.width, outPixels);

        int[] nonHabitatPixels = IntStream.range(0, data.habitat_binary_data.length)
                .filter(i -> data.habitat_binary_data[i] == 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] habitatPixels = IntStream.range(0, data.habitat_binary_data.length)
                .filter(i -> data.habitat_binary_data[i] == 1)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] accessibleNonHabitatPixels = IntStream.range(0, data.accessible_areas_data.length)
                .filter(i -> data.accessible_areas_data[i] == 2 && data.habitat_binary_data[i] == 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] nonHabitatNonAccessiblePixels = IntStream.range(0, data.habitat_binary_data.length)
                .filter(i -> data.habitat_binary_data[i] == 0 && data.accessible_areas_data[i] <= 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        System.out.println("Current landscape state loaded");
        System.out.println("    Habitat cells = " + habitatPixels.length + " ");
        System.out.println("    Non habitat cells = " + nonHabitatPixels.length + " ");
        System.out.println("    Accessible non habitat cells = " + accessibleNonHabitatPixels.length + " ");
        System.out.println("    Out cells = " + outPixels.length);

        // ------------------ //
        // INITIALIZE PROBLEM //
        // ------------------ //

        habitat = new Region(
                "habitat",
                Neighborhoods.PARTIAL_FOUR_CONNECTED,
                SetType.BIPARTITESET,
                habitatPixels,
                habitatPixels
        );
        nonHabitat = new Region(
                "nonForest",
                Neighborhoods.PARTIAL_FOUR_CONNECTED,
                SetType.BIPARTITESET,
                nonHabitatNonAccessiblePixels,
                nonHabitatPixels
        );

        restore = new Region(
                "reforest",
                Neighborhoods.PARTIAL_FOUR_CONNECTED,
                SetType.BIPARTITESET,
                new int[] {},
                accessibleNonHabitatPixels
        );

        potentialHabitat = new ComposedRegion(
                "potentialHabitat",
                SetType.BIPARTITESET,
                habitat,
                restore
        );

        this.reserveModel = new ReserveModel(
                grid,
                new Region[] {habitat, nonHabitat, restore},
                new ComposedRegion[] {potentialHabitat}
        );
    }

    public void postNbComponentsConstraint(int minNbCC, int maxNbCC) {
        reserveModel.nbConnectedComponents(restore, minNbCC, maxNbCC).post();
    }

    public void postCompactnessConstraint(double maxDiameter) {
        reserveModel.maxDiameterSpatial(restore, maxDiameter).post();
    }

    public void maximizeMESH(int precision) {
        MESH = reserveModel.effectiveMeshSize(potentialHabitat, precision, true);
        double MESH_initial = FragmentationIndices.effectiveMeshSize(
                potentialHabitat.getSetVar().getGLB(),
                grid.getNbCells()
        );
        System.out.println("MESH initial = " + MESH_initial);
        Solver solver = reserveModel.getChocoSolver();
        solver.showStatistics();
        solver.setSearch(Search.minDomUBSearch(reserveModel.getSites()));
        solver.findOptimalSolution(MESH, true);
    }

    public void maximizeIIC(int precision) {
        IIC = reserveModel.integralIndexOfConnectivity(
                potentialHabitat,
                Neighborhoods.PARTIAL_TWO_WIDE_FOUR_CONNECTED,
                precision,
                true
        );
        double IIC_initial = ConnectivityIndices.getIIC(
                potentialHabitat.getSetVar().getGLB(),
                grid,
                Neighborhoods.PARTIAL_TWO_WIDE_FOUR_CONNECTED
        );
        System.out.println("IIC initial = " + IIC_initial);
        Solver solver = reserveModel.getChocoSolver();
        solver.showStatistics();
        solver.setSearch(Search.minDomUBSearch(reserveModel.getSites()));
        solver.findOptimalSolution(IIC, true);
    }

    public void postRestorableConstraint(int minAreaToRestore, int maxAreaToRestore, int cellArea, double minProportion) {
        // Minimum area to ensure every site to >= proportion
        assert minProportion >= 0 && minProportion <= 1;
        int[] minArea = new int[grid.getNbCells()];
        int threshold = (int) Math.ceil(cellArea - cellArea * minProportion);
        for (int i = 0; i < grid.getNbCells(); i++) {
            int restorable = data.restorable_area_data[grid.getCompleteIndex(i)];
            minArea[i] = restorable <= threshold ? 0 : restorable - 7;

        }
        // Post restorable area constraint
        minRestore = reserveModel.getChocoModel().intVar(minAreaToRestore, maxAreaToRestore);
        reserveModel.getChocoModel().sumElements(restore.getSetVar(), minArea, minRestore).post();
    }

    public void exportSolutionRaster(String exportPath, int[] solution) throws IOException {
        BufferedWriter br = null;
        SolutionExporter exp = new SolutionExporter(this, solution, exportPath);
        exp.generateRaster(exportPath);
    }

    public void exportSolutionRaster(String exportPath, Solution sol) throws IOException {
        int[] solution = IntStream.range(0, reserveModel.getSites().length)
                .map(j -> sol.getIntVal(reserveModel.getSites()[j]))
                .toArray();
        exportSolutionRaster(exportPath, solution);
    }
}
