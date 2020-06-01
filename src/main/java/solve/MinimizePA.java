package solve;

import baseproblem.BaseProblemBorendy;
import baseproblem.BaseProblemUnia;
import chocoreserve.solver.constraints.choco.graph.PropPerimeterSquareGridFourConnected;
import chocoreserve.solver.constraints.spatial.PerimeterSquareGridFourConnected;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;


public class MinimizePA {

    public static void main(String[] args) throws IOException {

        long t = System.currentTimeMillis();

        // Borendy //
        BaseProblemBorendy problemBorendy = new BaseProblemBorendy("PABorendy");

        Model modelBorendy = problemBorendy.reserveModel.getChocoModel();

        int forestArea = problemBorendy.forest.getSetVar().getUB().size();
        PropPerimeterSquareGridFourConnected per = new PropPerimeterSquareGridFourConnected(
                problemBorendy.forest.getSetVar(),
                modelBorendy.intVar(0)
        );
        int forestPerimeter = per.getPerimeter(problemBorendy.forest.getSetVar().getLB());

        IntVar areaBorendy = problemBorendy.potentialForest.getNbSites();

        PerimeterSquareGridFourConnected cPerB = new PerimeterSquareGridFourConnected(
                problemBorendy.reserveModel, problemBorendy.potentialForest
        );
        cPerB.post();

        IntVar perimeterBorendy = cPerB.perimeter;

        IntVar PABorendy = modelBorendy.intVar("ratioPerimeterArea", 0, (int) (1000 * forestPerimeter / forestArea));
        modelBorendy.div(modelBorendy.intScaleView(perimeterBorendy, 1000), areaBorendy, PABorendy).post();

        Solver solverBorendy = problemBorendy.reserveModel.getChocoModel().getSolver();

        solverBorendy.setSearch(Search.minDomUBSearch(problemBorendy.reserveModel.getSites()));

        Map<Integer, Integer[]> borendyFront = new HashMap<>();

        System.out.println("Perimeter initial = " + forestPerimeter);
        System.out.println("Area initial = " + forestArea);
        System.out.println("Ratio initial = " + (1.0 * forestPerimeter / forestArea));

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelBorendy.arithm(problemBorendy.minReforestAreaBorendy, "=", a);
            modelBorendy.post(area);
            Solution s = solverBorendy.findOptimalSolution(PABorendy,false);
            borendyFront.put(s.getIntVal(problemBorendy.minReforestAreaBorendy), new Integer[]{s.getIntVal(perimeterBorendy), s.getIntVal(areaBorendy)});
            System.out.println(Arrays.toString(new int[] {s.getIntVal(problemBorendy.minReforestAreaBorendy), s.getIntVal(PABorendy)}));
            modelBorendy.unpost(area);
            solverBorendy.reset();
        }

        System.out.println("minArea,perimeter,area,ratio");
        int[] keysBorendy = borendyFront.keySet().stream().mapToInt(i -> i).sorted().toArray();
        for (int x : keysBorendy) {
            int p = borendyFront.get(x)[0];
            int a = borendyFront.get(x)[1];
            double pa = 1.0 * p / a;
            System.out.println(x + "," + p + "," + a + "," + pa);
        }

        // Unia //
        BaseProblemUnia problemUnia = new BaseProblemUnia("PAUnia");

        Model modelUnia = problemUnia.reserveModel.getChocoModel();

        IntVar areaUnia = problemUnia.potentialForest.getNbSites();

        PerimeterSquareGridFourConnected cPerU = new PerimeterSquareGridFourConnected(
                problemUnia.reserveModel, problemUnia.potentialForest
        );
        cPerU.post();

        IntVar perimeterUnia = cPerU.perimeter;

        IntVar PAUnia = modelUnia.intVar("ratioPerimeterArea", 0, (int) (1000 * forestPerimeter / forestArea));
        modelBorendy.div(modelUnia.intScaleView(perimeterUnia, 1000), areaUnia, PAUnia).post();

        Solver solverUnia = problemUnia.reserveModel.getChocoModel().getSolver();

        solverUnia.setSearch(Search.minDomUBSearch(problemUnia.reserveModel.getSites()));

        Map<Integer, Integer[]> uniaFront = new HashMap<>();

        for (int a = 90; a <= 110; a++) {
            Constraint area = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", a);
            modelUnia.post(area);
            Solution s = solverUnia.findOptimalSolution(PAUnia,false);
            uniaFront.put(s.getIntVal(problemUnia.minReforestAreaUnia), new Integer[]{s.getIntVal(perimeterUnia), s.getIntVal(areaUnia)});
            System.out.println(Arrays.toString(new int[] {s.getIntVal(problemUnia.minReforestAreaUnia), s.getIntVal(PAUnia)}));
            modelUnia.unpost(area);
            solverUnia.reset();
        }

