package solve;

import baseproblem.BaseProblemBorendy;
import baseproblem.BaseProblemUnia;
import chocoreserve.solver.constraints.choco.graph.spatial.PropIICSpatialGraph;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;


public class MaximizeIIC {

    public static void main(String[] args) throws IOException {

        long t = System.currentTimeMillis();

        // Borendy //
        BaseProblemBorendy problemBorendy = new BaseProblemBorendy("IICBorendy");

        Model modelBorendy = problemBorendy.reserveModel.getChocoModel();

        IntVar iicBorendy = modelBorendy.intVar(0, 10000);
        PropIICSpatialGraph propIICBorendy = new PropIICSpatialGraph(problemBorendy.potentialForestGraphVar, iicBorendy, 4);
        modelBorendy.post(new Constraint("IIC", propIICBorendy));

        propIICBorendy.computeAllPairsShortestPathsLB(problemBorendy.grid);
        int forestIIC = 0;
        try {
            forestIIC = propIICBorendy.computeIIC_LB();
        } catch (ContradictionException e) {
            e.printStackTrace();
        }

        System.out.println("Initial IIC value = " + forestIIC);

        Solver solverBorendy = problemBorendy.reserveModel.getChocoModel().getSolver();

        solverBorendy.setSearch(Search.minDomUBSearch(problemBorendy.reserveModel.getSites()));

        Map<Integer, Integer> borendyFront = new HashMap<>();

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelBorendy.arithm(problemBorendy.minReforestAreaBorendy, "=", a);
            modelBorendy.post(area);
            Solution s = solverBorendy.findOptimalSolution(iicBorendy,true);
            borendyFront.put(s.getIntVal(problemBorendy.minReforestAreaBorendy), s.getIntVal(iicBorendy));
            System.out.println(Arrays.toString(new int[] {s.getIntVal(problemBorendy.minReforestAreaBorendy), s.getIntVal(iicBorendy)}));
            modelBorendy.unpost(area);
            solverBorendy.reset();
        }

        System.out.println("minArea,iic");
        int[] keys = borendyFront.keySet().stream().mapToInt(i -> i).sorted().toArray();
        for (int x : keys) {
            System.out.println(x + "," + borendyFront.get(x));
        }

        // Unia //
        BaseProblemUnia problemUnia = new BaseProblemUnia("IICUnia");

        Model modelUnia = problemUnia.reserveModel.getChocoModel();

        IntVar iicUnia = modelUnia.intVar(0, 10000);
        PropIICSpatialGraph propIICUnia = new PropIICSpatialGraph(problemUnia.potentialForestGraphVar, iicUnia, 4);
        modelUnia.post(new Constraint("IIC", propIICUnia));

        Solver solverUnia = problemUnia.reserveModel.getChocoModel().getSolver();

        solverUnia.setSearch(Search.minDomUBSearch(problemUnia.reserveModel.getSites()));

