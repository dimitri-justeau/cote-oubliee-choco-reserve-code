package restopt;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.PartialRegularSquareGrid;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.constraints.choco.PropSmallestEnclosingCircleSpatialGraph;
import chocoreserve.solver.constraints.choco.connectivity.PropIIC;
import chocoreserve.solver.constraints.choco.fragmentation.PropEffectiveMeshSize;
import chocoreserve.util.connectivity.ConnectivityIndices;
import chocoreserve.util.fragmentation.FragmentationIndices;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.limits.TimeCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.util.graphOperations.connectivity.ConnectivityFinder;
import org.chocosolver.util.objects.graphs.GraphFactory;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.stream.IntStream;

public class BaseProblemVars extends BaseProblem {

    public Data data;
    public PartialRegularSquareGrid grid;
    public int accessibleVal;

    public Model model;
    public UndirectedGraphVar habitatGraph, restoreGraph;
    public UndirectedGraph habGraph;
    public SetVar restoreSet;
    public BoolVar[] bools;

    public int nonHabNonAcc;
    public int nCC;
    public int[] sizeCells;
    public int[] accessibleNonHabitatPixels;
    Map<Integer, Integer> accMap;

    public IntVar minRestore, maxRestorable;
    public IntVar MESH;
    public IntVar IIC;
    public IntVar nbCC;

    public BaseProblemVars() {}

    public BaseProblemVars(Data data, int accessibleVal) {

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

        int[] nonHabitatNonAccessiblePixels = IntStream.range(0, data.habitat_binary_data.length)
                .filter(i -> data.habitat_binary_data[i] == 0 && data.accessible_areas_data[i] != accessibleVal)
                .toArray();

        nonHabNonAcc = nonHabitatNonAccessiblePixels.length;

        this.grid = new PartialRegularSquareGrid(data.height, data.width, ArrayUtils.concat(outPixels, nonHabitatNonAccessiblePixels));

        int[] nonHabitatPixels = IntStream.range(0, data.habitat_binary_data.length)
                .filter(i -> data.habitat_binary_data[i] == 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] habitatPixels = IntStream.range(0, data.habitat_binary_data.length)
                .filter(i -> data.habitat_binary_data[i] == 1)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        accessibleNonHabitatPixels = IntStream.range(0, data.accessible_areas_data.length)
                .filter(i -> data.accessible_areas_data[i] == accessibleVal && data.habitat_binary_data[i] == 0)
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

        model = new Model();

        // Find existing CCs
        habGraph = Neighborhoods.PARTIAL_FOUR_CONNECTED.getPartialGraph(grid, model, habitatPixels, SetType.BIPARTITESET, SetType.BIPARTITESET);
        ConnectivityFinder cFinder = new ConnectivityFinder(habGraph);
        cFinder.findAllCC();
        nCC = cFinder.getNBCC();
        ISet ccs[] = new ISet[nCC];
        for (int cc = 0 ; cc < nCC; cc++) {
            ccs[cc] = SetFactory.makeBitSet(0);
            int i = cFinder.getCCFirstNode()[cc];
            while (i != -1) {
                ccs[cc].add(i);
                i = cFinder.getCCNextNode()[i];
            }
        }

        accMap = new HashMap<>();
        for (int i = 0; i < accessibleNonHabitatPixels.length; i++) {
            accMap.put(accessibleNonHabitatPixels[i], i);
        }

        sizeCells = new int[accessibleNonHabitatPixels.length + nCC];
        for (int i = 0; i < nCC; i++) {
            sizeCells[i] = cFinder.getSizeCC()[i];
        }
        for (int i = nCC; i < sizeCells.length; i++) {
            sizeCells[i] = 1;
        }

        UndirectedGraph hab_LB = GraphFactory.makeStoredUndirectedGraph(model, accessibleNonHabitatPixels.length + nCC, SetType.BIPARTITESET, SetType.BIPARTITESET);
        for (int i = 0; i < nCC; i++) {
            hab_LB.addNode(i);
        }

        UndirectedGraph hab_UB = GraphFactory.makeStoredUndirectedGraph(model, accessibleNonHabitatPixels.length + nCC, SetType.BIPARTITESET, SetType.BIPARTITESET);
        for (int i = 0; i < nCC; i++) {
            hab_UB.addNode(i);
        }
        for (int i = 0; i < accessibleNonHabitatPixels.length; i++) {
            hab_UB.addNode(i + nCC);
        }
        for (int i = 0; i < accessibleNonHabitatPixels.length; i++) {
            for (int j : Neighborhoods.PARTIAL_FOUR_CONNECTED.getNeighbors(grid, accessibleNonHabitatPixels[i])) {
                if (habGraph.getNodes().contains(j)) {
                    for (int cc = 0; cc < nCC; cc++) {
                        if (ccs[cc].contains(j)) {
                            hab_UB.addEdge(i + nCC, cc);
                            break;
                        }
                    }
                } else {
                    int node = accMap.get(j);
                    hab_UB.addEdge(i + nCC, node + nCC);
                }
            }
        }

        habitatGraph = model.nodeInducedGraphVar(
                "habitatGraph",
                hab_LB,
                hab_UB
        );

        restoreSet = model.setVar(new int[] {}, IntStream.range(nCC, accessibleNonHabitatPixels.length + nCC).toArray());
        SetVar habitatNodeSet = model.setVar(habitatGraph.getMandatoryNodes().toArray(), habitatGraph.getPotentialNodes().toArray());
        model.nodesChanneling(habitatGraph, habitatNodeSet).post();
        model.intersection(new SetVar[] {habitatNodeSet, model.setVar(IntStream.range(nCC, accessibleNonHabitatPixels.length + nCC).toArray())}, restoreSet).post();
        UndirectedGraph hab_UB2 = GraphFactory.makeStoredUndirectedGraph(model, accessibleNonHabitatPixels.length + nCC, SetType.BIPARTITESET, SetType.BIPARTITESET);
        for (int i = 0; i < accessibleNonHabitatPixels.length; i++) {
            hab_UB2.addNode(i + nCC);
        }
        for (int i = 0; i < accessibleNonHabitatPixels.length; i++) {
            for (int j : Neighborhoods.PARTIAL_FOUR_CONNECTED.getNeighbors(grid, accessibleNonHabitatPixels[i])) {
                if (!habGraph.getNodes().contains(j)) {
                    int node = accMap.get(j);
                    hab_UB2.addEdge(i + nCC, node + nCC);
                }
            }
        }
        restoreGraph = model.nodeInducedGraphVar(
                "restoreGraph",
                GraphFactory.makeStoredUndirectedGraph(model, accessibleNonHabitatPixels.length + nCC, SetType.BIPARTITESET, SetType.BIPARTITESET),
                hab_UB2
        );
        model.nodesChanneling(restoreGraph, restoreSet).post();
    }

