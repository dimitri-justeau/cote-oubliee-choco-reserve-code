package solve;

import baseproblem.BaseProblemBorendy;
import baseproblem.BaseProblemUnia;
import chocoreserve.util.fragmentation.FragmentationIndices;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;


public class MaximizeMESH {

    public static void main(String[] args) throws IOException {

        long t = System.currentTimeMillis();

        int precision = 3;

        // Base problem Borendy //
        BaseProblemBorendy problemBorendy = new BaseProblemBorendy("MESH_Borendy");
        Model modelBorendy = problemBorendy.reserveModel.getChocoModel();
        IntVar MESH_Borendy = problemBorendy.reserveModel.effectiveMeshSize(problemBorendy.potentialForest, precision, true);
        double MESH_initial_Borendy = FragmentationIndices.effectiveMeshSize(problemBorendy.potentialForestGraphVar.getLB(), problemBorendy.grid.getNbCells());
        Solver solverBorendy = problemBorendy.reserveModel.getChocoModel().getSolver();
        solverBorendy.setSearch(Search.minDomUBSearch(problemBorendy.reserveModel.getSites()));

        System.out.println("MESH initial = " + MESH_initial_Borendy);

        Map<Integer, List<Solution>> borendySols = new HashMap<>();
        Map<Integer, Integer> borendyFront = new HashMap<>();

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelBorendy.arithm(problemBorendy.minReforestAreaBorendy, "=", a);
            modelBorendy.post(area);
            List<Solution> s = problemBorendy.reserveModel.findAllOptimalSolutions(MESH_Borendy,true);
            borendyFront.put(
                    s.get(0).getIntVal(problemBorendy.minReforestAreaBorendy),
                    s.get(0).getIntVal(MESH_Borendy)
            );
            borendySols.put(a, s);
            System.out.println(
                    Arrays.toString(new int[] {
                            s.get(0).getIntVal(problemBorendy.minReforestAreaBorendy),
                            s.get(0).getIntVal(MESH_Borendy)
                    })
                            + " No. sols = " + s.size()
            );
            modelBorendy.unpost(area);
            solverBorendy.reset();
        }

        System.out.println("minArea,MESH,no");
        int[] keysBorendy = borendyFront.keySet().stream().mapToInt(i -> i).sorted().toArray();
        for (int x : keysBorendy) {
            System.out.println(x + "," + borendyFront.get(x) + "," + borendySols.get(x).size());
        }

        // Unia //
        BaseProblemUnia problemUnia = new BaseProblemUnia("MESH_Unia");
        Model modelUnia = problemUnia.reserveModel.getChocoModel();
        IntVar MESH_Unia = problemUnia.reserveModel.effectiveMeshSize(problemUnia.potentialForest, precision, true);
        Solver solverUnia = problemUnia.reserveModel.getChocoModel().getSolver();
        solverUnia.setSearch(Search.minDomUBSearch(problemUnia.reserveModel.getSites()));

