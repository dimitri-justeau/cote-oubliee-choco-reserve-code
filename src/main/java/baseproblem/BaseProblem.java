package baseproblem;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.PartialRegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.region.ComposedRegion;
import chocoreserve.solver.region.Region;
import chocoreserve.solver.variable.SpatialGraphVar;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.IntStream;

public class BaseProblem {

    public String name;
    public Data data;
    public String resultsPath;
    public PartialRegularSquareGrid grid;
    public SpatialGraphVar potentialForestGraphVar;
    public ReserveModel reserveModel;

    public Region nonForest, forest, reforestUnia, reforestBorendy;
    public ComposedRegion potentialForest;

    public IntVar minReforestAreaUnia, maxReforestAreaUnia, minReforestAreaBorendy,maxReforestAreaBorendy;

    public BaseProblem(String name) throws IOException {
        this.name = name;
        this.data = new Data();
        File f = new File(this.getClass().getResource("../results").getPath() + "/" + name);
        f.mkdir();
        this.resultsPath = this.getClass().getResource("../results/" + name).getPath();

        System.out.println("Height = " + data.height);
        System.out.println("Width = " + data.width);

        System.out.println(data.forest_binary_data.length);
        System.out.println(data.buffer_data.length);

        int[] outPixels = IntStream.range(0, data.forest_binary_data.length)
                .filter(i -> data.forest_binary_data[i] <= -1)
                .toArray();

        this.grid = new PartialRegularSquareGrid(data.height, data.width, outPixels);

        int[] nonForestPixels = IntStream.range(0, data.forest_binary_data.length)
                .filter(i -> data.forest_binary_data[i] == 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] forestPixels = IntStream.range(0, data.forest_binary_data.length)
                .filter(i -> data.forest_binary_data[i] == 1)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] restorableAreaPixels = IntStream.of(nonForestPixels)
                .map(i -> data.restorable_area_data[grid.getCompleteIndex(i)])
                .toArray();

        int[] nonForestBufferUniaPixels = IntStream.range(0, data.buffer_data.length)
                .filter(i -> data.buffer_data[i] == Data.UNIA_RASTER_VALUE && data.forest_binary_data[i] == 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] nonForestBufferBorendyPixels = IntStream.range(0, data.buffer_data.length)
                .filter(i -> data.buffer_data[i] == Data.BORENDY_RASTER_VALUE && data.forest_binary_data[i] == 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] nonForestNonBufferPixels = IntStream.range(0, data.forest_binary_data.length)
                .filter(i -> data.forest_binary_data[i] == 0 && data.buffer_data[i] <= 0)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        System.out.println("Current landscape state loaded");
        System.out.println("    forest sites = " + forestPixels.length + " ");
        System.out.println("    non forest sites = " + nonForestPixels.length + " ");
        System.out.println("    non forest sites in buffer Unia = " + nonForestBufferUniaPixels.length + " ");
        System.out.println("    non forest sites in buffer Borendy = " + nonForestBufferBorendyPixels.length + " ");
        System.out.println("    out sites = " + outPixels.length);

        forest = new Region(
                "forest",
                Neighborhoods.PARTIAL_FOUR_CONNECTED,
                SetType.SMALLBIPARTITESET,
                forestPixels,
                forestPixels
        );
        nonForest = new Region(
                "nonForest",
                Neighborhoods.PARTIAL_FOUR_CONNECTED,
                SetType.SMALLBIPARTITESET,
                nonForestNonBufferPixels,
                nonForestPixels
        );
        reforestUnia = new Region(
                "reforest",
                Neighborhoods.PARTIAL_FOUR_CONNECTED,
                SetType.SMALLBIPARTITESET,
                new int[] {},
                nonForestBufferUniaPixels
        );

        reforestBorendy = new Region(
                "reforest",
                Neighborhoods.PARTIAL_FOUR_CONNECTED,
                SetType.SMALLBIPARTITESET,
                new int[] {},
                nonForestBufferBorendyPixels
        );

        potentialForest = new ComposedRegion("potentialForest", false, forest, reforestBorendy, reforestUnia);

        System.out.println("Regions created");

        this.reserveModel = new ReserveModel(
                grid,
                new Region[] {nonForest, forest, reforestUnia, reforestBorendy},
                new ComposedRegion[] {potentialForest}
        );

        int nbSites = reserveModel.getSites().length;

        GraphModel model = reserveModel.getChocoModel();

        potentialForestGraphVar = potentialForest.getSetVar();

        // Connectivity constraints on reforest regions
        reserveModel.nbConnectedComponents(reforestBorendy, 1, 1).post();
        reserveModel.nbConnectedComponents(reforestUnia, 1, 1).post();

        // Minimum area to ensure every site to >= 0.7 forest proportion (in ha)
        int[] minArea = new int[nbSites];
        int[] maxRestorableArea = new int[nbSites];
        for (int i = 0; i < nbSites; i++) {
            int restorable = data.restorable_area_data[grid.getCompleteIndex(i)];
            minArea[i] = restorable <= 7 ? 0 : restorable - 7;
            maxRestorableArea[i] = restorable;
        }

        // Unia restorable area constraint
        minReforestAreaUnia = model.intVar(90, 110);
        maxReforestAreaUnia = model.intVar(90, 10000);
        model.sumElements(reforestUnia.getSetVar(), minArea, minReforestAreaUnia).post();
        model.sumElements(reforestUnia.getSetVar(), maxRestorableArea, maxReforestAreaUnia).post();

        // Borendy restorable area constraint
        minReforestAreaBorendy = model.intVar(90, 110);
        maxReforestAreaBorendy = model.intVar(90, 10000);
        model.sumElements(reforestBorendy.getSetVar(), minArea, minReforestAreaBorendy).post();
        model.sumElements(reforestBorendy.getSetVar(), maxRestorableArea, maxReforestAreaBorendy).post();

        model.arithm(minReforestAreaBorendy, "+", minReforestAreaUnia, "<=", 200).post();
        model.arithm(maxReforestAreaBorendy, "+", maxReforestAreaBorendy, ">=", 200).post();

        // Radius constraint for Unia area
        reserveModel.maxDiameterSpatial(reforestUnia, 6).post();

        // Radius constraint for Borendy area
        reserveModel.maxDiameterSpatial(reforestBorendy, 6).post();

    }

    public void saveSolution(String name) throws IOException {
        String path = Paths.get(resultsPath, name + ".csv").toString();
        int[] solution = IntStream.range(0, reserveModel.getSites().length)
                .map(j -> reserveModel.getSites()[j].getValue())
                .toArray();

        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter(path));
            StringBuilder sb = new StringBuilder();

            // Append strings from array
            for (int element : solution) {
                sb.append(element);
                sb.append(",");
            }

            br.write(sb.toString());
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SolutionExporter exp = new SolutionExporter(resultsPath, name + ".csv");
        exp.exportCompleteCsv();
        exp.generateRaster();
    }

    public static void main(String[] args) throws IOException {
        BaseProblem baseProblem = new BaseProblem("test");
        System.out.println("AREA = " + baseProblem.potentialForestGraphVar.getNbMaxNodes());
        String path = Paths.get(baseProblem.resultsPath, "LB_EDGES" + ".txt").toString();
        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter(path));

            // Append strings from array
            for (int node : Arrays.stream(baseProblem.potentialForest.getSetVar().getMandatoryNodes().toArray()).sorted().toArray()) {
                for (int n : baseProblem.potentialForest.getSetVar().getMandNeighOf(node)) {
                    br.write(node + " " + n + " " + 1);
                    br.write("\n");
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
