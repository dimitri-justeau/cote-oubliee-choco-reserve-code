package solve;

import baseproblem.BaseProblem;
import chocoreserve.solver.constraints.spatial.NbEdges;
import chocoreserve.util.fragmentation.FragmentationIndices;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


public class MaximizeAIBoth {

    public static void main(String[] args) throws IOException {

        long t = System.currentTimeMillis();

        // Borendy //
        BaseProblem problem = new BaseProblem("AI");

        Model model = problem.reserveModel.getChocoModel();

        IntVar AI = problem.reserveModel.aggregationIndex(problem.potentialForest, 4);

        double AI_initial = FragmentationIndices.aggregationIndex(problem.potentialForestGraphVar.getGLB());
        int nbEdges_initial = FragmentationIndices.getNbEdges(problem.potentialForestGraphVar.getGLB());
        int nbNodes_initial = problem.potentialForestGraphVar.getLB().size();

        Solver solver = problem.reserveModel.getChocoModel().getSolver();

        solver.setSearch(Search.minDomUBSearch(problem.reserveModel.getSites()));

        solver.showStatistics();

        System.out.println("AI initial = " + AI_initial);

        AtomicInteger i = new AtomicInteger(1);

        solver.plugMonitor((IMonitorSolution) () -> {
//            System.out.println("Nb potential forest = " + problem.potentialForest.getNbSites().getValue());
            System.out.println("Nb forest = " + problem.forest.getNbSites().getValue());
            System.out.println("AI = " + AI.getValue());
            System.out.println("Min area reforest Unia = " + problem.minReforestAreaUnia.getValue());
            System.out.println("Min area reforest Borendy = " + problem.minReforestAreaBorendy.getValue());
            try {
                problem.saveSolution("nbEdgesOptimal" + i);
            } catch (IOException e) {
                e.printStackTrace();
            }
            i.addAndGet(1);

        });

        solver.findOptimalSolution(AI, true);
    }
}