        Map<Integer, Integer> uniaFront = new HashMap<>();

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", a);
            modelUnia.post(area);
            Solution s = solverUnia.findOptimalSolution(MESH_Unia,true);
            uniaFront.put(s.getIntVal(problemUnia.minReforestAreaUnia), s.getIntVal(MESH_Unia));
            System.out.println(
                    Arrays.toString(new int[] {
                            s.getIntVal(problemUnia.minReforestAreaUnia),
                            s.getIntVal(MESH_Unia)
                    })
            );
            modelUnia.unpost(area);
            solverUnia.reset();
        }

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
        solver.showStatistics();
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
            for (int n = 0; n < solsB.size(); n++) {
                Solution sol = solsB.get(n);
                int[] set = sol.getSetVal(problemBorendy.reforestBorendy.getSetVar());
                for (int i : set) {
                    occurrencesInOptimalSolutionBorendy[i] += 1;
                }
                try {
                    problemBorendy.saveSolution("MESH_" + alloc[0] + "_" + alloc[1] + "_Borendy_" + n, sol);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Sols B : " + solsB.size());
            // Unia
            Constraint areaU = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", alloc[1]);
            Constraint valU = modelUnia.arithm(MESH_Unia, "=", uniaFront.get(alloc[1]));
            areaU.post();
            valU.post();
            List<Solution> solsU = solverUnia.findAllSolutions();
            modelUnia.unpost(areaU);
            modelUnia.unpost(valU);
            solverUnia.reset();
            for (int n = 0; n < solsU.size(); n++) {
                Solution sol = solsU.get(n);
                int[] set = sol.getSetVal(problemUnia.reforestUnia.getSetVar());
                for (int i : set) {
                    occurrencesInOptimalSolutionUnia[i] += 1;
                }
                try {
                    problemUnia.saveSolution("MESH_" + alloc[0] + "_" + alloc[1] + "_Unia_" + n, sol);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Count occurrences
            nbOptimalSolutions += solsB.size() * solsU.size();
            for (int j = 0; j < problemUnia.grid.getNbCells(); j++) {
                occurrencesInOptimalSolution[j] += occurrencesInOptimalSolutionBorendy[j] * solsU.size();
                occurrencesInOptimalSolution[j] += occurrencesInOptimalSolutionUnia[j] * solsB.size();
            }
            // Compute overall index value
            System.out.println(solsU.size());
            int[] totalReforested = ArrayUtils.concat(
                    problemBorendy.forest.getSetVar().getLB().toArray(),
                    ArrayUtils.concat(
                            solsB.get(0).getSetVal(problemBorendy.reforestBorendy.getSetVar()),
                            solsU.get(0).getSetVal(problemUnia.reforestUnia.getSetVar())
                    )
            );
            UndirectedGraph g = problemBorendy.reforestBorendy.getNeighborhood().getPartialGraph(
                    problemBorendy.grid,
                    problemBorendy.reserveModel.getChocoModel(),
                    totalReforested,
                    SetType.BIPARTITESET,
                    SetType.BIPARTITESET
            );
            double totalMesh = FragmentationIndices.effectiveMeshSize(g, problemBorendy.grid.getNbCells());
            int roundedTotalMesh = (int) Math.round(totalMesh * Math.pow(10, precision));
            System.out.println("TOTAL MESH = " + totalMesh + " - Rounded: " + roundedTotalMesh);
        }

        // Export occurrences count
        double[] completeOccurrences = new double[problemBorendy.grid.getNbCols() * problemBorendy.grid.getNbRows()];
        for (int j = 0; j < completeOccurrences.length; j++) {
            if (problemBorendy.grid.getDiscardSet().contains(j)) {
                completeOccurrences[j] = 0;
            } else {
                completeOccurrences[j] = 1.0 * occurrencesInOptimalSolution[problemBorendy.grid.getPartialIndex(j)] / nbOptimalSolutions;
            }
        }

        try {
            BufferedWriter br = new BufferedWriter(new FileWriter(Paths.get(problemBorendy.resultsPath, "occurrences.csv").toString()));
            StringBuilder sb = new StringBuilder();
            for (double element : completeOccurrences) {
                sb.append(element);
                sb.append(",");
            }
            br.write(sb.toString());
            br.close();
            Runtime.getRuntime().exec(
                    "python3 " +
                            baseproblem.SolutionExporter.PYTHON_SCRIPT + " " +
                            baseproblem.SolutionExporter.TEMPLATE_PATH + " " +
                            Paths.get(problemBorendy.resultsPath, "occurrences.csv").toString() + " " +
                            Paths.get(problemBorendy.resultsPath, "occurrences.tif").toString()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Nb optimal solutions = " + nbOptimalSolutions);
        System.out.println("Total time enumerate = " + (System.currentTimeMillis() - t) + " ms");
    }
}
