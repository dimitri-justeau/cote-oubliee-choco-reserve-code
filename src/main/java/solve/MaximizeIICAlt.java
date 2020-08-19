package solve;

import baseproblem.BaseProblemBorendy;
import baseproblem.BaseProblemUnia;
import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.util.connectivity.ConnectivityIndices;
import chocoreserve.util.fragmentation.FragmentationIndices;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorInitialize;
import org.chocosolver.solver.search.loop.monitors.IMonitorRestart;
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


public class MaximizeIICAlt {

    public static void main(String[] args) throws IOException, ContradictionException {

        long t = System.currentTimeMillis();

        int precision = 6;

        // Borendy //
        BaseProblemBorendy problemBorendy = new BaseProblemBorendy("IIC_Borendy");
        Model modelBorendy = problemBorendy.reserveModel.getChocoModel();
        IntVar IIC_Borendy = problemBorendy.reserveModel.integralIndexOfConnectivity(
                problemBorendy.potentialForest,
                Neighborhoods.PARTIAL_TWO_WIDE_FOUR_CONNECTED,
                precision
        );
        double IIC_initial_Borendy = ConnectivityIndices.getIIC(
                problemBorendy.potentialForestGraphVar.getGLB(),
                problemBorendy.grid,
                Neighborhoods.PARTIAL_TWO_WIDE_FOUR_CONNECTED
        );

        Solver solverBorendy = problemBorendy.reserveModel.getChocoModel().getSolver();

        solverBorendy.setSearch(Search.minDomUBSearch(problemBorendy.reserveModel.getSites()));

        System.out.println("IIC initial = " + IIC_initial_Borendy);

        Map<Integer, List<Solution>> borendySols = new HashMap<>();
        Map<Integer, Integer> borendyFront = new HashMap<>();
//        borendyFront.put(90, 215132);
//        borendyFront.put(91,215132);
//        borendyFront.put(92,215132);
//        borendyFront.put(93,215132);
//        borendyFront.put(94,215132);
//        borendyFront.put(95,215225);
//        borendyFront.put(96,215225);
//        borendyFront.put(97,215225);
//        borendyFront.put(98,215132);
//        borendyFront.put(99,215225);
//        borendyFront.put(100,215225);
//        borendyFront.put(101,215225);
//        borendyFront.put(102,215225);
//        borendyFront.put(103,215225);
//        borendyFront.put(104,215225);
//        borendyFront.put(105,215225);
//        borendyFront.put(106,215316);
//        borendyFront.put(107,215316);
//        borendyFront.put(108,215316);
//        borendyFront.put(109,215225);
//        borendyFront.put(110,215225);

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelBorendy.arithm(problemBorendy.minReforestAreaBorendy, "=", a);
            modelBorendy.post(area);
            List<Solution> s = problemBorendy.reserveModel.findAllOptimalSolutions(IIC_Borendy,true);
            borendyFront.put(
                    s.get(0).getIntVal(problemBorendy.minReforestAreaBorendy),
                    s.get(0).getIntVal(IIC_Borendy)
            );
            borendySols.put(a, s);
            System.out.println(
                    Arrays.toString(new int[] {
                            s.get(0).getIntVal(problemBorendy.minReforestAreaBorendy),
                            s.get(0).getIntVal(IIC_Borendy)
                    })
                            + " No. sols = " + s.size()
            );
            modelBorendy.unpost(area);
            solverBorendy.reset();
        }

        System.out.println("minArea,IIC,no");
        int[] keysBorendy = borendyFront.keySet().stream().mapToInt(i -> i).sorted().toArray();
        for (int x : keysBorendy) {
            System.out.println(x + "," + borendyFront.get(x) + "," + borendySols.get(x).size());
        }

        // Unia //
        BaseProblemUnia problemUnia = new BaseProblemUnia("IIC_Unia");
        Model modelUnia = problemUnia.reserveModel.getChocoModel();
        IntVar IIC_Unia = problemUnia.reserveModel.integralIndexOfConnectivity(
                problemUnia.potentialForest,
                Neighborhoods.PARTIAL_TWO_WIDE_FOUR_CONNECTED,
                precision
        );
        Solver solverUnia = problemUnia.reserveModel.getChocoModel().getSolver();
        solverUnia.setSearch(Search.minDomUBSearch(problemUnia.reserveModel.getSites()));