        Map<Integer, Integer> uniaFront = new HashMap<>();

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", a);
            modelUnia.post(area);
            Solution s = solverUnia.findOptimalSolution(iicUnia,true);
            uniaFront.put(s.getIntVal(problemUnia.minReforestAreaUnia), s.getIntVal(iicUnia));
            System.out.println(Arrays.toString(new int[] {s.getIntVal(problemUnia.minReforestAreaUnia), s.getIntVal(iicUnia)}));
            modelUnia.unpost(area);
            solverUnia.reset();
        }

        System.out.println("minArea,iic");
        keys = uniaFront.keySet().stream().mapToInt(i -> i).sorted().toArray();
        for (int x : keys) {
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

        IntVar valBorendy = model.intVar("IICBorendy",0, 10000);
        IntVar valUnia = model.intVar("nbCCUnia",0, 10000);

        model.element(areaBorendy, areas, indexBorendy).post();
        model.element(areaUnia, areas, indexUnia).post();
        model.element(valBorendy, valsBorendy, indexBorendy).post();
        model.element(valUnia, valsUnia, indexUnia).post();

        model.arithm(areaBorendy, "+", areaUnia, "<=", 200).post();

        IntVar total = model.intVar("totalIIC", 0, 20000);
        model.arithm(model.intOffsetView(valBorendy, -forestIIC), "+", valUnia, "=", total).post();

        List<Integer[]> optimalAllocations = new ArrayList<>();

        Solver solver = model.getSolver();
        solver.plugMonitor((IMonitorSolution) () -> {
            System.out.println("Area Borendy = " + areaBorendy.getValue());
            System.out.println("IIC Borendy = " + valBorendy.getValue());
            System.out.println("Area Unia = " + areaUnia.getValue());
            System.out.println("IIC Unia = " + valUnia.getValue());
            System.out.println("Sum = " + total.getValue());
        });
        solver.showStatistics();
        List<Solution> allocs = solver.findAllOptimalSolutions(total, true);
        for (Solution s : allocs) {
            optimalAllocations.add(new Integer[]{s.getIntVal(areaBorendy), s.getIntVal(areaUnia)});
        }

        System.out.println("Total time optimize = " + (System.currentTimeMillis() - t) + " ms");

        for (Integer[] a : optimalAllocations) {
            System.out.println(Arrays.toString(a));
        }

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

        final int[] i1 = {1};
        final int[] i2 = {1};

        solverBorendy.plugMonitor((IMonitorSolution) () -> {
            for (int s : problemBorendy.reforestBorendy.getSetVar().getValue()) {
                occurrencesInOptimalSolutionBorendy[s] += 1;
            }
            System.out.println("Borendy : " + problemBorendy.reforestBorendy.getSetVar().getValue());
            try {
                problemBorendy.saveSolution("BorendyOptimalIIC_" + i1[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            i1[0] += 1;
        });

        solverUnia.plugMonitor((IMonitorSolution) () -> {
            for (int s : problemUnia.reforestUnia.getSetVar().getValue()) {
                occurrencesInOptimalSolutionUnia[s] += 1;
            }
            System.out.println("Unia : " + problemUnia.reforestUnia.getSetVar().getValue());
            try {
                problemUnia.saveSolution("UniaOptimalIIC_" + i2[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            i2[0] += 1;
        });

        int[] occurrencesInOptimalSolution = new int[problemUnia.grid.getNbCells()];
        for (int j = 0; j < problemUnia.grid.getNbCells(); j++) {
            occurrencesInOptimalSolution[j] = 0;
        }

        int nbOptimalSolutions = 0;

        for (Integer[] alloc : optimalAllocations) {
            System.out.println("\nAlloc : " + Arrays.toString(alloc));
            for (int j = 0; j < problemBorendy.grid.getNbCells(); j++) {
                occurrencesInOptimalSolutionBorendy[j] = 0;
                occurrencesInOptimalSolutionUnia[j] = 0;
            }
            // Borendy
            Constraint areaB = modelBorendy.arithm(problemBorendy.minReforestAreaBorendy, "=", alloc[0]);
            Constraint valB = modelBorendy.arithm(iicBorendy, "=", borendyFront.get(alloc[0]));
            areaB.post();
            valB.post();
            List<Solution> solsB = solverBorendy.findAllSolutions();
            modelBorendy.unpost(areaB);
            modelBorendy.unpost(valB);
            solverBorendy.reset();
            // Unia
            Constraint areaU = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", alloc[1]);
            Constraint valU = modelUnia.arithm(iicUnia, "=", uniaFront.get(alloc[1]));
            areaU.post();
            valU.post();
            List<Solution> solsU = solverUnia.findAllSolutions();
            modelUnia.unpost(areaU);
            modelUnia.unpost(valU);
            solverUnia.reset();
            // Count occurrences
            nbOptimalSolutions += solsB.size() * solsU.size();
            for (int j = 0; j < problemUnia.grid.getNbCells(); j++) {
                occurrencesInOptimalSolution[j] += occurrencesInOptimalSolutionBorendy[j] * solsU.size();
                occurrencesInOptimalSolution[j] += occurrencesInOptimalSolutionUnia[j] * solsB.size();
            }
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
