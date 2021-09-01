package solve;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.util.fragmentation.FragmentationIndices;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;
import restopt.BaseProblemVars;
import restopt.BaseProblemViews;
import restopt.Data;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;


public class MaximizeMESH_Vars {

    public static final String BASEPATH = baseproblem.Data.class.getResource("/data").getPath();

    public static void main(String[] args) throws IOException {

        long t = System.currentTimeMillis();

        int precision = 3;

        // Base problem Borendy //
        String forest = Paths.get(BASEPATH, "forest_binary_480x480_0.7_wgs84.tif").toString();
        String accessible = Paths.get(BASEPATH, "buffer_1000m_wgs84.tif").toString();
        String restorable = Paths.get(BASEPATH, "restorable_area_480x480_wgs84_ha.tif").toString();
        BaseProblemVars problemBorendy = new BaseProblemVars(new Data(forest, accessible, restorable), 1);
        problemBorendy.postNbComponentsConstraint(1, 1);
        problemBorendy.postCompactnessConstraint(6);
        problemBorendy.postRestorableConstraint(90, 110, 200, 23, 0.7);
        problemBorendy.initMesh(precision);

        Model modelBorendy = problemBorendy.model;

        Map<Integer, List<Solution>> borendySols = new HashMap<>();
        Map<Integer, Integer> borendyFront = new HashMap<>();

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelBorendy.arithm(problemBorendy.minRestore, "=", a);
            modelBorendy.post(area);
            List<Solution> s = modelBorendy.getSolver().findAllOptimalSolutions(problemBorendy.MESH, true);
            borendyFront.put(
                    s.get(0).getIntVal(problemBorendy.minRestore),
                    s.get(0).getIntVal(problemBorendy.MESH)
            );
            borendySols.put(a, s);
            System.out.println(
                    Arrays.toString(new int[] {
                            s.get(0).getIntVal(problemBorendy.minRestore),
                            s.get(0).getIntVal(problemBorendy.MESH)
                    })
                            + " No. sols = " + s.size()
            );
            modelBorendy.unpost(area);
            modelBorendy.getSolver().reset();
        }

        System.out.println("minArea,MESH,no");
        int[] keysBorendy = borendyFront.keySet().stream().mapToInt(i -> i).sorted().toArray();
        for (int x : keysBorendy) {
            System.out.println(x + "," + borendyFront.get(x) + "," + borendySols.get(x).size());
        }

        // Unia //
        BaseProblemVars problemUnia = new BaseProblemVars(new Data(forest, accessible, restorable), 2);
        problemUnia.postNbComponentsConstraint(1, 1);
        problemUnia.postCompactnessConstraint(6);
        problemUnia.postRestorableConstraint(90, 110, 200, 23, 0.7);
        problemUnia.initMesh(precision);

        Model modelUnia = problemUnia.model;

        Map<Integer, List<Solution>> uniaSols = new HashMap<>();
        Map<Integer, Integer> uniaFront = new HashMap<>();

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelUnia.arithm(problemUnia.minRestore, "=", a);
            modelUnia.post(area);
            List<Solution> s = modelUnia.getSolver().findAllOptimalSolutions(problemUnia.MESH, true);
            uniaFront.put(s.get(0).getIntVal(problemUnia.minRestore), s.get(0).getIntVal(problemUnia.MESH));
            borendySols.put(a, s);
            System.out.println(
                    Arrays.toString(new int[] {
                            s.get(0).getIntVal(problemUnia.minRestore),
                            s.get(0).getIntVal(problemUnia.MESH)
                    })
            );
            modelUnia.unpost(area);
            modelUnia.getSolver().reset();
        }