        Map<Integer, Integer> uniaFront = new HashMap<>();
//        uniaFront.put(90,1089606);
//        uniaFront.put(91,1090424);
//        uniaFront.put(92,1089606);
//        uniaFront.put(93,1090425);
//        uniaFront.put(94,1090424);
//        uniaFront.put(95,1089606);
//        uniaFront.put(96,1090425);
//        uniaFront.put(97,1090424);
//        uniaFront.put(98,1090425);
//        uniaFront.put(99,1090424);
//        uniaFront.put(100,1089606);
//        uniaFront.put(101,1091243);
//        uniaFront.put(102,1090425);
//        uniaFront.put(103,1090424);
//        uniaFront.put(104,1090424);
//        uniaFront.put(105,1091243);
//        uniaFront.put(106,1090425);
//        uniaFront.put(107,1091243);
//        uniaFront.put(108,1091243);
//        uniaFront.put(109,1091243);
//        uniaFront.put(110,1091243);

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", a);
            modelUnia.post(area);
            Solution s = solverUnia.findOptimalSolution(IIC_Unia,true);
            uniaFront.put(s.getIntVal(problemUnia.minReforestAreaUnia), s.getIntVal(IIC_Unia));
            System.out.println(
                    Arrays.toString(new int[] {
                            s.getIntVal(problemUnia.minReforestAreaUnia),
                            s.getIntVal(IIC_Unia)
                    })
            );
            modelUnia.unpost(area);
            solverUnia.reset();
        }

        System.out.println("minArea,IIC");
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

        IntVar valBorendy = model.intVar("IIC_Borendy",0, (int) Math.pow(10, precision));
        IntVar valUnia = model.intVar("IIC_Unia",0, (int) Math.pow(10, precision));

        model.element(areaBorendy, areas, indexBorendy).post();
        model.element(areaUnia, areas, indexUnia).post();
        model.element(valBorendy, valsBorendy, indexBorendy).post();
        model.element(valUnia, valsUnia, indexUnia).post();

        model.arithm(areaBorendy, "+", areaUnia, "<=", 200).post();

        IntVar total = model.intVar("sumIIC", 0, 2 * (int) Math.pow(10, precision));
        model.arithm(valBorendy, "+", valUnia, "=", total).post();

        List<Integer[]> optimalAllocations = new ArrayList<>();

        Solver solver = model.getSolver();
        solver.plugMonitor((IMonitorSolution) () -> {
            System.out.println("Area Borendy = " + areaBorendy.getValue());
            System.out.println("IIC Borendy = " + valBorendy.getValue());
            System.out.println("Area Unia = " + areaUnia.getValue());
            System.out.println("IIC Unia = " + valUnia.getValue());
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
            // Borendy
            List<Solution> solsB = borendySols.get(alloc[0]);
            for (int n = 0; n < solsB.size(); n++) {
                Solution sol = solsB.get(n);
                sol.restore();
                int[] set = sol.getSetVal(problemBorendy.reforestBorendy.getSetVar());
                for (int i : set) {
                    occurrencesInOptimalSolutionBorendy[i] += 1;
                }
                try {
                    problemBorendy.saveSolution("IIC_" + alloc[0] + "_" + alloc[1] + "_Borendy_" + n);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Unia
            Constraint areaU = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", alloc[1]);
            Constraint valU = modelUnia.arithm(IIC_Unia, "=", uniaFront.get(alloc[1]));
            areaU.post();
            valU.post();
            List<Solution> solsU = solverUnia.findAllSolutions();
            modelUnia.unpost(areaU);
            modelUnia.unpost(valU);
            solverUnia.reset();
            for (int n = 0; n < solsU.size(); n++) {
                Solution sol = solsU.get(n);
                sol.restore();
                int[] set = sol.getSetVal(problemUnia.reforestUnia.getSetVar());
                for (int i : set) {
                    occurrencesInOptimalSolutionUnia[i] += 1;
                }
                try {
                    problemUnia.saveSolution("IIC_" + alloc[0] + "_" + alloc[1] + "_Unia_" + n);
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
                    SetType.BIPARTITESET
            );
            double totalIIC = ConnectivityIndices.getIIC(g, problemBorendy.grid, Neighborhoods.PARTIAL_TWO_WIDE_FOUR_CONNECTED);
            int roundedTotalIIC = (int) Math.round(totalIIC * Math.pow(10, precision));
            System.out.println("TOTAL IIC = " + totalIIC + " - Rounded: " + roundedTotalIIC);
        }

        // Export occurrences count
        double[] completeOccurrences = new double[problemBorendy.grid.getNbCols() * problemBorendy.grid.getNbRows()];
        for (int j = 0; j < completeOccurrences.length; j++) {
//            completeOccurrences[j] = 1.0 * occurrencesInOptimalSolution[j] / nbOptimalSolutions;
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