    public void postNbComponentsConstraint(int minNbCC, int maxNbCC) {
        model.connected(restoreGraph).post();
    }

    public void postCompactnessConstraint(double maxDiameter) {

        double[][] coords = new double[accessibleNonHabitatPixels.length + nCC][];
        for (int i = 0; i < accessibleNonHabitatPixels.length; i++) {
            coords[i + nCC] = grid.getCartesianCoordinates()[accessibleNonHabitatPixels[i]];
        }

        Constraint cons = new Constraint("maxDiam", new PropSmallestEnclosingCircleSpatialGraph(
                restoreGraph,
//                grid.getCartesianCoordinates(),
                coords,
                model.realVar("radius", 0, 0.5 * maxDiameter, 1e-5),
                model.realVar(
                        Arrays.stream(grid.getCartesianCoordinates())
                                .mapToDouble(c -> c[0]).min().getAsDouble(),
                        Arrays.stream(grid.getCartesianCoordinates())
                                .mapToDouble(c -> c[0]).max().getAsDouble(),
                        1e-5
                ),
                model.realVar(
                        Arrays.stream(grid.getCartesianCoordinates())
                                .mapToDouble(c -> c[0]).min().getAsDouble(),
                        Arrays.stream(grid.getCartesianCoordinates())
                                .mapToDouble(c -> c[0]).max().getAsDouble(),
                        1e-5
                )
        ));
        model.post(cons);
//        reserveModel.maxDiameterSpatial(restore, maxDiameter).post();
    }