//        uniaFront.put(90,1089607);
//        uniaFront.put(91,1090424);
//        uniaFront.put(92,1089607);
//        uniaFront.put(93,1090426);
//        uniaFront.put(94,1090424);
//        uniaFront.put(95,1089607);
//        uniaFront.put(96,1090426);
//        uniaFront.put(97,1090424);
//        uniaFront.put(98,1090426);
//        uniaFront.put(99,1090424);
//        uniaFront.put(100,1090424);
//        uniaFront.put(101,1091244);
//        uniaFront.put(102,1090426);
//        uniaFront.put(103,1090426);
//        uniaFront.put(104,1090426);
//        uniaFront.put(105,1091244);
//        uniaFront.put(106,1091245);
//        uniaFront.put(107,1091244);
//        uniaFront.put(108,1091244);
//        uniaFront.put(109,1091244);
//        uniaFront.put(110,1091244);

        System.out.println("minArea,MESH");
        int[] keysUnia = uniaFront.keySet().stream().mapToInt(i -> i).sorted().toArray();
        for (int x : keysUnia) {
            System.out.println(x + "," + uniaFront.get(x));
        }

        // Combine //
        Model model = new Model();

        IntVar indexBorendy = model.intVar("indexBorendy", 0, 20);
        IntVar indexUnia = model.intVar("indexBorendy", 0, 20);

        IntVar areaBorendy = model.intVar("areaBorendy", 90, 110);
        IntVar areaUnia = model.intVar("areaUnia", 90, 110);

        int[] areas = IntStream.range(90, 111).toArray();
        int[] valsBorendy = IntStream.range(90, 111).map(i -> borendyFront.get(i)).toArray();
        int[] valsUnia = IntStream.range(90, 111).map(i -> uniaFront.get(i)).toArray();

        IntVar valBorendy = model.intVar("MESH_Borendy",0, 2 * (int) Math.pow(10, 2 * precision));
        IntVar valUnia = model.intVar("MESH_Unia",0, 2 *(int) Math.pow(10, 2 * precision));

        model.element(areaBorendy, areas, indexBorendy).post();
        model.element(areaUnia, areas, indexUnia).post();
        model.element(valBorendy, valsBorendy, indexBorendy).post();
        model.element(valUnia, valsUnia, indexUnia).post();

        model.arithm(areaBorendy, "+", areaUnia, "<=", 200).post();

        IntVar total = model.intVar("sumMESH", 0, 3 * (int) Math.pow(10, 2 * precision));
        model.arithm(valBorendy, "+", valUnia, "=", total).post();

        List<Integer[]> optimalAllocations = new ArrayList<>();

        Solver solver = model.getSolver();
        solver.plugMonitor((IMonitorSolution) () -> {
            System.out.println("Area Borendy = " + areaBorendy.getValue());
            System.out.println("MESH Borendy = " + valBorendy.getValue());
            System.out.println("Area Unia = " + areaUnia.getValue());
            System.out.println("MESH Unia = " + valUnia.getValue());
        });