        System.out.println("minArea,perimeter,area,ratio");
        int[] keysUnia = uniaFront.keySet().stream().mapToInt(i -> i).sorted().toArray();
        for (int x : keysUnia) {
            int p = uniaFront.get(x)[0];
            int a = uniaFront.get(x)[1];
            double pa = 1.0 * p / a;
            System.out.println(x + "," + p + "," + a + "," + pa);
        }

        // Combine //
        Model model = new Model();

        IntVar indexBorendy = model.intVar("indexBorendy", 0, 20);
        IntVar indexUnia = model.intVar("indexBorendy", 0, 20);

        IntVar minAreaBorendy = model.intVar("areaBorendy", 90, 110);
        IntVar minAreaUnia = model.intVar("areaUnia", 90, 110);

        int[] areas = IntStream.range(90, 111).toArray();
        int[] persBorendy = IntStream.range(90, 111).map(i -> borendyFront.get(i)[0]).toArray();
        int[] persUnia = IntStream.range(90, 111).map(i -> uniaFront.get(i)[0]).toArray();
        int[] areasBorendy = IntStream.range(90, 111).map(i -> borendyFront.get(i)[1]).toArray();
        int[] areasUnia = IntStream.range(90, 111).map(i -> uniaFront.get(i)[1]).toArray();

        IntVar aBorendy = model.intVar("arBorendy",0, 40000);
        IntVar aUnia = model.intVar("arUnia",0, 40000);

        IntVar pBorendy = model.intVar("pBorendy",0, 4000000);
        IntVar pUnia = model.intVar("pUnia",0, 4000000);

        model.element(minAreaBorendy, areas, indexBorendy).post();
        model.element(minAreaUnia, areas, indexUnia).post();
        model.element(aBorendy, areasBorendy, indexBorendy).post();
        model.element(aUnia, areasUnia, indexUnia).post();
        model.element(pBorendy, persBorendy, indexBorendy).post();
        model.element(pUnia, persUnia, indexUnia).post();

        model.arithm(minAreaBorendy, "+", minAreaUnia, "<=", 200).post();

        IntVar totalArea = model.intVar("totalArea", 0, problemBorendy.grid.getNbCells());
        model.arithm(totalArea, "=", aBorendy, "+", model.intOffsetView(aUnia, -forestArea)).post();
        IntVar totalPerimeter = model.intVar("totalPerimeter", 0, problemBorendy.grid.getNbCells() * 4);
        model.arithm(totalPerimeter, "=", pBorendy, "+", model.intOffsetView(pUnia, -forestPerimeter)).post();
        IntVar ratioPerimeterArea = model.intVar("ratioPerimeterArea", 0, (int) (1000 * forestPerimeter / forestArea));
        model.div(model.intScaleView(totalPerimeter, 1000), totalArea, ratioPerimeterArea).post();



        List<Integer[]> optimalAllocations = new ArrayList<>();

        Solver solver = model.getSolver();
        solver.plugMonitor((IMonitorSolution) () -> {
            System.out.println("Area Borendy = " + areaBorendy.getValue());
            System.out.println("Area Unia = " + areaUnia.getValue());
            System.out.println("Total PA = " + ratioPerimeterArea.getValue());
        });
        solver.showStatistics();
        List<Solution> allocs = solver.findAllOptimalSolutions(ratioPerimeterArea, false);
        for (Solution s : allocs) {
            optimalAllocations.add(new Integer[]{s.getIntVal(minAreaBorendy), s.getIntVal(minAreaUnia)});
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
                problemBorendy.saveSolution("BorendyOptimalPA_" + i1[0]);
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
                problemUnia.saveSolution("UniaOptimalPA_" + i2[0]);
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
            Constraint aB = modelBorendy.arithm(areaBorendy, "=", borendyFront.get(alloc[0])[1]);
            Constraint pB = modelBorendy.arithm(perimeterBorendy, "=", borendyFront.get(alloc[0])[0]);
            areaB.post();
            aB.post();
            pB.post();
            List<Solution> solsB = solverBorendy.findAllSolutions();
            modelBorendy.unpost(areaB);
            modelBorendy.unpost(aB);
            modelBorendy.unpost(pB);
            solverBorendy.reset();
            // Unia
            Constraint areaU = modelUnia.arithm(problemUnia.minReforestAreaUnia, "=", alloc[0]);
            Constraint aU = modelUnia.arithm(areaUnia, "=", uniaFront.get(alloc[0])[1]);
            Constraint pU = modelUnia.arithm(perimeterUnia, "=", uniaFront.get(alloc[0])[0]);
            areaU.post();
            aU.post();
            pU.post();
            List<Solution> solsU = solverUnia.findAllSolutions();
            modelUnia.unpost(areaU);
            modelUnia.unpost(aU);
            modelUnia.unpost(pU);
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
