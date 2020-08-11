package solve;

import baseproblem.BaseProblemBorendy;
import baseproblem.BaseProblemUnia;
import chocoreserve.solver.constraints.spatial.NbEdges;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;


public class MaximizeNbEdges {

    public static void main(String[] args) throws IOException {

        long t = System.currentTimeMillis();

        // Borendy //
        BaseProblemBorendy problemBorendy = new BaseProblemBorendy("nbEdgesBorendy");

        Model modelBorendy = problemBorendy.reserveModel.getChocoModel();

        int nbEdgesForest = 0;
        ISet env = problemBorendy.forest.getSetVar().getPotentialNodes();
        for (int i : env) {
            nbEdgesForest += problemBorendy.forest.getSetVar().getMandNeighOf(i).size();
        }
        nbEdgesForest /= 2;

        NbEdges cNbEdge = new NbEdges(problemBorendy.reserveModel, problemBorendy.potentialForest);

        IntVar nbEdgesBorendy = cNbEdge.nbEdges;

        cNbEdge.post();

        Solver solverBorendy = problemBorendy.reserveModel.getChocoModel().getSolver();

        solverBorendy.setSearch(Search.minDomUBSearch(problemBorendy.reserveModel.getSites()));

        System.out.println("Nb Edges initial = " + nbEdgesForest);

        Map<Integer, Integer> borendyFront = new HashMap<>();
        borendyFront.put(90, 6349);
        borendyFront.put(91,6349);
        borendyFront.put(92,6349);
        borendyFront.put(93,6348);
        borendyFront.put(94,6348);
        borendyFront.put(95,6351);
        borendyFront.put(96,6351);
        borendyFront.put(97,6351);
        borendyFront.put(98,6349);
        borendyFront.put(99,6351);
        borendyFront.put(100,6352);
        borendyFront.put(101,6352);
        borendyFront.put(102,6353);
        borendyFront.put(103,6351);
        borendyFront.put(104,6351);
        borendyFront.put(105,6352);
        borendyFront.put(106,6354);
        borendyFront.put(107,6355);
        borendyFront.put(108,6354);
        borendyFront.put(109,6352);
        borendyFront.put(110,6352);

//        for (int a = 90; a <= 110; a++) {
//            Constraint area = modelBorendy.arithm(problemBorendy.minReforestAreaBorendy, "=", a);
//            modelBorendy.post(area);
//            Solution s = solverBorendy.findOptimalSolution(nbEdgesBorendy,true);
//            borendyFront.put(s.getIntVal(problemBorendy.minReforestAreaBorendy), s.getIntVal(nbEdgesBorendy));
//            System.out.println(Arrays.toString(new int[] {s.getIntVal(problemBorendy.minReforestAreaBorendy), s.getIntVal(nbEdgesBorendy)}));
//            modelBorendy.unpost(area);
//            solverBorendy.reset();
//        }

        System.out.println("minArea,nbEdges");
        int[] keysBorendy = borendyFront.keySet().stream().mapToInt(i -> i).sorted().toArray();
        for (int x : keysBorendy) {
            System.out.println(x + "," + borendyFront.get(x));
        }

        // Unia //
        BaseProblemUnia problemUnia = new BaseProblemUnia("nbEdgesUnia");

        Model modelUnia = problemUnia.reserveModel.getChocoModel();

        NbEdges cNbEdgeUnia = new NbEdges(problemUnia.reserveModel, problemUnia.potentialForest);

        IntVar nbEdgesUnia = cNbEdgeUnia.nbEdges;

        cNbEdgeUnia.post();

        Solver solverUnia = problemUnia.reserveModel.getChocoModel().getSolver();

        solverUnia.setSearch(Search.minDomUBSearch(problemUnia.reserveModel.getSites()));

        Map<Integer, Integer> uniaFront = new HashMap<>();
        uniaFront.put(90, 6347);
        uniaFront.put(91,6346);
        uniaFront.put(92,6347);
        uniaFront.put(93,6347);
        uniaFront.put(94,6347);
        uniaFront.put(95,6350);
        uniaFront.put(96, 6345);
        uniaFront.put(97,6346);
        uniaFront.put(98,6346);
        uniaFront.put(99,6346);
        uniaFront.put(100,6346);
        uniaFront.put(101,6348);
        uniaFront.put(102,6349);
        uniaFront.put(103,6348);
        uniaFront.put(104,6349);
        uniaFront.put(105,6348);
        uniaFront.put(106,6349);
        uniaFront.put(107,6349);
        uniaFront.put(108,6348);
        uniaFront.put(109,6352);
        uniaFront.put(110,6342);

//        for (int a = 90; a <= 110; a++) {
//            Constraint area = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", a);
//            modelUnia.post(area);
//            Solution s = solverUnia.findOptimalSolution(nbEdgesUnia,true);
//            uniaFront.put(s.getIntVal(problemUnia.minReforestAreaUnia), s.getIntVal(nbEdgesUnia));
//            System.out.println(Arrays.toString(new int[] {s.getIntVal(problemUnia.minReforestAreaUnia), s.getIntVal(nbEdgesUnia)}));
//            modelUnia.unpost(area);
//            solverUnia.reset();
//        }

        System.out.println("minArea,nbEdges");
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

        IntVar valBorendy = model.intVar("nbEdgesBorendy",0, 10000);
        IntVar valUnia = model.intVar("nbEdgesUnia",0, 10000);

        model.element(areaBorendy, areas, indexBorendy).post();
        model.element(areaUnia, areas, indexUnia).post();
        model.element(valBorendy, valsBorendy, indexBorendy).post();
        model.element(valUnia, valsUnia, indexUnia).post();

        model.arithm(areaBorendy, "+", areaUnia, "<=", 200).post();

        IntVar total = model.intVar("totalEdges", 0, 10000);
        model.arithm(model.intOffsetView(valBorendy, -nbEdgesForest), "+", valUnia, "=", total).post();

        List<Integer[]> optimalAllocations = new ArrayList<>();

        Solver solver = model.getSolver();
        solver.plugMonitor((IMonitorSolution) () -> {
            System.out.println("Area Borendy = " + areaBorendy.getValue());
            System.out.println("Nb Edges Borendy = " + valBorendy.getValue());
            System.out.println("Area Unia = " + areaUnia.getValue());
            System.out.println("Nb Edges Unia = " + valUnia.getValue());
            System.out.println("Total nb edges = " + total.getValue());
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

        final int[] i1 = {1};
        final int[] i2 = {1};

        solverBorendy.plugMonitor((IMonitorSolution) () -> {
            for (int s : problemBorendy.reforestBorendy.getSetVar().getValue()) {
                occurrencesInOptimalSolutionBorendy[s] += 1;
            }
            try {
                problemBorendy.saveSolution("BorendyOptimalnbEdges_" + i1[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            i1[0] += 1;
        });

        solverUnia.plugMonitor((IMonitorSolution) () -> {
            for (int s : problemUnia.reforestUnia.getSetVar().getValue()) {
                occurrencesInOptimalSolutionUnia[s] += 1;
            }
            try {
                problemUnia.saveSolution("UniaOptimalnbEdges_" + i2[0]);
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
            for (int j = 0; j < problemBorendy.grid.getNbCells(); j++) {
                occurrencesInOptimalSolutionBorendy[j] = 0;
                occurrencesInOptimalSolutionUnia[j] = 0;
            }
            // Borendy
            Constraint areaB = modelBorendy.arithm(problemBorendy.minReforestAreaBorendy, "=", alloc[0]);
            Constraint valB = modelBorendy.arithm(nbEdgesBorendy, "=", borendyFront.get(alloc[0]));
            areaB.post();
            valB.post();
            List<Solution> solsB = solverBorendy.findAllSolutions();
            modelBorendy.unpost(areaB);
            modelBorendy.unpost(valB);
            solverBorendy.reset();
            // Unia
            Constraint areaU = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", alloc[1]);
            Constraint valU = modelUnia.arithm(nbEdgesUnia, "=", uniaFront.get(alloc[1]));
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
