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
import org.chocosolver.solver.search.limits.TimeCounter;
import org.chocosolver.solver.search.loop.lns.INeighborFactory;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.io.IOException;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.stream.IntStream;

public class BaseProblem {

    public Data data;
    public PartialRegularSquareGrid grid;
    public Region habitat, nonHabitat, restore;
    public ComposedRegion potentialHabitat;
    public ReserveModel reserveModel;
    public int accessibleVal;

    public IntVar minRestore, maxRestorable;
    public IntVar MESH;
    public IntVar IIC;

    public BaseProblem(Data data, int accessibleVal) {

        this.data = data;
        this.accessibleVal = accessibleVal;
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
                .filter(i -> data.accessible_areas_data[i] == accessibleVal && data.habitat_binary_data[i] == 0)
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
                new Region[] {nonHabitat, habitat, restore},
                new ComposedRegion[] {potentialHabitat}
        );
    }

    public void postNbComponentsConstraint(int minNbCC, int maxNbCC) {
        reserveModel.nbConnectedComponents(restore, minNbCC, maxNbCC).post();
    }

    public void postCompactnessConstraint(double maxDiameter) {
        reserveModel.maxDiameterSpatial(restore, maxDiameter).post();
    }

    public void maximizeMESH(int precision, String outputPath, int timeLimit, boolean lns) throws IOException {
        MESH = reserveModel.effectiveMeshSize(potentialHabitat, precision, true);
        double MESH_initial = FragmentationIndices.effectiveMeshSize(
                potentialHabitat.getSetVar().getGLB(),
                grid.getNbCells()
        );
        System.out.println("\nMESH initial = " + MESH_initial + "\n");
        Solver solver = reserveModel.getChocoSolver();
        solver.showShortStatistics();
        solver.setSearch(Search.minDomUBSearch(reserveModel.getSites()));
        if (lns) {
            if (timeLimit == 0) {
                throw new InputMismatchException("LNS cannot be used without a time limit, as it breaks completeness " +
                        "and is not guaranteed to terminate without a limit.");
            }
            solver.setLNS(INeighborFactory.random(reserveModel.getSites()));
        }
        long t = System.currentTimeMillis();
        Solution solution;
        if (timeLimit > 0) {
            TimeCounter timeCounter = new TimeCounter(reserveModel.getChocoModel(), (long) (timeLimit * 1e9));
            solution = solver.findOptimalSolution(MESH, true, timeCounter);
        } else {
            solution = solver.findOptimalSolution(MESH, true);
        }
        String[][] solCharacteristics = new String[][]{
                {"Minimum area to restore", "Maximum restorable area", "no. planning units", "initial MESH value", "optimal MESH value", "solving time (ms)"},
                {
                    String.valueOf(solution.getIntVal(minRestore)),
                    String.valueOf(solution.getIntVal(maxRestorable)),
                    String.valueOf(solution.getSetVal(restore.getSetVar()).length),
                    String.valueOf(1.0 * Math.round(MESH_initial * Math.pow(10, precision)) / Math.pow(10, precision)),
                    String.valueOf((1.0 * solution.getIntVal(MESH)) / Math.pow(10, precision)),
                    String.valueOf((System.currentTimeMillis() - t))
                }
        };
        System.out.println("\n--- Best solution ---\n");
        System.out.println("Minimum area to restore : " + solCharacteristics[1][0]);
        System.out.println("Maximum restorable area : " + solCharacteristics[1][1]);
        System.out.println("No. planning units : " + solCharacteristics[1][2]);
        System.out.println("Initial MESH value : " + solCharacteristics[1][3]);
        System.out.println("Optimal MESH value : " + solCharacteristics[1][4]);
        System.out.println("Solving time (ms) : " + solCharacteristics[1][5]);
        System.out.println("\nRaster exported at " + outputPath + ".tif");
        System.out.println("Solution characteristics exported at " + outputPath + ".csv");
        exportSolution(outputPath, solution, solCharacteristics);
    }

    public void maximizeIIC(int precision, String outputPath, int timeLimit, boolean lns) throws IOException {
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
        System.out.println("\nIIC initial = " + IIC_initial + "\n");
        Solver solver = reserveModel.getChocoSolver();
        solver.showShortStatistics();
        solver.setSearch(Search.minDomUBSearch(reserveModel.getSites()));
        if (lns) {
            if (timeLimit == 0) {
                throw new InputMismatchException("LNS cannot be used without a time limit, as it breaks completeness " +
                        "and is not guaranteed to terminate without a limit.");
            }
            solver.setLNS(INeighborFactory.random(reserveModel.getSites()));
        }
        long t = System.currentTimeMillis();
        Solution solution;
        if (timeLimit > 0) {
            TimeCounter timeCounter = new TimeCounter(reserveModel.getChocoModel(), (long) (timeLimit * 1e9));
            solution = solver.findOptimalSolution(IIC, true, timeCounter);
        } else {
             solution = solver.findOptimalSolution(IIC, true);
        }
        String[][] solCharacteristics = new String[][]{
                {"Minimum area to restore", "Maximum restorable area", "no. planning units", "initial IIC value", "optimal IIC value", "solving time (ms)"},
                {
                        String.valueOf(solution.getIntVal(minRestore)),
                        String.valueOf(solution.getIntVal(maxRestorable)),
                        String.valueOf(solution.getSetVal(restore.getSetVar()).length),
                        String.valueOf(1.0 * Math.round(IIC_initial * Math.pow(10, precision)) / Math.pow(10, precision)),
                        String.valueOf((1.0 * solution.getIntVal(IIC)) / Math.pow(10, precision)),
                        String.valueOf((System.currentTimeMillis() - t))
                }
        };
        System.out.println("\n--- Best solution ---\n");
        System.out.println("Minimum area to restore : " + solCharacteristics[1][0]);
        System.out.println("Maximum restorable area : " + solCharacteristics[1][1]);
        System.out.println("No. planning units : " + solCharacteristics[1][2]);
        System.out.println("Initial IIC value : " + solCharacteristics[1][3]);
        System.out.println("Optimal IIC value : " + solCharacteristics[1][4]);
        System.out.println("Solving time (ms) : " + solCharacteristics[1][5]);
        System.out.println("\nRaster exported at " + outputPath + ".tif");
        System.out.println("Solution characteristics exported at " + outputPath + ".csv");
        exportSolution(outputPath, solution, solCharacteristics);
    }

    public void postRestorableConstraint(int minAreaToRestore, int maxAreaToRestore, int cellArea, double minProportion) {
        // Minimum area to ensure every site to >= proportion
        assert minProportion >= 0 && minProportion <= 1;
        int[] minArea = new int[grid.getNbCells()];
        int[] maxRestorableArea = new int[grid.getNbCells()];
        int threshold = (int) Math.ceil(cellArea - cellArea * minProportion);
        for (int i = 0; i < grid.getNbCells(); i++) {
            maxRestorableArea[i] = data.restorable_area_data[grid.getCompleteIndex(i)];
            int restorable = data.restorable_area_data[grid.getCompleteIndex(i)];
            minArea[i] = restorable <= threshold ? 0 : restorable - threshold;

        }
        // Post restorable area constraint
        minRestore = reserveModel.getChocoModel().intVar(minAreaToRestore, maxAreaToRestore);
        maxRestorable = reserveModel.getChocoModel().intVar(0, maxAreaToRestore * cellArea);
        reserveModel.getChocoModel().sumElements(restore.getSetVar(), minArea, minRestore).post();
        reserveModel.getChocoModel().sumElements(restore.getSetVar(), maxRestorableArea, maxRestorable).post();
    }

    public void exportSolution(String exportPath, Solution solution, String[][] characteristics) throws IOException {
        int[] sites = Arrays.stream(reserveModel.getSites()).mapToInt(v -> solution.getIntVal(v)).toArray();
        SolutionExporter exporter = new SolutionExporter(
                this,
                sites,
                data.habitatBinaryRasterPath,
                exportPath + ".cvs",
                exportPath + ".tif"
        );
        exporter.exportCharacteristics(characteristics);
        exporter.generateRaster();
    }
}