    public void initMesh(int precision) {
        MESH = model.intVar(
                "MESH",
                0, (int) ((grid.getNbCells() + nonHabNonAcc) * Math.pow(10, precision))
        );
        Constraint meshCons = new Constraint(
                "MESH_constraint",
                new PropEffectiveMeshSize(
                        habitatGraph,
                        MESH,
                        sizeCells,
                        (grid.getNbCells() + nonHabNonAcc),
                        precision,
                        true
                )
        );
        model.post(meshCons);
    }
    public void maximizeMESH(int precision, String outputPath, int timeLimit, boolean lns) throws IOException, ContradictionException {
        initMesh(precision);
        double MESH_initial = FragmentationIndices.effectiveMeshSize(
                habGraph,
                (grid.getNbCells() + nonHabNonAcc)
        );
        System.out.println("\nMESH initial = " + MESH_initial + "\n");
        Solver solver = model.getSolver();
        solver.showShortStatistics();
        solver.setSearch(Search.setVarSearch(restoreSet));
        if (lns) {
            if (timeLimit == 0) {
                throw new InputMismatchException("LNS cannot be used without a time limit, as it breaks completeness " +
                        "and is not guaranteed to terminate without a limit.");
            }
//            solver.setLNS(INeighborFactory.random(reserveModel.getSites()));
        }
        long t = System.currentTimeMillis();
        Solution solution;
        if (timeLimit > 0) {
            TimeCounter timeCounter = new TimeCounter(model, (long) (timeLimit * 1e9));
            solution = solver.findOptimalSolution(MESH, true, timeCounter);
        } else {
            solution = solver.findAllOptimalSolutions(MESH, true).get(0);
//            solution = solver.findSolution();
        }
        String[][] solCharacteristics = new String[][]{
                {"Minimum area to restore", "Maximum restorable area", "no. planning units", "initial MESH value", "optimal MESH value", "solving time (ms)"},
                {
                    String.valueOf(solution.getIntVal(minRestore)),
                    String.valueOf(solution.getIntVal(maxRestorable)),
                    String.valueOf(solution.getSetVal(restoreSet).length),
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

    public void maximizeIIC(int precision, String outputPath, int timeLimit, boolean lns) throws IOException, ContradictionException {
        IIC = model.intVar(
                "IIC",
                0, (int) (Math.pow(10, precision))
        );
        Constraint consIIC = new Constraint(
                "IIC_constraint",
                new PropIIC(
                        habitatGraph,
                        IIC,
                        (RegularSquareGrid) grid,
                        grid.getNbCells(),
                        Neighborhoods.PARTIAL_TWO_WIDE_FOUR_CONNECTED,
                        precision,
                        true
                )
        );
        model.post(consIIC);
        double IIC_initial = ConnectivityIndices.getIIC(
                habitatGraph.getLB(),
                grid,
                Neighborhoods.PARTIAL_TWO_WIDE_FOUR_CONNECTED
        );
        System.out.println("\nIIC initial = " + IIC_initial + "\n");
        Solver solver = model.getSolver();
        solver.showShortStatistics();
//        solver.setSearch(Search.minDomUBSearch(reserveModel.getSites()));
        if (lns) {
            if (timeLimit == 0) {
                throw new InputMismatchException("LNS cannot be used without a time limit, as it breaks completeness " +
                        "and is not guaranteed to terminate without a limit.");
            }
//            solver.setLNS(INeighborFactory.random(reserveModel.getSites()));
        }
        long t = System.currentTimeMillis();
        Solution solution;
        if (timeLimit > 0) {
            TimeCounter timeCounter = new TimeCounter(model, (long) (timeLimit * 1e9));
            solution = solver.findOptimalSolution(IIC, true, timeCounter);
        } else {
             solution = solver.findOptimalSolution(IIC, true);
        }
        String[][] solCharacteristics = new String[][]{
                {"Minimum area to restore", "Maximum restorable area", "no. planning units", "initial IIC value", "optimal IIC value", "solving time (ms)"},
                {
                        String.valueOf(solution.getIntVal(minRestore)),
                        String.valueOf(solution.getIntVal(maxRestorable)),
                        String.valueOf(solution.getSetVal(restoreSet).length),
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

    public void postRestorableConstraint(int minAreaToRestore, int maxAreaToRestore, int totalMax, int cellArea, double minProportion) {
        // Minimum area to ensure every site to >= proportion
        assert minProportion >= 0 && minProportion <= 1;

        int[] minArea = new int[accessibleNonHabitatPixels.length + nCC];
        int[] maxRestorableArea = new int[accessibleNonHabitatPixels.length + nCC];
        int threshold = (int) Math.ceil(cellArea - cellArea * minProportion);
        for (int i = 0; i < accessibleNonHabitatPixels.length; i++) {
            maxRestorableArea[i + nCC] = data.restorable_area_data[grid.getCompleteIndex(accessibleNonHabitatPixels[i])];
            int restorable = data.restorable_area_data[grid.getCompleteIndex(accessibleNonHabitatPixels[i])];
            minArea[i + nCC] = restorable <= threshold ? 0 : restorable - threshold;
        }

        minRestore = model.intVar(minAreaToRestore, maxAreaToRestore);
        maxRestorable = model.intVar(0, maxAreaToRestore * cellArea);
        model.sumElements(restoreSet, minArea, minRestore).post();
        model.sumElements(restoreSet, maxRestorableArea, maxRestorable).post();
    }

    public void exportSolution(String exportPath, Solution solution, String[][] characteristics) throws IOException, ContradictionException {
        solution.restore();
        int[] sites = new int[grid.getNbCells()];
        for (int i = 0; i < grid.getNbCells(); i++) {
            if (accMap.keySet().contains(i)) {
                int j = accMap.get(i) + nCC;
                if (restoreSet.getValue().contains(j)) {
                    sites[i] = 2;
                }
            } else if (habGraph.getNodes().contains(i)) {
                sites[i] = 1;
            } else {
                sites[i] = 0;
            }
        }
//          Arrays.stream(reserveModel.getSites()).mapToInt(v -> solution.getIntVal(v)).toArray();
        System.out.println(restoreSet.getValue());
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
