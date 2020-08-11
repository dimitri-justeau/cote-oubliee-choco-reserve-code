package solve;

import baseproblem.BaseProblem;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


public class MaximizeNbEdgesBoth {

    public static void main(String[] args) throws IOException {

        long t = System.currentTimeMillis();

        // Borendy //
        BaseProblem problem = new BaseProblem("nbEdges");

        Model model = problem.reserveModel.getChocoModel();

        int nbEdgesForest = 0;
        ISet env = problem.forest.getSetVar().getPotentialNodes();
        for (int i : env) {
            nbEdgesForest += problem.forest.getSetVar().getMandNeighOf(i).size();
        }
        nbEdgesForest /= 2;

        NbEdges cNbEdges = new NbEdges(problem.reserveModel, problem.potentialForest);

        IntVar nbEdges = cNbEdges.nbEdges;

        cNbEdges.post();

        Solver solver = problem.reserveModel.getChocoModel().getSolver();

        solver.setSearch(Search.domOverWDegSearch(problem.reserveModel.getSites()));

        solver.showStatistics();

        System.out.println("Nb Edges initial = " + nbEdgesForest);

        AtomicInteger i = new AtomicInteger(1);

        solver.plugMonitor((IMonitorSolution) () -> {
//            System.out.println("Nb potential forest = " + problem.potentialForest.getNbSites().getValue());
            System.out.println("Nb forest = " + problem.forest.getNbSites().getValue());
            System.out.println("Nb Edges = " + nbEdges.getValue());
            System.out.println("Min area reforest Unia = " + problem.minReforestAreaUnia.getValue());
            System.out.println("Min area reforest Borendy = " + problem.minReforestAreaBorendy.getValue());
            try {
                problem.saveSolution("nbEdgesOptimal" + i);
            } catch (IOException e) {
                e.printStackTrace();
            }
            i.addAndGet(1);

        });

        solver.findOptimalSolution(nbEdges, true);
    }
}