//        solver.showStatistics();
        List<Solution> allocs = solver.findAllOptimalSolutions(total, true);
        for (Solution s : allocs) {
            optimalAllocations.add(new Integer[]{s.getIntVal(areaBorendy), s.getIntVal(areaUnia)});
        }

        System.out.println("Total time optimize = " + (System.currentTimeMillis() - t) + " ms");

        // Enumerate optimal solutions

        t = System.currentTimeMillis();

        int[] occurrencesInOptimalSolutionBorendy = new int[problemBorendy.grid.getNbCells()];
        for (int j = 0; j < problemBorendy.grid.getNbCells(); j++) {
            occurrencesInOptimalSolutionBorendy[j] = 0;
        }

        int[] occurrencesInOptimalSolutionUnia = new int[problemUnia.grid.getNbCells()];
        for (int j = 0; j < problemUnia.grid.getNbCells(); j++) {
            occurrencesInOptimalSolutionUnia[j] = 0;
        }

        int[] occurrencesInOptimalSolution = new int[problemUnia.grid.getNbCells()];
        for (int j = 0; j < problemUnia.grid.getNbCells(); j++) {
            occurrencesInOptimalSolution[j] = 0;
        }

        int nbOptimalSolutions = 0;

        for (Integer[] alloc : optimalAllocations) {
            System.out.println("Alloc : " + Arrays.toString(alloc));
            // Borendy
            List<Solution> solsB = borendySols.get(alloc[0]);
            List<Solution> solsU = borendySols.get(alloc[1]);
            modelUnia.getSolver().reset();
//            for (int n = 0; n < solsB.size(); n++) {
//                Solution sol = solsB.get(n);
//                int[] set = sol.getSetVal(problemBorendy.restoreSet);
//                for (int i : set) {
//                    occurrencesInOptimalSolutionBorendy[i] += 1;
//                }
//                try {
////                    problemBorendy.saveSolution("MESH_" + alloc[0] + "_" + alloc[1] + "_Borendy_" + n, sol);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            System.out.println("Sols B : " + solsB.size());
//            // Unia
//            Constraint areaU = modelUnia.arithm(problemUnia.minRestore, "=", alloc[1]);
//            Constraint valU = modelUnia.arithm(problemUnia.MESH, ">=", uniaFront.get(alloc[1]));
//            areaU.post();
//            valU.post();
//            List<Solution> solsU = modelUnia.getSolver().findAllSolutions();
//            modelUnia.unpost(areaU);
//            modelUnia.unpost(valU);
//            for (int n = 0; n < solsU.size(); n++) {
//                Solution sol = solsU.get(n);
//                int[] set = sol.getSetVal(problemUnia.restoreSet);
//                for (int i : set) {
//                    occurrencesInOptimalSolutionUnia[i] += 1;
//                }
//                try {
//                    problemUnia.saveSolution("MESH_" + alloc[0] + "_" + alloc[1] + "_Unia_" + n, sol);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
            // Count occurrences
            nbOptimalSolutions += solsB.size() * solsU.size();
//            for (int j = 0; j < problemUnia.grid.getNbCells(); j++) {
//                occurrencesInOptimalSolution[j] += occurrencesInOptimalSolutionBorendy[j] * solsU.size();
//                occurrencesInOptimalSolution[j] += occurrencesInOptimalSolutionUnia[j] * solsB.size();
//            }
            // Compute overall index value
            System.out.println(solsU.size());
            int[] totalReforested = ArrayUtils.concat(
                    problemBorendy.habGraph.getNodes().toArray(),
                    ArrayUtils.concat(
                            solsB.get(0).getSetVal(problemBorendy.restoreSet),
                            solsU.get(0).getSetVal(problemUnia.restoreSet)
                    )
            );
            UndirectedGraph g = Neighborhoods.PARTIAL_FOUR_CONNECTED.getPartialGraph(
                    problemBorendy.grid,
                    problemBorendy.model,
                    totalReforested,
                    SetType.BIPARTITESET,
                    SetType.BIPARTITESET
            );
            double totalMesh = FragmentationIndices.effectiveMeshSize(g, problemBorendy.grid.getNbCells());
            int roundedTotalMesh = (int) Math.round(totalMesh * Math.pow(10, precision));
            System.out.println("TOTAL MESH = " + totalMesh + " - Rounded: " + roundedTotalMesh);
        }

        // Export occurrences count
//        double[] completeOccurrences = new double[problemBorendy.grid.getNbCols() * problemBorendy.grid.getNbRows()];
//        for (int j = 0; j < completeOccurrences.length; j++) {
//            if (problemBorendy.grid.getDiscardSet().contains(j)) {
//                completeOccurrences[j] = 0;
//            } else {
//                completeOccurrences[j] = 1.0 * occurrencesInOptimalSolution[problemBorendy.grid.getPartialIndex(j)] / nbOptimalSolutions;
//            }
//        }
//
//        try {
//            BufferedWriter br = new BufferedWriter(new FileWriter(Paths.get(problemBorendy.resultsPath, "occurrences.csv").toString()));
//            StringBuilder sb = new StringBuilder();
//            for (double element : completeOccurrences) {
//                sb.append(element);
//                sb.append(",");
//            }
//            br.write(sb.toString());
//            br.close();
//            Runtime.getRuntime().exec(
//                    "python3 " +
//                            baseproblem.SolutionExporter.PYTHON_SCRIPT + " " +
//                            baseproblem.SolutionExporter.TEMPLATE_PATH + " " +
//                            Paths.get(problemBorendy.resultsPath, "occurrences.csv").toString() + " " +
//                            Paths.get(problemBorendy.resultsPath, "occurrences.tif").toString()
//            );
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        System.out.println("Nb optimal solutions = " + nbOptimalSolutions);
        System.out.println("Total time enumerate = " + (System.currentTimeMillis() - t) + " ms");
    }
}
